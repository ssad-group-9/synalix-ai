package ai.synalix.synalixai.service;

import ai.synalix.synalixai.config.MinioConfig;
import ai.synalix.synalixai.dto.dataset.CreateDatasetRequest;
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
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;

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
    private final MinioConfig minioConfig;
    private final RestTemplate restTemplate;

    @Value("${app.backend-base-url}")
    private String backendBaseUrl;

    @Autowired
    public DatasetService(DatasetRepository datasetRepository,
            UserRepository userRepository,
            MinioService minioService,
            AuditService auditService,
            MinioConfig minioConfig,
            RestTemplate restTemplate) {
        this.datasetRepository = datasetRepository;
        this.userRepository = userRepository;
        this.minioService = minioService;
        this.auditService = auditService;
        this.minioConfig = minioConfig;
        this.restTemplate = restTemplate;
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
     * Create a new dataset without uploading file
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
        dataset.setOwner(owner);
        dataset.setPath("");
        dataset.setStatus(DatasetStatus.PENDING_UPLOAD); // Dataset is pending until file is uploaded
        dataset.setSize(0L); // Size will be updated after upload

        var savedDataset = datasetRepository.save(dataset);
        log.info("Dataset created without file: {} by user {}", savedDataset.getName(), userId);

        auditService.logDatasetCreate(
                userId,
                savedDataset.getId().toString(),
                savedDataset.getName(),
                "",
                0L);

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

        String storagePath = minioService.generateDatasetStorageKey(datasetId, dataset.getName());
        dataset.setPath(storagePath);
        datasetRepository.save(dataset);

        var presignedUrl = minioService.generateDatasetUploadUrl(datasetId, dataset.getName());

        log.info("Upload URL generated for dataset: {} by user {}", datasetId, userId);

        auditService.logAsync(
                AuditOperationType.DATASET_UPLOAD_URL_GENERATED,
                userId,
                datasetId.toString(),
                Map.of("expiresAt", presignedUrl.getExpiresAt().toString()));

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

        auditService.logDatasetDownload(
                userId,
                dataset.getId().toString(),
                dataset.getName());

        return presignedUrl;
    }

    /**
     * Update dataset by sending generated download URL to backend
     * /api/data/upload_url.
     * The URL parameter is obtained from generateDownloadUrl().
     *
     * @param datasetId 数据集ID
     * @param userId    操作用户ID
     * @return 是否更新成功
     */
    @Transactional
    public boolean updateDataset(UUID datasetId, UUID userId) {
        var downloadUrl = generateDownloadUrl(datasetId, userId).getUrl();
        if (downloadUrl == null || downloadUrl.isBlank()) {
            throw new ApiException(ApiErrorCode.INTERNAL_SERVER_ERROR, "Failed to generate download URL");
        }

        var base = backendBaseUrl.endsWith("/") ? backendBaseUrl + "api/data/upload_url"
                : backendBaseUrl + "/api/data/upload_url";

        // 以 JSON 请求体提交，避免 & 被编码成 %xx
        var body = Map.of("url", downloadUrl);
        try {
            ResponseEntity<Void> resp = restTemplate.exchange(
                    base,
                    HttpMethod.POST,
                    new HttpEntity<>(body),
                    Void.class);
            if (!resp.getStatusCode().is2xxSuccessful()) {
                throw new ApiException(ApiErrorCode.INTERNAL_SERVER_ERROR, "Backend upload_url failed");
            }
        } catch (Exception e) {
            throw new ApiException(ApiErrorCode.INTERNAL_SERVER_ERROR, "Backend upload_url failed");
        }

        auditService.logOperation(
                AuditOperationType.DATASET_UPDATE,
                userId,
                datasetId.toString(),
                Map.of("downloadUrl", downloadUrl));

        return true;
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

        String filePath = dataset.getPath();
        String datasetName = dataset.getName();

        try {
            minioService.deleteFile(minioConfig.getDatasetsBucket(), filePath);
            log.info("Dataset file deleted from MinIO: {} for dataset: {}", filePath, datasetName);
        } catch (Exception e) {
            // Log the error and do not delete the database record
            log.error(
                    "Failed to delete dataset file from MinIO: {} for dataset: {}. Database record NOT deleted. Operation aborted.",
                    filePath, datasetName, e);
            throw new ApiException(ApiErrorCode.DATASET_DELETE_NOT_ALLOWED,
                    "Failed to delete dataset file from MinIO. Database record not deleted. Please retry or contact support.");
        }
        datasetRepository.delete(dataset);

        log.info("Dataset deleted: {} by user {}", datasetId, userId);

        auditService.logDatasetDelete(userId, datasetId.toString(), datasetName);
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
    public DatasetResponse updateDataset(UUID datasetId, UUID userId, Long size, String path) {
        var dataset = getDatasetEntityByIdAndOwner(datasetId, userId);

        dataset.setSize(size);
        dataset.setStatus(DatasetStatus.READY);
        if (path != null && !path.isBlank()) {
            dataset.setPath(path);
        }
        var savedDataset = datasetRepository.save(dataset);

        log.info("Dataset size updated: {} to {} bytes by user {}. Path set to: {}", datasetId, size, userId,
                dataset.getPath());
        auditService.logDatasetUpdate(
                userId,
                datasetId.toString(),
                dataset.getName(),
                Map.of(
                        "name", dataset.getName(),
                        "size", size,
                        "path", path));
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
                createdAt);
    }
}