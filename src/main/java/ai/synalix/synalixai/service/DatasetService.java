package ai.synalix.synalixai.service;

import ai.synalix.synalixai.dto.dataset.CreateDatasetRequest;
import ai.synalix.synalixai.dto.dataset.DatasetResponse;
import ai.synalix.synalixai.dto.storage.PresignedUrlResponse;
import ai.synalix.synalixai.entity.Dataset;
import ai.synalix.synalixai.enums.ApiErrorCode;
import ai.synalix.synalixai.enums.AuditOperationType;
import ai.synalix.synalixai.exception.ApiException;
import ai.synalix.synalixai.repository.DatasetRepository;
import ai.synalix.synalixai.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
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
     * Create a new dataset
     *
     * @param request the create dataset request
     * @param userId  the user ID
     * @return the created dataset response
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
        dataset.setPath(request.getPath());
        dataset.setOwner(owner);
        dataset.setStatus("PENDING_UPLOAD");
        dataset.setSize(0L);

        var savedDataset = datasetRepository.save(dataset);

        log.info("Dataset created: {} by user {}", savedDataset.getName(), userId);

        auditService.logAsync(
                AuditOperationType.DATASET_CREATE,
                userId,
                savedDataset.getId().toString(),
                Map.of(
                        "name", savedDataset.getName(),
                        "path", savedDataset.getPath()
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
    public PresignedUrlResponse generateUploadUrl(UUID datasetId, UUID userId) {
        var dataset = getDatasetEntityByIdAndOwner(datasetId, userId);

        var presignedUrl = minioService.generateDatasetUploadUrl(datasetId, dataset.getName());

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
     * Generate a presigned URL for downloading dataset file
     *
     * @param datasetId the dataset ID
     * @param userId    the user ID
     * @return presigned URL response
     */
    public PresignedUrlResponse generateDownloadUrl(UUID datasetId, UUID userId) {
        var dataset = getDatasetEntityByIdAndOwner(datasetId, userId);

        var presignedUrl = minioService.generateDatasetDownloadUrl(dataset.getPath());

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
     * Delete a dataset
     *
     * @param datasetId the dataset ID
     * @param userId    the user ID
     */
    @Transactional
    public void deleteDataset(UUID datasetId, UUID userId) {
        var dataset = getDatasetEntityByIdAndOwner(datasetId, userId);

        datasetRepository.delete(dataset);
        log.info("Dataset deleted: {} by user {}", datasetId, userId);

        auditService.logAsync(
                AuditOperationType.DATASET_DELETE,
                userId,
                datasetId.toString(),
                Map.of("name", dataset.getName())
        );
    }

    /**
     * Update dataset size after upload
     *
     * @param datasetId the dataset ID
     * @param userId    the user ID
     * @param size      the file size in bytes
     * @return the updated dataset response
     */
    @Transactional
    public DatasetResponse updateDatasetSize(UUID datasetId, UUID userId, Long size) {
        var dataset = getDatasetEntityByIdAndOwner(datasetId, userId);

        dataset.setSize(size);
        dataset.setStatus("READY");
        var savedDataset = datasetRepository.save(dataset);

        log.info("Dataset size updated: {} to {} bytes by user {}", datasetId, size, userId);

        return convertToResponse(savedDataset);
    }

    /**
     * Get dataset entity by ID and owner (for internal use)
     *
     * @param datasetId the dataset ID
     * @param userId    the user ID
     * @return the dataset entity
     */
    private Dataset getDatasetEntityByIdAndOwner(UUID datasetId, UUID userId) {
        return datasetRepository.findByIdAndOwnerId(datasetId, userId)
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
        LocalDateTime createdAt = dataset.getCreatedAt();
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }

        return new DatasetResponse(
                dataset.getId(),
                dataset.getName(),
                dataset.getDescription(),
                dataset.getSize(),
                dataset.getPath(),
                dataset.getOwner().getId(),
                createdAt
        );
    }
}