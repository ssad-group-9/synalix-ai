package ai.synalix.synalixai.service;

import ai.synalix.synalixai.dto.model.BackendCheckpointDownloadRequest;
import ai.synalix.synalixai.dto.model.CheckpointDownloadUrlResponse;
import ai.synalix.synalixai.dto.model.CheckpointResponse;
import ai.synalix.synalixai.dto.model.CheckpointQueryRequest;
import ai.synalix.synalixai.entity.Checkpoint;
import ai.synalix.synalixai.enums.ApiErrorCode;
import ai.synalix.synalixai.exception.ApiException;
import ai.synalix.synalixai.repository.CheckpointRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;

import ai.synalix.synalixai.repository.ModelRepository;
import ai.synalix.synalixai.dto.model.BackendCheckpointDownloadRequest;
import ai.synalix.synalixai.dto.model.BackendCheckpointsResponse;
import ai.synalix.synalixai.dto.model.CheckpointDownloadUrlResponse;

import ai.synalix.synalixai.service.MinioService;
import org.springframework.http.HttpMethod;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.transaction.annotation.Transactional;
import ai.synalix.synalixai.enums.CheckpointType;

/**
 * Service for checkpoint operations.
 */
@Service
public class CheckpointService {

    private final CheckpointRepository checkpointRepository;
    private final ModelRepository modelRepository;
    private final RestTemplate restTemplate;
    private final MinioService minioService;

    @Value("${app.backend-base-url}")
    private String backendBaseUrl;

    @Autowired
    public CheckpointService(CheckpointRepository checkpointRepository, RestTemplate restTemplate,
            ModelRepository modelRepository, MinioService minioService) {
        this.checkpointRepository = checkpointRepository;
        this.restTemplate = restTemplate;
        this.modelRepository = modelRepository;
        this.minioService = minioService;
    }

    /**
     * Get checkpoints by modelId.
     */
    public List<CheckpointResponse> getByModelId(UUID modelId, UUID userId) {
        if (modelId == null) {
            throw new ApiException(ApiErrorCode.MODEL_NOT_FOUND, "modelId cannot be null");
        }
        var entities = checkpointRepository.findByModelId(modelId);
        return entities.stream().map(this::toResponse).toList();
    }

    /**
     * 从后端 ${app.backend-base-url}/api/checkpoints 获取指定模型名的所有检查点
     *
     * @param modelName 模型名（例如：llamafactory/tiny-random-Llama-3）
     * @return 后端返回的检查点结构
     */
    public BackendCheckpointsResponse fetchBackendCheckpoints(UUID modelId) {
        var model = modelRepository.findById(modelId)
                .orElseThrow(() -> new ApiException(ApiErrorCode.MODEL_NOT_FOUND,
                        Map.of("modelId", modelId.toString())));
        String modelName = model.getName();
        if (modelName == null || modelName.isBlank()) {
            throw new ApiException(ApiErrorCode.MODEL_NOT_FOUND, "model_name cannot be blank");
        }
        var url = backendBaseUrl.endsWith("/")
                ? backendBaseUrl + "api/checkpoints"
                : backendBaseUrl + "/api/checkpoints";
        try {
            var req = new CheckpointQueryRequest();
            req.setModelName(modelName);
            return restTemplate.postForObject(url, req, BackendCheckpointsResponse.class);
        } catch (Exception e) {
            throw new ApiException(ApiErrorCode.INTERNAL_SERVER_ERROR, "Fetch checkpoints failed");
        }
    }

    /**
     * 获取后端检查点并存入本地仓库
     *
     * @param modelId 模型ID
     * @return 保存后的检查点列表
     */
    @Transactional
    public List<CheckpointResponse> fetchAndStoreBackendCheckpoints(UUID modelId) {
        var resp = fetchBackendCheckpoints(modelId);
        if (resp == null || resp.getTasks() == null || resp.getTasks().isEmpty()) {
            throw new ApiException(ApiErrorCode.RESOURCE_NOT_FOUND, "No checkpoints found from backend");
        }

        var saved = new java.util.ArrayList<Checkpoint>();
        // tasks: key = type(lora/full), value = map(taskId -> List<path>)
        resp.getTasks().forEach((typeKey, taskMap) -> {
            var type = mapType(typeKey);
            taskMap.forEach((taskId, paths) -> {
                if (paths == null || paths.isEmpty())
                    return;
                for (var path : paths) {
                    var cp = new Checkpoint();
                    cp.setModelId(modelId);
                    cp.setType(type);
                    cp.setPath(path);
                    cp.setName(extractName(taskId, path));
                    cp.setTaskId(taskId);
                    cp.setCreatedAt(extractTime(taskId));
                    saved.add(cp);
                }
            });
        });

        var persisted = checkpointRepository.saveAll(saved);
        return persisted.stream().map(this::toResponse).toList();
    }

