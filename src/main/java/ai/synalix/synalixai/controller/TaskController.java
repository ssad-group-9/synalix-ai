package ai.synalix.synalixai.controller;

import ai.synalix.synalixai.config.JwtUserPrincipal;
import ai.synalix.synalixai.dto.task.CreateTaskRequest;
import ai.synalix.synalixai.dto.task.TaskMetricsResponse;
import ai.synalix.synalixai.dto.task.TaskResponse;
import ai.synalix.synalixai.entity.Task;
import ai.synalix.synalixai.service.TaskService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Task management REST controller
 */
@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    private final TaskService taskService;

    @Autowired
    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    /**
     * Create new task
     */
    @PostMapping
    public ResponseEntity<TaskResponse> createTask(
            @Valid @RequestBody CreateTaskRequest request,
            @AuthenticationPrincipal JwtUserPrincipal principal) {
        var userId = principal.getId();

        var createdTask = taskService.createTask(
                request.getName(),
                request.getType(),
                request.getModelId(),
                request.getDatasetId(),
                request.getGpuIds(),
                request.getConfig(),
                userId
        );

        return ResponseEntity.ok(convertToTaskResponse(createdTask));
    }

    /**
     * Get all tasks
     */
    @GetMapping
    public ResponseEntity<List<TaskResponse>> getAllTasks() {
        var tasks = taskService.getAllTasks();
        var responses = tasks.stream()
                .map(this::convertToTaskResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(responses);
    }

    /**
     * Get task by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<TaskResponse> getTaskById(@PathVariable UUID id) {
        var task = taskService.getTaskById(id);
        return ResponseEntity.ok(convertToTaskResponse(task));
    }

    /**
     * Stop task
     */
    @PostMapping("/{id}/stop")
    public ResponseEntity<Void> stopTask(
            @PathVariable UUID id,
            @AuthenticationPrincipal JwtUserPrincipal principal) {
        var userId = principal.getId();
        taskService.stopTask(id, userId);
        return ResponseEntity.ok().build();
    }

    /**
     * Get task metrics
     */
    @GetMapping("/{id}/metrics")
    public ResponseEntity<TaskMetricsResponse> getTaskMetrics(@PathVariable UUID id) {
        var metrics = taskService.getTaskMetrics(id);
        return ResponseEntity.ok(metrics);
    }

    private TaskResponse convertToTaskResponse(Task task) {
        var response = new TaskResponse();
        response.setId(task.getId());
        response.setName(task.getName());
        response.setType(task.getType());
        response.setStatus(task.getStatus());
        response.setModelId(task.getModelId());
        response.setDatasetId(task.getDatasetId());
        response.setConfig(task.getConfig());
        response.setCreatedBy(task.getCreatedBy());
        response.setCreatedAt(task.getCreatedAt());
        response.setUpdatedAt(task.getUpdatedAt());
        return response;
    }
}
