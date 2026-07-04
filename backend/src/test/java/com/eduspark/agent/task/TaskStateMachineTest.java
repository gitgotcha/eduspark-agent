package com.eduspark.agent.task;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class TaskStateMachineTest {

  @Test
  void allowsPlannedLifecycleTransitions() {
    assertThat(TaskStateMachine.canTransit(TaskStatus.PENDING, TaskStatus.PLANNING)).isTrue();
    assertThat(TaskStateMachine.canTransit(TaskStatus.PLANNING, TaskStatus.EXECUTING)).isTrue();
    assertThat(TaskStateMachine.canTransit(TaskStatus.EXECUTING, TaskStatus.REVIEWING)).isTrue();
    assertThat(TaskStateMachine.canTransit(TaskStatus.REVIEWING, TaskStatus.COMPLETED)).isTrue();
  }

  @Test
  void rejectsSkippedLifecycleTransitions() {
    assertThat(TaskStateMachine.canTransit(TaskStatus.PENDING, TaskStatus.EXECUTING)).isFalse();
    assertThat(TaskStateMachine.canTransit(TaskStatus.PLANNING, TaskStatus.COMPLETED)).isFalse();
    assertThat(TaskStateMachine.canTransit(TaskStatus.COMPLETED, TaskStatus.FAILED)).isFalse();
  }

  @Test
  void allowsFailureFromNonTerminalStatesOnly() {
    assertThat(TaskStateMachine.canTransit(TaskStatus.PENDING, TaskStatus.FAILED)).isTrue();
    assertThat(TaskStateMachine.canTransit(TaskStatus.PLANNING, TaskStatus.FAILED)).isTrue();
    assertThat(TaskStateMachine.canTransit(TaskStatus.EXECUTING, TaskStatus.FAILED)).isTrue();
    assertThat(TaskStateMachine.canTransit(TaskStatus.REVIEWING, TaskStatus.FAILED)).isTrue();
    assertThat(TaskStateMachine.canTransit(TaskStatus.COMPLETED, TaskStatus.FAILED)).isFalse();
  }
}
