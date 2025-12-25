package ai.synalix.synalixai.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UuidGenerator;

import ai.synalix.synalixai.enums.CheckpointType;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Checkpoint entity representing a saved model or adapter checkpoint.
 */
@Entity
@Table(name = "checkpoints")
@Data
@NoArgsConstructor
public class Checkpoint {

    /**
     * Primary key (UUID).
     */
    @Id
    @UuidGenerator
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    /**
     * Filesystem or object storage path of the checkpoint.
     */
    @NotBlank(message = "Path cannot be blank")
    @Size(max = 512, message = "Path cannot exceed 512 characters")
    @Column(name = "path", nullable = false, length = 512)
    private String path;

    /**
     * Type of checkpoint: model or adapter.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private CheckpointType type;

    /**
     * Human-readable name of the checkpoint.
     */
    @NotBlank(message = "Name cannot be blank")
    @Size(max = 200, message = "Name cannot exceed 200 characters")
    @Column(name = "name", nullable = false, length = 200)
    private String name;

    /**
     * Associated model id this checkpoint belongs to.
     */
    @NotNull(message = "Model ID cannot be null")
    @Column(name = "model_id", nullable = false)
    private UUID modelId;

    /**
     * Associated task id this checkpoint belongs to.
     */
    @NotNull(message = "Task ID cannot be null")
    @Column(name = "task_id", nullable = false)
    private String taskId;

    /**
     * Created timestamp.
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}