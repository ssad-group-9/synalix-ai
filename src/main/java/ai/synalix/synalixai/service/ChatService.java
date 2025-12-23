package ai.synalix.synalixai.service;

import ai.synalix.synalixai.dto.chat.ChatCompletionsRequest;
import ai.synalix.synalixai.dto.chat.ChatCompletionsResponse;
import ai.synalix.synalixai.enums.ApiErrorCode;
import ai.synalix.synalixai.exception.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.util.UUID;

/**
 * Chat 服务：转发到后端 /api/chat/completions
 */
@Service
public class ChatService {

    private final RestTemplate restTemplate;
    private final TaskService taskService;

    @Value("${app.backend-base-url}")
    private String backendBaseUrl;

    public ChatService(RestTemplate restTemplate, TaskService taskService) {
        this.restTemplate = restTemplate;
        this.taskService = taskService;
    }

    /**
     * 调用后端 /api/chat/completions
     */
    public ChatCompletionsResponse chat(ChatCompletionsRequest req, UUID taskId) {

        var task = taskService.getTaskById(taskId);
        req.setTaskId(task.getExternalTaskId().toString());
        var url = backendBaseUrl.endsWith("/")
                ? backendBaseUrl + "api/chat/completions"
                : backendBaseUrl + "/api/chat/completions";

        try {
            return restTemplate.postForObject(url, req, ChatCompletionsResponse.class);
        } catch (Exception e) {
            throw new ApiException(ApiErrorCode.INTERNAL_SERVER_ERROR, "Chat completions failed");
        }
    }
}