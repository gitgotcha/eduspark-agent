package com.eduspark.agent.task;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eduspark.agent.task.dto.TaskCreateRequest;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TaskControllerIntegrationTest {

  @Autowired private MockMvc mockMvc;

  @Autowired private TaskService taskService;

  @Autowired private TaskMapper taskMapper;

  @Autowired private TaskOrchestrator taskOrchestrator;

  @Test
  void createTaskEndpointReturnsPendingTaskId() throws Exception {
    AuthSession user = register();
    mockMvc
        .perform(
            post("/api/users/{userId}/tasks", user.userId())
                .header("Authorization", "Bearer " + user.token())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"instruction\":\"生成一份英语阅读理解\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.taskId").isNotEmpty())
        .andExpect(jsonPath("$.status").value("PENDING"));
  }

  @Test
  void createTaskEndpointAttachesUploadedDocuments() throws Exception {
    AuthSession user = register();
    MockMultipartFile file =
        new MockMultipartFile(
            "file",
            "lesson.txt",
            "text/plain",
            "课堂材料：比例应用题。".getBytes(StandardCharsets.UTF_8));

    String documentId =
        com.jayway.jsonpath.JsonPath.read(
            mockMvc
                .perform(
                    multipart("/api/users/{userId}/documents", user.userId())
                        .file(file)
                        .header("Authorization", "Bearer " + user.token()))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            "$.id");

    String taskId =
        com.jayway.jsonpath.JsonPath.read(
            mockMvc
                .perform(
                    post("/api/users/{userId}/tasks", user.userId())
                        .header("Authorization", "Bearer " + user.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                            """
                            {"instruction":"基于材料生成练习","documentIds":["%s"]}
                            """
                                .formatted(documentId)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            "$.taskId");

    mockMvc
        .perform(
            get("/api/users/{userId}/documents/{documentId}", user.userId(), documentId)
                .header("Authorization", "Bearer " + user.token()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.taskId").value(taskId));
  }

  @Test
  void getTaskEndpointReturnsPersistedTask() throws Exception {
    AuthSession user = register();
    TaskCreateResponse response = taskService.createTask(user.userId(), new TaskCreateRequest("整理知识点"));

    mockMvc
        .perform(
            get("/api/users/{userId}/tasks/{taskId}", user.userId(), response.taskId())
                .header("Authorization", "Bearer " + user.token()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(response.taskId()))
        .andExpect(jsonPath("$.userId").value(user.userId()))
        .andExpect(jsonPath("$.userInstruction").value("整理知识点"))
        .andExpect(jsonPath("$.status").value("PENDING"));
  }

  @Test
  void listTasksEndpointReturnsUserScopedRecentTasks() throws Exception {
    AuthSession user = register();
    AuthSession other = register();
    TaskCreateResponse first = taskService.createTask(user.userId(), new TaskCreateRequest("第一条历史"));
    taskService.createTask(other.userId(), new TaskCreateRequest("其他用户历史"));
    TaskCreateResponse second = taskService.createTask(user.userId(), new TaskCreateRequest("第二条历史"));

    mockMvc
        .perform(
            get("/api/users/{userId}/tasks?limit=10", user.userId())
                .header("Authorization", "Bearer " + user.token()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].id").value(second.taskId()))
        .andExpect(jsonPath("$[1].id").value(first.taskId()))
        .andExpect(jsonPath("$[0].userId").value(user.userId()))
        .andExpect(jsonPath("$[1].userId").value(user.userId()));
  }

  @Test
  void getTaskEndpointReturnsContractErrorWhenTaskMissing() throws Exception {
    AuthSession user = register();
    mockMvc
        .perform(
            get("/api/users/{userId}/tasks/{taskId}", user.userId(), "missing-task-id")
                .header("Authorization", "Bearer " + user.token()))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("TASK_NOT_FOUND"))
        .andExpect(jsonPath("$.message").value("Task not found"));
  }

  @Test
  void createTaskEndpointReturnsValidationErrorForBlankInstruction() throws Exception {
    AuthSession user = register();
    mockMvc
        .perform(
            post("/api/users/{userId}/tasks", user.userId())
                .header("Authorization", "Bearer " + user.token())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"instruction\":\"\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
        .andExpect(jsonPath("$.details.instruction").isNotEmpty());
  }

  @Test
  void streamEndpointStartsSseResponseForExistingTask() throws Exception {
    AuthSession user = register();
    TaskCreateResponse response = taskService.createTask(user.userId(), new TaskCreateRequest("模拟 SSE"));

    mockMvc
        .perform(
            get("/api/users/{userId}/tasks/{taskId}/stream", user.userId(), response.taskId())
                .header("Authorization", "Bearer " + user.token()))
        .andExpect(status().isOk())
        .andExpect(request().asyncStarted());

    assertThat(taskMapper.selectById(response.taskId())).isNotNull();
  }

  @Test
  void getTaskArtifactsEndpointReturnsPersistedToolOutputs() throws Exception {
    AuthSession user = register();
    TaskCreateResponse response = taskService.createTask(user.userId(), new TaskCreateRequest("生成一组数学练习"));
    taskOrchestrator.run(response.taskId());

    mockMvc
        .perform(
            get("/api/users/{userId}/tasks/{taskId}/artifacts", user.userId(), response.taskId())
                .header("Authorization", "Bearer " + user.token()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].taskId").value(response.taskId()))
        .andExpect(jsonPath("$[0].artifactType").value("textSummaryTool"))
        .andExpect(jsonPath("$[0].contentJson").isNotEmpty())
        .andExpect(jsonPath("$[1].artifactType").value("quizGeneratorTool"));
  }

  @Test
  void corsPreflightAllowsLocalViteFrontend() throws Exception {
    mockMvc
        .perform(
            options("/api/users/user-id/tasks")
                .header("Origin", "http://localhost:5173")
                .header("Access-Control-Request-Method", "POST"))
        .andExpect(status().isOk())
        .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"));
  }

  private AuthSession register() throws Exception {
    String username = "task-test-" + UUID.randomUUID();
    String response =
        mockMvc
            .perform(
                post("/api/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {"username":"%s","password":"secret123"}
                        """
                            .formatted(username)))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    return new AuthSession(
        com.jayway.jsonpath.JsonPath.read(response, "$.userId"),
        com.jayway.jsonpath.JsonPath.read(response, "$.token"));
  }

  private record AuthSession(String userId, String token) {}
}
