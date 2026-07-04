package com.eduspark.agent.worksheet.attempt;

import static org.assertj.core.api.Assertions.assertThat;

import com.eduspark.agent.worksheet.attempt.dto.WorksheetAnswerInput;
import com.eduspark.agent.worksheet.attempt.dto.WorksheetAttemptRequest;
import com.eduspark.agent.worksheet.attempt.dto.WorksheetAttemptResponse;
import com.eduspark.agent.worksheet.dto.WorksheetQuestion;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class AnswerGradingServiceTest {

  private final DeterministicAnswerGradingService gradingService = new DeterministicAnswerGradingService();

  @Test
  void gradesAnswersCaseInsensitively() {
    WorksheetAttemptResponse response =
        gradingService.grade(
            "attempt-1",
            "worksheet-1",
            List.of(
                new WorksheetQuestion("q1", "single", "Pick A", List.of("A", "B"), "A", "Because A", "easy", List.of()),
                new WorksheetQuestion("q2", "single", "Pick B", List.of("A", "B"), "B", "Because B", "easy", List.of())),
            new WorksheetAttemptRequest(
                List.of(
                    new WorksheetAnswerInput("q1", " a "),
                    new WorksheetAnswerInput("q2", "wrong"))),
            LocalDateTime.parse("2026-07-04T10:00:00"));

    assertThat(response.score()).isEqualTo(50.0);
    assertThat(response.items()).extracting("correct").containsExactly(true, false);
    assertThat(response.items().get(0).correctAnswer()).isEqualTo("A");
  }
}
