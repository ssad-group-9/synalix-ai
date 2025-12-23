package ai.synalix.synalixai.dto.task;

import ai.synalix.synalixai.enums.TaskStatus;
import ai.synalix.synalixai.enums.TaskType;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Response DTO for task details
 */
@Data
public class TaskResponse {
    private UUID id;
    private String name;
    private TaskType type;
    private TaskStatus status;
    private UUID modelId;
    private UUID datasetId;
    private Map<String, Object> config;
    private UUID createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
