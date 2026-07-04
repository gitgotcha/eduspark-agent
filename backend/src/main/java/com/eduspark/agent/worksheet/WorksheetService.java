package com.eduspark.agent.worksheet;

import com.eduspark.agent.document.DocumentService;
import com.eduspark.agent.document.EduDocument;
import com.eduspark.agent.generation.WorksheetGenerationStrategy;
import com.eduspark.agent.task.EduTask;
import com.eduspark.agent.task.TaskCreateResponse;
import com.eduspark.agent.task.TaskService;
import com.eduspark.agent.task.TaskStateMachine;
import com.eduspark.agent.task.TaskStatus;
import com.eduspark.agent.task.dto.TaskCreateRequest;
import com.eduspark.agent.vector.DocumentChunk;
import com.eduspark.agent.vector.VectorSearchService;
import com.eduspark.agent.worksheet.dto.WorksheetCreateRequest;
import com.eduspark.agent.worksheet.dto.WorksheetCreateResponse;
import com.eduspark.agent.worksheet.dto.WorksheetDetailResponse;
import com.eduspark.agent.worksheet.dto.WorksheetGenerationRationale;
import com.eduspark.agent.worksheet.dto.WorksheetGenerationResult;
import com.eduspark.agent.worksheet.dto.WorksheetListItemResponse;
import com.eduspark.agent.worksheet.dto.WorksheetQuestion;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class WorksheetService {

  private static final TypeReference<List<String>> STRING_LIST = new TypeReference<>() {};
  private static final TypeReference<List<WorksheetQuestion>> QUESTION_LIST =
      new TypeReference<>() {};

  private final WorksheetMapper worksheetMapper;
  private final TaskService taskService;
  private final ObjectMapper objectMapper;
  private final VectorSearchService vectorSearchService;
  private final WorksheetGenerationStrategy worksheetGenerationStrategy;
  private final DocumentService documentService;
  private final WorksheetQuestionEnricher worksheetQuestionEnricher;

  public WorksheetService(
      WorksheetMapper worksheetMapper,
      TaskService taskService,
      ObjectMapper objectMapper,
      VectorSearchService vectorSearchService,
      WorksheetGenerationStrategy worksheetGenerationStrategy,
      DocumentService documentService,
      WorksheetQuestionEnricher worksheetQuestionEnricher) {
    this.worksheetMapper = worksheetMapper;
    this.taskService = taskService;
    this.objectMapper = objectMapper;
    this.vectorSearchService = vectorSearchService;
    this.worksheetGenerationStrategy = worksheetGenerationStrategy;
    this.documentService = documentService;
    this.worksheetQuestionEnricher = worksheetQuestionEnricher;
  }

  @Transactional(noRollbackFor = ResponseStatusException.class)
  public WorksheetCreateResponse create(String userId, WorksheetCreateRequest request) {
    TaskCreateResponse task =
        taskService.createTask(
            userId, new TaskCreateRequest("生成练习卷：" + request.title(), request.documentIds()));

    LocalDateTime now = LocalDateTime.now();
    EduWorksheet worksheet = new EduWorksheet();
    worksheet.setId(UUID.randomUUID().toString());
    worksheet.setUserId(userId);
    worksheet.setTaskId(task.taskId());
    worksheet.setTitle(request.title());
    worksheet.setDocumentIds(writeJson(request.documentIds()));
    worksheet.setConfigJson(writeJson(request));
    worksheet.setStatus(WorksheetStatus.PENDING);
    worksheet.setCreatedAt(now);
    worksheet.setUpdatedAt(now);
    worksheetMapper.insert(worksheet);

    List<DocumentChunk> chunks =
        vectorSearchService.search(
            userId, request.documentIds(), request.title() + " " + request.gradeLevel(), 8);
    if (chunks.isEmpty()) {
      chunks = buildFallbackChunks(userId, request.documentIds());
    }

    worksheet.setStatus(WorksheetStatus.GENERATING);
    worksheet.setUpdatedAt(LocalDateTime.now());
    worksheetMapper.updateById(worksheet);

    try {
      WorksheetGenerationResult result = worksheetGenerationStrategy.generateResult(request, chunks);
      List<WorksheetQuestion> questions = worksheetQuestionEnricher.enrich(request, result, chunks);
      worksheet.setGenerationRationaleJson(writeJson(result.generationRationale()));
      worksheet.setQuestionsJson(writeJson(questions));
      worksheet.setStatus(WorksheetStatus.COMPLETED);
      worksheet.setUpdatedAt(LocalDateTime.now());
      worksheetMapper.updateById(worksheet);
      completeTask(task.taskId());
    } catch (RuntimeException exception) {
      worksheet.setStatus(WorksheetStatus.FAILED);
      worksheet.setUpdatedAt(LocalDateTime.now());
      worksheetMapper.updateById(worksheet);
      failTask(task.taskId(), exception.getMessage());
      throw new ResponseStatusException(
          HttpStatus.BAD_GATEWAY, "练习卷生成失败: " + exception.getMessage(), exception);
    }

    return new WorksheetCreateResponse(
        worksheet.getId(), task.taskId(), WorksheetStatus.COMPLETED.getValue());
  }

  public WorksheetDetailResponse findDetail(String userId, String worksheetId) {
    EduWorksheet worksheet = worksheetMapper.selectByIdAndUserId(worksheetId, userId);
    if (worksheet == null) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Worksheet not found");
    }

    return new WorksheetDetailResponse(
        worksheet.getId(),
        worksheet.getUserId(),
        worksheet.getTaskId(),
        worksheet.getTitle(),
        readJson(worksheet.getDocumentIds(), STRING_LIST),
        readJson(worksheet.getConfigJson(), WorksheetCreateRequest.class),
        readGenerationRationale(worksheet.getGenerationRationaleJson()),
        readQuestions(worksheet.getQuestionsJson()),
        worksheet.getStatus().getValue(),
        worksheet.getCreatedAt().toString(),
        worksheet.getUpdatedAt().toString());
  }

  public List<WorksheetListItemResponse> list(String userId, int limit) {
    int safeLimit = limit > 0 ? limit : 20;
    return worksheetMapper.selectByUserIdOrderByCreatedAtDesc(userId, safeLimit).stream()
        .map(
            worksheet ->
                new WorksheetListItemResponse(
                    worksheet.getId(),
                    worksheet.getTaskId(),
                    worksheet.getTitle(),
                    worksheet.getStatus().getValue(),
                    worksheet.getCreatedAt().toString(),
                    worksheet.getUpdatedAt().toString()))
        .toList();
  }

  private List<DocumentChunk> buildFallbackChunks(String userId, List<String> documentIds) {
    List<DocumentChunk> chunks = new ArrayList<>();
    for (String documentId : documentIds) {
      EduDocument document = documentService.findDocumentOrThrow(userId, documentId);
      String extractedText = document.getExtractedText();
      if (extractedText == null || extractedText.isBlank()) {
        continue;
      }
      DocumentChunk chunk = new DocumentChunk();
      chunk.setId("doc-" + documentId);
      chunk.setContent(extractedText);
      chunks.add(chunk);
    }
    return chunks;
  }

  private String writeJson(Object value) {
    try {
      return objectMapper.writeValueAsString(value);
    } catch (JsonProcessingException exception) {
      throw new IllegalStateException("Failed to serialize worksheet data", exception);
    }
  }

  private <T> T readJson(String json, Class<T> valueType) {
    try {
      return objectMapper.readValue(json, valueType);
    } catch (JsonProcessingException exception) {
      throw new IllegalStateException("Failed to deserialize worksheet data", exception);
    }
  }

  private <T> T readJson(String json, TypeReference<T> valueType) {
    try {
      return objectMapper.readValue(json, valueType);
    } catch (JsonProcessingException exception) {
      throw new IllegalStateException("Failed to deserialize worksheet data", exception);
    }
  }

  private List<WorksheetQuestion> readQuestions(String questionsJson) {
    if (questionsJson == null || questionsJson.isBlank()) {
      return List.of();
    }
    return readJson(questionsJson, QUESTION_LIST);
  }

  private WorksheetGenerationRationale readGenerationRationale(String generationRationaleJson) {
    if (generationRationaleJson == null || generationRationaleJson.isBlank()) {
      return null;
    }
    return readJson(generationRationaleJson, WorksheetGenerationRationale.class);
  }

  private void completeTask(String taskId) {
    taskService.transit(taskId, TaskStatus.PLANNING, "Worksheet generation started");
    taskService.transit(taskId, TaskStatus.EXECUTING, "Generating worksheet");
    taskService.transit(taskId, TaskStatus.REVIEWING, "Reviewing worksheet");
    taskService.transit(taskId, TaskStatus.COMPLETED, "Worksheet generated");
  }

  private void failTask(String taskId, String reason) {
    taskService.saveFailureReason(taskId, reason);
    EduTask task = taskService.findTaskOrThrow(taskId);
    if (TaskStateMachine.canTransit(task.getStatus(), TaskStatus.FAILED)) {
      taskService.transit(taskId, TaskStatus.FAILED, "Worksheet generation failed: " + reason);
    }
  }
}
