package ai.synalix.synalixai.dto.task;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO for task metrics
 */
@Data
public class TaskMetricsResponse {
    private UUID taskId;
    private Integer epoch;
    private Double loss;
    private Double accuracy;
    private LocalDateTime timestamp;
}
