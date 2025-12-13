package ai.synalix.synalixai.service;

import ai.synalix.synalixai.entity.Message;
import ai.synalix.synalixai.enums.ApiErrorCode;
import ai.synalix.synalixai.enums.AuditOperationType;
import ai.synalix.synalixai.enums.MessageVisibility;
import ai.synalix.synalixai.exception.ApiException;
import ai.synalix.synalixai.repository.MessageRepository;
import ai.synalix.synalixai.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for MessageService
 */
@ExtendWith(MockitoExtension.class)
class MessageServiceTest {

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AuditService auditService;

    @InjectMocks
    private MessageService messageService;

    /**
     * Should throw when private message lacks target user
     */
    @Test
    void createMessage_privateWithoutTarget_throwsMissingField() {
        var operatorId = UUID.randomUUID();

        var ex = assertThrows(ApiException.class, () -> messageService.createMessage(
            "ALERT",
            "content",
            MessageVisibility.PRIVATE,
            null,
            operatorId
        ));

        assertEquals(ApiErrorCode.MISSING_REQUIRED_FIELD, ex.getErrorCode());
    }

    /**
     * Should throw when private message target user does not exist
     */
    @Test
    void createMessage_privateInvalidUser_throwsUserNotFound() {
        var operatorId = UUID.randomUUID();
        var targetUserId = UUID.randomUUID();
        when(userRepository.existsById(targetUserId)).thenReturn(false);

        var ex = assertThrows(ApiException.class, () -> messageService.createMessage(
            "ALERT",
            "content",
            MessageVisibility.PRIVATE,
            targetUserId,
            operatorId
        ));

        assertEquals(ApiErrorCode.USER_NOT_FOUND, ex.getErrorCode());
    }

    /**
     * Should create public message successfully
     */
    @Test
    void createMessage_public_success() {
        var operatorId = UUID.randomUUID();
        var savedId = UUID.randomUUID();
        var saved = new Message();
        saved.setId(savedId);
        saved.setMessageType("INFO");
        saved.setMessageContent("hello");
        saved.setVisibility(MessageVisibility.PUBLIC);
        when(messageRepository.save(any(Message.class))).thenReturn(saved);
        doNothing().when(auditService).logAsync(eq(AuditOperationType.MESSAGE_CREATE), eq(operatorId), eq(savedId.toString()), anyMap());

        var result = messageService.createMessage("INFO", "hello", MessageVisibility.PUBLIC, null, operatorId);

        assertEquals(savedId, result.getId());
        verify(messageRepository).save(any(Message.class));
        verify(auditService).logAsync(eq(AuditOperationType.MESSAGE_CREATE), eq(operatorId), eq(savedId.toString()), anyMap());
    }

    /**
     * Should return all messages when includeAll is true
     */
    @Test
    void getMessages_includeAll_returnsAll() {
        var requesterId = UUID.randomUUID();
        var messages = List.of(new Message(), new Message());
        when(messageRepository.findAllByOrderByCreatedAtDesc()).thenReturn(messages);

        var result = messageService.getMessages(requesterId, true);

        assertEquals(2, result.size());
        verify(messageRepository).findAllByOrderByCreatedAtDesc();
    }

    /**
     * Should return public and targeted messages for non-admin
     */
    @Test
    void getMessages_filtered_returnsVisible() {
        var requesterId = UUID.randomUUID();
        var messages = List.of(new Message());
        when(messageRepository.findAllByVisibilityOrTargetUserIdOrderByCreatedAtDesc(MessageVisibility.PUBLIC, requesterId))
            .thenReturn(messages);

        var result = messageService.getMessages(requesterId, false);

        assertEquals(1, result.size());
        verify(messageRepository).findAllByVisibilityOrTargetUserIdOrderByCreatedAtDesc(MessageVisibility.PUBLIC, requesterId);
    }

    /**
     * Should allow reading public message
     */
    @Test
    void getMessage_public_ok() {
        var requesterId = UUID.randomUUID();
        var messageId = UUID.randomUUID();
        var message = new Message();
        message.setId(messageId);
        message.setVisibility(MessageVisibility.PUBLIC);
        when(messageRepository.findById(messageId)).thenReturn(Optional.of(message));

        var result = messageService.getMessage(messageId, requesterId, false);

        assertEquals(messageId, result.getId());
    }

    /**
     * Should allow reading private message when requester is target
     */
    @Test
    void getMessage_privateTarget_ok() {
        var requesterId = UUID.randomUUID();
        var messageId = UUID.randomUUID();
        var message = new Message();
        message.setId(messageId);
        message.setVisibility(MessageVisibility.PRIVATE);
        message.setTargetUserId(requesterId);
        when(messageRepository.findById(messageId)).thenReturn(Optional.of(message));

        var result = messageService.getMessage(messageId, requesterId, false);

        assertEquals(messageId, result.getId());
    }

    /**
     * Should deny reading private message when requester is not target and not admin
     */
    @Test
    void getMessage_privateNotOwner_denied() {
        var requesterId = UUID.randomUUID();
        var messageId = UUID.randomUUID();
        var message = new Message();
        message.setId(messageId);
        message.setVisibility(MessageVisibility.PRIVATE);
        message.setTargetUserId(UUID.randomUUID());
        when(messageRepository.findById(messageId)).thenReturn(Optional.of(message));

        var ex = assertThrows(ApiException.class, () -> messageService.getMessage(messageId, requesterId, false));

        assertEquals(ApiErrorCode.ACCESS_DENIED, ex.getErrorCode());
    }

    /**
     * Should delete message and write audit
     */
    @Test
    void deleteMessage_success() {
        var operatorId = UUID.randomUUID();
        var messageId = UUID.randomUUID();
        var message = new Message();
        message.setId(messageId);
        message.setMessageType("INFO");
        message.setVisibility(MessageVisibility.PUBLIC);
        when(messageRepository.findById(messageId)).thenReturn(Optional.of(message));
        doNothing().when(auditService).logAsync(eq(AuditOperationType.MESSAGE_DELETE), eq(operatorId), eq(messageId.toString()), anyMap());

        messageService.deleteMessage(messageId, operatorId);

        verify(messageRepository).delete(message);
        verify(auditService).logAsync(eq(AuditOperationType.MESSAGE_DELETE), eq(operatorId), eq(messageId.toString()), anyMap());
    }
}
