package ai.synalix.synalixai.repository;

import ai.synalix.synalixai.entity.UserGpuPermission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * User GPU permission repository interface
 */
@Repository
public interface UserGpuPermissionRepository extends JpaRepository<UserGpuPermission, Long> {

    /**
     * Find all permissions for a specific user
     */
    List<UserGpuPermission> findByUserId(UUID userId);

    /**
     * Delete all permissions for a specific user
     */
    @Modifying
    @Query("DELETE FROM UserGpuPermission u WHERE u.userId = :userId")
    void deleteByUserId(@Param("userId") UUID userId);

    /**
     * Check if a permission exists for a user and GPU
     */
    boolean existsByUserIdAndGpuId(UUID userId, Long gpuId);

    /**
     * Find all GPU IDs allowed for a specific user
     */
    @Query("SELECT u.gpuId FROM UserGpuPermission u WHERE u.userId = :userId")
    List<Long> findGpuIdsByUserId(@Param("userId") UUID userId);
}

