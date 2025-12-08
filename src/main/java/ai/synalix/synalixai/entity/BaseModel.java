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
 * Base model entity class
 * Represents a foundation model from HuggingFace used for fine-tuning
 */
@Entity
@Table(name = "base_models",
       uniqueConstraints = @UniqueConstraint(columnNames = "name"))
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BaseModel {

    @Id
    @UuidGenerator
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    /**
     * Display name of the model
     */
    @NotBlank(message = "Model name cannot be blank")
    @Size(max = 100, message = "Model name cannot exceed 100 characters")
    @Column(name = "name", unique = true, nullable = false, length = 100)
    private String name;

    /**
     * HuggingFace model identifier (e.g., "meta-llama/Llama-2-7b")
     */
    @NotBlank(message = "HuggingFace model ID cannot be blank")
    @Size(max = 255, message = "HuggingFace model ID cannot exceed 255 characters")
    @Column(name = "huggingface_model_id", nullable = false, length = 255)
    private String huggingfaceModelId;

    /**
     * Optional description of the model
     */
    @Size(max = 1000, message = "Description cannot exceed 1000 characters")
    @Column(name = "description", length = 1000)
    private String description;

    /**
     * Whether the model is enabled and available for use
     */
    @Column(name = "enabled", nullable = false)
    private boolean enabled = true;

    /**
     * Timestamp when the model was registered
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Admin user who registered this model
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;
}