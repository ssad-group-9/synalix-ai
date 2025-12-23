package ai.synalix.synalixai.service;

import ai.synalix.synalixai.entity.Message;
import ai.synalix.synalixai.enums.ApiErrorCode;
import ai.synalix.synalixai.enums.AuditOperationType;
import ai.synalix.synalixai.enums.MessageVisibility;
import ai.synalix.synalixai.exception.ApiException;
import ai.synalix.synalixai.repository.MessageRepository;
import ai.synalix.synalixai.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Message service for message management
 */
@Service
public class MessageService {

    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;

    @Autowired
    public MessageService(MessageRepository messageRepository,
                          UserRepository userRepository,
                          AuditService auditService) {
        this.messageRepository = messageRepository;
        this.userRepository = userRepository;
        this.auditService = auditService;
    }

    /**
     * Create a new message
     */
    @Transactional
    public Message createMessage(String messageType, String messageContent, MessageVisibility visibility, UUID targetUserId, UUID operatorId) {
        if (visibility == MessageVisibility.PRIVATE && targetUserId == null) {
            throw new ApiException(ApiErrorCode.MISSING_REQUIRED_FIELD, "Target userId is required for private message");
        }

        if (visibility == MessageVisibility.PRIVATE && !userRepository.existsById(targetUserId)) {
            throw new ApiException(ApiErrorCode.USER_NOT_FOUND, Map.of("userId", targetUserId));
        }

        var message = new Message();
        message.setMessageType(messageType);
        message.setMessageContent(messageContent);
        message.setVisibility(visibility);
        message.setTargetUserId(targetUserId);

        var saved = messageRepository.save(message);

        auditService.logAsync(
            AuditOperationType.MESSAGE_CREATE,
            operatorId,
            saved.getId().toString(),
            Map.of(
                "type", saved.getMessageType(),
                "visibility", saved.getVisibility().name(),
                "targetUserId", saved.getTargetUserId() != null ? saved.getTargetUserId().toString() : ""
            )
        );

        return saved;
    }

    /**
     * Get messages for requester
     */
    public List<Message> getMessages(UUID requesterId, boolean includeAll) {
        if (includeAll) {
            return messageRepository.findAllByOrderByCreatedAtDesc();
        }
        return messageRepository.findAllByVisibilityOrTargetUserIdOrderByCreatedAtDesc(MessageVisibility.PUBLIC, requesterId);
    }

    /**
     * Get a single message with access check
     */
    public Message getMessage(UUID messageId, UUID requesterId, boolean isAdmin) {
        var message = messageRepository.findById(messageId)
            .orElseThrow(() -> new ApiException(ApiErrorCode.MESSAGE_NOT_FOUND, Map.of("messageId", messageId)));

        if (isAdmin) {
            return message;
        }

        if (message.getVisibility() == MessageVisibility.PUBLIC) {
            return message;
        }

        if (message.getTargetUserId() != null && message.getTargetUserId().equals(requesterId)) {
            return message;
        }

        throw new ApiException(ApiErrorCode.ACCESS_DENIED);
    }

    /**
     * Delete a message
     */
    @Transactional
    public void deleteMessage(UUID messageId, UUID operatorId) {
        var message = messageRepository.findById(messageId)
            .orElseThrow(() -> new ApiException(ApiErrorCode.MESSAGE_NOT_FOUND, Map.of("messageId", messageId)));

        messageRepository.delete(message);

        auditService.logAsync(
            AuditOperationType.MESSAGE_DELETE,
            operatorId,
            messageId.toString(),
            Map.of(
                "type", message.getMessageType(),
                "visibility", message.getVisibility().name(),
                "targetUserId", message.getTargetUserId() != null ? message.getTargetUserId().toString() : ""
            )
        );
    }
}
