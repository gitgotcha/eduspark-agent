package com.eduspark.agent.vector;

import static org.assertj.core.api.Assertions.assertThat;

import com.eduspark.agent.document.DocumentService;
import com.eduspark.agent.document.EduDocument;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class DocumentIndexServiceIntegrationTest {

  @Autowired private DocumentService documentService;
  @Autowired private DocumentChunkMapper documentChunkMapper;

  @Test
  void uploadsTextDocumentAndPersistsEmbeddingChunks() {
    String userId = UUID.randomUUID().toString();
    String text =
        "这是一份用于切片索引的课堂资料。".repeat(120)
            + "最后一段用于确认长文本上传后会被自动索引。";
    MockMultipartFile file =
        new MockMultipartFile(
            "file", "long-lesson.txt", "text/plain", text.getBytes(StandardCharsets.UTF_8));

    EduDocument document = documentService.upload(userId, file);

    List<DocumentChunk> chunks =
        documentChunkMapper.selectByDocumentIdAndUserId(document.getId(), userId);
    assertThat(chunks).isNotEmpty();
    assertThat(chunks)
        .allSatisfy(
            chunk -> {
              assertThat(chunk.getUserId()).isEqualTo(userId);
              assertThat(chunk.getDocumentId()).isEqualTo(document.getId());
              assertThat(chunk.getEmbeddingJson()).startsWith("[");
            });
  }
}
