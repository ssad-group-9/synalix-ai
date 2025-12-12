package ai.synalix.synalixai.dto.dataset;

import java.lang.annotation.Documented;
import java.lang.annotation.Target;
import java.lang.annotation.Retention;

import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.Payload;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

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

    /**
     * Class-level validation annotation to ensure train/test/eval ratios sum to 1.0
     */
    @Target({TYPE})
    @Retention(RUNTIME)
    @Constraint(validatedBy = ValidRatiosValidator.class)
    @Documented
    public @interface ValidRatios {
        String message() default "Train, test, and eval ratios must sum to 1.0";
        Class<?>[] groups() default {};
        Class<? extends Payload>[] payload() default {};
    }
    
    /**
     * Validator for the ValidRatios annotation
     */
    public static class ValidRatiosValidator implements ConstraintValidator<ValidRatios, DatasetPreprocessRequest> {
        
        @Override
        public boolean isValid(DatasetPreprocessRequest request, ConstraintValidatorContext context) {
            if (request.getTrainRatio() == null || request.getTestRatio() == null || request.getEvalRatio() == null) {
                return true; // Let field-level validations handle null values
            }
            
            double sum = request.getTrainRatio() + request.getTestRatio() + request.getEvalRatio();
            return Math.abs(sum - 1.0) < 0.0001; // Allow small floating point differences
        }
    }
}