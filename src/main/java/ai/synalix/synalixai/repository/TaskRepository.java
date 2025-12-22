package ai.synalix.synalixai.repository;

import ai.synalix.synalixai.entity.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Task repository interface
 */
@Repository
public interface TaskRepository extends JpaRepository<Task, UUID> {
    
    /**
     * Find tasks created by a specific user
     */
    List<Task> findByCreatedBy(UUID createdBy);

    List<Task> findByStatus(ai.synalix.synalixai.enums.TaskStatus status);

    List<Task> findByType(ai.synalix.synalixai.enums.TaskType type);

    List<Task> findByStatusAndType(ai.synalix.synalixai.enums.TaskStatus status, ai.synalix.synalixai.enums.TaskType type);
}
