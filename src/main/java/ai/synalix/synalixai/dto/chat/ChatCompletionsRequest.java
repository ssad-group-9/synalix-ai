package ai.synalix.synalixai.dto.chat;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 请求 DTO：转发到后端 /api/chat/completions
 */
@Data
@NoArgsConstructor
public class ChatCompletionsRequest {

    /**
     * Backend task id
     */
    @NotBlank(message = "task_id cannot be blank")
    @JsonProperty("task_id")
    private String taskId;

    @NotNull
    @Valid
    @JsonProperty("completions")
    private Completions completions;

    /**
     * Completions payload
     */
    @Data
    @NoArgsConstructor
    public static class Completions {

        @NotBlank(message = "model cannot be blank")
        private String model;

        @NotNull
        @Valid
        private List<Message> messages;

        @JsonProperty("max_completion_tokens")
        private Integer maxCompletionTokens;
    }

    /**
     * Chat message supports:
     * - content as string
     * - content as array of parts (text/image_url) in JSON
     */
    @Data
    @NoArgsConstructor
    public static class Message {

        @NotBlank(message = "role cannot be blank")
        private String role;

        @NotNull(message = "content cannot be null")
        private JsonNode content;
    }
}