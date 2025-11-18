package ai.synalix.synalixai.dto.audit;

import ai.synalix.synalixai.enums.AuditOperationType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;
import java.util.UUID;

/**
 * Audit log message DTO for RabbitMQ
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuditLogMessage {

    private AuditOperationType operationType;
    private UUID userId;
    private String resourceId;
    private Map<String, Object> eventDescription;
}