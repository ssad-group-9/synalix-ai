package ai.synalix.synalixai.dto.message;

import ai.synalix.synalixai.enums.MessageVisibility;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.UUID;

/**
 * Create message request DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateMessageRequest {

    @NotBlank(message = "Message type cannot be blank")
    @Size(max = 100, message = "Message type cannot exceed 100 characters")
    private String messageType;

    @NotBlank(message = "Message content cannot be blank")
    @Size(max = 2000, message = "Message content cannot exceed 2000 characters")
    @ToString.Exclude
    private String messageContent;

    @NotNull(message = "Message visibility cannot be null")
    private MessageVisibility visibility;

    private UUID targetUserId;
}
