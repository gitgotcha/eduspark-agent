package com.eduspark.agent.task;

import java.util.concurrent.CompletableFuture;
import org.springframework.stereotype.Service;

@Service
public class MockTaskOrchestrator {

  private final TaskService taskService;

  public MockTaskOrchestrator(TaskService taskService) {
    this.taskService = taskService;
  }

  public void run(String taskId) {
    taskService.transit(taskId, TaskStatus.PLANNING, "Mock planner generated task steps");
    taskService.transit(taskId, TaskStatus.EXECUTING, "Mock executor is running tools");
    taskService.transit(taskId, TaskStatus.REVIEWING, "Mock reviewer is checking output quality");
    taskService.transit(taskId, TaskStatus.COMPLETED, "Mock task completed");
  }

  public void runAsync(String taskId) {
    CompletableFuture.runAsync(() -> run(taskId));
  }
}
