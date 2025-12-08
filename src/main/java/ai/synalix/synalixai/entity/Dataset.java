package ai.synalix.synalixai.entity;

import ai.synalix.synalixai.enums.DatasetStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Dataset entity class
 * Represents a user-uploaded dataset for model training
 */
@Entity
@Table(name = "datasets",
       uniqueConstraints = @UniqueConstraint(columnNames = {"name", "owner_id"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Dataset {

    @Id
    @UuidGenerator
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    /**
     * Display name of the dataset
     */
    @NotBlank(message = "Dataset name cannot be blank")
    @Size(max = 100, message = "Dataset name cannot exceed 100 characters")
    @Column(name = "name", nullable = false, length = 100)
    private String name;

    /**
     * Optional description of the dataset
     */
    @Size(max = 1000, message = "Description cannot exceed 1000 characters")
    @Column(name = "description", length = 1000)
    private String description;

    /**
     * Current status of the dataset
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private DatasetStatus status = DatasetStatus.PENDING_UPLOAD;

    /**
     * Object key in MinIO storage (internal, not exposed to user)
     */
    @Column(name = "storage_key", length = 255)
    private String storageKey;

    /**
     * Original filename uploaded by user
     */
    @Size(max = 255, message = "Filename cannot exceed 255 characters")
    @Column(name = "original_filename", length = 255)
    private String originalFilename;

    /**
     * File size in bytes
     */
    @Column(name = "file_size")
    private Long fileSize;

    /**
     * MIME type of the file
     */
    @Size(max = 100, message = "Content type cannot exceed 100 characters")
    @Column(name = "content_type", length = 100)
    private String contentType;

    /**
     * Training set ratio (0.0 - 1.0)
     */
    @Column(name = "train_ratio")
    private Double trainRatio;

    /**
     * Test set ratio (0.0 - 1.0)
     */
    @Column(name = "test_ratio")
    private Double testRatio;

    /**
     * Evaluation set ratio (0.0 - 1.0)
     */
    @Column(name = "eval_ratio")
    private Double evalRatio;

    /**
     * Owner of this dataset
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    /**
     * Timestamp when the dataset was created
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Timestamp when the dataset was last updated
     */
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}