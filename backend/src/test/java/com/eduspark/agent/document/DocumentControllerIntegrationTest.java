package com.eduspark.agent.document;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.http.MediaType;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DocumentControllerIntegrationTest {

  @Autowired private MockMvc mockMvc;

  @Test
  void uploadsTextDocumentAndReturnsExtractedText() throws Exception {
    AuthSession user = register();
    MockMultipartFile file =
        new MockMultipartFile(
            "file",
            "lesson.txt",
            "text/plain",
            "本节课学习分数加减法。".getBytes(StandardCharsets.UTF_8));

    mockMvc
        .perform(
            multipart("/api/users/{userId}/documents", user.userId())
                .file(file)
                .header("Authorization", "Bearer " + user.token()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").isNotEmpty())
        .andExpect(jsonPath("$.userId").value(user.userId()))
        .andExpect(jsonPath("$.fileName").value("lesson.txt"))
        .andExpect(jsonPath("$.mimeType").value("text/plain"))
        .andExpect(jsonPath("$.extractedText").value("本节课学习分数加减法。"));
  }

  @Test
  void getsUploadedDocumentById() throws Exception {
    AuthSession user = register();
    MockMultipartFile file =
        new MockMultipartFile(
            "file",
            "reading.txt",
            "text/plain",
            "阅读材料：春天来了。".getBytes(StandardCharsets.UTF_8));

    MvcResult result =
        mockMvc
            .perform(
                multipart("/api/users/{userId}/documents", user.userId())
                    .file(file)
                    .header("Authorization", "Bearer " + user.token()))
            .andExpect(status().isOk())
            .andReturn();

    String documentId =
        com.jayway.jsonpath.JsonPath.read(result.getResponse().getContentAsString(), "$.id");

    mockMvc
        .perform(
            get("/api/users/{userId}/documents/{documentId}", user.userId(), documentId)
                .header("Authorization", "Bearer " + user.token()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(documentId))
        .andExpect(jsonPath("$.userId").value(user.userId()))
        .andExpect(jsonPath("$.fileName").value("reading.txt"))
        .andExpect(jsonPath("$.extractedText").value("阅读材料：春天来了。"));
  }

  @Test
  void listsDeletesAndReindexesUserScopedDocuments() throws Exception {
    AuthSession user = register();
    AuthSession otherUser = register();
    String documentId = upload(user, "history.txt", "History material for list delete reindex.");
    upload(otherUser, "other.txt", "Other user material should stay isolated.");

    mockMvc
        .perform(
            get("/api/users/{userId}/documents", user.userId())
                .header("Authorization", "Bearer " + user.token()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].id").value(documentId))
        .andExpect(jsonPath("$[0].userId").value(user.userId()))
        .andExpect(jsonPath("$[0].textPreview").value("History material for list delete reindex."));

    mockMvc
        .perform(
            post("/api/users/{userId}/documents/{documentId}/reindex", user.userId(), documentId)
                .header("Authorization", "Bearer " + user.token()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(documentId))
        .andExpect(jsonPath("$.parseStatus").value("PARSED"));

    mockMvc
        .perform(
            delete("/api/users/{userId}/documents/{documentId}", user.userId(), documentId)
                .header("Authorization", "Bearer " + user.token()))
        .andExpect(status().isNoContent());

    mockMvc
        .perform(
            get("/api/users/{userId}/documents/{documentId}", user.userId(), documentId)
                .header("Authorization", "Bearer " + user.token()))
        .andExpect(status().isNotFound());
  }

  @Test
  void allowsDeletePreflightForUserScopedApis() throws Exception {
    mockMvc
        .perform(
            options("/api/users/{userId}/documents/{documentId}", "user-1", "document-1")
                .header("Origin", "http://localhost:5173")
                .header("Access-Control-Request-Method", "DELETE")
                .header("Access-Control-Request-Headers", "authorization,content-type"))
        .andExpect(status().isOk());
  }

  private String upload(AuthSession user, String fileName, String content) throws Exception {
    MockMultipartFile file =
        new MockMultipartFile(
            "file", fileName, "text/plain", content.getBytes(StandardCharsets.UTF_8));

    MvcResult result =
        mockMvc
            .perform(
                multipart("/api/users/{userId}/documents", user.userId())
                    .file(file)
                    .header("Authorization", "Bearer " + user.token()))
            .andExpect(status().isOk())
            .andReturn();
    return com.jayway.jsonpath.JsonPath.read(result.getResponse().getContentAsString(), "$.id");
  }

  private AuthSession register() throws Exception {
    String username = "document-test-" + UUID.randomUUID();
    String body =
        """
        {"username":"%s","password":"secret123"}
        """
            .formatted(username);
    String response =
        mockMvc
            .perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
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
