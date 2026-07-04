package com.eduspark.agent.worksheet.attempt;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
class WorksheetAttemptIntegrationTest {

  @Autowired private MockMvc mockMvc;

  @Test
  void submitsAndListsWorksheetAttempt() throws Exception {
    AuthSession user = register();
    String documentId = uploadDocument(user);
    String worksheetId = createWorksheet(user, documentId);
    String questionId =
        com.jayway.jsonpath.JsonPath.read(
            findWorksheet(user, worksheetId), "$.questions[0].id");
    Object correctAnswer =
        com.jayway.jsonpath.JsonPath.read(
            findWorksheet(user, worksheetId), "$.questions[0].answer");

    mockMvc
        .perform(
            post("/api/users/{userId}/worksheets/{worksheetId}/attempts", user.userId(), worksheetId)
                .header("Authorization", "Bearer " + user.token())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"answers":[{"questionId":"%s","answer":"%s"}]}
                    """
                        .formatted(questionId, String.valueOf(correctAnswer))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.attemptId").isNotEmpty())
        .andExpect(jsonPath("$.score").isNumber())
        .andExpect(jsonPath("$.items[0].questionId").value(questionId));

    mockMvc
        .perform(
            get("/api/users/{userId}/worksheets/{worksheetId}/attempts", user.userId(), worksheetId)
                .header("Authorization", "Bearer " + user.token()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].attemptId").isNotEmpty());
  }

  @Test
  void crossUserAttemptListReturnsNotFound() throws Exception {
    AuthSession owner = register();
    AuthSession other = register();
    String worksheetId = createWorksheet(owner, uploadDocument(owner));

    mockMvc
        .perform(
            get("/api/users/{userId}/worksheets/{worksheetId}/attempts", other.userId(), worksheetId)
                .header("Authorization", "Bearer " + other.token()))
        .andExpect(status().isNotFound());
  }

  private String createWorksheet(AuthSession user, String documentId) throws Exception {
    String response =
        mockMvc
            .perform(
                post("/api/users/{userId}/worksheets", user.userId())
                    .header("Authorization", "Bearer " + user.token())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {
                          "title": "Fraction practice",
                          "documentIds": ["%s"],
                          "questionCount": 2,
                          "gradeLevel": "Grade 5",
                          "difficulty": "Medium",
                          "questionTypes": ["Multiple choice"],
                          "includeExplanation": true
                        }
                        """
                            .formatted(documentId)))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    return com.jayway.jsonpath.JsonPath.read(response, "$.worksheetId");
  }

  private String findWorksheet(AuthSession user, String worksheetId) throws Exception {
    return mockMvc
        .perform(
            get("/api/users/{userId}/worksheets/{worksheetId}", user.userId(), worksheetId)
                .header("Authorization", "Bearer " + user.token()))
        .andExpect(status().isOk())
        .andReturn()
        .getResponse()
        .getContentAsString();
  }

  private String uploadDocument(AuthSession user) throws Exception {
    MockMultipartFile file =
        new MockMultipartFile(
            "file", "source.txt", "text/plain", "Fractions with same denominators can be added.".getBytes(StandardCharsets.UTF_8));
    String response =
        mockMvc
            .perform(
                multipart("/api/users/{userId}/documents", user.userId())
                    .file(file)
                    .header("Authorization", "Bearer " + user.token()))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    return com.jayway.jsonpath.JsonPath.read(response, "$.id");
  }

  private AuthSession register() throws Exception {
    String username = "attempt-test-" + UUID.randomUUID();
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
