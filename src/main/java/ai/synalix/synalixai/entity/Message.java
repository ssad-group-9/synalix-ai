package ai.synalix.synalixai.entity;

import ai.synalix.synalixai.enums.MessageVisibility;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Message entity class
 */
@Entity
@Table(name = "messages")
@Data
@NoArgsConstructor
public class Message {

    @Id
    @UuidGenerator
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @NotBlank(message = "Message type cannot be blank")
    @Size(max = 100, message = "Message type cannot exceed 100 characters")
    @Column(name = "message_type", nullable = false, length = 100)
    private String messageType;

    @NotBlank(message = "Message content cannot be blank")
    @Size(max = 2000, message = "Message content cannot exceed 2000 characters")
    @Column(name = "message_content", nullable = false, length = 2000)
    private String messageContent;

    @Enumerated(EnumType.STRING)
    @Column(name = "visibility", nullable = false, length = 20)
    private MessageVisibility visibility = MessageVisibility.PUBLIC;

    @Column(name = "target_user_id")
    private UUID targetUserId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