    /**
     * Generate MinIO download URL for checkpoint, notify backend
     * /api/checkpoints/download,
     * then return download URL to frontend.
     *
     * @param checkpointId checkpoint UUID
     * @param taskId       backend task id
     * @return response with minio download url
     */
    @Transactional(readOnly = true)
    public CheckpointDownloadUrlResponse prepareCheckpointDownload(UUID checkpointId) {
        var checkpoint = checkpointRepository.findById(checkpointId)
                .orElseThrow(() -> new ApiException(ApiErrorCode.RESOURCE_NOT_FOUND,
                        Map.of("checkpointId", checkpointId.toString())));
        var taskId = checkpoint.getTaskId();
        if (taskId == null || taskId.isBlank()) {
            throw new ApiException(ApiErrorCode.TASK_NOT_FOUND, "task_id cannot be blank");
        }

        // 1) 从 MinIO 获取下载链接（这里假定 path 可直接作为 objectKey 使用）
        var uploadUrl = minioService.generateCheckpointUploadUrl(checkpointId).getUrl();
        if (uploadUrl == null || uploadUrl.isBlank()) {
            throw new ApiException(ApiErrorCode.INTERNAL_SERVER_ERROR, "Failed to generate checkpoint download url");
        }

        // 2) 通知后端 /api/checkpoints/download
        var base = backendBaseUrl.endsWith("/")
                ? backendBaseUrl + "api/checkpoints/download"
                : backendBaseUrl + "/api/checkpoints/download";

        try {
            var req = new BackendCheckpointDownloadRequest(
                    taskId,
                    checkpoint.getName(),
                    uploadUrl);
            ResponseEntity<Object> backendResp = restTemplate.exchange(base, HttpMethod.POST,
                    new org.springframework.http.HttpEntity<>(req), Object.class);
            var downloadUrl = minioService
                    .generateCheckpointDownloadUrl(minioService.generateCheckpointStorageKey(checkpointId)).getUrl();
            return new CheckpointDownloadUrlResponse(downloadUrl, backendResp.getBody());
        } catch (Exception e) {
            throw new ApiException(ApiErrorCode.INTERNAL_SERVER_ERROR, "Backend checkpoint download failed");
        }
    }

    /**
     * 将后端类型字符串映射到枚举
     */
    private CheckpointType mapType(String typeKey) {
        if (typeKey == null)
            return CheckpointType.MODEL;
        return switch (typeKey.toLowerCase()) {
            case "lora", "adapter" -> CheckpointType.ADAPTER;
            case "full", "model" -> CheckpointType.MODEL;
            default -> CheckpointType.MODEL;
        };
    }

    /**
     * 从 taskId 与路径生成可读名称
     */
    private String extractName(String taskId, String path) {
        if (path != null && !path.isBlank()) {
            var idx = path.lastIndexOf('/');
            if (idx >= 0 && idx < path.length() - 1) {
                return path.substring(idx + 1);
            }
        }
        return taskId != null ? taskId : "checkpoint";
    }

    /**
     * 从 taskId 中提取时间戳
     */
    private LocalDateTime extractTime(String taskId) {
        if (taskId != null && !taskId.isBlank()) {
            try {
                var parts = taskId.split("_");
                if (parts.length > 0) {
                    var dateTimePart = parts[0];
                    var formatter = java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
                    var localDateTime = java.time.LocalDateTime.parse(dateTimePart, formatter);
                    return localDateTime;
                }
            } catch (Exception e) {
                // Log the exception if necessary
            }
        }
        return LocalDateTime.now();
    }

    public CheckpointResponse toResponse(Checkpoint c) {
        var dto = new CheckpointResponse();
        dto.setId(c.getId());
        dto.setModelId(c.getModelId());
        dto.setName(c.getName());
        dto.setPath(c.getPath());
        dto.setCreatedAt(c.getCreatedAt());
        dto.setType(c.getType());
        return dto;
    }
}