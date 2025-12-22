package ai.synalix.synalixai.dto.task;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO for task metrics
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TaskMetricsResponse {
    private UUID taskId;
    private Integer epoch;
    private Double loss;
    private Double accuracy;
    private LocalDateTime timestamp;
}
