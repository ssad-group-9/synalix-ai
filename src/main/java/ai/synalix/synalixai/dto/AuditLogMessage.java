package ai.synalix.synalixai.dto;

import ai.synalix.synalixai.enums.AuditOperationType;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Map;
import java.util.UUID;

/**
 * DTO for audit log messages sent via RabbitMQ
 */
@Data
@NoArgsConstructor
public class AuditLogMessage implements Serializable {
    
    private AuditOperationType operationType;
    private UUID userId;
    private String resourceId;
    private Map<String, Object> details;
    
    public AuditLogMessage(AuditOperationType operationType, UUID userId, String resourceId, Map<String, Object> details) {
        this.operationType = operationType;
        this.userId = userId;
        this.resourceId = resourceId;
        this.details = details;
    }
}