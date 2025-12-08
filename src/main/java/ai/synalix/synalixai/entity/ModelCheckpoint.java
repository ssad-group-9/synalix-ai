package ai.synalix.synalixai.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Model checkpoint entity class
 * Represents a fine-tuned model checkpoint created by training
 */
@Entity
@Table(name = "model_checkpoints",
       uniqueConstraints = @UniqueConstraint(columnNames = {"name", "owner_id"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ModelCheckpoint {

    @Id
    @UuidGenerator
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    /**
     * Display name of the checkpoint
     */
    @NotBlank(message = "Checkpoint name cannot be blank")
    @Size(max = 100, message = "Checkpoint name cannot exceed 100 characters")
    @Column(name = "name", nullable = false, length = 100)
    private String name;

    /**
     * Optional description of the checkpoint
     */
    @Size(max = 1000, message = "Description cannot exceed 1000 characters")
    @Column(name = "description", length = 1000)
    private String description;

    /**
     * Object key in MinIO storage (internal, not exposed to user)
     */
    @Column(name = "storage_key", length = 255)
    private String storageKey;

    /**
     * File size in bytes
     */
    @Column(name = "file_size")
    private Long fileSize;

    /**
     * The base model used for fine-tuning
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "base_model_id", nullable = false)
    private BaseModel baseModel;

    /**
     * The dataset used for training
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dataset_id", nullable = false)
    private Dataset dataset;

    /**
     * Owner of this checkpoint
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    /**
     * Training job ID that created this checkpoint (for reference)
     */
    @Column(name = "training_job_id")
    private UUID trainingJobId;

    /**
     * Whether the checkpoint upload is complete
     */
    @Column(name = "upload_complete", nullable = false)
    private boolean uploadComplete = false;

    /**
     * Timestamp when the checkpoint was created
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}