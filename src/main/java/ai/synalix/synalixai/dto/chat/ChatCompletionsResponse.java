package ai.synalix.synalixai.dto.chat;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 响应 DTO：后端 /api/chat/completions 返回结构
 */
@Data
@NoArgsConstructor
public class ChatCompletionsResponse {

    private String message;

    @JsonProperty("sent_to_port")
    private String sentToPort;

    private Response response;

    @Data
    @NoArgsConstructor
    public static class Response {
        private String id;
        private String object;
        private Long created;
        private String model;
        private List<Choice> choices;
        private Usage usage;
    }

    @Data
    @NoArgsConstructor
    public static class Choice {
        private Integer index;
        private Message message;

        @JsonProperty("finish_reason")
        private String finishReason;
    }

    /**
     * Message supports:
     * - "content": "plain string"
     * - "content": [ { "type": "text", ... }, { "type": "image_url", ... } ]
     */
    @Data
    @NoArgsConstructor
    public static class Message {
        private String role;

        /**
         * Multi-modal content. Could be text string or an array of content parts.
         */
        private JsonNode content;

        @JsonProperty("tool_calls")
        private Object toolCalls;
    }

    @Data
    @NoArgsConstructor
    public static class Usage {
        @JsonProperty("prompt_tokens")
        private Integer promptTokens;

        @JsonProperty("completion_tokens")
        private Integer completionTokens;

        @JsonProperty("total_tokens")
        private Integer totalTokens;
    }
}