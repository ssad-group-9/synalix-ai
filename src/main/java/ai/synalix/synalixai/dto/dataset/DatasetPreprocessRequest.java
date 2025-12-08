package ai.synalix.synalixai.dto.dataset;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for dataset preprocessing
 * Used to configure train/test/eval split ratios
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DatasetPreprocessRequest {

    /**
     * Training set ratio (0.0 - 1.0)
     */
    @NotNull(message = "Train ratio cannot be null")
    @DecimalMin(value = "0.0", message = "Train ratio must be at least 0.0")
    @DecimalMax(value = "1.0", message = "Train ratio must be at most 1.0")
    private Double trainRatio;

    /**
     * Test set ratio (0.0 - 1.0)
     */
    @NotNull(message = "Test ratio cannot be null")
    @DecimalMin(value = "0.0", message = "Test ratio must be at least 0.0")
    @DecimalMax(value = "1.0", message = "Test ratio must be at most 1.0")
    private Double testRatio;

    /**
     * Evaluation set ratio (0.0 - 1.0)
     */
    @NotNull(message = "Eval ratio cannot be null")
    @DecimalMin(value = "0.0", message = "Eval ratio must be at least 0.0")
    @DecimalMax(value = "1.0", message = "Eval ratio must be at most 1.0")
    private Double evalRatio;
}