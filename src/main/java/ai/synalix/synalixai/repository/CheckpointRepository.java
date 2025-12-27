package ai.synalix.synalixai.repository;

import ai.synalix.synalixai.entity.Checkpoint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

/**
 * Repository for checkpoints.
 */
public interface CheckpointRepository extends JpaRepository<Checkpoint, UUID> {
    List<Checkpoint> findByModelId(UUID modelId);
    @Modifying
    @Query("DELETE FROM Checkpoint c WHERE c.modelId = :modelId")
    void deleteByModelId(@Param("modelId") UUID modelId);
}