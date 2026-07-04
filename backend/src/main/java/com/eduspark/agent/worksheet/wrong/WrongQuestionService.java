package com.eduspark.agent.worksheet.wrong;

import com.eduspark.agent.worksheet.WorksheetService;
import com.eduspark.agent.worksheet.attempt.dto.WorksheetGradingItem;
import com.eduspark.agent.worksheet.dto.WorksheetCreateRequest;
import com.eduspark.agent.worksheet.dto.WorksheetDetailResponse;
import com.eduspark.agent.worksheet.dto.WorksheetQuestion;
import com.eduspark.agent.worksheet.attempt.dto.WorksheetAttemptResponse;
import com.eduspark.agent.worksheet.wrong.dto.WrongQuestionResponse;
import com.eduspark.agent.worksheet.wrong.dto.WrongQuestionRetryRequest;
import com.eduspark.agent.worksheet.dto.WorksheetCreateResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class WrongQuestionService {

  private static final TypeReference<WorksheetQuestion> QUESTION_TYPE = new TypeReference<>() {};
  private final WrongQuestionMapper wrongQuestionMapper;
  private final WorksheetService worksheetService;
  private final ObjectMapper objectMapper;

  public WrongQuestionService(
      WrongQuestionMapper wrongQuestionMapper,
      WorksheetService worksheetService,
      ObjectMapper objectMapper) {
    this.wrongQuestionMapper = wrongQuestionMapper;
    this.worksheetService = worksheetService;
    this.objectMapper = objectMapper;
  }

  @Transactional
  public void collectFromAttempt(
      String userId, WorksheetDetailResponse worksheet, WorksheetAttemptResponse attempt) {
    LocalDateTime now = LocalDateTime.now();
    for (WorksheetGradingItem item : attempt.items()) {
      if (item.correct()) {
        continue;
      }
      WorksheetQuestion question =
          worksheet.questions().stream()
              .filter(candidate -> candidate.id().equals(item.questionId()))
              .findFirst()
              .orElse(null);
      if (question == null) {
        continue;
      }
      WrongQuestion wrongQuestion = new WrongQuestion();
      wrongQuestion.setId(UUID.randomUUID().toString());
      wrongQuestion.setUserId(userId);
      wrongQuestion.setWorksheetId(worksheet.id());
      wrongQuestion.setAttemptId(attempt.attemptId());
      wrongQuestion.setQuestionId(question.id());
      wrongQuestion.setQuestionJson(writeJson(question));
      wrongQuestion.setSubmittedAnswer(item.submittedAnswer());
      wrongQuestion.setCorrectAnswer(item.correctAnswer());
      wrongQuestion.setExplanation(item.explanation());
      wrongQuestion.setWeaknessTag(buildWeaknessTag(question));
      wrongQuestion.setResolved(false);
      wrongQuestion.setCreatedAt(now);
      wrongQuestion.setUpdatedAt(now);
      wrongQuestionMapper.insert(wrongQuestion);
    }
  }

  public List<WrongQuestionResponse> listOpen(String userId) {
    return wrongQuestionMapper.selectOpenByUserId(userId).stream()
        .map(this::toResponse)
        .toList();
  }

  public List<WrongQuestionResponse> listByWorksheet(String userId, String worksheetId) {
    return wrongQuestionMapper.selectByWorksheetId(userId, worksheetId).stream()
        .map(this::toResponse)
        .toList();
  }

  @Transactional
  public void resolve(String userId, String wrongQuestionId) {
    int deleted = wrongQuestionMapper.deleteByIdAndUserId(userId, wrongQuestionId);
    if (deleted == 0) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Wrong question not found");
    }
  }

  @Transactional
  public WorksheetCreateResponse retry(String userId, WrongQuestionRetryRequest request) {
    List<WrongQuestion> wrongQuestions =
        request.wrongQuestionIds().stream()
            .map(id -> wrongQuestionMapper.selectByIdAndUserId(userId, id))
            .toList();
    if (wrongQuestions.stream().anyMatch(question -> question == null)) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Wrong question not found");
    }

    String worksheetId = wrongQuestions.get(0).getWorksheetId();
    if (wrongQuestions.stream().anyMatch(question -> !worksheetId.equals(question.getWorksheetId()))) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Wrong questions must belong to same worksheet");
    }

    WorksheetDetailResponse sourceWorksheet = worksheetService.findDetail(userId, worksheetId);
    WorksheetCreateRequest createRequest =
        new WorksheetCreateRequest(
            request.title(),
            sourceWorksheet.documentIds(),
            Math.max(3, wrongQuestions.size() * 2),
            sourceWorksheet.config().gradeLevel(),
            sourceWorksheet.config().difficulty(),
            sourceWorksheet.config().questionTypes(),
            true);

    WorksheetCreateResponse created = worksheetService.create(userId, createRequest);
    wrongQuestionMapper.attachRetryWorksheet(
        userId, request.wrongQuestionIds(), created.worksheetId(), LocalDateTime.now());
    return created;
  }

  private WrongQuestionResponse toResponse(WrongQuestion wrongQuestion) {
    WorksheetQuestion question = readQuestion(wrongQuestion.getQuestionJson());
    return new WrongQuestionResponse(
        wrongQuestion.getId(),
        wrongQuestion.getWorksheetId(),
        wrongQuestion.getAttemptId(),
        wrongQuestion.getQuestionId(),
        question.stem(),
        wrongQuestion.getSubmittedAnswer(),
        wrongQuestion.getCorrectAnswer(),
        wrongQuestion.getExplanation(),
        wrongQuestion.getWeaknessTag(),
        wrongQuestion.getRetryWorksheetId(),
        Boolean.TRUE.equals(wrongQuestion.getResolved()),
        wrongQuestion.getCreatedAt().toString(),
        wrongQuestion.getUpdatedAt().toString());
  }

  private String buildWeaknessTag(WorksheetQuestion question) {
    String stem = question.stem() == null ? "" : question.stem();
    return "Needs review: " + stem.substring(0, Math.min(24, stem.length()));
  }

  private String writeJson(Object value) {
    try {
      return objectMapper.writeValueAsString(value);
    } catch (JsonProcessingException exception) {
      throw new IllegalStateException("Failed to serialize wrong question", exception);
    }
  }

  private WorksheetQuestion readQuestion(String json) {
    try {
      return objectMapper.readValue(json, QUESTION_TYPE);
    } catch (JsonProcessingException exception) {
      throw new IllegalStateException("Failed to deserialize wrong question", exception);
    }
  }
}
