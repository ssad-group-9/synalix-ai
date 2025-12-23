package ai.synalix.synalixai.service;

import ai.synalix.synalixai.dto.task.TaskChartResponse;
import ai.synalix.synalixai.dto.task.TaskMetricsResponse;
import ai.synalix.synalixai.dto.task.TrainResponse;
import ai.synalix.synalixai.entity.Task;
import ai.synalix.synalixai.enums.ApiErrorCode;
import ai.synalix.synalixai.enums.AuditOperationType;
import ai.synalix.synalixai.enums.TaskStatus;
import ai.synalix.synalixai.enums.TaskType;
import ai.synalix.synalixai.exception.ApiException;
import ai.synalix.synalixai.repository.DatasetRepository;
import ai.synalix.synalixai.repository.ModelRepository;
import ai.synalix.synalixai.repository.TaskRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpMethod;
import org.springframework.web.util.UriComponentsBuilder;
import ai.synalix.synalixai.dto.task.TaskStatusResponse;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Task management service
 */
@Service
public class TaskService {

    private static final Logger logger = LoggerFactory.getLogger(TaskService.class);

    @Value("${app.backend-base-url}")
    private String backendBaseUrl;

    private final TaskRepository taskRepository;
    private final ModelRepository modelRepository;
    private final DatasetRepository datasetRepository;
    private final AuditService auditService;
    private final MinioService minioService;
    private final RestTemplate restTemplate;

    @Autowired
    public TaskService(TaskRepository taskRepository,
            ModelRepository modelRepository,
            DatasetRepository datasetRepository,
            AuditService auditService,
            MinioService minioService,
            RestTemplate restTemplate) {
        this.taskRepository = taskRepository;
        this.modelRepository = modelRepository;
        this.datasetRepository = datasetRepository;
        this.auditService = auditService;
        this.minioService = minioService;
        this.restTemplate = restTemplate;
    }

    @Transactional
    public Task submitTask(Task task, Map<String, Object> config, UUID userId) {
        // POST config to backend
        String taskType = task.getType().equals(TaskType.TRAINING) ? "train" : "infer";
        var url = backendBaseUrl.endsWith("/") ? backendBaseUrl + "api/" + taskType
                : backendBaseUrl + "/api/" + taskType;
        TrainResponse resp;
        try {
            resp = restTemplate.postForObject(url, config, TrainResponse.class);
        } catch (Exception e) {
            throw new ApiException(ApiErrorCode.INTERNAL_SERVER_ERROR, "Submit training failed");
        }
        if (resp == null || resp.getRequest() == null || resp.getRequest().getTaskId() == null) {
            throw new ApiException(ApiErrorCode.INTERNAL_SERVER_ERROR, "Invalid train response");
        }

        // Save external task id and update status
        task.setExternalTaskId(resp.getRequest().getTaskId());
        task.setStatus(TaskStatus.RUNNING);
        task.setConfig(config);
        var saved = taskRepository.save(task);

        // Audit
        auditService.logAsync(
                AuditOperationType.TASK_CREATE,
                userId,
                saved.getId().toString(),
                Map.of("externalTaskId", saved.getExternalTaskId(), "status", saved.getStatus().name()));

        return saved;
    }

    /**
     * Create new task
     */
    @Transactional
    public Task createTask(String name, TaskType type, UUID modelId, UUID datasetId,
            List<Integer> gpuIds, Map<String, Object> config, UUID userId) {

        // Validate Model exists
        if (!modelRepository.existsById(modelId)) {
            throw new ApiException(ApiErrorCode.MODEL_NOT_FOUND);
        }

        // Validate Dataset exists
        if (datasetId != null && !datasetRepository.existsById(datasetId)) {
            throw new ApiException(ApiErrorCode.DATASET_NOT_FOUND);
        }
        if (datasetId == null) {
            datasetId = UUID.fromString("00000000-0000-0000-0000-000000000000");
        }

        // Create task
        var task = new Task();
        task.setName(name);
        task.setType(type);
        task.setModelId(modelId);
        task.setDatasetId(datasetId);
        task.setCreatedBy(userId);
        task.setStatus(TaskStatus.PENDING);

        // Add gpuIds to config if present
        if (gpuIds != null && !gpuIds.isEmpty()) {
            if (config == null) {
                config = new java.util.HashMap<>();
            }
            config.put("gpuIds", gpuIds);
        }
        task.setConfig(config);

        var savedTask = taskRepository.save(task);

        // Audit log
        Map<String, Object> details = Map.of(
                "name", name,
                "type", type,
                "modelId", modelId,
                "datasetId", datasetId);
        auditService.logAsync(AuditOperationType.TASK_CREATE, userId, savedTask.getId().toString(), details);

        savedTask = submitTask(savedTask, config, userId);

        logger.info("Task created successfully: id={}, name={}, type={}",
                savedTask.getId(), savedTask.getName(), savedTask.getType());

        return savedTask;
    }

    /**
     * Get all tasks
     */
    public List<Task> getAllTasks(TaskStatus status, TaskType type) {
        List<Task> results;
        if (status != null && type != null) {
            results = taskRepository.findByStatusAndType(status, type);
        } else if (status != null) {
            results = taskRepository.findByStatus(status);
        } else if (type != null) {
            results = taskRepository.findByType(type);
        } else {
            results = taskRepository.findAll();
        }
        results.forEach(this::refreshStatus);
        return results;
    }

    /**
     * Get task by ID
     */
    public Task getTaskById(UUID taskId) {
        return taskRepository.findById(taskId)
                .orElseThrow(() -> new ApiException(ApiErrorCode.TASK_NOT_FOUND));
    }

