package ai.synalix.synalixai.dto.message;

import ai.synalix.synalixai.enums.MessageVisibility;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Message response DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MessageResponse {

    private UUID id;
    private String messageType;
    private String messageContent;
    private MessageVisibility visibility;
    private UUID targetUserId;
    private LocalDateTime createdAt;
}
