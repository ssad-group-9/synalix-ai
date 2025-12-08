package ai.synalix.synalixai.repository;

import ai.synalix.synalixai.entity.BaseModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for BaseModel entity
 * Provides database access for base model operations
 */
@Repository
public interface BaseModelRepository extends JpaRepository<BaseModel, UUID> {

    /**
     * Find a base model by its display name
     *
     * @param name the model name
     * @return optional containing the model if found
     */
    Optional<BaseModel> findByName(String name);

    /**
     * Find a base model by its HuggingFace model ID
     *
     * @param huggingfaceModelId the HuggingFace model identifier
     * @return optional containing the model if found
     */
    Optional<BaseModel> findByHuggingfaceModelId(String huggingfaceModelId);

    /**
     * Check if a model with the given name exists
     *
     * @param name the model name
     * @return true if exists, false otherwise
     */
    boolean existsByName(String name);

    /**
     * Check if a model with the given HuggingFace model ID exists
     *
     * @param huggingfaceModelId the HuggingFace model identifier
     * @return true if exists, false otherwise
     */
    boolean existsByHuggingfaceModelId(String huggingfaceModelId);

    /**
     * Find all enabled base models
     *
     * @return list of enabled base models
     */
    List<BaseModel> findByEnabledTrue();
}