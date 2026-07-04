package com.eduspark.agent.task;

import static org.assertj.core.api.Assertions.assertThat;

import com.eduspark.agent.document.DocumentService;
import com.eduspark.agent.task.dto.TaskCreateRequest;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class TaskOrchestratorIntegrationTest {

  @Autowired private TaskService taskService;

  @Autowired private TaskMapper taskMapper;

  @Autowired private TaskLogMapper taskLogMapper;

  @Autowired private TaskOrchestrator orchestrator;

  @Autowired private DocumentService documentService;

  @Test
  void runsPlannerAndRegisteredToolsThenPersistsPlanAndFinalAnswer() {
    TaskCreateResponse response = taskService.createTask(new TaskCreateRequest("本节课学习分数加减法。"));

    orchestrator.run(response.taskId());

    EduTask task = taskMapper.selectById(response.taskId());
    List<String> messages =
        taskLogMapper.selectByTaskId(response.taskId()).stream().map(EduTaskLog::getMessage).toList();

    assertThat(task.getStatus()).isEqualTo(TaskStatus.COMPLETED);
    assertThat(task.getPlanJson()).contains("textSummaryTool", "quizGeneratorTool");
    assertThat(task.getFinalAnswer()).contains("textSummaryTool", "quizGeneratorTool", "summary");
    assertThat(messages)
        .contains(
            "Planner generated 2 tool steps",
            "Tool executed: textSummaryTool",
            "Tool executed: quizGeneratorTool");
  }

  @Test
  void includesUploadedDocumentTextInPlannerInput() {
    String documentId =
        documentService
            .upload(
                new MockMultipartFile(
                    "file",
                    "fractions.txt",
                    "text/plain",
                    "文档知识点：同分母分数相加。".getBytes(StandardCharsets.UTF_8)))
            .getId();
    TaskCreateResponse response =
        taskService.createTask(new TaskCreateRequest("请生成课堂练习", List.of(documentId)));

    orchestrator.run(response.taskId());

    EduTask task = taskMapper.selectById(response.taskId());
    assertThat(task.getPlanJson()).contains("请生成课堂练习", "文档知识点：同分母分数相加。");
  }
}
