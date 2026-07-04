package com.eduspark.agent.task;

import com.eduspark.agent.document.DocumentService;
import com.eduspark.agent.task.dto.TaskCreateRequest;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

@Service
public class TaskService {

  private final TaskMapper taskMapper;
  private final TaskLogMapper taskLogMapper;
  private final TaskArtifactMapper taskArtifactMapper;
  private final TaskStreamService taskStreamService;
  private final DocumentService documentService;

  public TaskService(
      TaskMapper taskMapper,
      TaskLogMapper taskLogMapper,
      TaskArtifactMapper taskArtifactMapper,
      TaskStreamService taskStreamService,
      DocumentService documentService) {
    this.taskMapper = taskMapper;
    this.taskLogMapper = taskLogMapper;
    this.taskArtifactMapper = taskArtifactMapper;
    this.taskStreamService = taskStreamService;
    this.documentService = documentService;
  }

  @Transactional
  public TaskCreateResponse createTask(TaskCreateRequest request) {
    return createTask("system-test-user", request);
  }

  @Transactional
  public TaskCreateResponse createTask(String userId, TaskCreateRequest request) {
    LocalDateTime now = LocalDateTime.now();
    String taskId = UUID.randomUUID().toString();

    EduTask task = new EduTask();
    task.setId(taskId);
    task.setUserId(userId);
    task.setUserInstruction(request.instruction());
    task.setStatus(TaskStatus.PENDING);
    task.setRetryCount(0);
    task.setCreatedAt(now);
    task.setUpdatedAt(now);
    taskMapper.insert(task);
    documentService.attachDocumentsToTask(userId, request.documentIds(), taskId);

    EduTaskLog log = new EduTaskLog();
    log.setId(UUID.randomUUID().toString());
    log.setTaskId(taskId);
    log.setStage(TaskStatus.PENDING.getValue());
    log.setMessage("Task created");
    log.setCreatedAt(now);
    taskLogMapper.insert(log);

    return new TaskCreateResponse(taskId, TaskStatus.PENDING);
  }

  public EduTask findTaskOrThrow(String taskId) {
    EduTask task = taskMapper.selectById(taskId);
    if (task == null) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Task not found");
    }
    return task;
  }

  public EduTask findTaskOrThrow(String userId, String taskId) {
    EduTask task = taskMapper.selectByIdAndUserId(taskId, userId);
    if (task == null) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Task not found");
    }
    return task;
  }

  public List<EduTask> listTasks(String userId, int limit) {
    int safeLimit = Math.min(Math.max(limit, 1), 50);
    return taskMapper.selectByUserIdOrderByCreatedAtDesc(userId, safeLimit);
  }

  public List<EduTaskLog> findLogs(String taskId) {
    findTaskOrThrow(taskId);
    return taskLogMapper.selectByTaskId(taskId);
  }

  public List<EduTaskLog> findLogs(String userId, String taskId) {
    findTaskOrThrow(userId, taskId);
    return taskLogMapper.selectByTaskId(taskId);
  }

  public List<EduTaskArtifact> findArtifacts(String taskId) {
    findTaskOrThrow(taskId);
    return taskArtifactMapper.selectByTaskId(taskId);
  }

  public List<EduTaskArtifact> findArtifacts(String userId, String taskId) {
    findTaskOrThrow(userId, taskId);
    return taskArtifactMapper.selectByTaskId(taskId);
  }

  @Transactional
  public void savePlanJson(String taskId, String planJson) {
    EduTask task = findTaskOrThrow(taskId);
    task.setPlanJson(planJson);
    task.setUpdatedAt(LocalDateTime.now());
    taskMapper.updateById(task);
  }

  @Transactional
  public void saveFinalAnswer(String taskId, String finalAnswer) {
    EduTask task = findTaskOrThrow(taskId);
    task.setFinalAnswer(finalAnswer);
    task.setUpdatedAt(LocalDateTime.now());
    taskMapper.updateById(task);
  }

  @Transactional
  public void saveFailureReason(String taskId, String failureReason) {
    EduTask task = findTaskOrThrow(taskId);
    task.setFailureReason(failureReason);
    task.setUpdatedAt(LocalDateTime.now());
    taskMapper.updateById(task);
  }

  @Transactional
  public void appendLog(String taskId, TaskStatus stage, String message) {
    findTaskOrThrow(taskId);
    EduTaskLog log = newTaskLog(taskId, stage, message, LocalDateTime.now());
    taskLogMapper.insert(log);
    taskStreamService.sendLog(taskId, log);
  }

  @Transactional
  public void saveArtifact(String taskId, String artifactType, String contentJson) {
    findTaskOrThrow(taskId);
    EduTaskArtifact artifact = new EduTaskArtifact();
    artifact.setId(UUID.randomUUID().toString());
    artifact.setTaskId(taskId);
    artifact.setArtifactType(artifactType);
    artifact.setContentJson(contentJson);
    artifact.setCreatedAt(LocalDateTime.now());
    taskArtifactMapper.insert(artifact);
  }

  @Transactional
  public void transit(String taskId, TaskStatus toStatus, String message) {
    EduTask task = findTaskOrThrow(taskId);
    if (!TaskStateMachine.canTransit(task.getStatus(), toStatus)) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Invalid task status transition");
    }

    LocalDateTime now = LocalDateTime.now();
    task.setStatus(toStatus);
    task.setUpdatedAt(now);
    taskMapper.updateById(task);

    EduTaskLog log = newTaskLog(taskId, toStatus, message, now);
    taskLogMapper.insert(log);

    taskStreamService.sendLog(taskId, log);
    taskStreamService.sendStatus(taskId, task);
    if (toStatus == TaskStatus.COMPLETED || toStatus == TaskStatus.FAILED) {
      taskStreamService.complete(taskId);
    }
  }

  private EduTaskLog newTaskLog(String taskId, TaskStatus stage, String message, LocalDateTime now) {
    EduTaskLog log = new EduTaskLog();
    log.setId(UUID.randomUUID().toString());
    log.setTaskId(taskId);
    log.setStage(stage.getValue());
    log.setMessage(message);
    log.setCreatedAt(now);
    return log;
  }
}
