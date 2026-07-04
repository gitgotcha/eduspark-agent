package com.eduspark.agent.task;

import static org.assertj.core.api.Assertions.assertThat;

import com.eduspark.agent.task.dto.TaskCreateRequest;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class MockTaskOrchestratorIntegrationTest {

  @Autowired private TaskService taskService;

  @Autowired private TaskMapper taskMapper;

  @Autowired private TaskLogMapper taskLogMapper;

  @Autowired private MockTaskOrchestrator orchestrator;

  @Test
  void runMockLifecyclePersistsEveryStateTransition() {
    TaskCreateResponse response = taskService.createTask(new TaskCreateRequest("生成学习计划"));

    orchestrator.run(response.taskId());

    EduTask task = taskMapper.selectById(response.taskId());
    List<String> stages =
        taskLogMapper.selectByTaskId(response.taskId()).stream().map(EduTaskLog::getStage).toList();

    assertThat(task.getStatus()).isEqualTo(TaskStatus.COMPLETED);
    assertThat(stages)
        .containsExactly("PENDING", "PLANNING", "EXECUTING", "REVIEWING", "COMPLETED");
  }
}
