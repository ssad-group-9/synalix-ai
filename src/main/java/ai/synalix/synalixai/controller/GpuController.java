package ai.synalix.synalixai.controller;

import ai.synalix.synalixai.dto.gpu.GpuResourceResponse;
import ai.synalix.synalixai.dto.gpu.UserGpuPermissionResponse;
import ai.synalix.synalixai.dto.gpu.UpdateUserGpuPermissionRequest;
import ai.synalix.synalixai.dto.gpu.ResourceDetail;
import ai.synalix.synalixai.service.GpuPermissionService;
import ai.synalix.synalixai.service.ResourceService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import ai.synalix.synalixai.config.JwtUserPrincipal;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * GPU management REST controller
 */
@RestController
@RequestMapping("/api/gpu")
public class GpuController {

    private final ResourceService resourceService;
    private final GpuPermissionService gpuPermissionService;

    @Autowired
    public GpuController(ResourceService resourceService, GpuPermissionService gpuPermissionService) {
        this.resourceService = resourceService;
        this.gpuPermissionService = gpuPermissionService;
    }

    /**
     * Get all GPU resources
     */
    @GetMapping("/resources")
    public ResponseEntity<List<GpuResourceResponse>> getGpuResources() {
        var resourceDetails = resourceService.getAllResources();
        var responses = resourceDetails.stream()
                .map(this::convertToGpuResourceResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    /**
     * Get all user GPU permissions (Admin only)
     */
    @GetMapping("/permissions")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserGpuPermissionResponse>> getUserGpuPermissions() {
        var permissions = gpuPermissionService.getAllUserGpuPermissions();
        return ResponseEntity.ok(permissions);
    }

    /**
     * Get GPU permissions for a specific user (Admin only)
     */
    @GetMapping("/permissions/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserGpuPermissionResponse> getUserGpuPermission(
            @PathVariable UUID userId) {
        var permission = gpuPermissionService.getUserGpuPermission(userId);
        return ResponseEntity.ok(permission);
    }

    /**
     * Update GPU permissions for a specific user (Admin only)
     */
    @PutMapping("/permissions/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserGpuPermissionResponse> updateUserGpuPermission(
            @PathVariable UUID userId,
            @Valid @RequestBody UpdateUserGpuPermissionRequest request,
            @AuthenticationPrincipal JwtUserPrincipal principal) {
        var permission = gpuPermissionService.updateUserGpuPermission(userId, request.getGpuIds());
        return ResponseEntity.ok(permission);
    }

    /**
     * Convert ResourceDetail to GpuResourceResponse
     */
    private GpuResourceResponse convertToGpuResourceResponse(ResourceDetail resourceDetail) {
        var response = new GpuResourceResponse();
        response.setId(resourceDetail.getId());
        response.setName(resourceDetail.getName());
        response.setStatus(resourceDetail.getStatus());
        
        // Parse memory information from "memoryUsed" string like "447/15360MB"
        if (resourceDetail.getMemoryUsed() != null && resourceDetail.getMemoryUsed().contains("/")) {
            try {
                var memoryParts = resourceDetail.getMemoryUsed().replace("MB", "").split("/");
                var memoryUsed = Integer.parseInt(memoryParts[0].trim());
                var memoryTotal = Integer.parseInt(memoryParts[1].trim());
                response.setMemoryUsed(memoryUsed);
                response.setMemoryTotal(memoryTotal);
                response.setMemoryFree(memoryTotal - memoryUsed);
            } catch (Exception e) {
                // If parsing fails, set defaults
                response.setMemoryUsed(0);
                response.setMemoryTotal(0);
                response.setMemoryFree(0);
            }
        } else {
            response.setMemoryUsed(0);
            response.setMemoryTotal(0);
            response.setMemoryFree(0);
        }
        
        return response;
    }
}

