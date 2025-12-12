package ai.synalix.synalixai.dto.dataset;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for creating a new dataset
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateDatasetRequest {

    /**
     * Display name of the dataset
     */
    @NotBlank(message = "Dataset name cannot be blank")
    @Size(max = 100, message = "Dataset name cannot exceed 100 characters")
    private String name;

    /**
     * Optional description of the dataset
     */
    @Size(max = 1000, message = "Description cannot exceed 1000 characters")
    private String description;
}