package ai.synalix.synalixai.controller;

import ai.synalix.synalixai.config.JwtUserPrincipal;
import ai.synalix.synalixai.dto.model.CreateModelRequest;
import ai.synalix.synalixai.dto.model.ModelResponse;
import ai.synalix.synalixai.service.ModelService;
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
 */
@RestController
@RequestMapping("/api/models")
public class ModelController {

    private final ModelService modelService;

    @Autowired
    public ModelController(ModelService modelService) {
        this.modelService = modelService;
    }

    /**
     * Get all models
     *
     * @return list of models
     */
    @GetMapping
    public ResponseEntity<List<ModelResponse>> getModels() {
        var models = modelService.getAllModels();
        return ResponseEntity.ok(models);
    }

    /**
     * Get a model by ID
     *
     * @param id the model ID
     * @return the model
     */
    @GetMapping("/{id}")
    public ResponseEntity<ModelResponse> getModelById(@PathVariable UUID id) {
        var model = modelService.getModelById(id);
        return ResponseEntity.ok(model);
    }

    /**
     * Create a new model (admin only)
     *
     * @param request   the create model request
     * @param principal the authenticated admin
     * @return the created model
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ModelResponse> createModel(
            @Valid @RequestBody CreateModelRequest request,
            @AuthenticationPrincipal JwtUserPrincipal principal) {
        var adminId = principal.getId();
        var model = modelService.createModel(request, adminId);
        return ResponseEntity.status(HttpStatus.CREATED).body(model);
    }

    /**
     * Delete a model (admin only)
     *
     * @param id        the model ID
     * @param principal the authenticated admin
     * @return no content
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteModel(
            @PathVariable UUID id,
            @AuthenticationPrincipal JwtUserPrincipal principal) {
        var adminId = principal.getId();
        modelService.deleteModel(id, adminId);
        return ResponseEntity.noContent().build();
    }
}