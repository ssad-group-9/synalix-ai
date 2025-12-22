package ai.synalix.synalixai.dto.task;

import lombok.Data;

import java.util.UUID;

/**
 * Response DTO for task chart
 */
@Data
public class TaskChartResponse {
    private UUID taskId;
    private String ChartUrl;
}
