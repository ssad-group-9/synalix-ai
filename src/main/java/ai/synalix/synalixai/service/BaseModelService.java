package ai.synalix.synalixai.service;

import ai.synalix.synalixai.dto.model.BaseModelResponse;
import ai.synalix.synalixai.dto.model.CreateBaseModelRequest;
import ai.synalix.synalixai.entity.BaseModel;
import ai.synalix.synalixai.enums.ApiErrorCode;
import ai.synalix.synalixai.enums.AuditOperationType;
import ai.synalix.synalixai.exception.ApiException;
import ai.synalix.synalixai.repository.BaseModelRepository;
import ai.synalix.synalixai.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Service for base model management operations
 * Handles CRUD operations for HuggingFace base models
 */
@Service
@Slf4j
public class BaseModelService {

    private final BaseModelRepository baseModelRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;

    @Autowired
    public BaseModelService(BaseModelRepository baseModelRepository,
                            UserRepository userRepository,
                            AuditService auditService) {
        this.baseModelRepository = baseModelRepository;
        this.userRepository = userRepository;
        this.auditService = auditService;
    }

    /**
     * Get all base models (for users, only enabled models)
     *
     * @return list of enabled base models
     */
    public List<BaseModelResponse> getAllEnabledModels() {
        return baseModelRepository.findByEnabledTrue()
                .stream()
                .map(this::convertToResponse)
                .toList();
    }

    /**
     * Get all base models (for admin, includes disabled models)
     *
     * @return list of all base models
     */
    public List<BaseModelResponse> getAllModels() {
        return baseModelRepository.findAll()
                .stream()
                .map(this::convertToResponse)
                .toList();
    }

    /**
     * Get a base model by ID
     *
     * @param id the model ID
     * @return the base model response
     */
    public BaseModelResponse getModelById(UUID id) {
        var model = baseModelRepository.findById(id)
                .orElseThrow(() -> new ApiException(ApiErrorCode.MODEL_NOT_FOUND,
                        Map.of("modelId", id.toString())));
        return convertToResponse(model);
    }

    /**
     * Register a new base model (admin only)
     *
     * @param request   the create model request
     * @param adminId   the admin user ID
     * @return the created base model response
     */
    @Transactional
    public BaseModelResponse createModel(CreateBaseModelRequest request, UUID adminId) {
        // Check if name already exists
        if (baseModelRepository.existsByName(request.getName())) {
            throw new ApiException(ApiErrorCode.MODEL_NAME_EXISTS,
                    Map.of("name", request.getName()));
        }

        // Check if HuggingFace model ID already exists
        if (baseModelRepository.existsByHuggingfaceModelId(request.getHuggingfaceModelId())) {
            throw new ApiException(ApiErrorCode.MODEL_NAME_EXISTS,
                    "HuggingFace model ID already registered: " + request.getHuggingfaceModelId());
        }

        var admin = userRepository.findById(adminId)
                .orElseThrow(() -> new ApiException(ApiErrorCode.USER_NOT_FOUND,
                        Map.of("userId", adminId.toString())));

        var model = new BaseModel();
        model.setName(request.getName());
        model.setHuggingfaceModelId(request.getHuggingfaceModelId());
        model.setDescription(request.getDescription());
        model.setEnabled(true);
        model.setCreatedBy(admin);

        var savedModel = baseModelRepository.save(model);

        log.info("Base model registered: {} (HuggingFace: {}) by admin {}",
                savedModel.getName(), savedModel.getHuggingfaceModelId(), adminId);

        auditService.logAsync(
                AuditOperationType.MODEL_CREATE,
                adminId,
                savedModel.getId().toString(),
                Map.of(
                        "name", savedModel.getName(),
                        "huggingfaceModelId", savedModel.getHuggingfaceModelId()
                )
        );

        return convertToResponse(savedModel);
    }

    /**
     * Delete a base model (admin only)
     *
     * @param id      the model ID
     * @param adminId the admin user ID
     */
    @Transactional
    public void deleteModel(UUID id, UUID adminId) {
        var model = baseModelRepository.findById(id)
                .orElseThrow(() -> new ApiException(ApiErrorCode.MODEL_NOT_FOUND,
                        Map.of("modelId", id.toString())));

        baseModelRepository.delete(model);

        log.info("Base model deleted: {} by admin {}", model.getName(), adminId);

        auditService.logAsync(
                AuditOperationType.MODEL_DELETE,
                adminId,
                id.toString(),
                Map.of("name", model.getName())
        );
    }

    /**
     * Enable or disable a base model (admin only)
     *
     * @param id      the model ID
     * @param enabled whether to enable or disable
     * @param adminId the admin user ID
     * @return the updated base model response
     */
    @Transactional
    public BaseModelResponse setModelEnabled(UUID id, boolean enabled, UUID adminId) {
        var model = baseModelRepository.findById(id)
                .orElseThrow(() -> new ApiException(ApiErrorCode.MODEL_NOT_FOUND,
                        Map.of("modelId", id.toString())));

        model.setEnabled(enabled);
        var savedModel = baseModelRepository.save(model);

        log.info("Base model {} {}: by admin {}",
                model.getName(), enabled ? "enabled" : "disabled", adminId);

        auditService.logAsync(
                AuditOperationType.MODEL_UPDATE,
                adminId,
                id.toString(),
                Map.of("name", model.getName(), "enabled", enabled)
        );

        return convertToResponse(savedModel);
    }

    /**
     * Get a base model entity by ID (for internal use)
     *
     * @param id the model ID
     * @return the base model entity
     */
    public BaseModel getModelEntityById(UUID id) {
        return baseModelRepository.findById(id)
                .orElseThrow(() -> new ApiException(ApiErrorCode.MODEL_NOT_FOUND,
                        Map.of("modelId", id.toString())));
    }

    /**
     * Convert entity to response DTO
     *
     * @param model the base model entity
     * @return the response DTO
     */
    private BaseModelResponse convertToResponse(BaseModel model) {
        return new BaseModelResponse(
                model.getId(),
                model.getName(),
                model.getHuggingfaceModelId(),
                model.getDescription(),
                model.isEnabled(),
                model.getCreatedAt(),
                model.getCreatedBy().getUsername()
        );
    }
}