package ai.synalix.synalixai.repository;

import ai.synalix.synalixai.entity.Message;
import ai.synalix.synalixai.enums.MessageVisibility;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/**
 * Message repository interface
 */
public interface MessageRepository extends JpaRepository<Message, UUID> {

    /**
     * Find all messages ordered by creation time in descending order
     */
    List<Message> findAllByOrderByCreatedAtDesc();

    /**
     * Find public messages or messages targeted to the specified user, ordered by creation time
     */
    List<Message> findAllByVisibilityOrTargetUserIdOrderByCreatedAtDesc(MessageVisibility visibility, UUID targetUserId);
}
