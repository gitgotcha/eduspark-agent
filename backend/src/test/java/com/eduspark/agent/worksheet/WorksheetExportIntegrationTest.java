package com.eduspark.agent.worksheet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class WorksheetExportIntegrationTest {

  private static final String DOCX_CONTENT_TYPE =
      "application/vnd.openxmlformats-officedocument.wordprocessingml.document";

  @Autowired private MockMvc mockMvc;

  @Test
  void exportsGeneratedWorksheetAsWordDocument() throws Exception {
    AuthSession user = register();
    String documentId = uploadDocument(user);
    String createResponse =
        createWorksheet(
            user,
            """
            {
              "title": "Word练习",
              "documentIds": ["%s"],
              "questionCount": 2,
              "gradeLevel": "五年级",
              "difficulty": "中等",
              "questionTypes": ["选择题", "判断题"],
              "includeExplanation": true
            }
            """
                .formatted(documentId));
    String worksheetId = JsonPath.read(createResponse, "$.worksheetId");

    byte[] exportedBytes =
        mockMvc
            .perform(
                get("/api/users/{userId}/worksheets/{worksheetId}/export.docx", user.userId(), worksheetId)
                    .header("Authorization", "Bearer " + user.token()))
            .andExpect(status().isOk())
            .andExpect(header().string(HttpHeaders.CONTENT_TYPE, DOCX_CONTENT_TYPE))
            .andReturn()
            .getResponse()
            .getContentAsByteArray();

    assertThat(readDocumentText(exportedBytes)).contains("Word练习", "AI 出题依据", "答案与解析");
  }

  private String readDocumentText(byte[] exportedBytes) throws Exception {
    try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(exportedBytes))) {
      return document.getParagraphs().stream()
          .map(paragraph -> paragraph.getText())
          .reduce("", (left, right) -> left + "\n" + right);
    }
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
            "word-export-source.txt",
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
    return JsonPath.read(response, "$.id");
  }

  private AuthSession register() throws Exception {
    String username = "worksheet-export-test-" + UUID.randomUUID();
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
    return new AuthSession(JsonPath.read(response, "$.userId"), JsonPath.read(response, "$.token"));
  }

  private record AuthSession(String userId, String token) {}
}
