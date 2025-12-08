package ai.synalix.synalixai.service;

import ai.synalix.synalixai.config.MinioConfig;
import ai.synalix.synalixai.dto.storage.PresignedUrlResponse;
import ai.synalix.synalixai.enums.ApiErrorCode;
import ai.synalix.synalixai.exception.ApiException;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.http.Method;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Service for MinIO object storage operations
 * Handles presigned URL generation for upload and download
 */
@Service
@Slf4j
public class MinioService {

    private final MinioClient minioClient;
    private final MinioConfig minioConfig;

    @Autowired
    public MinioService(MinioClient minioClient, MinioConfig minioConfig) {
        this.minioClient = minioClient;
        this.minioConfig = minioConfig;
    }

    /**
     * Generate a presigned URL for uploading a dataset file
     *
     * @param datasetId the dataset ID
     * @param filename  the original filename
     * @return presigned URL response with upload URL
     */
    public PresignedUrlResponse generateDatasetUploadUrl(UUID datasetId, String filename) {
        var storageKey = generateDatasetStorageKey(datasetId, filename);
        return generatePresignedUrl(minioConfig.getDatasetsBucket(), storageKey, Method.PUT);
    }

    /**
     * Generate a presigned URL for downloading a dataset file
     *
     * @param storageKey the storage key of the file
     * @return presigned URL response with download URL
     */
    public PresignedUrlResponse generateDatasetDownloadUrl(String storageKey) {
        return generatePresignedUrl(minioConfig.getDatasetsBucket(), storageKey, Method.GET);
    }

    /**
     * Generate a presigned URL for uploading a checkpoint file
     *
     * @param checkpointId the checkpoint ID
     * @return presigned URL response with upload URL
     */
    public PresignedUrlResponse generateCheckpointUploadUrl(UUID checkpointId) {
        var storageKey = generateCheckpointStorageKey(checkpointId);
        return generatePresignedUrl(minioConfig.getCheckpointsBucket(), storageKey, Method.PUT);
    }

    /**
     * Generate a presigned URL for downloading a checkpoint file
     *
     * @param storageKey the storage key of the file
     * @return presigned URL response with download URL
     */
    public PresignedUrlResponse generateCheckpointDownloadUrl(String storageKey) {
        return generatePresignedUrl(minioConfig.getCheckpointsBucket(), storageKey, Method.GET);
    }

    /**
     * Generate storage key for a dataset file
     *
     * @param datasetId the dataset ID
     * @param filename  the original filename
     * @return the storage key
     */
    public String generateDatasetStorageKey(UUID datasetId, String filename) {
        var extension = getFileExtension(filename);
        return String.format("datasets/%s/data%s", datasetId.toString(), extension);
    }

    /**
     * Generate storage key for a checkpoint file
     *
     * @param checkpointId the checkpoint ID
     * @return the storage key
     */
    public String generateCheckpointStorageKey(UUID checkpointId) {
        return String.format("checkpoints/%s/model.bin", checkpointId.toString());
    }

    /**
     * Generate a presigned URL for the specified bucket, key, and method
     *
     * @param bucket     the bucket name
     * @param objectKey  the object key
     * @param method     the HTTP method (PUT for upload, GET for download)
     * @return presigned URL response
     */
    private PresignedUrlResponse generatePresignedUrl(String bucket, String objectKey, Method method) {
        try {
            var expirySeconds = minioConfig.getPresignedUrlExpiry();
            var url = minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(method)
                            .bucket(bucket)
                            .object(objectKey)
                            .expiry(expirySeconds, TimeUnit.SECONDS)
                            .build()
            );

            var expiresAt = LocalDateTime.now().plusSeconds(expirySeconds);
            var httpMethod = method == Method.PUT ? "PUT" : "GET";

            log.debug("Generated presigned {} URL for {}/{}, expires at {}", 
                    httpMethod, bucket, objectKey, expiresAt);

            return new PresignedUrlResponse(url, httpMethod, expiresAt);
        } catch (Exception e) {
            log.error("Failed to generate presigned URL for {}/{}: {}", bucket, objectKey, e.getMessage());
            throw new ApiException(ApiErrorCode.PRESIGNED_URL_GENERATION_FAILED,
                    "Failed to generate presigned URL: " + e.getMessage());
        }
    }

    /**
     * Extract file extension from filename
     *
     * @param filename the filename
     * @return the file extension including the dot, or empty string if none
     */
    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf("."));
    }
}