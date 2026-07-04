package com.eduspark.agent.knowledge;

import com.eduspark.agent.document.DocumentService;
import com.eduspark.agent.document.EduDocument;
import com.eduspark.agent.knowledge.dto.KnowledgeGraphEdge;
import com.eduspark.agent.knowledge.dto.KnowledgeGraphNode;
import com.eduspark.agent.knowledge.dto.KnowledgeGraphCreateRequest;
import com.eduspark.agent.knowledge.dto.KnowledgeGraphCreateResponse;
import com.eduspark.agent.knowledge.dto.KnowledgeGraphRecord;
import com.eduspark.agent.knowledge.dto.KnowledgeGraphResponse;
import com.eduspark.agent.task.EduTask;
import com.eduspark.agent.task.TaskCreateResponse;
import com.eduspark.agent.task.TaskService;
import com.eduspark.agent.task.TaskStateMachine;
import com.eduspark.agent.task.TaskStatus;
import com.eduspark.agent.task.dto.TaskCreateRequest;
import com.eduspark.agent.vector.DocumentChunk;
import com.eduspark.agent.vector.DocumentChunkMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.time.LocalDateTime;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class KnowledgeGraphService {

  private static final TypeReference<List<String>> STRING_LIST = new TypeReference<>() {};
  private static final Pattern TERM_PATTERN = Pattern.compile("[\\p{IsHan}]{2,8}|[A-Za-z][A-Za-z0-9-]{2,}");

  private final KnowledgeGraphMapper knowledgeGraphMapper;
  private final TaskService taskService;
  private final DocumentService documentService;
  private final DocumentChunkMapper chunkMapper;
  private final ObjectMapper objectMapper;

  public KnowledgeGraphService(
      KnowledgeGraphMapper knowledgeGraphMapper,
      TaskService taskService,
      DocumentService documentService,
      DocumentChunkMapper chunkMapper,
      ObjectMapper objectMapper) {
    this.knowledgeGraphMapper = knowledgeGraphMapper;
    this.taskService = taskService;
    this.documentService = documentService;
    this.chunkMapper = chunkMapper;
    this.objectMapper = objectMapper;
  }

  public KnowledgeGraphResponse buildGraph(String userId, int limit) {
    List<EduDocument> documents =
        documentService.listDocuments(userId).stream().limit(limit).toList();
    return buildGraphFromDocuments(userId, documents);
  }

  @Transactional
  public KnowledgeGraphCreateResponse create(String userId, KnowledgeGraphCreateRequest request) {
    if (request.documentIds() == null || request.documentIds().isEmpty()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Document ids are required");
    }

    TaskCreateResponse task =
        taskService.createTask(
            userId, new TaskCreateRequest("生成知识图谱：" + request.title(), request.documentIds()));

    LocalDateTime now = LocalDateTime.now();
    EduKnowledgeGraph graph = new EduKnowledgeGraph();
    graph.setId(UUID.randomUUID().toString());
    graph.setUserId(userId);
    graph.setTaskId(task.taskId());
    graph.setTitle(request.title());
    graph.setDocumentIdsJson(writeJson(request.documentIds()));
    graph.setGraphJson(writeJson(new KnowledgeGraphResponse(List.of(), List.of())));
    graph.setStatus("PENDING");
    graph.setCreatedAt(now);
    graph.setUpdatedAt(now);
    knowledgeGraphMapper.insert(graph);

    try {
      taskService.transit(task.taskId(), TaskStatus.PLANNING, "Knowledge graph planning started");
      KnowledgeGraphResponse response = buildGraphFromDocumentIds(userId, request.documentIds());
      graph.setGraphJson(writeJson(response));
      graph.setStatus("COMPLETED");
      graph.setUpdatedAt(LocalDateTime.now());
      knowledgeGraphMapper.updateById(graph);
      taskService.transit(task.taskId(), TaskStatus.EXECUTING, "Knowledge graph generated");
      taskService.transit(task.taskId(), TaskStatus.REVIEWING, "Knowledge graph reviewed");
      taskService.transit(task.taskId(), TaskStatus.COMPLETED, "Knowledge graph completed");
      return new KnowledgeGraphCreateResponse(graph.getId(), task.taskId(), "PENDING");
    } catch (RuntimeException exception) {
      graph.setStatus("FAILED");
      graph.setUpdatedAt(LocalDateTime.now());
      knowledgeGraphMapper.updateById(graph);
      taskService.saveFailureReason(task.taskId(), exception.getMessage());
      markTaskFailedIfPossible(task.taskId(), exception.getMessage());
      throw new ResponseStatusException(
          HttpStatus.BAD_GATEWAY, "知识图谱生成失败: " + exception.getMessage(), exception);
    }
  }

  public List<KnowledgeGraphRecord> list(String userId) {
    return knowledgeGraphMapper.selectByUserIdOrderByCreatedAtDesc(userId).stream()
        .map(this::toRecord)
        .toList();
  }

  public KnowledgeGraphRecord findOrThrow(String userId, String graphId) {
    EduKnowledgeGraph graph = knowledgeGraphMapper.selectByIdAndUserId(graphId, userId);
    if (graph == null) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Knowledge graph not found");
    }
    return toRecord(graph);
  }

  @Transactional
  public void delete(String userId, String graphId) {
    int deleted = knowledgeGraphMapper.deleteByIdAndUserId(graphId, userId);
    if (deleted == 0) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Knowledge graph not found");
    }
  }

  private KnowledgeGraphResponse buildGraphFromDocuments(String userId, List<EduDocument> documents) {
    List<String> documentIds = documents.stream().map(EduDocument::getId).toList();
    List<DocumentChunk> chunks = chunkMapper.selectByUserIdAndDocumentIds(userId, documentIds);
    Map<String, Integer> termWeights = new LinkedHashMap<>();
    Map<String, List<String>> termsByDocument = new LinkedHashMap<>();

    for (EduDocument document : documents) {
      String text = documentText(document, chunks);
      List<String> terms = extractTerms(text);
      termsByDocument.put(document.getId(), terms);
      for (String term : terms) {
        termWeights.merge(term, 1, Integer::sum);
      }
    }

    List<KnowledgeGraphNode> nodes = new ArrayList<>();
    for (EduDocument document : documents) {
      nodes.add(new KnowledgeGraphNode("doc:" + document.getId(), document.getFileName(), "document", 1));
    }
    termWeights.entrySet().stream()
        .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
        .limit(24)
        .forEach(entry -> nodes.add(new KnowledgeGraphNode("term:" + entry.getKey(), entry.getKey(), "concept", entry.getValue())));

    List<KnowledgeGraphEdge> edges = new ArrayList<>();
    for (Map.Entry<String, List<String>> entry : termsByDocument.entrySet()) {
      entry.getValue().stream()
          .filter(termWeights::containsKey)
          .distinct()
          .limit(6)
          .forEach(term -> edges.add(new KnowledgeGraphEdge("doc:" + entry.getKey(), "term:" + term, "contains", termWeights.get(term))));
    }
    return new KnowledgeGraphResponse(nodes, edges);
  }

  private KnowledgeGraphResponse buildGraphFromDocumentIds(String userId, List<String> documentIds) {
    List<EduDocument> documents =
        documentIds.stream().map(documentId -> documentService.findDocumentOrThrow(userId, documentId)).toList();
    return buildGraphFromDocuments(userId, documents);
  }

  private String documentText(EduDocument document, List<DocumentChunk> chunks) {
    String fromChunks =
        chunks.stream()
            .filter(chunk -> document.getId().equals(chunk.getDocumentId()))
            .map(DocumentChunk::getContent)
            .reduce("", (left, right) -> left + " " + right);
    if (!fromChunks.isBlank()) {
      return fromChunks;
    }
    return document.getTextPreview() == null ? "" : document.getTextPreview();
  }

  private List<String> extractTerms(String text) {
    Map<String, Integer> weights = new LinkedHashMap<>();
    Matcher matcher = TERM_PATTERN.matcher(text == null ? "" : text);
    while (matcher.find()) {
      String term = matcher.group();
      if (term.matches("[A-Za-z].*")) {
        term = term.toLowerCase(Locale.ROOT);
      }
      if (!isStopTerm(term)) {
        weights.merge(term, 1, Integer::sum);
      }
    }
    return weights.entrySet().stream()
        .sorted(Map.Entry.<String, Integer>comparingByValue().reversed().thenComparing(Map.Entry.comparingByKey()))
        .map(Map.Entry::getKey)
        .limit(10)
        .toList();
  }

  private boolean isStopTerm(String term) {
    return List.of("the", "and", "with", "that", "this", "from", "can", "are").contains(term);
  }

  private KnowledgeGraphRecord toRecord(EduKnowledgeGraph graph) {
    return new KnowledgeGraphRecord(
        graph.getId(),
        graph.getUserId(),
        graph.getTitle(),
        readJson(graph.getDocumentIdsJson(), STRING_LIST),
        readJson(graph.getGraphJson(), KnowledgeGraphResponse.class),
        graph.getStatus(),
        graph.getTaskId(),
        graph.getCreatedAt() == null ? null : graph.getCreatedAt().toString(),
        graph.getUpdatedAt() == null ? null : graph.getUpdatedAt().toString());
  }

  private String writeJson(Object value) {
    try {
      return objectMapper.writeValueAsString(value);
    } catch (JsonProcessingException exception) {
      throw new IllegalStateException("Failed to serialize knowledge graph data", exception);
    }
  }

  private <T> T readJson(String json, Class<T> valueType) {
    try {
      return objectMapper.readValue(json, valueType);
    } catch (JsonProcessingException exception) {
      throw new IllegalStateException("Failed to deserialize knowledge graph data", exception);
    }
  }

  private <T> T readJson(String json, TypeReference<T> valueType) {
    try {
      return objectMapper.readValue(json, valueType);
    } catch (JsonProcessingException exception) {
      throw new IllegalStateException("Failed to deserialize knowledge graph data", exception);
    }
  }

  private void markTaskFailedIfPossible(String taskId, String reason) {
    try {
      EduTask task = taskService.findTaskOrThrow(taskId);
      if (TaskStateMachine.canTransit(task.getStatus(), TaskStatus.FAILED)) {
        taskService.transit(taskId, TaskStatus.FAILED, "Knowledge graph generation failed: " + reason);
      }
    } catch (RuntimeException ignored) {
      // keep the original failure as the primary signal
    }
  }
}
