package ai.synalix.synalixai.service;

import ai.synalix.synalixai.dto.audit.AuditLogMessage;
import ai.synalix.synalixai.entity.AuditLog;
import ai.synalix.synalixai.enums.AuditOperationType;
import ai.synalix.synalixai.repository.AuditLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

/**
 * Audit service for logging system operations
 */
@Service
public class AuditService {

    private static final Logger logger = LoggerFactory.getLogger(AuditService.class);

    private final RabbitTemplate rabbitTemplate;
    private final AuditLogRepository auditLogRepository;

    @Value("${audit.exchange.name}")
    private String auditExchangeName;

    @Value("${audit.routing.key}")
    private String auditRoutingKey;

    @Autowired
    public AuditService(RabbitTemplate rabbitTemplate, AuditLogRepository auditLogRepository) {
        this.rabbitTemplate = rabbitTemplate;
        this.auditLogRepository = auditLogRepository;
    }

    /**
     * Asynchronously submit audit log via RabbitMQ
     */
    public void logAsync(AuditOperationType operationType, UUID userId, String resourceId, Map<String, Object> eventDescription) {
        try {
            var message = new AuditLogMessage(operationType, userId, resourceId, eventDescription);
            rabbitTemplate.convertAndSend(auditExchangeName, auditRoutingKey, message);
            logger.debug("Audit log message sent to queue: {}", message);
        } catch (Exception e) {
            logger.error("Failed to send audit log message to queue", e);
            // Fallback: log directly to database
            logDirect(operationType, userId, resourceId, eventDescription);
        }
    }

    /**
     * Directly log to database (synchronous)
     */
    @Transactional
    public void logDirect(AuditOperationType operationType, UUID userId, String resourceId, Map<String, Object> eventDescription) {
        try {
            var auditLog = new AuditLog();
            auditLog.setOperationType(operationType);
            auditLog.setUserId(userId);
            auditLog.setResourceId(resourceId);
            auditLog.setDetails(eventDescription);
            auditLogRepository.save(auditLog);
            logger.debug("Audit log saved directly to database: {}", auditLog);
        } catch (Exception e) {
            logger.error("Failed to save audit log to database", e);
        }
    }

    /**
     * Log user authentication events
     */
    public void logUserAuthentication(AuditOperationType operationType, UUID userId, String username, boolean success) {
        Map<String, Object> eventDescription = Map.of(
                "success", success
        );
        logAsync(operationType, userId, userId.toString(), eventDescription);
    }

    /**
     * Log user management events
     */
    public void logUserManagement(AuditOperationType operationType, UUID operatorId, UUID targetUserId, Map<String, Object> changes) {
        Map<String, Object> eventDescription = Map.of(
                "changes", changes
        );
        logAsync(operationType, operatorId, targetUserId.toString(), eventDescription);
    }

    /**
     * Log password events
     */
    public void logPasswordEvent(AuditOperationType operationType, UUID userId, String username, boolean success) {
        Map<String, Object> eventDescription = Map.of(
                "success", success
        );
        logAsync(operationType, userId, userId.toString(), eventDescription);
    }

    /**
     * Log token events
     */
    public void logTokenEvent(AuditOperationType operationType, UUID userId, String tokenType, String action) {
        Map<String, Object> eventDescription = Map.of(
                "tokenType", tokenType,
                "action", action
        );
        logAsync(operationType, userId, null, eventDescription);
    }
}