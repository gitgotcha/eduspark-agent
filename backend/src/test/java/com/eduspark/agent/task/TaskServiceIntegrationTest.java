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
class TaskServiceIntegrationTest {

  @Autowired private TaskService taskService;

  @Autowired private TaskMapper taskMapper;

  @Autowired private TaskLogMapper taskLogMapper;

  @Test
  void createTaskPersistsPendingTaskAndInitialLog() {
    TaskCreateResponse response = taskService.createTask(new TaskCreateRequest("生成一份初中数学练习"));

    EduTask task = taskMapper.selectById(response.taskId());
    List<EduTaskLog> logs = taskLogMapper.selectByTaskId(response.taskId());

    assertThat(response.status()).isEqualTo(TaskStatus.PENDING);
    assertThat(task.getUserInstruction()).isEqualTo("生成一份初中数学练习");
    assertThat(task.getStatus()).isEqualTo(TaskStatus.PENDING);
    assertThat(logs).hasSize(1);
    assertThat(logs.get(0).getStage()).isEqualTo("PENDING");
    assertThat(logs.get(0).getMessage()).isEqualTo("Task created");
  }
}
