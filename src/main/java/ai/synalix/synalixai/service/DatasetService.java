package ai.synalix.synalixai.service;

import ai.synalix.synalixai.dto.dataset.CreateDatasetRequest;
import ai.synalix.synalixai.dto.dataset.DatasetPreprocessRequest;
import ai.synalix.synalixai.dto.dataset.DatasetResponse;
import ai.synalix.synalixai.dto.storage.PresignedUrlResponse;
import ai.synalix.synalixai.entity.Dataset;
import ai.synalix.synalixai.enums.ApiErrorCode;
import ai.synalix.synalixai.enums.AuditOperationType;
import ai.synalix.synalixai.enums.DatasetStatus;
import ai.synalix.synalixai.exception.ApiException;
import ai.synalix.synalixai.repository.DatasetRepository;
import ai.synalix.synalixai.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Service for dataset management operations
 * Handles CRUD operations and file upload/download for datasets
 */
@Service
@Slf4j
public class DatasetService {

    private final DatasetRepository datasetRepository;
    private final UserRepository userRepository;
    private final MinioService minioService;
    private final AuditService auditService;

    @Autowired
    public DatasetService(DatasetRepository datasetRepository,
                          UserRepository userRepository,
                          MinioService minioService,
                          AuditService auditService) {
        this.datasetRepository = datasetRepository;
        this.userRepository = userRepository;
        this.minioService = minioService;
        this.auditService = auditService;
    }

    /**
     * Get all datasets for a user
     *
     * @param userId the user ID
     * @return list of datasets owned by the user
     */
    public List<DatasetResponse> getDatasetsByOwner(UUID userId) {
        return datasetRepository.findByOwnerId(userId)
                .stream()
                .filter(dataset -> dataset.getStatus() != DatasetStatus.DELETED)
                .map(this::convertToResponse)
                .toList();
    }

    /**
     * Get a dataset by ID for a specific user
     *
     * @param id     the dataset ID
     * @param userId the user ID
     * @return the dataset response
     */
    public DatasetResponse getDatasetById(UUID id, UUID userId) {
        var dataset = getDatasetEntityByIdAndOwner(id, userId);
        return convertToResponse(dataset);
    }

    /**
     * Create a new dataset and generate upload URL
     *
     * @param request the create dataset request
     * @param userId  the user ID
     * @return the created dataset response with upload URL
     */
    @Transactional
    public DatasetResponse createDataset(CreateDatasetRequest request, UUID userId) {
        // Check if name already exists for this user
        if (datasetRepository.existsByNameAndOwnerId(request.getName(), userId)) {
            throw new ApiException(ApiErrorCode.DATASET_NAME_EXISTS,
                    Map.of("name", request.getName()));
        }

        var owner = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(ApiErrorCode.USER_NOT_FOUND,
                        Map.of("userId", userId.toString())));

        var dataset = new Dataset();
        dataset.setName(request.getName());
        dataset.setDescription(request.getDescription());
        dataset.setOriginalFilename(request.getFilename());
        dataset.setContentType(request.getContentType());
        dataset.setStatus(DatasetStatus.PENDING_UPLOAD);
        dataset.setOwner(owner);

        var savedDataset = datasetRepository.save(dataset);

        // Generate storage key after we have the dataset ID
        var storageKey = minioService.generateDatasetStorageKey(
                savedDataset.getId(), request.getFilename());
        savedDataset.setStorageKey(storageKey);
        savedDataset = datasetRepository.save(savedDataset);

        log.info("Dataset created: {} by user {}", savedDataset.getName(), userId);

        auditService.logAsync(
                AuditOperationType.DATASET_CREATE,
                userId,
                savedDataset.getId().toString(),
                Map.of(
                        "name", savedDataset.getName(),
                        "filename", request.getFilename()
                )
        );

