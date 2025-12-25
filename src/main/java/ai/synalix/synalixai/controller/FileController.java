package ai.synalix.synalixai.controller;

import ai.synalix.synalixai.config.JwtUserPrincipal;
import ai.synalix.synalixai.dto.file.CreateFileRequest;
import ai.synalix.synalixai.dto.file.FileResponse;
import ai.synalix.synalixai.dto.storage.PresignedUrlResponse;
import ai.synalix.synalixai.service.FileService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * File controller for file metadata and presigned URL operations.
 */
@RestController
@RequestMapping("/api/files")
public class FileController {

    private final FileService fileService;

    /**
     * Create file controller.
     *
     * @param fileService file service
     */
    public FileController(FileService fileService) {
        this.fileService = fileService;
    }

    /**
     * Create a file record (metadata only).
     *
     * @param request   create file request
     * @param principal authenticated user
     * @return created file response
     */
    @PostMapping
    public ResponseEntity<FileResponse> createFile(
            @Valid @RequestBody CreateFileRequest request,
            @AuthenticationPrincipal JwtUserPrincipal principal) {
        var userId = principal.getId();
        var created = fileService.createFile(request, userId);
        return ResponseEntity.ok(created);
    }

    /**
     * Generate a presigned URL for uploading file content.
     *
     * @param id        the file ID
     * @param principal the authenticated user
     * @return presigned upload URL response
     */
    @GetMapping("/{id}/upload-url")
    public ResponseEntity<PresignedUrlResponse> getUploadUrl(
            @PathVariable @NotNull UUID id,
            @AuthenticationPrincipal JwtUserPrincipal principal) {
        var userId = principal.getId();
        var presignedUrl = fileService.generateUploadUrl(id, userId);
        return ResponseEntity.ok(presignedUrl);
    }

    /**
     * Generate a presigned URL for downloading file content.
     *
     * @param id        the file ID
     * @param principal the authenticated user
     * @return presigned download URL response
     */
    @GetMapping("/{id}/download-url")
    public ResponseEntity<PresignedUrlResponse> getDownloadUrl(
            @PathVariable @NotNull UUID id,
            @AuthenticationPrincipal JwtUserPrincipal principal) {
        var userId = principal.getId();
        var presignedUrl = fileService.generateDownloadUrl(id, userId);
        // System.out.println("Generated download URL: " + presignedUrl.getUrl());
        return ResponseEntity.ok(presignedUrl);
    }

    /**
     * Mark file as uploaded (client calls after PUT to presigned url).
     *
     * @param id        file id
     * @param principal authenticated user
     * @return updated file response
     */
    @PostMapping("/{id}/uploaded")
    public ResponseEntity<FileResponse> markUploaded(
            @PathVariable @NotNull UUID id,
            @RequestParam(value = "sizeBytes", required = false) Long sizeBytes,
            @AuthenticationPrincipal JwtUserPrincipal principal) {
        var userId = principal.getId();
        var updated = fileService.markUploaded(id, sizeBytes, userId);
        return ResponseEntity.ok(updated);
    }
}