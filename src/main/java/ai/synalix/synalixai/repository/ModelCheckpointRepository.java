package ai.synalix.synalixai.repository;

import ai.synalix.synalixai.entity.ModelCheckpoint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for ModelCheckpoint entity
 * Provides database access for model checkpoint operations
 */
@Repository
public interface ModelCheckpointRepository extends JpaRepository<ModelCheckpoint, UUID> {

    /**
     * Find all checkpoints owned by a specific user
     *
     * @param ownerId the owner's user ID
     * @return list of checkpoints owned by the user
     */
    List<ModelCheckpoint> findByOwnerId(UUID ownerId);

    /**
     * Find a checkpoint by ID and owner ID
     *
     * @param id      the checkpoint ID
     * @param ownerId the owner's user ID
     * @return optional containing the checkpoint if found
     */
    Optional<ModelCheckpoint> findByIdAndOwnerId(UUID id, UUID ownerId);

    /**
     * Find a checkpoint by name and owner ID
     *
     * @param name    the checkpoint name
     * @param ownerId the owner's user ID
     * @return optional containing the checkpoint if found
     */
    Optional<ModelCheckpoint> findByNameAndOwnerId(String name, UUID ownerId);

    /**
     * Check if a checkpoint with the given name exists for the owner
     *
     * @param name    the checkpoint name
     * @param ownerId the owner's user ID
     * @return true if exists, false otherwise
     */
    boolean existsByNameAndOwnerId(String name, UUID ownerId);

    /**
     * Find all checkpoints for a specific base model
     *
     * @param baseModelId the base model ID
     * @return list of checkpoints for the base model
     */
    List<ModelCheckpoint> findByBaseModelId(UUID baseModelId);

    /**
     * Find all checkpoints for a specific dataset
     *
     * @param datasetId the dataset ID
     * @return list of checkpoints for the dataset
     */
    List<ModelCheckpoint> findByDatasetId(UUID datasetId);

    /**
     * Find a checkpoint by its storage key
     *
     * @param storageKey the MinIO storage key
     * @return optional containing the checkpoint if found
     */
    Optional<ModelCheckpoint> findByStorageKey(String storageKey);

    /**
     * Find all completed checkpoints owned by a user
     *
     * @param ownerId        the owner's user ID
     * @param uploadComplete whether the upload is complete
     * @return list of matching checkpoints
     */
    List<ModelCheckpoint> findByOwnerIdAndUploadComplete(UUID ownerId, boolean uploadComplete);
}