package ai.synalix.synalixai.service;

import ai.synalix.synalixai.dto.file.CreateFileRequest;
import ai.synalix.synalixai.dto.file.FileResponse;
import ai.synalix.synalixai.dto.storage.PresignedUrlResponse;
import ai.synalix.synalixai.entity.Files;
import ai.synalix.synalixai.enums.FileStatus;
import ai.synalix.synalixai.enums.ApiErrorCode;
import ai.synalix.synalixai.enums.AuditOperationType;
import ai.synalix.synalixai.exception.ApiException;
import ai.synalix.synalixai.repository.FileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

/**
 * File service for file metadata and presigned URL operations.
 */
@Service
public class FileService {

    private final FileRepository fileRepository;
    private final MinioService minioService;
    private final AuditService auditService;

    /**
     * Create file service.
     *
     * @param fileRepository file repository
     * @param minioService   minio service
     * @param auditService   audit service
     */
    public FileService(FileRepository fileRepository, MinioService minioService, AuditService auditService) {
        this.fileRepository = fileRepository;
        this.minioService = minioService;
        this.auditService = auditService;
    }

    /**
     * Create a file record (metadata only). Client should upload content via
     * presigned URL.
     *
     * @param request create file request
     * @param userId  current user id
     * @return created file response
     */
    @Transactional
    public FileResponse createFile(CreateFileRequest request, UUID userId) {
        if (userId == null) {
            throw new ApiException(ApiErrorCode.UNAUTHORIZED);
        }

        var file = new Files();
        file.setCreatedBy(userId);
        file.setOriginalFilename(request != null ? request.getOriginalFilename() : null);
        file.setContentType(request != null ? request.getContentType() : null);
        file.setStatus(FileStatus.PENDING_UPLOAD);
        file.setStorageKey("");

        // StorageKey includes id, so we must save first to get UUID
        var saved = fileRepository.save(file);
        var StorageKey = minioService.generateFileStorageKey(saved.getId(), saved.getOriginalFilename());
        saved.setStorageKey(StorageKey);
        // System.out.println("saved:----------------------------- " + saved);
        saved = fileRepository.save(saved);

        auditService.logOperation(
                AuditOperationType.FILE_UPLOAD_URL_GENERATED,
                userId,
                saved.getId().toString(),
                Map.of("StorageKey", saved.getStorageKey(), "originalFilename", saved.getOriginalFilename()));

        return toResponse(saved);
    }

    /**
     * Generate presigned upload URL by file ID and update status to PENDING (no
     * state change).
     *
     * @param fileId file id
     * @param userId current user id
     * @return presigned upload url response
     */
    public PresignedUrlResponse generateUploadUrl(UUID fileId, UUID userId) {
        var file = getFile(fileId);
        assertOwnerOrThrow(file, userId);

        var url = minioService.generateFileUploadUrl(fileId, file.getOriginalFilename());
        if (url.getUrl() == null || url.getUrl().isBlank()) {
            throw new ApiException(ApiErrorCode.INTERNAL_SERVER_ERROR, "Failed to generate upload url");
        }
        return url;
    }

    /**
     * Generate presigned download URL by file ID.
     *
     * @param fileId file id
     * @param userId current user id
     * @return presigned download url response
     */
    public PresignedUrlResponse generateDownloadUrl(UUID fileId, UUID userId) {
        var file = getFile(fileId);
        assertOwnerOrThrow(file, userId);

        var url = minioService.generateFileDownloadUrl(file.getStorageKey());
        if (url.getUrl() == null || url.getUrl().isBlank()) {
            throw new ApiException(ApiErrorCode.INTERNAL_SERVER_ERROR, "Failed to generate download url");
        }
        return url;
    }

    /**
     * Mark file as uploaded (optional endpoint usage).
     *
     * @param fileId    file id
     * @param sizeBytes size in bytes
     * @param userId    current user id
     * @return updated file response
     */
    @Transactional
    public FileResponse markUploaded(UUID fileId, Long sizeBytes, UUID userId) {
        var file = getFile(fileId);
        assertOwnerOrThrow(file, userId);

        file.setStatus(FileStatus.UPLOADED);
        if (sizeBytes != null && sizeBytes > 0) {
            file.setSizeBytes(sizeBytes);
        }
        var saved = fileRepository.save(file);

        auditService.logOperation(
                AuditOperationType.DATASET_UPLOAD_COMPLETED,
                userId,
                saved.getId().toString(),
                Map.of("StorageKey", saved.getStorageKey(), "sizeBytes", saved.getSizeBytes(), "status",
                        saved.getStatus().name()));

        return toResponse(saved);
    }

    /**
     * Get file by id.
     *
     * @param fileId file id
     * @return file entity
     */
    public Files getFile(UUID fileId) {
        return fileRepository.findById(fileId)
                .orElseThrow(
                        () -> new ApiException(ApiErrorCode.RESOURCE_NOT_FOUND, Map.of("fileId", fileId.toString())));
    }

    /**
     * Convert entity to response.
     *
     * @param file file entity
     * @return response dto
     */
    private FileResponse toResponse(Files file) {
        return new FileResponse(
                file.getId(),
                file.getStorageKey(),
                file.getOriginalFilename(),
                file.getContentType(),
                file.getSizeBytes(),
                file.getStatus(),
                file.getCreatedBy(),
                file.getCreatedAt());
    }

    /**
     * Ensure current user owns the file.
     *
     * @param file   file entity
     * @param userId user id
     */
    private void assertOwnerOrThrow(Files file, UUID userId) {
        if (userId == null || file.getCreatedBy() == null || !file.getCreatedBy().equals(userId)) {
            throw new ApiException(ApiErrorCode.UNAUTHORIZED);
        }
    }

}