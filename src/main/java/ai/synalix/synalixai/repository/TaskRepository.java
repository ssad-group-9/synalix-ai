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

    /**
     * Find tasks by status
     *
     * @param status the status of the tasks to find
     * @return a list of tasks with the specified status
     */
    List<Task> findByStatus(ai.synalix.synalixai.enums.TaskStatus status);

    /**
     * Find tasks by type
     *
     * @param type the type of the tasks to find
     * @return a list of tasks with the specified type
     */
    List<Task> findByType(ai.synalix.synalixai.enums.TaskType type);

    /**
     * Find tasks by status and type
     *
     * @param status the status of the tasks to find
     * @param type the type of the tasks to find
     * @return a list of tasks with the specified status and type
     */
    List<Task> findByStatusAndType(ai.synalix.synalixai.enums.TaskStatus status, ai.synalix.synalixai.enums.TaskType type);
}