    /**
     * Stop task by calling backend /api/tasks/cancel?task_id=externalTaskId
     */
    @Transactional
    public Task stopTask(UUID taskId, UUID userId) {
        var task = getTaskById(taskId);

        if (task.getStatus() == TaskStatus.COMPLETED ||
                task.getStatus() == TaskStatus.FAILED ||
                task.getStatus() == TaskStatus.STOPPED) {
            throw new ApiException(ApiErrorCode.TASK_CANNOT_STOP);
        }

        // 后端取消调用
        if (task.getExternalTaskId() != null && !task.getExternalTaskId().isBlank()) {
            var base = backendBaseUrl.endsWith("/") ? backendBaseUrl + "api/tasks/cancel"
                    : backendBaseUrl + "/api/tasks/cancel";
            var url = UriComponentsBuilder.fromUriString(base)
                    .queryParam("task_id", task.getExternalTaskId())
                    .toUriString();
            try {
                ResponseEntity<Void> resp = restTemplate.exchange(url, HttpMethod.POST, null, Void.class);
                if (!resp.getStatusCode().is2xxSuccessful()) {
                    throw new ApiException(ApiErrorCode.INTERNAL_SERVER_ERROR, "Backend cancel failed");
                }
            } catch (Exception e) {
                throw new ApiException(ApiErrorCode.INTERNAL_SERVER_ERROR, "Backend cancel failed");
            }
        }

        var previousStatus = task.getStatus();
        task.setStatus(TaskStatus.STOPPED);
        var savedTask = taskRepository.save(task);

        // 审计
        auditService.logAsync(
                AuditOperationType.TASK_STOP,
                userId,
                savedTask.getId().toString(),
                Map.of("previousStatus", previousStatus, "externalTaskId", savedTask.getExternalTaskId()));

        logger.info("Task stopped successfully: id={}, externalTaskId={}", taskId, task.getExternalTaskId());
        return savedTask;
    }

    /**
     * Get task metrics
     */
    public List<TaskMetricsResponse> getTaskMetrics(UUID taskId) {
        var task = getTaskById(taskId);

        // In a real implementation, this would query the training backend or metrics
        // database
        // For now, we return a basic response based on task status

        var metrics = new TaskMetricsResponse();
        metrics.setTaskId(taskId);
        metrics.setTimestamp(LocalDateTime.now());

        if (task.getStatus() == TaskStatus.RUNNING) {
            // Mock data for running task
            metrics.setEpoch(1);
            metrics.setLoss(0.5);
            metrics.setAccuracy(0.8);
        } else {
            metrics.setEpoch(0);
            metrics.setLoss(0.0);
            metrics.setAccuracy(0.0);
        }

        return List.of(metrics);
    }

    /**
     * Get task chart
     */
    public TaskChartResponse getTaskChart(UUID taskId) {
        var task = getTaskById(taskId);

        // In a real implementation, this would query the training backend or metrics
        // database
        // For now, we return a basic response based on task status

        var base = backendBaseUrl.endsWith("/") ? backendBaseUrl + "api/chart"
                : backendBaseUrl + "/api/chart";
        var url = UriComponentsBuilder.fromUriString(base)
                .queryParam("task_id", task.getExternalTaskId())
                .toUriString();
        var chart = new TaskChartResponse();
        chart.setTaskId(taskId);
        chart.setChartUrl(url);
        return chart;
    }

    /**
     * Get task logs
     */
    public String getTaskLogs(UUID taskId) {
        // Verify task exists
        getTaskById(taskId);

        // Retrieve logs from MinIO
        return minioService.getTaskLogs(taskId);
    }

    /**
     * Query backend ${app.backend-base-url}/api/tasks by task_id and update local
     * Task status.
     * If backend returns multiple items, match by task.externalTaskId.
     */
    @Transactional
    public Task refreshStatus(Task task) {
        if (task.getExternalTaskId() == null || task.getExternalTaskId().isBlank()) {
            return task;
        }
        var base = backendBaseUrl.endsWith("/") ? backendBaseUrl + "api/tasks" : backendBaseUrl + "/api/tasks";
        var url = UriComponentsBuilder.fromUriString(base)
                .queryParam("task_id", task.getExternalTaskId())
                .toUriString();
        Map<String, TaskStatusResponse> resp;
        try {
            ResponseEntity<Map<String, TaskStatusResponse>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<Map<String, TaskStatusResponse>>() {
                    });
            resp = response.getBody();
        } catch (Exception e) {
            return task;
        }
        if (resp == null || resp.isEmpty()) {
            return task;
        }

        var item = resp.get(task.getExternalTaskId());
        if (item == null) {
            return task;
        }
        var newStatus = mapBackendStatus(item.getStatus());
        task.setStatus(newStatus);
        var saved = taskRepository.save(task);
        return saved;
    }

    /**
     * Map backend status string to local TaskStatus enum.
     */
    private TaskStatus mapBackendStatus(String backendStatus) {
        if (backendStatus == null)
            return TaskStatus.PENDING;
        return switch (backendStatus.toLowerCase()) {
            case "pending" -> TaskStatus.PENDING;
            case "running", "in_progress" -> TaskStatus.RUNNING;
            case "completed", "success" -> TaskStatus.COMPLETED;
            case "failed", "error" -> TaskStatus.FAILED;
            case "stopped", "cancelled", "canceled" -> TaskStatus.STOPPED;
            default -> TaskStatus.PENDING;
        };
    }
}
