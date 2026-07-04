package com.eduspark.agent.task;

import com.eduspark.agent.document.DocumentService;
import com.eduspark.agent.document.EduDocument;
import com.eduspark.agent.agent.PlannerService;
import com.eduspark.agent.agent.TaskPlan;
import com.eduspark.agent.agent.TaskStep;
import com.eduspark.agent.tool.ToolCallResult;
import com.eduspark.agent.tool.ToolRegistry;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.springframework.stereotype.Service;

@Service
public class TaskOrchestrator {

  private final TaskService taskService;
  private final PlannerService plannerService;
  private final ToolRegistry toolRegistry;
  private final ObjectMapper objectMapper;
  private final DocumentService documentService;

  public TaskOrchestrator(
      TaskService taskService,
      PlannerService plannerService,
      ToolRegistry toolRegistry,
      ObjectMapper objectMapper,
      DocumentService documentService) {
    this.taskService = taskService;
    this.plannerService = plannerService;
    this.toolRegistry = toolRegistry;
    this.objectMapper = objectMapper;
    this.documentService = documentService;
  }

  public void run(String taskId) {
    try {
      EduTask task = taskService.findTaskOrThrow(taskId);
      taskService.transit(taskId, TaskStatus.PLANNING, "Planning task");

      TaskPlan plan = plannerService.plan(buildPlannerInput(task));
      taskService.savePlanJson(taskId, toJson(plan));
      taskService.transit(taskId, TaskStatus.EXECUTING, "Planner generated " + plan.steps().size() + " tool steps");

      List<ToolCallResult> results = new ArrayList<>();
      for (TaskStep step : plan.steps()) {
        ToolCallResult result = toolRegistry.call(step.toolName(), step.input());
        results.add(result);
        taskService.saveArtifact(taskId, step.toolName(), toJson(result.output()));
        taskService.appendLog(taskId, TaskStatus.EXECUTING, "Tool executed: " + step.toolName());
      }

      taskService.transit(taskId, TaskStatus.REVIEWING, "Tool outputs generated");
      taskService.saveFinalAnswer(taskId, toJson(results));
      taskService.transit(taskId, TaskStatus.COMPLETED, "Task completed");
    } catch (RuntimeException exception) {
      markFailed(taskId, exception);
      throw exception;
    }
  }

  public void runAsync(String taskId) {
    CompletableFuture.runAsync(() -> run(taskId));
  }

  private String toJson(Object value) {
    try {
      return objectMapper.writeValueAsString(value);
    } catch (JsonProcessingException exception) {
      throw new IllegalStateException("Failed to serialize task orchestration output", exception);
    }
  }

  private String buildPlannerInput(EduTask task) {
    List<EduDocument> documents = documentService.findDocumentsByTaskId(task.getUserId(), task.getId());
    if (documents.isEmpty()) {
      return task.getUserInstruction();
    }

    StringBuilder input = new StringBuilder(task.getUserInstruction());
    input.append("\n\n参考文档：");
    for (EduDocument document : documents) {
      input.append("\n[")
          .append(document.getFileName())
          .append("]\n")
          .append(document.getExtractedText() == null ? "" : document.getExtractedText());
    }
    return input.toString();
  }

  private void markFailed(String taskId, RuntimeException exception) {
    taskService.saveFailureReason(taskId, exception.getMessage());
    EduTask task = taskService.findTaskOrThrow(taskId);
    if (TaskStateMachine.canTransit(task.getStatus(), TaskStatus.FAILED)) {
      taskService.transit(taskId, TaskStatus.FAILED, "Task failed: " + exception.getMessage());
    }
  }
}
