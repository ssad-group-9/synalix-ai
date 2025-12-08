package ai.synalix.synalixai.dto.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO for base model information
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BaseModelResponse {

    /**
     * Unique identifier of the model
     */
    private UUID id;

    /**
     * Display name of the model
     */
    private String name;

    /**
     * HuggingFace model identifier
     */
    private String huggingfaceModelId;

    /**
     * Description of the model
     */
    private String description;

    /**
     * Whether the model is enabled
     */
    private boolean enabled;

    /**
     * Timestamp when the model was registered
     */
    private LocalDateTime createdAt;

    /**
     * Username of the admin who registered this model
     */
    private String createdBy;
}