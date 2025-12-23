package ai.synalix.synalixai.dto.task;

import ai.synalix.synalixai.enums.TaskType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Request DTO for creating a new task
 */
@Data
public class CreateTaskRequest {

    @NotBlank(message = "Task name is required")
    private String name;

    @NotNull(message = "Task type is required")
    private TaskType type;

    @NotNull(message = "Model ID is required")
    private UUID modelId;

    private UUID datasetId;

    private List<Integer> gpuIds;

    private Map<String, Object> config;
}
