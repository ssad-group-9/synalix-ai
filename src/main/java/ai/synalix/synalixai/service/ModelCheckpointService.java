package ai.synalix.synalixai.service;

import ai.synalix.synalixai.dto.model.ModelCheckpointResponse;
import ai.synalix.synalixai.dto.storage.PresignedUrlResponse;
import ai.synalix.synalixai.entity.ModelCheckpoint;
import ai.synalix.synalixai.enums.ApiErrorCode;
import ai.synalix.synalixai.enums.AuditOperationType;
import ai.synalix.synalixai.exception.ApiException;
import ai.synalix.synalixai.repository.ModelCheckpointRepository;
import ai.synalix.synalixai.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Service for model checkpoint management operations
 * Handles CRUD operations and file download for checkpoints
 */
@Service
@Slf4j
public class ModelCheckpointService {

    private final ModelCheckpointRepository checkpointRepository;
    private final UserRepository userRepository;
    private final MinioService minioService;
    private final BaseModelService baseModelService;
    private final DatasetService datasetService;
    private final AuditService auditService;

    @Autowired
    public ModelCheckpointService(ModelCheckpointRepository checkpointRepository,
                                   UserRepository userRepository,
                                   MinioService minioService,
                                   BaseModelService baseModelService,
                                   DatasetService datasetService,
                                   AuditService auditService) {
        this.checkpointRepository = checkpointRepository;
        this.userRepository = userRepository;
        this.minioService = minioService;
        this.baseModelService = baseModelService;
        this.datasetService = datasetService;
        this.auditService = auditService;
    }

    /**
     * Get all completed checkpoints for a user
     *
     * @param userId the user ID
     * @return list of completed checkpoints owned by the user
     */
    public List<ModelCheckpointResponse> getCheckpointsByOwner(UUID userId) {
        return checkpointRepository.findByOwnerIdAndUploadComplete(userId, true)
                .stream()
                .map(this::convertToResponse)
                .toList();
    }

    /**
     * Get a checkpoint by ID for a specific user
     *
     * @param id     the checkpoint ID
     * @param userId the user ID
     * @return the checkpoint response
     */
    public ModelCheckpointResponse getCheckpointById(UUID id, UUID userId) {
        var checkpoint = getCheckpointEntityByIdAndOwner(id, userId);
        return convertToResponse(checkpoint);
    }

    /**
     * Create a new checkpoint record (called by training service)
     * This is used when a training job completes and needs to upload the model
     *
     * @param name         the checkpoint name
     * @param description  the checkpoint description
     * @param baseModelId  the base model ID used for training
     * @param datasetId    the dataset ID used for training
     * @param trainingJobId the training job ID
     * @param userId       the owner user ID
     * @return the created checkpoint with upload URL
     */
    @Transactional
    public ModelCheckpointResponse createCheckpoint(String name,
                                                     String description,
                                                     UUID baseModelId,
                                                     UUID datasetId,
                                                     UUID trainingJobId,
                                                     UUID userId) {
        // Check if name already exists for this user
        if (checkpointRepository.existsByNameAndOwnerId(name, userId)) {
            throw new ApiException(ApiErrorCode.MODEL_NAME_EXISTS,
                    Map.of("name", name));
        }

        var owner = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(ApiErrorCode.USER_NOT_FOUND,
                        Map.of("userId", userId.toString())));

        var baseModel = baseModelService.getModelEntityById(baseModelId);
        var dataset = datasetService.getDatasetEntityById(datasetId);

        var checkpoint = new ModelCheckpoint();
        checkpoint.setName(name);
        checkpoint.setDescription(description);
        checkpoint.setBaseModel(baseModel);
        checkpoint.setDataset(dataset);
        checkpoint.setTrainingJobId(trainingJobId);
        checkpoint.setOwner(owner);
        checkpoint.setUploadComplete(false);

        var savedCheckpoint = checkpointRepository.save(checkpoint);

        // Generate storage key
        var storageKey = minioService.generateCheckpointStorageKey(savedCheckpoint.getId());
        savedCheckpoint.setStorageKey(storageKey);
        savedCheckpoint = checkpointRepository.save(savedCheckpoint);

        log.info("Checkpoint created: {} for training job {} by user {}",
                savedCheckpoint.getName(), trainingJobId, userId);

