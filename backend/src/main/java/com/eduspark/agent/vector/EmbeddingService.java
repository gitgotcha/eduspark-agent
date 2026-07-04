package com.eduspark.agent.vector;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class EmbeddingService {

  private final ObjectMapper objectMapper;
  private final String plannerMode;
  private final String openAiApiKey;
  private final String openAiBaseUrl;
  private final String embeddingModel;
  private final HttpClient httpClient;

  public EmbeddingService(
      ObjectMapper objectMapper,
      @Value("${agent.planner.mode:mock}") String plannerMode,
      @Value("${spring.ai.openai.api-key:}") String openAiApiKey,
      @Value("${spring.ai.openai.base-url:https://dashscope.aliyuncs.com/compatible-mode}")
          String openAiBaseUrl,
      @Value(
              "${spring.ai.openai.embedding.options.model:${spring.ai.openai.embedding.model:text-embedding-v4}}")
          String embeddingModel) {
    this.objectMapper = objectMapper;
    this.plannerMode = plannerMode;
    this.openAiApiKey = openAiApiKey == null ? "" : openAiApiKey.trim();
    this.openAiBaseUrl = openAiBaseUrl == null ? "" : openAiBaseUrl.trim();
    this.embeddingModel = embeddingModel;
    this.httpClient = HttpClient.newHttpClient();
  }

  public String embed(String text) {
    if (!openAiApiKey.isBlank()) {
      return embedWithOpenAi(text);
    }
    try {
      return objectMapper.writeValueAsString(mockVector(text));
    } catch (JsonProcessingException exception) {
      throw new IllegalStateException("Failed to serialize embedding", exception);
    }
  }

  private String embedWithOpenAi(String text) {
    try {
      String body =
          objectMapper.writeValueAsString(
              Map.of("model", embeddingModel, "input", text == null ? "" : text));
      HttpRequest request =
          HttpRequest.newBuilder(URI.create(embeddingsUrl()))
              .header("Authorization", "Bearer " + openAiApiKey)
              .header("Content-Type", "application/json")
              .POST(HttpRequest.BodyPublishers.ofString(body))
              .build();
      HttpResponse<String> response =
          httpClient.send(request, HttpResponse.BodyHandlers.ofString());
      if (response.statusCode() < 200 || response.statusCode() >= 300) {
        throw new IllegalStateException("OpenAI embedding request failed: " + response.statusCode());
      }
      JsonNode embedding = objectMapper.readTree(response.body()).at("/data/0/embedding");
      if (!embedding.isArray()) {
        throw new IllegalStateException("OpenAI embedding response did not contain an embedding");
      }
      return objectMapper.writeValueAsString(embedding);
    } catch (IOException exception) {
      throw new IllegalStateException("Failed to call OpenAI embedding API", exception);
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("OpenAI embedding request was interrupted", exception);
    }
  }

  private String embeddingsUrl() {
    String normalized = openAiBaseUrl.endsWith("/") ? openAiBaseUrl.substring(0, openAiBaseUrl.length() - 1) : openAiBaseUrl;
    return normalized.endsWith("/v1") ? normalized + "/embeddings" : normalized + "/v1/embeddings";
  }

  private List<Double> mockVector(String text) {
    String source = (plannerMode == null ? "mock" : plannerMode) + ":" + (text == null ? "" : text);
    int hash = source.hashCode();
    List<Double> vector = new ArrayList<>();
    for (int i = 0; i < 16; i++) {
      int value = Math.floorMod(hash + (i * 31), 1000);
      vector.add(value / 1000.0);
    }
    return vector;
  }
}