        return convertToResponse(savedDataset);
    }

    /**
     * Generate a presigned URL for uploading dataset file
     *
     * @param datasetId the dataset ID
     * @param userId    the user ID
     * @return presigned URL response
     */
    @Transactional
    public PresignedUrlResponse generateUploadUrl(UUID datasetId, UUID userId) {
        var dataset = getDatasetEntityByIdAndOwner(datasetId, userId);

        // Only allow upload in PENDING_UPLOAD or FAILED status
        if (dataset.getStatus() != DatasetStatus.PENDING_UPLOAD
                && dataset.getStatus() != DatasetStatus.FAILED) {
            throw new ApiException(ApiErrorCode.DATASET_UPLOAD_NOT_ALLOWED,
                    "Dataset upload not allowed in current status: " + dataset.getStatus());
        }

        dataset.setStatus(DatasetStatus.UPLOADING);
        datasetRepository.save(dataset);

        var presignedUrl = minioService.generateDatasetUploadUrl(
                datasetId, dataset.getOriginalFilename());

        log.info("Upload URL generated for dataset: {} by user {}", datasetId, userId);

        auditService.logAsync(
                AuditOperationType.DATASET_UPLOAD_URL_GENERATED,
                userId,
                datasetId.toString(),
                Map.of("expiresAt", presignedUrl.getExpiresAt().toString())
        );

        return presignedUrl;
    }

    /**
     * Confirm that dataset upload is complete
     *
     * @param datasetId the dataset ID
     * @param userId    the user ID
     * @param fileSize  the uploaded file size in bytes
     * @return the updated dataset response
     */
    @Transactional
    public DatasetResponse confirmUpload(UUID datasetId, UUID userId, Long fileSize) {
        var dataset = getDatasetEntityByIdAndOwner(datasetId, userId);

        if (dataset.getStatus() != DatasetStatus.UPLOADING) {
            throw new ApiException(ApiErrorCode.DATASET_UPLOAD_NOT_ALLOWED,
                    "Cannot confirm upload in current status: " + dataset.getStatus());
        }

        dataset.setStatus(DatasetStatus.READY);
        dataset.setFileSize(fileSize);
        var savedDataset = datasetRepository.save(dataset);

        log.info("Dataset upload confirmed: {} by user {}", datasetId, userId);

        auditService.logAsync(
                AuditOperationType.DATASET_UPLOAD_COMPLETED,
                userId,
                datasetId.toString(),
                Map.of("fileSize", fileSize)
        );

        return convertToResponse(savedDataset);
    }

    /**
     * Generate a presigned URL for downloading dataset file
     *
     * @param datasetId the dataset ID
     * @param userId    the user ID
     * @return presigned URL response
     */
    public PresignedUrlResponse generateDownloadUrl(UUID datasetId, UUID userId) {
        var dataset = getDatasetEntityByIdAndOwner(datasetId, userId);

        if (dataset.getStatus() != DatasetStatus.READY
                && dataset.getStatus() != DatasetStatus.PROCESSING) {
            throw new ApiException(ApiErrorCode.DATASET_ACCESS_DENIED,
                    "Dataset not available for download in current status: " + dataset.getStatus());
        }

        var presignedUrl = minioService.generateDatasetDownloadUrl(dataset.getStorageKey());

        log.info("Download URL generated for dataset: {} by user {}", datasetId, userId);

        auditService.logAsync(
                AuditOperationType.DATASET_DOWNLOAD_URL_GENERATED,
                userId,
                datasetId.toString(),
                Map.of("expiresAt", presignedUrl.getExpiresAt().toString())
        );

        return presignedUrl;
    }

    /**
     * Preprocess dataset by setting train/test/eval split ratios
     *
     * @param datasetId the dataset ID
     * @param request   the preprocess request with split ratios
     * @param userId    the user ID
     * @return the updated dataset response
     */
    @Transactional
    public DatasetResponse preprocessDataset(UUID datasetId, DatasetPreprocessRequest request, UUID userId) {
        var dataset = getDatasetEntityByIdAndOwner(datasetId, userId);

        if (dataset.getStatus() != DatasetStatus.READY) {
            throw new ApiException(ApiErrorCode.DATASET_ACCESS_DENIED,
                    "Dataset preprocessing only allowed in READY status");
        }

        // Validate that ratios sum to 1.0
        var totalRatio = request.getTrainRatio() + request.getTestRatio() + request.getEvalRatio();
        if (Math.abs(totalRatio - 1.0) > 0.001) {
            throw new ApiException(ApiErrorCode.VALIDATION_FAILED,
                    "Train, test, and eval ratios must sum to 1.0");
        }

        dataset.setTrainRatio(request.getTrainRatio());
        dataset.setTestRatio(request.getTestRatio());
        dataset.setEvalRatio(request.getEvalRatio());
        var savedDataset = datasetRepository.save(dataset);

        log.info("Dataset preprocessed: {} with ratios train={}, test={}, eval={} by user {}",
                datasetId, request.getTrainRatio(), request.getTestRatio(), request.getEvalRatio(), userId);

        auditService.logAsync(
                AuditOperationType.DATASET_PREPROCESS,
                userId,
                datasetId.toString(),
                Map.of(
                        "trainRatio", request.getTrainRatio(),
                        "testRatio", request.getTestRatio(),
                        "evalRatio", request.getEvalRatio()
                )
        );

        return convertToResponse(savedDataset);
    }

    /**
     * Delete a dataset (soft delete)
     *
     * @param datasetId the dataset ID
     * @param userId    the user ID
     */
    @Transactional
    public void deleteDataset(UUID datasetId, UUID userId) {
        var dataset = getDatasetEntityByIdAndOwner(datasetId, userId);

        if (dataset.getStatus() == DatasetStatus.PROCESSING) {
            throw new ApiException(ApiErrorCode.DATASET_DELETE_NOT_ALLOWED,
                    "Cannot delete dataset while processing");
        }

        dataset.setStatus(DatasetStatus.DELETED);
        datasetRepository.save(dataset);

        log.info("Dataset deleted: {} by user {}", datasetId, userId);

        auditService.logAsync(
                AuditOperationType.DATASET_DELETE,
                userId,
                datasetId.toString(),
                Map.of("name", dataset.getName())
        );
    }

    /**
     * Get dataset entity by ID and owner (for internal use)
     *
     * @param datasetId the dataset ID
     * @param userId    the user ID
     * @return the dataset entity
     */
    public Dataset getDatasetEntityByIdAndOwner(UUID datasetId, UUID userId) {
        return datasetRepository.findByIdAndOwnerId(datasetId, userId)
                .orElseThrow(() -> new ApiException(ApiErrorCode.DATASET_NOT_FOUND,
                        Map.of("datasetId", datasetId.toString())));
    }

    /**
     * Get dataset entity by ID (for internal use, e.g., training service)
     *
     * @param datasetId the dataset ID
     * @return the dataset entity
     */
    public Dataset getDatasetEntityById(UUID datasetId) {
        return datasetRepository.findById(datasetId)
                .orElseThrow(() -> new ApiException(ApiErrorCode.DATASET_NOT_FOUND,
                        Map.of("datasetId", datasetId.toString())));
    }

    /**
     * Convert entity to response DTO
     *
     * @param dataset the dataset entity
     * @return the response DTO
     */
    private DatasetResponse convertToResponse(Dataset dataset) {
        return new DatasetResponse(
                dataset.getId(),
                dataset.getName(),
                dataset.getDescription(),
                dataset.getStatus(),
                dataset.getOriginalFilename(),
                dataset.getFileSize(),
                dataset.getContentType(),
                dataset.getTrainRatio(),
                dataset.getTestRatio(),
                dataset.getEvalRatio(),
                dataset.getCreatedAt(),
                dataset.getUpdatedAt()
        );
    }
}