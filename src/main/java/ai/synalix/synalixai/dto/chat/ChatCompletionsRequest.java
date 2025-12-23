package ai.synalix.synalixai.dto.chat;

import com.fasterxml.jackson.annotation.JsonProperty;
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

    @JsonProperty("task_id")
    private String taskId;

    @NotNull
    @Valid
    @JsonProperty("completions")
    private Completions completions;

    @Data
    @NoArgsConstructor
    public static class Completions {
        @NotBlank
        private String model;

        @NotNull
        @Valid
        private List<Message> messages;

        @JsonProperty("max_completion_tokens")
        private Integer maxCompletionTokens;
    }

    @Data
    @NoArgsConstructor
    public static class Message {
        @NotBlank
        private String role;

        @NotBlank
        private String content;
    }
}