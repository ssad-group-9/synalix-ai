package ai.synalix.synalixai.dto.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for registering a new base model
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateBaseModelRequest {

    /**
     * Display name of the model
     */
    @NotBlank(message = "Model name cannot be blank")
    @Size(max = 100, message = "Model name cannot exceed 100 characters")
    private String name;

    /**
     * HuggingFace model identifier (e.g., "meta-llama/Llama-2-7b")
     */
    @NotBlank(message = "HuggingFace model ID cannot be blank")
    @Size(max = 255, message = "HuggingFace model ID cannot exceed 255 characters")
    private String huggingfaceModelId;

    /**
     * Optional description of the model
     */
    @Size(max = 1000, message = "Description cannot exceed 1000 characters")
    private String description;
}