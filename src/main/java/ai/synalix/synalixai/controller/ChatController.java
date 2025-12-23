package ai.synalix.synalixai.controller;

import ai.synalix.synalixai.dto.chat.ChatCompletionsRequest;
import ai.synalix.synalixai.dto.chat.ChatCompletionsResponse;
import ai.synalix.synalixai.config.JwtUserPrincipal;
import ai.synalix.synalixai.service.ChatService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

/**
 * Chat 控制器：对外提供 /api/chat/completions
 */
@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    /**
     * 前端提交 id 与 completions，转发到后端并返回结果
     */
    @PostMapping("/{id}/completions")
    public ResponseEntity<ChatCompletionsResponse> chatCompletions(
            @Valid @RequestBody ChatCompletionsRequest request,
            @PathVariable UUID id,
            @AuthenticationPrincipal JwtUserPrincipal principal) {

        // 可选：基于 principal.getId() 做权限/所属校验
        var resp = chatService.chat(request, id);
        return ResponseEntity.ok(resp);
    }
}