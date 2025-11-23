package ai.synalix.synalixai.dto.audit;

import ai.synalix.synalixai.enums.AuditOperationType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Audit log response DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuditLogResponse {

    private UUID id;
    private String username;
    private AuditOperationType operationType;
    private String resourceType;
    private String resourceId;
    private Map<String, Object> details;
    private LocalDateTime timestamp;
}