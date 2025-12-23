package ai.synalix.synalixai.repository;

import ai.synalix.synalixai.entity.Checkpoint;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/**
 * Repository for checkpoints.
 */
public interface CheckpointRepository extends JpaRepository<Checkpoint, UUID> {
    List<Checkpoint> findByModelId(UUID modelId);
}