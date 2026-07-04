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
class TaskArtifactPersistenceIntegrationTest {

  @Autowired private TaskService taskService;

  @Autowired private TaskOrchestrator orchestrator;

  @Autowired private TaskArtifactMapper taskArtifactMapper;

  @Test
  void persistsOneArtifactForEachExecutedTool() {
    TaskCreateResponse response = taskService.createTask(new TaskCreateRequest("本节课学习分数加减法。"));

    orchestrator.run(response.taskId());

    List<EduTaskArtifact> artifacts = taskArtifactMapper.selectByTaskId(response.taskId());

    assertThat(artifacts).hasSize(2);
    assertThat(artifacts).extracting(EduTaskArtifact::getArtifactType)
        .containsExactly("textSummaryTool", "quizGeneratorTool");
    assertThat(artifacts.get(0).getContentJson()).contains("summary", "本节课学习");
    assertThat(artifacts.get(1).getContentJson()).contains("questions", "answer");
  }
}
