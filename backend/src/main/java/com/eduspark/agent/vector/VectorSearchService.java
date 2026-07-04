package com.eduspark.agent.vector;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class VectorSearchService {

  private static final TypeReference<List<Double>> DOUBLE_LIST = new TypeReference<>() {};

  private final DocumentChunkMapper chunkMapper;
  private final EmbeddingService embeddingService;
  private final ObjectMapper objectMapper;

  public VectorSearchService(
      DocumentChunkMapper chunkMapper, EmbeddingService embeddingService, ObjectMapper objectMapper) {
    this.chunkMapper = chunkMapper;
    this.embeddingService = embeddingService;
    this.objectMapper = objectMapper;
  }

  public List<DocumentChunk> search(
      String userId, List<String> documentIds, String query, int limit) {
    List<DocumentChunk> chunks = chunkMapper.selectByUserIdAndDocumentIds(userId, documentIds);
    return rank(embeddingService.embed(query), chunks, limit);
  }

  List<DocumentChunk> rank(String queryEmbeddingJson, List<DocumentChunk> chunks, int limit) {
    if (limit < 0) {
      throw new IllegalArgumentException("Limit must not be negative");
    }
    if (limit == 0) {
      return List.of();
    }

    List<Double> queryEmbedding = parseEmbedding(queryEmbeddingJson);

    return chunks.stream()
        .map(
            chunk ->
                new ScoredChunk(
                    chunk, cosine(queryEmbedding, parseEmbedding(chunk.getEmbeddingJson()))))
        .sorted(Comparator.comparingDouble(ScoredChunk::score).reversed())
        .limit(limit)
        .map(ScoredChunk::chunk)
        .toList();
  }

  private List<Double> parseEmbedding(String embeddingJson) {
    try {
      List<Double> embedding = objectMapper.readValue(embeddingJson, DOUBLE_LIST);
      validateEmbedding(embedding);
      return embedding;
    } catch (JsonProcessingException | IllegalArgumentException exception) {
      throw new IllegalArgumentException("Invalid embedding JSON", exception);
    }
  }

  private void validateEmbedding(List<Double> embedding) {
    if (embedding == null || embedding.isEmpty()) {
      throw new IllegalArgumentException("Embedding must not be null or empty");
    }
    for (Double value : embedding) {
      if (value == null || !Double.isFinite(value)) {
        throw new IllegalArgumentException("Embedding values must be finite numbers");
      }
    }
  }

  private double cosine(List<Double> left, List<Double> right) {
    if (left.size() != right.size()) {
      throw new IllegalArgumentException("Embedding dimensions must match");
    }

    double dotProduct = 0.0;
    double leftNorm = 0.0;
    double rightNorm = 0.0;
    for (int i = 0; i < left.size(); i++) {
      double leftValue = left.get(i);
      double rightValue = right.get(i);
      dotProduct += leftValue * rightValue;
      leftNorm += leftValue * leftValue;
      rightNorm += rightValue * rightValue;
    }

    if (leftNorm == 0.0 || rightNorm == 0.0) {
      return 0.0;
    }
    return dotProduct / (Math.sqrt(leftNorm) * Math.sqrt(rightNorm));
  }

  private record ScoredChunk(DocumentChunk chunk, double score) {}
}
