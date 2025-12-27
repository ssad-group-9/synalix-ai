package ai.synalix.synalixai.service;

import ai.synalix.synalixai.dto.gpu.UserGpuPermissionResponse;
import ai.synalix.synalixai.entity.Resource;
import ai.synalix.synalixai.entity.User;
import ai.synalix.synalixai.entity.UserGpuPermission;
import ai.synalix.synalixai.enums.ApiErrorCode;
import ai.synalix.synalixai.exception.ApiException;
import ai.synalix.synalixai.repository.ResourceRepository;
import ai.synalix.synalixai.repository.UserGpuPermissionRepository;
import ai.synalix.synalixai.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * GPU permission management service
 */
@Service
public class GpuPermissionService {

    private static final Logger logger = LoggerFactory.getLogger(GpuPermissionService.class);

    private final UserGpuPermissionRepository userGpuPermissionRepository;
    private final UserRepository userRepository;
    private final ResourceRepository resourceRepository;

    @Autowired
    public GpuPermissionService(UserGpuPermissionRepository userGpuPermissionRepository,
                               UserRepository userRepository,
                               ResourceRepository resourceRepository) {
        this.userGpuPermissionRepository = userGpuPermissionRepository;
        this.userRepository = userRepository;
        this.resourceRepository = resourceRepository;
    }

    /**
     * Get all user GPU permissions
     */
    public List<UserGpuPermissionResponse> getAllUserGpuPermissions() {
        var allUsers = userRepository.findAll();
        return allUsers.stream()
                .map(user -> {
                    var gpuIds = userGpuPermissionRepository.findGpuIdsByUserId(user.getId());
                    return convertToResponse(user, gpuIds);
                })
                .collect(Collectors.toList());
    }

    /**
     * Get GPU permissions for a specific user
     */
    public UserGpuPermissionResponse getUserGpuPermission(UUID userId) {
        var user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(ApiErrorCode.USER_NOT_FOUND));
        
        var gpuIds = userGpuPermissionRepository.findGpuIdsByUserId(userId);
        return convertToResponse(user, gpuIds);
    }

    /**
     * Update GPU permissions for a specific user
     */
    @Transactional
    public UserGpuPermissionResponse updateUserGpuPermission(UUID userId, List<Long> gpuIds) {
        // Validate user exists
        var user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(ApiErrorCode.USER_NOT_FOUND));

        // Validate all GPU IDs exist
        if (gpuIds != null && !gpuIds.isEmpty()) {
            for (Long gpuId : gpuIds) {
                if (!resourceRepository.existsById(gpuId)) {
                    throw new ApiException(ApiErrorCode.RESOURCE_NOT_FOUND, "GPU with ID " + gpuId + " not found");
                }
            }
        }

        // Delete existing permissions
        userGpuPermissionRepository.deleteByUserId(userId);

        // Create new permissions
        if (gpuIds != null && !gpuIds.isEmpty()) {
            for (Long gpuId : gpuIds) {
                var permission = new UserGpuPermission();
                permission.setUserId(userId);
                permission.setGpuId(gpuId);
                userGpuPermissionRepository.save(permission);
            }
        }

        logger.info("Updated GPU permissions for user: id={}, username={}, gpuIds={}", 
                   userId, user.getUsername(), gpuIds);

        return convertToResponse(user, gpuIds != null ? gpuIds : List.of());
    }

    /**
     * Check if a user has permission to use a specific GPU
     */
    public boolean hasGpuPermission(UUID userId, Long gpuId) {
        return userGpuPermissionRepository.existsByUserIdAndGpuId(userId, gpuId);
    }

    /**
     * Get all allowed GPU IDs for a user
     */
    public List<Long> getAllowedGpuIds(UUID userId) {
        return userGpuPermissionRepository.findGpuIdsByUserId(userId);
    }

    /**
     * Convert User entity and GPU IDs to response DTO
     */
    private UserGpuPermissionResponse convertToResponse(User user, List<Long> gpuIds) {
        var response = new UserGpuPermissionResponse();
        response.setUserId(user.getId());
        response.setUsername(user.getUsername());
        response.setNickname(user.getNickname());
        response.setAllowedGpuIds(gpuIds != null ? gpuIds : List.of());
        return response;
    }
}

