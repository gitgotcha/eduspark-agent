package com.eduspark.agent.worksheet;

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
class WorksheetControllerIntegrationTest {

  @Autowired private MockMvc mockMvc;

  @Test
  void createsWorksheetAndAssociatedTask() throws Exception {
    AuthSession user = register();
    String documentId = uploadDocument(user);

    mockMvc
        .perform(
            post("/api/users/{userId}/worksheets", user.userId())
                .header("Authorization", "Bearer " + user.token())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "title": "分数加减法练习",
                      "documentIds": ["%s"],
                      "questionCount": 5,
                      "gradeLevel": "五年级",
                      "difficulty": "中等",
                      "questionTypes": ["选择题", "应用题"],
                      "includeExplanation": true
                    }
                    """
                        .formatted(documentId)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.worksheetId").isNotEmpty())
        .andExpect(jsonPath("$.taskId").isNotEmpty())
        .andExpect(jsonPath("$.status").value("COMPLETED"));
  }

  @Test
  void generatedWorksheetCanBeRetrievedWithQuestions() throws Exception {
    AuthSession user = register();
    String documentId = uploadDocument(user);

    String createResponse =
        mockMvc
            .perform(
                post("/api/users/{userId}/worksheets", user.userId())
                    .header("Authorization", "Bearer " + user.token())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {
                          "title": "分数加减法练习",
                          "documentIds": ["%s"],
                          "questionCount": 3,
                          "gradeLevel": "五年级",
                          "difficulty": "中等",
                          "questionTypes": ["选择题", "判断题"],
                          "includeExplanation": true
                        }
                        """
                            .formatted(documentId)))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    String worksheetId = com.jayway.jsonpath.JsonPath.read(createResponse, "$.worksheetId");

    mockMvc
        .perform(
            get("/api/users/{userId}/worksheets/{worksheetId}", user.userId(), worksheetId)
                .header("Authorization", "Bearer " + user.token()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("COMPLETED"))
        .andExpect(jsonPath("$.generationRationale.summary").isNotEmpty())
        .andExpect(jsonPath("$.generationRationale.keyPoints").isArray())
        .andExpect(jsonPath("$.questions").isArray())
        .andExpect(jsonPath("$.questions[0].stem").isNotEmpty())
        .andExpect(jsonPath("$.questions[0].sourceChunkIds").isArray());
  }

  @Test
  void generatedWorksheetCompletesAssociatedTask() throws Exception {
    AuthSession user = register();
    String documentId = uploadDocument(user);

    String createResponse =
        createWorksheet(
            user,
            """
            {
              "title": "分数加减法练习",
              "documentIds": ["%s"],
              "questionCount": 2,
              "gradeLevel": "五年级",
              "difficulty": "中等",
              "questionTypes": ["选择题"],
              "includeExplanation": false
            }
            """
                .formatted(documentId));
    String taskId = com.jayway.jsonpath.JsonPath.read(createResponse, "$.taskId");

    mockMvc
        .perform(
            get("/api/users/{userId}/tasks/{taskId}", user.userId(), taskId)
                .header("Authorization", "Bearer " + user.token()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("COMPLETED"));
  }

  @Test
  void listsWorksheetsForUserInNewestFirstOrder() throws Exception {
    AuthSession user = register();
    AuthSession otherUser = register();
    String userDocumentId = uploadDocument(user);
    String otherUserDocumentId = uploadDocument(otherUser);

    String firstWorksheetId =
        com.jayway.jsonpath.JsonPath.read(
            createWorksheet(
                user,
                """
                {
                  "title": "第一份练习卷",
                  "documentIds": ["%s"],
                  "questionCount": 2,
                  "gradeLevel": "五年级",
                  "difficulty": "中等",
                  "questionTypes": ["选择题"],
                  "includeExplanation": true
                }
                    """
                    .formatted(userDocumentId)),
            "$.worksheetId");

    Thread.sleep(1100L);

    String secondWorksheetId =
        com.jayway.jsonpath.JsonPath.read(
            createWorksheet(
                user,
                """
                {
                  "title": "第二份练习卷",
                  "documentIds": ["%s"],
                  "questionCount": 2,
                  "gradeLevel": "五年级",
                  "difficulty": "中等",
                  "questionTypes": ["选择题"],
                  "includeExplanation": true
                }
                """
                    .formatted(userDocumentId)),
            "$.worksheetId");

    createWorksheet(
        otherUser,
        """
        {
          "title": "其他用户练习卷",
          "documentIds": ["%s"],
          "questionCount": 2,
          "gradeLevel": "五年级",
          "difficulty": "中等",
          "questionTypes": ["选择题"],
          "includeExplanation": true
        }
        """
            .formatted(otherUserDocumentId));

    mockMvc
        .perform(
            get("/api/users/{userId}/worksheets", user.userId())
                .param("limit", "20")
                .header("Authorization", "Bearer " + user.token()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(2))
        .andExpect(jsonPath("$[0].id").value(secondWorksheetId))
        .andExpect(jsonPath("$[0].taskId").isNotEmpty())
        .andExpect(jsonPath("$[0].title").value("第二份练习卷"))
        .andExpect(jsonPath("$[0].status").isNotEmpty())
        .andExpect(jsonPath("$[0].createdAt").isNotEmpty())
        .andExpect(jsonPath("$[0].updatedAt").isNotEmpty())
        .andExpect(jsonPath("$[1].id").value(firstWorksheetId))
        .andExpect(jsonPath("$[1].title").value("第一份练习卷"));
  }

  @Test
  void rejectsBlankQuestionType() throws Exception {
    AuthSession user = register();
    String documentId = uploadDocument(user);

    mockMvc
        .perform(
            post("/api/users/{userId}/worksheets", user.userId())
                .header("Authorization", "Bearer " + user.token())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "title": "分数加减法练习",
                      "documentIds": ["%s"],
                      "questionCount": 2,
                      "gradeLevel": "五年级",
                      "difficulty": "中等",
                      "questionTypes": ["选择题", "   "],
                      "includeExplanation": true
                    }
                    """
                        .formatted(documentId)))
        .andExpect(status().isBadRequest());
  }

  @Test
  void crossUserWorksheetDetailReturnsNotFound() throws Exception {
    AuthSession owner = register();
    AuthSession otherUser = register();
    String documentId = uploadDocument(owner);

    String createResponse =
        createWorksheet(
            owner,
            """
            {
              "title": "分数加减法练习",
              "documentIds": ["%s"],
              "questionCount": 2,
              "gradeLevel": "五年级",
              "difficulty": "中等",
              "questionTypes": ["选择题"],
              "includeExplanation": true
            }
            """
                .formatted(documentId));
    String worksheetId = com.jayway.jsonpath.JsonPath.read(createResponse, "$.worksheetId");

    mockMvc
        .perform(
            get("/api/users/{userId}/worksheets/{worksheetId}", otherUser.userId(), worksheetId)
                .header("Authorization", "Bearer " + otherUser.token()))
        .andExpect(status().isNotFound());
  }

  private String createWorksheet(AuthSession user, String requestJson) throws Exception {
    return mockMvc
        .perform(
            post("/api/users/{userId}/worksheets", user.userId())
                .header("Authorization", "Bearer " + user.token())
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
        .andExpect(status().isOk())
        .andReturn()
        .getResponse()
        .getContentAsString();
  }

  private String uploadDocument(AuthSession user) throws Exception {
    MockMultipartFile file =
        new MockMultipartFile(
            "file",
            "worksheet-source.txt",
            "text/plain",
            "课堂材料：同分母分数加减法。".getBytes(StandardCharsets.UTF_8));

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
    String username = "worksheet-test-" + UUID.randomUUID();
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
