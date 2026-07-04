package com.eduspark.agent.task;

import java.util.Map;
import java.util.Set;

public final class TaskStateMachine {

  private static final Map<TaskStatus, Set<TaskStatus>> TRANSITIONS =
      Map.of(
          TaskStatus.PENDING, Set.of(TaskStatus.PLANNING, TaskStatus.FAILED),
          TaskStatus.PLANNING, Set.of(TaskStatus.EXECUTING, TaskStatus.FAILED),
          TaskStatus.EXECUTING, Set.of(TaskStatus.REVIEWING, TaskStatus.FAILED),
          TaskStatus.REVIEWING, Set.of(TaskStatus.COMPLETED, TaskStatus.FAILED),
          TaskStatus.COMPLETED, Set.of(),
          TaskStatus.FAILED, Set.of());

  private TaskStateMachine() {}

  public static boolean canTransit(TaskStatus from, TaskStatus to) {
    return TRANSITIONS.getOrDefault(from, Set.of()).contains(to);
  }
}
