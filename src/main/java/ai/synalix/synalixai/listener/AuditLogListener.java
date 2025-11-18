package ai.synalix.synalixai.listener;

import ai.synalix.synalixai.dto.AuditLogMessage;
import ai.synalix.synalixai.entity.AuditLog;
import ai.synalix.synalixai.repository.AuditLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * RabbitMQ listener for processing audit log messages
 */
@Component
public class AuditLogListener {

    private static final Logger logger = LoggerFactory.getLogger(AuditLogListener.class);

    private final AuditLogRepository auditLogRepository;

    public AuditLogListener(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    /**
     * Process audit log messages from RabbitMQ queue
     */
    @RabbitListener(queues = "${audit.queue.name}")
    @Transactional
    public void handleAuditLogMessage(AuditLogMessage message) {
        try {
            logger.debug("Processing audit log message: {}", message);

            // Convert DTO to entity
            AuditLog auditLog = new AuditLog();
            auditLog.setOperationType(message.getOperationType());
            auditLog.setUserId(message.getUserId());
            auditLog.setResourceId(message.getResourceId());
            auditLog.setDetails(message.getDetails());

            // Save to database
            auditLogRepository.save(auditLog);
            
            logger.info("Audit log processed successfully: operation={}, userId={}, resourceId={}", 
                    message.getOperationType(), message.getUserId(), message.getResourceId());

        } catch (Exception e) {
            logger.error("Failed to process audit log message: {}", message, e);
            // Note: In production, you might want to implement dead letter queue handling
            throw e; // Re-throw to trigger retry mechanism if configured
        }
    }
}