package ai.synalix.synalixai.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UuidGenerator;

import ai.synalix.synalixai.enums.DatasetStatus;

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
     * File size in bytes
     */
    @Column(name = "size")
    private Long size;

    /**
     * Storage path or URL
     */
    @NotBlank(message = "Path cannot be blank")
    @Size(max = 500, message = "Path cannot exceed 500 characters")
    @Column(name = "path", nullable = false, length = 500)
    private String path;

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

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private DatasetStatus status = DatasetStatus.PENDING_UPLOAD;
}