        return convertToResponse(savedCheckpoint);
    }

    /**
     * Generate a presigned URL for uploading checkpoint file (called by training service)
     *
     * @param checkpointId the checkpoint ID
     * @return presigned URL response
     */
    public PresignedUrlResponse generateUploadUrl(UUID checkpointId) {
        var checkpoint = checkpointRepository.findById(checkpointId)
                .orElseThrow(() -> new ApiException(ApiErrorCode.CHECKPOINT_NOT_FOUND,
                        Map.of("checkpointId", checkpointId.toString())));

        if (checkpoint.isUploadComplete()) {
            throw new ApiException(ApiErrorCode.VALIDATION_FAILED,
                    "Checkpoint upload already completed");
        }

        var presignedUrl = minioService.generateCheckpointUploadUrl(checkpointId);

        log.info("Upload URL generated for checkpoint: {}", checkpointId);

        return presignedUrl;
    }

    /**
     * Confirm that checkpoint upload is complete (called by training service)
     *
     * @param checkpointId the checkpoint ID
     * @param fileSize     the uploaded file size in bytes
     * @return the updated checkpoint response
     */
    @Transactional
    public ModelCheckpointResponse confirmUpload(UUID checkpointId, Long fileSize) {
        var checkpoint = checkpointRepository.findById(checkpointId)
                .orElseThrow(() -> new ApiException(ApiErrorCode.CHECKPOINT_NOT_FOUND,
                        Map.of("checkpointId", checkpointId.toString())));

        if (checkpoint.isUploadComplete()) {
            throw new ApiException(ApiErrorCode.VALIDATION_FAILED,
                    "Checkpoint upload already completed");
        }

        checkpoint.setUploadComplete(true);
        checkpoint.setFileSize(fileSize);
        var savedCheckpoint = checkpointRepository.save(checkpoint);

        log.info("Checkpoint upload confirmed: {}", checkpointId);

        auditService.logAsync(
                AuditOperationType.CHECKPOINT_UPLOAD_COMPLETED,
                checkpoint.getOwner().getId(),
                checkpointId.toString(),
                Map.of("fileSize", fileSize)
        );

        return convertToResponse(savedCheckpoint);
    }

    /**
     * Generate a presigned URL for downloading checkpoint file
     *
     * @param checkpointId the checkpoint ID
     * @param userId       the user ID
     * @return presigned URL response
     */
    public PresignedUrlResponse generateDownloadUrl(UUID checkpointId, UUID userId) {
        var checkpoint = getCheckpointEntityByIdAndOwner(checkpointId, userId);

        if (!checkpoint.isUploadComplete()) {
            throw new ApiException(ApiErrorCode.CHECKPOINT_NOT_FOUND,
                    "Checkpoint upload not yet completed");
        }

        var presignedUrl = minioService.generateCheckpointDownloadUrl(checkpoint.getStorageKey());

        log.info("Download URL generated for checkpoint: {} by user {}", checkpointId, userId);

        auditService.logAsync(
                AuditOperationType.CHECKPOINT_DOWNLOAD_URL_GENERATED,
                userId,
                checkpointId.toString(),
                Map.of("expiresAt", presignedUrl.getExpiresAt().toString())
        );

        return presignedUrl;
    }

    /**
     * Delete a checkpoint
     *
     * @param checkpointId the checkpoint ID
     * @param userId       the user ID
     */
    @Transactional
    public void deleteCheckpoint(UUID checkpointId, UUID userId) {
        var checkpoint = getCheckpointEntityByIdAndOwner(checkpointId, userId);

        checkpointRepository.delete(checkpoint);

        log.info("Checkpoint deleted: {} by user {}", checkpointId, userId);

        auditService.logAsync(
                AuditOperationType.MODEL_DELETE,
                userId,
                checkpointId.toString(),
                Map.of("name", checkpoint.getName())
        );
    }

    /**
     * Get checkpoint entity by ID and owner (for internal use)
     *
     * @param checkpointId the checkpoint ID
     * @param userId       the user ID
     * @return the checkpoint entity
     */
    private ModelCheckpoint getCheckpointEntityByIdAndOwner(UUID checkpointId, UUID userId) {
        return checkpointRepository.findByIdAndOwnerId(checkpointId, userId)
                .orElseThrow(() -> new ApiException(ApiErrorCode.CHECKPOINT_NOT_FOUND,
                        Map.of("checkpointId", checkpointId.toString())));
    }

    /**
     * Convert entity to response DTO
     *
     * @param checkpoint the checkpoint entity
     * @return the response DTO
     */
    private ModelCheckpointResponse convertToResponse(ModelCheckpoint checkpoint) {
        return new ModelCheckpointResponse(
                checkpoint.getId(),
                checkpoint.getName(),
                checkpoint.getDescription(),
                checkpoint.getFileSize(),
                checkpoint.getBaseModel().getId(),
                checkpoint.getBaseModel().getName(),
                checkpoint.getDataset().getId(),
                checkpoint.getDataset().getName(),
                checkpoint.isUploadComplete(),
                checkpoint.getCreatedAt()
        );
    }
}