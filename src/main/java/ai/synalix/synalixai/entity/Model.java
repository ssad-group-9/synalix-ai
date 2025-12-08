package ai.synalix.synalixai.entity;

import ai.synalix.synalixai.enums.ModelType;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Model entity class
 * Represents a model registered in the system
 */
@Entity
@Table(name = "models",
       uniqueConstraints = @UniqueConstraint(columnNames = "name"))
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Model {

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
     * Type of the model (LLM, CV, OTHER)
     */
    @NotNull(message = "Model type cannot be null")
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private ModelType type;

    /**
     * Optional description of the model
     */
    @Size(max = 1000, message = "Description cannot exceed 1000 characters")
    @Column(name = "description", length = 1000)
    private String description;

    /**
     * Version of the model
     */
    @NotBlank(message = "Model version cannot be blank")
    @Size(max = 50, message = "Version cannot exceed 50 characters")
    @Column(name = "version", nullable = false, length = 50)
    private String version;

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