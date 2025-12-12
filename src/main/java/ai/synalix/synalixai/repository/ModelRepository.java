package ai.synalix.synalixai.repository;

import ai.synalix.synalixai.entity.Model;
import ai.synalix.synalixai.enums.ModelType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for Model entity
 * Provides database access for model operations
 */
@Repository
public interface ModelRepository extends JpaRepository<Model, UUID> {

    /**
     * Find a model by its display name
     *
     * @param name the model name
     * @return optional containing the model if found
     */
    Optional<Model> findByName(String name);

    /**
     * Check if a model with the given name exists
     *
     * @param name the model name
     * @return true if exists, false otherwise
     */
    boolean existsByName(String name);

    /**
     * Find all models by type
     *
     * @param type the model type
     * @return list of models of the given type
     */
    List<Model> findByType(ModelType type);
}