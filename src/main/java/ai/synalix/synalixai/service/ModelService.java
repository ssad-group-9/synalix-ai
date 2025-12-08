package ai.synalix.synalixai.service;

import ai.synalix.synalixai.dto.model.CreateModelRequest;
import ai.synalix.synalixai.dto.model.ModelResponse;
import ai.synalix.synalixai.entity.Model;
import ai.synalix.synalixai.enums.ApiErrorCode;
import ai.synalix.synalixai.enums.AuditOperationType;
import ai.synalix.synalixai.exception.ApiException;
import ai.synalix.synalixai.repository.ModelRepository;
import ai.synalix.synalixai.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Service for model management operations
 * Handles CRUD operations for models
 */
@Service
@Slf4j
public class ModelService {

    private final ModelRepository modelRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;

    @Autowired
    public ModelService(ModelRepository modelRepository,
                        UserRepository userRepository,
                        AuditService auditService) {
        this.modelRepository = modelRepository;
        this.userRepository = userRepository;
        this.auditService = auditService;
    }

    /**
     * Get all models
     *
     * @return list of all models
     */
    public List<ModelResponse> getAllModels() {
        return modelRepository.findAll()
                .stream()
                .map(this::convertToResponse)
                .toList();
    }

    /**
     * Get a model by ID
     *
     * @param id the model ID
     * @return the model response
     */
    public ModelResponse getModelById(UUID id) {
        var model = modelRepository.findById(id)
                .orElseThrow(() -> new ApiException(ApiErrorCode.MODEL_NOT_FOUND,
                        Map.of("modelId", id.toString())));
        return convertToResponse(model);
    }

    /**
     * Create a new model (admin only)
     *
     * @param request the create model request
     * @param adminId the admin user ID
     * @return the created model response
     */
    @Transactional
    public ModelResponse createModel(CreateModelRequest request, UUID adminId) {
        // Check if name already exists
        if (modelRepository.existsByName(request.getName())) {
            throw new ApiException(ApiErrorCode.MODEL_NAME_EXISTS,
                    Map.of("name", request.getName()));
        }

        var admin = userRepository.findById(adminId)
                .orElseThrow(() -> new ApiException(ApiErrorCode.USER_NOT_FOUND,
                        Map.of("userId", adminId.toString())));

        var model = new Model();
        model.setName(request.getName());
        model.setType(request.getType());
        model.setDescription(request.getDescription());
        model.setVersion(request.getVersion());
        model.setCreatedBy(admin);

        var savedModel = modelRepository.save(model);

        log.info("Model created: {} (type: {}, version: {}) by admin {}",
                savedModel.getName(), savedModel.getType(), savedModel.getVersion(), adminId);

        auditService.logAsync(
                AuditOperationType.MODEL_CREATE,
                adminId,
                savedModel.getId().toString(),
                Map.of(
                        "name", savedModel.getName(),
                        "type", savedModel.getType().toString(),
                        "version", savedModel.getVersion()
                )
        );

        return convertToResponse(savedModel);
    }

    /**
     * Delete a model (admin only)
     *
     * @param id      the model ID
     * @param adminId the admin user ID
     */
    @Transactional
    public void deleteModel(UUID id, UUID adminId) {
        var model = modelRepository.findById(id)
                .orElseThrow(() -> new ApiException(ApiErrorCode.MODEL_NOT_FOUND,
                        Map.of("modelId", id.toString())));

        modelRepository.delete(model);

        log.info("Model deleted: {} by admin {}", model.getName(), adminId);

        auditService.logAsync(
                AuditOperationType.MODEL_DELETE,
                adminId,
                id.toString(),
                Map.of("name", model.getName())
        );
    }

    /**
     * Convert entity to response DTO
     *
     * @param model the model entity
     * @return the response DTO
     */
    private ModelResponse convertToResponse(Model model) {
        return new ModelResponse(
                model.getId(),
                model.getName(),
                model.getType(),
                model.getDescription(),
                model.getVersion(),
                model.getCreatedAt()
        );
    }
}