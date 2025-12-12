package ai.synalix.synalixai.dto.model;

import ai.synalix.synalixai.enums.ModelType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO for model information
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ModelResponse {

    /**
     * Unique identifier of the model
     */
    private UUID id;

    /**
     * Display name of the model
     */
    private String name;

    /**
     * Type of the model (LLM, CV, OTHER)
     */
    private ModelType type;

    /**
     * Description of the model
     */
    private String description;

    /**
     * Version of the model
     */
    private String version;

    /**
     * Timestamp when the model was created
     */
    private LocalDateTime createdAt;
}