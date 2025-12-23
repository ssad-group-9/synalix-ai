package ai.synalix.synalixai.dto.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 后端 /api/checkpoints 返回结构
 *
 * {
 * "model_name": "...",
 * "tasks": {
 * "lora": { "taskId": ["path1","path2"] },
 * "full": { "taskId": ["path3"] }
 * }
 * }
 */
@Data
@NoArgsConstructor
public class BackendCheckpointsResponse {

    @JsonProperty("model_name")
    private String modelName;

    // key: 类型(lora/full) -> value: map(taskId -> 路径列表)
    @JsonProperty("tasks")
    private Map<String, Map<String, List<String>>> tasks;
}