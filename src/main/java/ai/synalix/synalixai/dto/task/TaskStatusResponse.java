package ai.synalix.synalixai.dto.task;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.Map;

/**
 * Response DTO for GET /api/tasks?task_id=...
 * Contains list of task statuses from backend
 */
@Data
@NoArgsConstructor
public class TaskStatusResponse {

    @JsonProperty("request")
    private Map<String, Object> request;

    @JsonProperty("status")
    private String status;

    @JsonProperty("return_code")
    private Integer returnCode;

    @JsonProperty("message")
    private String message;

    @JsonProperty("update_time")
    private String updateTime;
}