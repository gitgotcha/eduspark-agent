package com.eduspark.agent.worksheet.attempt;

import com.eduspark.agent.worksheet.WorksheetService;
import com.eduspark.agent.worksheet.attempt.dto.WorksheetAttemptRequest;
import com.eduspark.agent.worksheet.attempt.dto.WorksheetAttemptResponse;
import com.eduspark.agent.worksheet.dto.WorksheetDetailResponse;
import com.eduspark.agent.worksheet.wrong.WrongQuestionService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WorksheetAttemptService {

  private final WorksheetService worksheetService;
  private final WorksheetAttemptMapper attemptMapper;
  private final ObjectMapper objectMapper;
  private final DeterministicAnswerGradingService answerGradingService;
  private final WrongQuestionService wrongQuestionService;

  public WorksheetAttemptService(
      WorksheetService worksheetService,
      WorksheetAttemptMapper attemptMapper,
      ObjectMapper objectMapper,
      DeterministicAnswerGradingService answerGradingService,
      WrongQuestionService wrongQuestionService) {
    this.worksheetService = worksheetService;
    this.attemptMapper = attemptMapper;
    this.objectMapper = objectMapper;
    this.answerGradingService = answerGradingService;
    this.wrongQuestionService = wrongQuestionService;
  }

  @Transactional
  public WorksheetAttemptResponse submitAttempt(
      String userId, String worksheetId, WorksheetAttemptRequest request) {
    WorksheetDetailResponse worksheet = worksheetService.findDetail(userId, worksheetId);
    LocalDateTime now = LocalDateTime.now();
    String attemptId = UUID.randomUUID().toString();
    WorksheetAttemptResponse response =
        answerGradingService.grade(attemptId, worksheetId, worksheet.questions(), request, now);

    EduWorksheetAttempt attempt = new EduWorksheetAttempt();
    attempt.setId(attemptId);
    attempt.setUserId(userId);
    attempt.setWorksheetId(worksheetId);
    attempt.setAnswersJson(writeJson(request));
    attempt.setGradingResultJson(writeJson(response));
    attempt.setScore(BigDecimal.valueOf(response.score()));
    attempt.setCreatedAt(now);
    attemptMapper.insert(attempt);
    wrongQuestionService.collectFromAttempt(userId, worksheet, response);
    return response;
  }

  public List<WorksheetAttemptResponse> listAttempts(String userId, String worksheetId) {
    worksheetService.findDetail(userId, worksheetId);
    return attemptMapper.selectByUserIdAndWorksheetId(userId, worksheetId).stream()
        .map(attempt -> readJson(attempt.getGradingResultJson(), WorksheetAttemptResponse.class))
        .toList();
  }

  private String writeJson(Object value) {
    try {
      return objectMapper.writeValueAsString(value);
    } catch (JsonProcessingException exception) {
      throw new IllegalStateException("Failed to serialize worksheet attempt", exception);
    }
  }

  private <T> T readJson(String json, Class<T> valueType) {
    try {
      return objectMapper.readValue(json, valueType);
    } catch (JsonProcessingException exception) {
      throw new IllegalStateException("Failed to deserialize worksheet attempt", exception);
    }
  }
}
