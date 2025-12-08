package ai.synalix.synalixai.controller;

import ai.synalix.synalixai.dto.model.BaseModelResponse;
import ai.synalix.synalixai.dto.model.CreateBaseModelRequest;
import ai.synalix.synalixai.dto.model.ModelCheckpointResponse;
import ai.synalix.synalixai.dto.storage.PresignedUrlResponse;
import ai.synalix.synalixai.config.JwtUserPrincipal;
import ai.synalix.synalixai.service.BaseModelService;
import ai.synalix.synalixai.service.ModelCheckpointService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for model management operations
 * Handles base models (admin) and model checkpoints (user)
 */
@RestController
@RequestMapping("/api/models")
public class ModelController {

    private final BaseModelService baseModelService;
    private final ModelCheckpointService checkpointService;

    @Autowired
    public ModelController(BaseModelService baseModelService,
                           ModelCheckpointService checkpointService) {
        this.baseModelService = baseModelService;
        this.checkpointService = checkpointService;
    }

    // ============================================
    // Base Model Endpoints (Admin)
    // ============================================

    /**
     * Get all base models
     * For regular users, returns only enabled models
     * For admins, returns all models
     *
     * @param principal the authenticated user
     * @return list of base models
     */
    @GetMapping("/base")
    public ResponseEntity<List<BaseModelResponse>> getBaseModels(
            @AuthenticationPrincipal JwtUserPrincipal principal) {
        List<BaseModelResponse> models;
        if (principal.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
            models = baseModelService.getAllModels();
        } else {
            models = baseModelService.getAllEnabledModels();
        }
        return ResponseEntity.ok(models);
    }

    /**
     * Get a base model by ID
     *
     * @param id the model ID
     * @return the base model
     */
    @GetMapping("/base/{id}")
    public ResponseEntity<BaseModelResponse> getBaseModelById(@PathVariable UUID id) {
        var model = baseModelService.getModelById(id);
        return ResponseEntity.ok(model);
    }

    /**
     * Register a new base model (admin only)
     *
     * @param request   the create model request
     * @param principal the authenticated admin
     * @return the created base model
     */
    @PostMapping("/base")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BaseModelResponse> createBaseModel(
            @Valid @RequestBody CreateBaseModelRequest request,
            @AuthenticationPrincipal JwtUserPrincipal principal) {
        var adminId = principal.getId();
        var model = baseModelService.createModel(request, adminId);
        return ResponseEntity.status(HttpStatus.CREATED).body(model);
    }

    /**
     * Enable a base model (admin only)
     *
     * @param id        the model ID
     * @param principal the authenticated admin
     * @return the updated base model
     */
    @PutMapping("/base/{id}/enable")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BaseModelResponse> enableBaseModel(
            @PathVariable UUID id,
            @AuthenticationPrincipal JwtUserPrincipal principal) {
        var adminId = principal.getId();
        var model = baseModelService.setModelEnabled(id, true, adminId);
        return ResponseEntity.ok(model);
    }

    /**
     * Disable a base model (admin only)
     *
     * @param id        the model ID
     * @param principal the authenticated admin
     * @return the updated base model
     */
    @PutMapping("/base/{id}/disable")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BaseModelResponse> disableBaseModel(
            @PathVariable UUID id,
            @AuthenticationPrincipal JwtUserPrincipal principal) {
        var adminId = principal.getId();
        var model = baseModelService.setModelEnabled(id, false, adminId);
        return ResponseEntity.ok(model);
    }

    /**
     * Delete a base model (admin only)
     *
     * @param id        the model ID
     * @param principal the authenticated admin
     * @return no content
     */
    @DeleteMapping("/base/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteBaseModel(
            @PathVariable UUID id,
            @AuthenticationPrincipal JwtUserPrincipal principal) {
        var adminId = principal.getId();
        baseModelService.deleteModel(id, adminId);
        return ResponseEntity.noContent().build();
    }

    // ============================================
    // Model Checkpoint Endpoints (User)
    // ============================================

    /**
     * Get all checkpoints for the current user
     *
     * @param principal the authenticated user
     * @return list of checkpoints
     */
    @GetMapping
    public ResponseEntity<List<ModelCheckpointResponse>> getCheckpoints(
            @AuthenticationPrincipal JwtUserPrincipal principal) {
        var userId = principal.getId();
        var checkpoints = checkpointService.getCheckpointsByOwner(userId);
        return ResponseEntity.ok(checkpoints);
    }

    /**
     * Get a checkpoint by ID
     *
     * @param id        the checkpoint ID
     * @param principal the authenticated user
     * @return the checkpoint
     */
    @GetMapping("/{id}")
    public ResponseEntity<ModelCheckpointResponse> getCheckpointById(
            @PathVariable UUID id,
            @AuthenticationPrincipal JwtUserPrincipal principal) {
        var userId = principal.getId();
        var checkpoint = checkpointService.getCheckpointById(id, userId);
        return ResponseEntity.ok(checkpoint);
    }

    /**
     * Generate a presigned URL for downloading a checkpoint
     *
     * @param id        the checkpoint ID
     * @param principal the authenticated user
     * @return presigned URL response
     */
    @GetMapping("/{id}/download")
    public ResponseEntity<PresignedUrlResponse> getCheckpointDownloadUrl(
            @PathVariable UUID id,
            @AuthenticationPrincipal JwtUserPrincipal principal) {
        var userId = principal.getId();
        var presignedUrl = checkpointService.generateDownloadUrl(id, userId);
        return ResponseEntity.ok(presignedUrl);
    }

    /**
     * Delete a checkpoint
     *
     * @param id        the checkpoint ID
     * @param principal the authenticated user
     * @return no content
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCheckpoint(
            @PathVariable UUID id,
            @AuthenticationPrincipal JwtUserPrincipal principal) {
        var userId = principal.getId();
        checkpointService.deleteCheckpoint(id, userId);
        return ResponseEntity.noContent().build();
    }
}