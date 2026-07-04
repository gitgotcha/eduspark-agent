package com.eduspark.agent.auth;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import jakarta.servlet.DispatcherType;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UserScopedApiIntegrationTest {

  @Autowired private MockMvc mockMvc;

  @Test
  void rejectsUserScopedApiWithoutToken() throws Exception {
    mockMvc
        .perform(
            post("/api/users/user-1/tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"instruction\":\"生成练习\"}"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void doesNotRejectErrorDispatchAtSecurityLayerAfterSseResponseHasCommitted() throws Exception {
    mockMvc
        .perform(
            get("/api/users/user-1/tasks/task-1/stream")
                .with(request -> {
                  request.setDispatcherType(DispatcherType.ERROR);
                  return request;
                }))
        .andExpect(status().isNotFound());
  }

  @Test
  void rejectsPathUserIdThatDoesNotMatchJwtSubject() throws Exception {
    AuthSession alice = register("teacher-forbidden-a");
    AuthSession bob = register("teacher-forbidden-b");

    mockMvc
        .perform(
            post("/api/users/{userId}/tasks", bob.userId())
                .header("Authorization", "Bearer " + alice.token())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"instruction\":\"生成练习\"}"))
        .andExpect(status().isForbidden());
  }

  @Test
  void createsTaskAndDocumentInsideUserScope() throws Exception {
    AuthSession user = register("teacher-scope-owner");
    String documentId = uploadDocument(user, "lesson.txt", "课堂材料：比例应用题。");

    String taskId =
        JsonPath.read(
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
                .andExpect(jsonPath("$.taskId").isNotEmpty())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            "$.taskId");

    mockMvc
        .perform(
            get("/api/users/{userId}/documents/{documentId}", user.userId(), documentId)
                .header("Authorization", "Bearer " + user.token()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(documentId))
        .andExpect(jsonPath("$.userId").value(user.userId()))
        .andExpect(jsonPath("$.taskId").value(taskId));

    mockMvc
        .perform(
            get("/api/users/{userId}/tasks/{taskId}", user.userId(), taskId)
                .header("Authorization", "Bearer " + user.token()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(taskId))
        .andExpect(jsonPath("$.userId").value(user.userId()));
  }

  @Test
  void hidesOtherUsersTaskAndDocument() throws Exception {
    AuthSession owner = register("teacher-resource-owner");
    AuthSession other = register("teacher-resource-other");
    String documentId = uploadDocument(owner, "owner.txt", "仅 owner 可见。");
    String taskId =
        JsonPath.read(
            mockMvc
                .perform(
                    post("/api/users/{userId}/tasks", owner.userId())
                        .header("Authorization", "Bearer " + owner.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                            """
                            {"instruction":"生成练习","documentIds":["%s"]}
                            """
                                .formatted(documentId)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            "$.taskId");

    mockMvc
        .perform(
            get("/api/users/{userId}/tasks/{taskId}", other.userId(), taskId)
                .header("Authorization", "Bearer " + other.token()))
        .andExpect(status().isNotFound());

    mockMvc
        .perform(
            get("/api/users/{userId}/documents/{documentId}", other.userId(), documentId)
                .header("Authorization", "Bearer " + other.token()))
        .andExpect(status().isNotFound());
  }

  @Test
  void rejectsBindingAnotherUsersDocumentToTask() throws Exception {
    AuthSession owner = register("teacher-document-owner");
    AuthSession other = register("teacher-document-other");
    String documentId = uploadDocument(owner, "owner.txt", "不能被其他用户绑定。");

    mockMvc
        .perform(
            post("/api/users/{userId}/tasks", other.userId())
                .header("Authorization", "Bearer " + other.token())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"instruction":"生成练习","documentIds":["%s"]}
                    """
                        .formatted(documentId)))
        .andExpect(status().isNotFound());
  }

  @Test
  void protectsSseStreamWithUserScope() throws Exception {
    AuthSession owner = register("teacher-stream-owner");
    AuthSession other = register("teacher-stream-other");
    String taskId =
        JsonPath.read(
            mockMvc
                .perform(
                    post("/api/users/{userId}/tasks", owner.userId())
                        .header("Authorization", "Bearer " + owner.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"instruction\":\"模拟 SSE\"}"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            "$.taskId");

    mockMvc
        .perform(
            get("/api/users/{userId}/tasks/{taskId}/stream", other.userId(), taskId)
                .header("Authorization", "Bearer " + other.token()))
        .andExpect(status().isNotFound());

    mockMvc
        .perform(
            get("/api/users/{userId}/tasks/{taskId}/stream", owner.userId(), taskId)
                .header("Authorization", "Bearer " + owner.token()))
        .andExpect(status().isOk())
        .andExpect(request().asyncStarted());
  }

  private AuthSession register(String username) throws Exception {
    MvcResult result =
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
            .andReturn();
    String body = result.getResponse().getContentAsString();
    return new AuthSession(JsonPath.read(body, "$.userId"), JsonPath.read(body, "$.token"));
  }

  private String uploadDocument(AuthSession user, String fileName, String text) throws Exception {
    MockMultipartFile file =
        new MockMultipartFile(
            "file", fileName, "text/plain", text.getBytes(StandardCharsets.UTF_8));
    return JsonPath.read(
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
  }

  private record AuthSession(String userId, String token) {}
}
