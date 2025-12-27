package ai.synalix.synalixai.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * User GPU permission entity class
 * Stores the relationship between users and allowed GPU resources
 */
@Entity
@Table(name = "user_gpu_permissions",
       uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "gpu_id"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserGpuPermission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", updatable = false, nullable = false)
    private Long id;

    @NotNull(message = "User ID cannot be null")
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @NotNull(message = "GPU ID cannot be null")
    @Column(name = "gpu_id", nullable = false)
    private Long gpuId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}

