package ai.synalix.synalixai.service;

import ai.synalix.synalixai.dto.task.TaskMetricsResponse;
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

    private final TaskRepository taskRepository;
    private final ModelRepository modelRepository;
    private final DatasetRepository datasetRepository;
    private final AuditService auditService;
    private final MinioService minioService;

    @Autowired
    public TaskService(TaskRepository taskRepository,
                       ModelRepository modelRepository,
                       DatasetRepository datasetRepository,
                       AuditService auditService,
                       MinioService minioService) {
        this.taskRepository = taskRepository;
        this.modelRepository = modelRepository;
        this.datasetRepository = datasetRepository;
        this.auditService = auditService;
        this.minioService = minioService;
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
        if (!datasetRepository.existsById(datasetId)) {
            throw new ApiException(ApiErrorCode.DATASET_NOT_FOUND);
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
                "datasetId", datasetId
        );
        auditService.logAsync(AuditOperationType.TASK_CREATE, userId, savedTask.getId().toString(), details);

        logger.info("Task created successfully: id={}, name={}, type={}", 
                   savedTask.getId(), savedTask.getName(), savedTask.getType());

        return savedTask;
    }

    /**
     * Get all tasks
     */
    public List<Task> getAllTasks(TaskStatus status, TaskType type) {
        if (status != null && type != null) {
            return taskRepository.findByStatusAndType(status, type);
        } else if (status != null) {
            return taskRepository.findByStatus(status);
        } else if (type != null) {
            return taskRepository.findByType(type);
        }
        return taskRepository.findAll();
    }

    /**
     * Get task by ID
     */
    public Task getTaskById(UUID taskId) {
        return taskRepository.findById(taskId)
                .orElseThrow(() -> new ApiException(ApiErrorCode.TASK_NOT_FOUND));
    }

    /**
     * Stop task
     */
    @Transactional
    public Task stopTask(UUID taskId, UUID userId) {
        var task = getTaskById(taskId);

        if (task.getStatus() == TaskStatus.COMPLETED || 
            task.getStatus() == TaskStatus.FAILED || 
            task.getStatus() == TaskStatus.STOPPED) {
            throw new ApiException(ApiErrorCode.TASK_CANNOT_STOP);
        }

        var previousStatus = task.getStatus();
        task.setStatus(TaskStatus.STOPPED);
        var savedTask = taskRepository.save(task);

        // Audit log
        auditService.logAsync(AuditOperationType.TASK_STOP, userId, taskId.toString(), Map.of("previousStatus", previousStatus));

        logger.info("Task stopped successfully: id={}", taskId);
        return savedTask;
    }

    /**
     * Get task metrics
     */
    public List<TaskMetricsResponse> getTaskMetrics(UUID taskId) {
        var task = getTaskById(taskId);
        
        // In a real implementation, this would query the training backend or metrics database
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
     * Get task logs
     */
    public String getTaskLogs(UUID taskId) {
        // Verify task exists
        getTaskById(taskId);
        
        // Retrieve logs from MinIO
        return minioService.getTaskLogs(taskId);
    }
}
