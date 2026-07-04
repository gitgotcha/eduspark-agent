package com.eduspark.agent.vector;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class VectorSearchServiceTest {

  private final VectorSearchService service =
      new VectorSearchService(
          Mockito.mock(DocumentChunkMapper.class),
          Mockito.mock(EmbeddingService.class),
          new ObjectMapper());

  @Test
  void rankReturnsMostSimilarChunkFirst() {
    List<DocumentChunk> results =
        service.rank("[0.9,0.1]", List.of(chunk("close", "[1.0,0.0]"), chunk("far", "[0.0,1.0]")), 1);

    assertThat(results).extracting(DocumentChunk::getId).containsExactly("close");
  }

  @Test
  void rankRejectsNullQueryEmbeddingJsonLiteral() {
    assertThatThrownBy(() -> service.rank("null", List.of(chunk("close", "[1.0,0.0]")), 1))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void rankRejectsChunkEmbeddingWithNullElement() {
    assertThatThrownBy(() -> service.rank("[1.0,0.0]", List.of(chunk("close", "[1.0,null]")), 1))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void rankReturnsEmptyListWhenLimitIsZero() {
    List<DocumentChunk> results =
        service.rank("[0.9,0.1]", List.of(chunk("close", "[1.0,0.0]")), 0);

    assertThat(results).isEmpty();
  }

  @Test
  void rankRejectsNegativeLimit() {
    assertThatThrownBy(() -> service.rank("[0.9,0.1]", List.of(chunk("close", "[1.0,0.0]")), -1))
        .isInstanceOf(IllegalArgumentException.class);
  }

  private DocumentChunk chunk(String id, String embeddingJson) {
    DocumentChunk chunk = new DocumentChunk();
    chunk.setId(id);
    chunk.setEmbeddingJson(embeddingJson);
    return chunk;
  }
}
