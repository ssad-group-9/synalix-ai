package ai.synalix.synalixai.controller;

import ai.synalix.synalixai.config.JwtUserPrincipal;
import ai.synalix.synalixai.dto.dataset.CreateDatasetRequest;
import ai.synalix.synalixai.dto.dataset.DatasetPreprocessRequest;
import ai.synalix.synalixai.dto.dataset.DatasetResponse;
import ai.synalix.synalixai.dto.storage.PresignedUrlResponse;
import ai.synalix.synalixai.service.DatasetService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for dataset management operations
 * Handles CRUD operations and file upload/download for datasets
 */
@RestController
@RequestMapping("/api/datasets")
public class DatasetController {

    private final DatasetService datasetService;

    @Autowired
    public DatasetController(DatasetService datasetService) {
        this.datasetService = datasetService;
    }

    /**
     * Get all datasets for the current user
     *
     * @param principal the authenticated user
     * @return list of datasets
     */
    @GetMapping
    public ResponseEntity<List<DatasetResponse>> getDatasets(
            @AuthenticationPrincipal JwtUserPrincipal principal) {
        var userId = principal.getId();
        var datasets = datasetService.getDatasetsByOwner(userId);
        return ResponseEntity.ok(datasets);
    }

    /**
     * Get a dataset by ID
     *
     * @param id        the dataset ID
     * @param principal the authenticated user
     * @return the dataset
     */
    @GetMapping("/{id}")
    public ResponseEntity<DatasetResponse> getDatasetById(
            @PathVariable UUID id,
            @AuthenticationPrincipal JwtUserPrincipal principal) {
        var userId = principal.getId();
        var dataset = datasetService.getDatasetById(id, userId);
        return ResponseEntity.ok(dataset);
    }

    /**
     * Create a new dataset
     *
     * @param request   the create dataset request
     * @param principal the authenticated user
     * @return the created dataset
     */
    @PostMapping
    public ResponseEntity<DatasetResponse> createDataset(
            @Valid @RequestBody CreateDatasetRequest request,
            @AuthenticationPrincipal JwtUserPrincipal principal) {
        var userId = principal.getId();
        var dataset = datasetService.createDataset(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(dataset);
    }

    /**
     * Generate a presigned URL for uploading dataset file
     *
     * @param id        the dataset ID
     * @param principal the authenticated user
     * @return presigned URL response
     */
    @GetMapping("/{id}/upload-url")
    public ResponseEntity<PresignedUrlResponse> getUploadUrl(
            @PathVariable UUID id,
            @AuthenticationPrincipal JwtUserPrincipal principal) {
        var userId = principal.getId();
        var presignedUrl = datasetService.generateUploadUrl(id, userId);
        return ResponseEntity.ok(presignedUrl);
    }

    /**
     * Confirm that dataset upload is complete
     *
     * @param id        the dataset ID
     * @param fileSize  the uploaded file size in bytes
     * @param principal the authenticated user
     * @return the updated dataset
     */
    @PostMapping("/{id}/upload-complete")
    public ResponseEntity<DatasetResponse> confirmUpload(
            @PathVariable UUID id,
            @RequestParam Long fileSize,
            @AuthenticationPrincipal JwtUserPrincipal principal) {
        var userId = principal.getId();
        var dataset = datasetService.confirmUpload(id, userId, fileSize);
        return ResponseEntity.ok(dataset);
    }

    /**
     * Generate a presigned URL for downloading dataset file
     *
     * @param id        the dataset ID
     * @param principal the authenticated user
     * @return presigned URL response
     */
    @GetMapping("/{id}/download-url")
    public ResponseEntity<PresignedUrlResponse> getDownloadUrl(
            @PathVariable UUID id,
            @AuthenticationPrincipal JwtUserPrincipal principal) {
        var userId = principal.getId();
        var presignedUrl = datasetService.generateDownloadUrl(id, userId);
        return ResponseEntity.ok(presignedUrl);
    }

    /**
     * Preprocess dataset by setting train/test/eval split ratios
     *
     * @param id        the dataset ID
     * @param request   the preprocess request with split ratios
     * @param principal the authenticated user
     * @return the updated dataset
     */
    @PostMapping("/{id}/preprocess")
    public ResponseEntity<DatasetResponse> preprocessDataset(
            @PathVariable UUID id,
            @Valid @RequestBody DatasetPreprocessRequest request,
            @AuthenticationPrincipal JwtUserPrincipal principal) {
        var userId = principal.getId();
        var dataset = datasetService.preprocessDataset(id, request, userId);
        return ResponseEntity.ok(dataset);
    }

    /**
     * Delete a dataset
     *
     * @param id        the dataset ID
     * @param principal the authenticated user
     * @return no content
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDataset(
            @PathVariable UUID id,
            @AuthenticationPrincipal JwtUserPrincipal principal) {
        var userId = principal.getId();
        datasetService.deleteDataset(id, userId);
        return ResponseEntity.noContent().build();
    }
}