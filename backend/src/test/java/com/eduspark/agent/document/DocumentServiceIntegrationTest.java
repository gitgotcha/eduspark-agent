package com.eduspark.agent.document;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

import com.eduspark.agent.vector.DocumentIndexService;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.server.ResponseStatusException;

@SpringBootTest
@ActiveProfiles("test")
class DocumentServiceIntegrationTest {

  @Autowired private DocumentService documentService;
  @Autowired private DocumentMapper documentMapper;

  @MockitoBean private DocumentIndexService documentIndexService;

  @Test
  void uploadReturnsAndPersistsDocumentWhenIndexingFails() {
    doThrow(new RuntimeException("index failed")).when(documentIndexService).index(any(EduDocument.class));
    String userId = UUID.randomUUID().toString();
    MockMultipartFile file =
        new MockMultipartFile(
            "file",
            "index-failure.txt",
            "text/plain",
            "上传成功不应依赖索引成功。".getBytes(StandardCharsets.UTF_8));

    EduDocument document = documentService.upload(userId, file);

    assertThat(document.getId()).isNotBlank();
    assertThat(document.getUserId()).isEqualTo(userId);
    EduDocument persisted = documentMapper.selectById(document.getId());
    assertThat(persisted).isNotNull();
    assertThat(persisted.getUserId()).isEqualTo(userId);
    assertThat(persisted.getExtractedText()).isEqualTo("上传成功不应依赖索引成功。");
    assertThat(persisted.getParseStatus()).isEqualTo("INDEX_FAILED");
    assertThat(persisted.getParseError()).contains("index failed");
    assertThat(persisted.getTextPreview()).contains("上传成功");
  }

  @Test
  void uploadRejectsUnreadablePdfBinaryContent() {
    String userId = UUID.randomUUID().toString();
    MockMultipartFile file =
        new MockMultipartFile(
            "file",
            "broken.pdf",
            "application/pdf",
            new byte[] {0x25, 0x50, 0x44, 0x46, 0x00, 0x01, 0x02});

    assertThatThrownBy(() -> documentService.upload(userId, file))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("Document parse failed");
  }
}
