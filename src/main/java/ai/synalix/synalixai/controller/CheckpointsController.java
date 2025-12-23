package ai.synalix.synalixai.controller;

import ai.synalix.synalixai.dto.model.BackendCheckpointsResponse;
import ai.synalix.synalixai.dto.model.CheckpointQueryRequest;
import ai.synalix.synalixai.dto.model.CheckpointResponse;
import ai.synalix.synalixai.config.JwtUserPrincipal;
import ai.synalix.synalixai.service.CheckpointService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Controller for querying checkpoints by model ID.
 */
@RestController
@RequestMapping("/api/checkpoints")
public class CheckpointsController {

    private final CheckpointService checkpointService;

    @Autowired
    public CheckpointsController(CheckpointService checkpointService) {
        this.checkpointService = checkpointService;
    }

    /**
     * Get all checkpoints for a given model.
     */
    @GetMapping("/{modelId}")
    public ResponseEntity<List<CheckpointResponse>> getByModelId(
            @PathVariable @NotNull UUID modelId,
            @AuthenticationPrincipal JwtUserPrincipal principal) {
        var userId = principal.getId();
        var list = checkpointService.fetchAndStoreBackendCheckpoints(modelId);
        return ResponseEntity.ok(list);
    }
}