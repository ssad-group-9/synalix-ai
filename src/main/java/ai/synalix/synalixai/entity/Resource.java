package ai.synalix.synalixai.entity;

import ai.synalix.synalixai.enums.ResourceStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Resource entity class (GPU)
 */
@Entity
@Table(name = "resources", 
       uniqueConstraints = @UniqueConstraint(columnNames = "name"))
@Data
@NoArgsConstructor
public class Resource {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", updatable = false, nullable = false)
    private Long id;

    @NotBlank(message = "Resource name cannot be blank")
    @Size(max = 100, message = "Resource name length cannot exceed 100 characters")
    @Column(name = "name", unique = true, nullable = false, length = 100)
    private String name;

    @NotNull(message = "Resource status cannot be null")
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ResourceStatus status = ResourceStatus.AVAILABLE;

    @Column(name = "memory_total", nullable = false)
    private Integer memoryTotal; // In MB

    @Column(name = "memory_used", nullable = false)
    private Integer memoryUsed = 0; // In MB

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
