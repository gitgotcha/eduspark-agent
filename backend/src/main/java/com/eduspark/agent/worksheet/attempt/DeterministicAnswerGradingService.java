package com.eduspark.agent.worksheet.attempt;

import com.eduspark.agent.worksheet.attempt.dto.WorksheetAnswerInput;
import com.eduspark.agent.worksheet.attempt.dto.WorksheetAttemptRequest;
import com.eduspark.agent.worksheet.attempt.dto.WorksheetAttemptResponse;
import com.eduspark.agent.worksheet.attempt.dto.WorksheetGradingItem;
import com.eduspark.agent.worksheet.dto.WorksheetQuestion;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class DeterministicAnswerGradingService {

  public WorksheetAttemptResponse grade(
      String attemptId,
      String worksheetId,
      List<WorksheetQuestion> questions,
      WorksheetAttemptRequest request,
      LocalDateTime createdAt) {
    Map<String, WorksheetAnswerInput> answerByQuestionId =
        request.answers().stream()
            .collect(Collectors.toMap(WorksheetAnswerInput::questionId, Function.identity(), (a, b) -> b));

    List<WorksheetGradingItem> items =
        questions.stream()
            .map(question -> gradeOne(question, answerByQuestionId.get(question.id())))
            .toList();

    long correctCount = items.stream().filter(WorksheetGradingItem::correct).count();
    double score =
        items.isEmpty() ? 0.0 : Math.round((correctCount * 10000.0 / items.size())) / 100.0;
    boolean allCorrect = !items.isEmpty() && correctCount == items.size();

    return new WorksheetAttemptResponse(
        attemptId,
        worksheetId,
        score,
        items,
        allCorrect
            ? "All answers are correct."
            : "Review the incorrect questions and compare your answers with the explanations.",
        allCorrect
            ? "You can move on to a harder practice set."
            : "Regenerate similar questions for the missed concepts and retry.",
        createdAt.toString());
  }

  private WorksheetGradingItem gradeOne(WorksheetQuestion question, WorksheetAnswerInput input) {
    String submitted = input == null || input.answer() == null ? "" : input.answer();
    String correctAnswer = question.answer() == null ? "" : String.valueOf(question.answer());
    return new WorksheetGradingItem(
        question.id(),
        question.stem(),
        submitted,
        correctAnswer,
        normalize(submitted).equals(normalize(correctAnswer)),
        question.explanation());
  }

  private String normalize(String value) {
    return value == null ? "" : value.trim().replaceAll("\\s+", " ").toLowerCase();
  }
}
