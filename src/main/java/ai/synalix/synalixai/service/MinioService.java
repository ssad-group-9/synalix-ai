package ai.synalix.synalixai.service;

import ai.synalix.synalixai.config.MinioConfig;
import ai.synalix.synalixai.dto.storage.PresignedUrlResponse;
import ai.synalix.synalixai.enums.ApiErrorCode;
import ai.synalix.synalixai.exception.ApiException;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.http.Method;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.InputStream;
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
     * Generate a presigned URL for uploading a file.
     *
     * @param fileId   the dataset ID
     * @param filename the original filename
     * @return presigned URL response with upload URL
     */
    public PresignedUrlResponse generateFileUploadUrl(UUID fileId, String filename) {
        var storageKey = generateFileStorageKey(fileId, filename);
        return generatePresignedUrl(minioConfig.getFilesBucket(), storageKey, Method.PUT);
    }

    /**
     * Generate a presigned URL for downloading a file
     *
     * @param storageKey the storage key of the file
     * @return presigned URL response with download URL
     */
    public PresignedUrlResponse generateFileDownloadUrl(String storageKey) {
        return generatePresignedUrl(minioConfig.getFilesBucket(), storageKey, Method.GET);
    }

    /**
     * Generate a presigned URL for uploading a dataset file.
     * <p>
     * Note: Only the file extension from the provided {@code filename} is preserved
     * in storage;
     * the original filename itself is not used. The storage key will be a generic
     * name (e.g., "data" + extension).
     *
     * @param datasetId the dataset ID
     * @param filename  the original filename (only the extension is used)
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
    public PresignedUrlResponse generateCheckpointUploadUrl(UUID checkpointId, String filename) {
        var storageKey = generateCheckpointStorageKey(checkpointId, filename);
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
    public String generateCheckpointStorageKey(UUID checkpointId, String filename) {
        var extension = getFileExtension(filename);
        return String.format("checkpoints/%s/model.zip", checkpointId.toString(), extension);
    }

    /**
     * Generate storage key for a checkpoint file
     *
     * @param checkpointId the checkpoint ID
     * @return the storage key
     */
    public String generateFileStorageKey(UUID fileId, String filename) {
        var extension = getFileExtension(filename);
        return String.format("files/%s/file%s", fileId.toString(), extension);
    }

    /**
     * Generate a presigned URL for the specified bucket, key, and method
     *
     * @param bucket     the bucket name
     * @param storageKey the object key
     * @param method     the HTTP method (PUT for upload, GET for download)
     * @return presigned URL response
     */
    private PresignedUrlResponse generatePresignedUrl(String bucket, String storageKey, Method method) {
        try {
            var expirySeconds = (method == Method.PUT)
                    ? minioConfig.getPresignedUrlUploadExpiry()
                    : minioConfig.getPresignedUrlDownloadExpiry();
            var url = minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(method)
                            .bucket(bucket)
                            .object(storageKey)
                            .expiry(expirySeconds, TimeUnit.SECONDS)
                            .build());

            var expiresAt = LocalDateTime.now().plusSeconds(expirySeconds);
            var httpMethod = method == Method.PUT ? "PUT" : "GET";

            log.debug("Generated presigned {} URL for {}/{}, expires at {}",
                    httpMethod, bucket, storageKey, expiresAt);

            return new PresignedUrlResponse(url, httpMethod, expiresAt);
        } catch (Exception e) {
            log.error("Failed to generate presigned URL for {}/{}: {}", bucket, storageKey, e.getMessage());
            throw new ApiException(ApiErrorCode.PRESIGNED_URL_GENERATION_FAILED,
                    "Failed to generate presigned URL: " + e.getMessage());
        }
    }

    /**
     * Extract file extension from filename.
     * <p>
     * Rules:
     * <ul>
     * <li>If the filename is null, empty, or does not contain a dot (other than as
     * the first character), returns empty string.</li>
     * <li>If the filename starts with a dot and contains no other dots (e.g.,
     * ".gitignore"), returns empty string.</li>
     * <li>If the filename ends with a dot (e.g., "file."), returns empty
     * string.</li>
     * <li>Otherwise, returns the substring from the last dot (including the dot),
     * e.g., ".gz" for "archive.tar.gz".</li>
     * </ul>
     *
     * @param filename the filename
     * @return the file extension including the dot, or empty string if none
     */
    private String getFileExtension(String filename) {
        if (filename == null || filename.isEmpty()) {
            return "";
        }
        int lastDot = filename.lastIndexOf('.');
        // No dot or dot is the first character (hidden file with no extension)
        if (lastDot <= 0) {
            return "";
        }
        // Dot is the last character (filename ends with a dot)
        if (lastDot == filename.length() - 1) {
            return "";
        }
        return filename.substring(lastDot);
    }

    /**
     * Upload a file to MinIO
     *
     * @param bucketName the bucket name
     * @param objectName the object name
     * @param stream     the input stream
     * @param size       the file size
     */
    public void uploadFile(String bucketName, String objectName, InputStream stream, long size) {
        try {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .stream(stream, size, -1)
                            .build());
            log.debug("File uploaded successfully to {}/{}", bucketName, objectName);
        } catch (Exception e) {
            log.error("Failed to upload file to {}/{}: {}", bucketName, objectName, e.getMessage());
            throw new ApiException(ApiErrorCode.STORAGE_ERROR,
                    "Failed to upload file: " + e.getMessage());
        }
    }

    /**
     * Delete a file from MinIO
     *
     * @param bucketName the bucket name
     * @param objectName the object name
     */
    public void deleteFile(String bucketName, String objectName) {
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .build());
            log.debug("File deleted successfully from {}/{}", bucketName, objectName);
        } catch (Exception e) {
            log.error("Failed to delete file from {}/{}: {}", bucketName, objectName, e.getMessage());
            throw new ApiException(ApiErrorCode.STORAGE_ERROR,
                    "Failed to delete file: " + e.getMessage());
        }
    }

    /**
     * Get task logs content
     *
     * @param taskId the task ID
     * @return the log content
     */
    public String getTaskLogs(UUID taskId) {
        var objectName = taskId.toString() + ".log";
        try (InputStream stream = minioClient.getObject(
                GetObjectArgs.builder()
                        .bucket(minioConfig.getLogsBucket())
                        .object(objectName)
                        .build())) {
            return new String(stream.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.warn("Failed to retrieve logs for task {}: {}", taskId, e.getMessage());
            return "No logs available for task " + taskId;
        }
    }
}