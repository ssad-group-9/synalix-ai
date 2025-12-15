package ai.synalix.synalixai.controller;

import ai.synalix.synalixai.config.JwtUserPrincipal;
import ai.synalix.synalixai.dto.message.CreateMessageRequest;
import ai.synalix.synalixai.dto.message.MessageResponse;
import ai.synalix.synalixai.entity.Message;
import ai.synalix.synalixai.enums.UserRole;
import ai.synalix.synalixai.service.MessageService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Message REST controller
 */
@RestController
@RequestMapping("/api/messages")
public class MessageController {

    private final MessageService messageService;

    @Autowired
    public MessageController(MessageService messageService) {
        this.messageService = messageService;
    }

    /**
     * Create message (Admin only)
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<MessageResponse> createMessage(
        @Valid @RequestBody CreateMessageRequest request,
        @AuthenticationPrincipal JwtUserPrincipal principal
    ) {
        var operatorId = principal.getId();

        var created = messageService.createMessage(
            request.getMessageType(),
            request.getMessageContent(),
            request.getVisibility(),
            request.getTargetUserId(),
            operatorId
        );

        var response = convertToResponse(created);
        return ResponseEntity.ok(response);
    }

    /**
     * Get messages visible to current user; admins can request all
     */
    @GetMapping
    public ResponseEntity<List<MessageResponse>> getMessages(
        @AuthenticationPrincipal JwtUserPrincipal principal,
        @RequestParam(name = "all", defaultValue = "false") boolean all
    ) {
        var requesterId = principal.getId();
        var includeAll = all && UserRole.ADMIN.name().equals(principal.getRole());

        var messages = messageService.getMessages(requesterId, includeAll);
        var responses = messages.stream()
            .map(this::convertToResponse)
            .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    /**
     * Get message by ID with access control
     */
    @GetMapping("/{messageId}")
    public ResponseEntity<MessageResponse> getMessage(
        @PathVariable("messageId") UUID messageId,
        @AuthenticationPrincipal JwtUserPrincipal principal
    ) {
        var requesterId = principal.getId();
        var isAdmin = UserRole.ADMIN.name().equals(principal.getRole());

        var message = messageService.getMessage(messageId, requesterId, isAdmin);
        var response = convertToResponse(message);
        return ResponseEntity.ok(response);
    }

    /**
     * Delete message (Admin only)
     */
    @DeleteMapping("/{messageId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteMessage(
        @PathVariable("messageId") UUID messageId,
        @AuthenticationPrincipal JwtUserPrincipal principal
    ) {
        var operatorId = principal.getId();
        messageService.deleteMessage(messageId, operatorId);
        return ResponseEntity.ok(null);
    }

    /**
     * Convert Message entity to response DTO
     */
    private MessageResponse convertToResponse(Message message) {
        return new MessageResponse(
            message.getId(),
            message.getMessageType(),
            message.getMessageContent(),
            message.getVisibility(),
            message.getTargetUserId(),
            message.getCreatedAt()
        );
    }
}
