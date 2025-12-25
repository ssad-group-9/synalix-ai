package ai.synalix.synalixai.dto.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for backend /api/checkpoints/download.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BackendCheckpointDownloadRequest {

    /**
     * Backend task id.
     */
    @NotBlank(message = "task_id cannot be blank")
    @JsonProperty("task_id")
    private String taskId;

    /**
     * Checkpoint id.
     */
    @NotBlank(message = "checkpoint_id cannot be blank")
    @JsonProperty("checkpoint_id")
    private String checkpointId;

    /**
     * MinIO presigned url (upload or download per backend contract).
     */
    @NotBlank(message = "url cannot be blank")
    @JsonProperty("url")
    private String url;
}