package ai.synalix.synalixai.dto.model;

import ai.synalix.synalixai.enums.ModelType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for creating a new model
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateModelRequest {

    /**
     * Display name of the model
     */
    @NotBlank(message = "Model name cannot be blank")
    @Size(max = 100, message = "Model name cannot exceed 100 characters")
    private String name;

    /**
     * Type of the model (LLM, CV, OTHER)
     */
    @NotNull(message = "Model type cannot be null")
    private ModelType type;

    /**
     * Optional description of the model
     */
    @Size(max = 1000, message = "Description cannot exceed 1000 characters")
    private String description;

    /**
     * Version of the model
     */
    @NotBlank(message = "Model version cannot be blank")
    @Size(max = 50, message = "Version cannot exceed 50 characters")
    private String version;
}