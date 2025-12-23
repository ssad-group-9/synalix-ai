package ai.synalix.synalixai.dto.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 后端 /api/checkpoints 查询请求
 */
@Data
@NoArgsConstructor
public class CheckpointQueryRequest {

    @NotBlank(message = "model_name cannot be blank")
    @JsonProperty("model_name")
    private String modelName;
}