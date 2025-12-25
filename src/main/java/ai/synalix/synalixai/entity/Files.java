package ai.synalix.synalixai.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UuidGenerator;
import ai.synalix.synalixai.enums.FileStatus;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * File object entity class for storing file metadata and its storage key.
 */
@Entity
@Table(name = "files")
@Data
@NoArgsConstructor
public class Files {

    /**
     * Primary key (UUID).
     */
    @Id
    @UuidGenerator
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    /**
     * Storage object key in MinIO/S3.
     */
    @NotBlank(message = "Storage key cannot be blank")
    @Size(max = 512, message = "storage key cannot exceed 512 characters")
    @Column(name = "storage_key", nullable = false, length = 512)
    private String storageKey;

    /**
     * Original filename provided by client.
     */
    @Size(max = 255, message = "Original filename cannot exceed 255 characters")
    @Column(name = "original_filename", length = 255)
    private String originalFilename;

    /**
     * Content type, e.g. image/png.
     */
    @Size(max = 100, message = "Content type cannot exceed 100 characters")
    @Column(name = "content_type", length = 100)
    private String contentType;

    /**
     * File size in bytes.
     */
    @Column(name = "size_bytes")
    private Long sizeBytes;

    /**
     * File status.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private FileStatus status = FileStatus.PENDING_UPLOAD;

    /**
     * Creator user id.
     */
    @Column(name = "created_by", nullable = false)
    private UUID createdBy;

    /**
     * Created timestamp.
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}