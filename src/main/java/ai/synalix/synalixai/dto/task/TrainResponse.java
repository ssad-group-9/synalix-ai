package ai.synalix.synalixai.dto.task;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;
import java.util.Map;

/**
 * Response DTO for /api/train
 */
@Data
@NoArgsConstructor
public class TrainResponse {

    @JsonProperty("message")
    private String message;

    @JsonProperty("gpu_allocated")
    private List<Integer> gpuAllocated;

    @JsonProperty("request")
    private Request request;

    @Data
    @NoArgsConstructor
    public static class Request {
        @JsonProperty("gpu_count")
        private Integer gpuCount;

        @JsonProperty("training_config")
        private Map<String, Object> trainingConfig;

        @JsonProperty("task_id")
        private String taskId;

        @JsonProperty("task_type")
        private String taskType;

        @JsonProperty("gpu_ids")
        private List<Integer> gpuIds;

        @JsonProperty("model_path")
        private String modelPath;

        @JsonProperty("output_dir")
        private String outputDir;

        @JsonProperty("config_path")
        private String configPath;
    }
}