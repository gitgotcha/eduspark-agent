package com.eduspark.agent.vector;

import com.eduspark.agent.document.EduDocument;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class DocumentIndexService {

  private final DocumentChunkMapper documentChunkMapper;
  private final DocumentChunker documentChunker;
  private final EmbeddingService embeddingService;

  public DocumentIndexService(
      DocumentChunkMapper documentChunkMapper,
      DocumentChunker documentChunker,
      EmbeddingService embeddingService) {
    this.documentChunkMapper = documentChunkMapper;
    this.documentChunker = documentChunker;
    this.embeddingService = embeddingService;
  }

  public void index(EduDocument document) {
    if (document == null) {
      return;
    }

    List<String> chunks = documentChunker.chunk(document.getExtractedText());
    for (int i = 0; i < chunks.size(); i++) {
      String content = chunks.get(i);
      DocumentChunk chunk = new DocumentChunk();
      chunk.setId(UUID.randomUUID().toString());
      chunk.setUserId(document.getUserId());
      chunk.setDocumentId(document.getId());
      chunk.setChunkIndex(i);
      chunk.setContent(content);
      chunk.setEmbeddingJson(embeddingService.embed(content));
      chunk.setTokenCount(content.length() / 2);
      chunk.setCreatedAt(LocalDateTime.now());
      documentChunkMapper.insert(chunk);
    }
  }
}
