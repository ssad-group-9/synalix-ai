package ai.synalix.synalixai.entity;

import ai.synalix.synalixai.enums.AuditOperationType;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Audit log entity class
 */
@Entity
@Table(name = "audit_logs")
@Data
@NoArgsConstructor
public class AuditLog {

    @Id
    @UuidGenerator
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @NotNull(message = "Operation type cannot be null")
    @Enumerated(EnumType.STRING)
    @Column(name = "operation_type", nullable = false)
    private AuditOperationType operationType;

    @NotNull(message = "Timestamp cannot be null")
    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp = LocalDateTime.now();

    @Column(name = "user_id")
    private UUID userId;

    @Size(max = 50, message = "Username cannot exceed 50 characters")
    @Column(name = "username", length = 50)
    private String username;

    @Column(name = "resource_id")
    private String resourceId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "details", columnDefinition = "jsonb")
    private Map<String, Object> details;

    // Association with User entity
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    private User user;
}