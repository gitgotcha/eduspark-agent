package com.eduspark.agent.task;

import com.eduspark.agent.task.dto.TaskCreateRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/users/{userId}/tasks")
public class TaskController {

  private final TaskService taskService;
  private final TaskStreamService taskStreamService;
  private final TaskOrchestrator taskOrchestrator;

  public TaskController(
      TaskService taskService,
      TaskStreamService taskStreamService,
      TaskOrchestrator taskOrchestrator) {
    this.taskService = taskService;
    this.taskStreamService = taskStreamService;
    this.taskOrchestrator = taskOrchestrator;
  }

  @PostMapping
  public TaskCreateResponse createTask(
      @PathVariable String userId, @Valid @RequestBody TaskCreateRequest request) {
    return taskService.createTask(userId, request);
  }

  @GetMapping("/{taskId}")
  public EduTask getTask(@PathVariable String userId, @PathVariable String taskId) {
    return taskService.findTaskOrThrow(userId, taskId);
  }

  @GetMapping
  public List<EduTask> listTasks(
      @PathVariable String userId, @RequestParam(defaultValue = "20") int limit) {
    return taskService.listTasks(userId, limit);
  }

  @GetMapping("/{taskId}/logs")
  public List<EduTaskLog> getTaskLogs(@PathVariable String userId, @PathVariable String taskId) {
    return taskService.findLogs(userId, taskId);
  }

  @GetMapping("/{taskId}/artifacts")
  public List<EduTaskArtifact> getTaskArtifacts(
      @PathVariable String userId, @PathVariable String taskId) {
    return taskService.findArtifacts(userId, taskId);
  }

  @GetMapping("/{taskId}/stream")
  public SseEmitter stream(@PathVariable String userId, @PathVariable String taskId) {
    EduTask task = taskService.findTaskOrThrow(userId, taskId);
    SseEmitter emitter = taskStreamService.subscribe(taskId);
    if (task.getStatus() == TaskStatus.PENDING) {
      taskOrchestrator.runAsync(taskId);
    }
    return emitter;
  }
}
