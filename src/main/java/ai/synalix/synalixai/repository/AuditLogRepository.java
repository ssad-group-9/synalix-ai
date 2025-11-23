package ai.synalix.synalixai.repository;

import ai.synalix.synalixai.entity.AuditLog;
import ai.synalix.synalixai.enums.AuditOperationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Audit log data access layer interface
 */
@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {

    /**
     * Find audit logs by user ID
     */
    List<AuditLog> findByUserIdOrderByTimestampDesc(UUID userId);

    /**
     * Find audit logs by operation type
     */
    List<AuditLog> findByOperationTypeOrderByTimestampDesc(AuditOperationType operationType);

    /**
     * Find audit logs by resource ID
     */
    List<AuditLog> findByResourceIdOrderByTimestampDesc(String resourceId);

    /**
     * Find audit logs within time range
     */
    @Query("SELECT al FROM AuditLog al WHERE al.timestamp BETWEEN :startTime AND :endTime ORDER BY al.timestamp DESC")
    List<AuditLog> findByTimestampBetween(@Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);

    /**
     * Find audit logs by user and operation type
     */
    List<AuditLog> findByUserIdAndOperationTypeOrderByTimestampDesc(UUID userId, AuditOperationType operationType);

    /**
     * Find recent audit logs for user
     */
    @Query("SELECT al FROM AuditLog al WHERE al.userId = :userId AND al.timestamp >= :since ORDER BY al.timestamp DESC")
    List<AuditLog> findRecentLogsByUserId(@Param("userId") UUID userId, @Param("since") LocalDateTime since);

    /**
     * Count audit logs by operation type
     */
    long countByOperationType(AuditOperationType operationType);

    /**
     * Count audit logs by user ID
     */
    long countByUserId(UUID userId);

    /**
     * Find audit logs older than specified time
     */
    @Query("SELECT al FROM AuditLog al WHERE al.timestamp <= :cutoffTime")
    List<AuditLog> findLogsOlderThan(@Param("cutoffTime") LocalDateTime cutoffTime);
}