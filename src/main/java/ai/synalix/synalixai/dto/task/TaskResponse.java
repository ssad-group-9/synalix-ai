package ai.synalix.synalixai.dto.task;

import ai.synalix.synalixai.enums.TaskStatus;
import ai.synalix.synalixai.enums.TaskType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Response DTO for task details
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
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
