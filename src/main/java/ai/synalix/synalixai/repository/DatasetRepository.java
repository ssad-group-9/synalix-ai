package ai.synalix.synalixai.repository;

import ai.synalix.synalixai.entity.Dataset;
import ai.synalix.synalixai.enums.DatasetStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for Dataset entity
 * Provides database access for dataset operations
 */
@Repository
public interface DatasetRepository extends JpaRepository<Dataset, UUID> {

    /**
     * Find all datasets owned by a specific user
     *
     * @param ownerId the owner's user ID
     * @return list of datasets owned by the user
     */
    List<Dataset> findByOwnerId(UUID ownerId);

    /**
     * Find all datasets owned by a user with a specific status
     *
     * @param ownerId the owner's user ID
     * @param status  the dataset status
     * @return list of matching datasets
     */
    List<Dataset> findByOwnerIdAndStatus(UUID ownerId, DatasetStatus status);

    /**
     * Find a dataset by ID and owner ID
     *
     * @param id      the dataset ID
     * @param ownerId the owner's user ID
     * @return optional containing the dataset if found
     */
    Optional<Dataset> findByIdAndOwnerId(UUID id, UUID ownerId);

    /**
     * Find a dataset by name and owner ID
     *
     * @param name    the dataset name
     * @param ownerId the owner's user ID
     * @return optional containing the dataset if found
     */
    Optional<Dataset> findByNameAndOwnerId(String name, UUID ownerId);

    /**
     * Check if a dataset with the given name exists for the owner
     *
     * @param name    the dataset name
     * @param ownerId the owner's user ID
     * @return true if exists, false otherwise
     */
    boolean existsByNameAndOwnerId(String name, UUID ownerId);

    /**
     * Find a dataset by its storage key
     *
     * @param storageKey the MinIO storage key
     * @return optional containing the dataset if found
     */
    Optional<Dataset> findByStorageKey(String storageKey);
}