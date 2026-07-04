package com.eduspark.agent.worksheet.wrong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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
class WrongQuestionIntegrationTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private WrongQuestionMapper wrongQuestionMapper;

  @Test
  void collectsAndResolvesWrongQuestions() throws Exception {
    AuthSession user = register();
    String worksheetId = createWorksheet(user, uploadDocument(user));
    WorksheetSnapshot snapshot = loadWorksheet(user, worksheetId);

    String wrongAnswer = snapshot.correctAnswer().equals("A") ? "B" : "A";

    mockMvc
        .perform(
            post("/api/users/{userId}/worksheets/{worksheetId}/attempts", user.userId(), worksheetId)
                .header("Authorization", "Bearer " + user.token())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"answers":[{"questionId":"%s","answer":"%s"}]}
                    """
                        .formatted(snapshot.questionId(), wrongAnswer)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.score").value(0.0));

    String wrongQuestionsJson =
        mockMvc
            .perform(
                get("/api/users/{userId}/wrong-questions", user.userId())
                    .header("Authorization", "Bearer " + user.token()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].questionId").value(snapshot.questionId()))
            .andExpect(jsonPath("$[0].resolved").value(false))
            .andReturn()
            .getResponse()
            .getContentAsString();

    String wrongQuestionId = com.jayway.jsonpath.JsonPath.read(wrongQuestionsJson, "$[0].id");

    mockMvc
        .perform(
            delete("/api/users/{userId}/wrong-questions/{wrongQuestionId}", user.userId(), wrongQuestionId)
                .header("Authorization", "Bearer " + user.token()))
        .andExpect(status().isOk());

    assertThat(wrongQuestionMapper.selectByIdAndUserId(user.userId(), wrongQuestionId)).isNull();

    mockMvc
        .perform(
            get("/api/users/{userId}/wrong-questions", user.userId())
                .header("Authorization", "Bearer " + user.token()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isArray());
  }

  @Test
  void retriesFromWrongQuestionsAndLinksRetryWorksheet() throws Exception {
    AuthSession user = register();
    String worksheetId = createWorksheet(user, uploadDocument(user));
    WorksheetSnapshot snapshot = loadWorksheet(user, worksheetId);

    mockMvc
        .perform(
            post("/api/users/{userId}/worksheets/{worksheetId}/attempts", user.userId(), worksheetId)
                .header("Authorization", "Bearer " + user.token())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"answers":[{"questionId":"%s","answer":"%s"}]}
                    """
                        .formatted(snapshot.questionId(), snapshot.correctAnswer().equals("A") ? "B" : "A")))
        .andExpect(status().isOk());

    String wrongQuestionId =
        com.jayway.jsonpath.JsonPath.read(
            mockMvc
                .perform(
                    get("/api/users/{userId}/wrong-questions", user.userId())
                        .header("Authorization", "Bearer " + user.token()))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            "$[0].id");

    String retryResponse =
        mockMvc
            .perform(
                post("/api/users/{userId}/wrong-questions/retry", user.userId())
                    .header("Authorization", "Bearer " + user.token())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {"wrongQuestionIds":["%s"],"title":"错题再练"}
                        """
                            .formatted(wrongQuestionId)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.worksheetId").isNotEmpty())
            .andExpect(jsonPath("$.taskId").isNotEmpty())
            .andReturn()
            .getResponse()
            .getContentAsString();

    String retryWorksheetId = com.jayway.jsonpath.JsonPath.read(retryResponse, "$.worksheetId");

    mockMvc
        .perform(
            get("/api/users/{userId}/worksheets/{worksheetId}/wrong-questions", user.userId(), worksheetId)
                .header("Authorization", "Bearer " + user.token()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].retryWorksheetId").value(retryWorksheetId));
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

  private WorksheetSnapshot loadWorksheet(AuthSession user, String worksheetId) throws Exception {
    String response =
        mockMvc
            .perform(
                get("/api/users/{userId}/worksheets/{worksheetId}", user.userId(), worksheetId)
                    .header("Authorization", "Bearer " + user.token()))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    return new WorksheetSnapshot(
        com.jayway.jsonpath.JsonPath.read(response, "$.questions[0].id"),
        readAnswer(response));
  }

  private String readAnswer(String response) {
    Object answer = com.jayway.jsonpath.JsonPath.read(response, "$.questions[0].answer");
    return answer == null ? "" : answer.toString();
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
    String username = "wrong-question-test-" + UUID.randomUUID();
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

  private record WorksheetSnapshot(String questionId, String correctAnswer) {}
}
