package com.eduspark.agent.worksheet.attempt;

import com.eduspark.agent.worksheet.attempt.dto.WorksheetAttemptRequest;
import com.eduspark.agent.worksheet.attempt.dto.WorksheetAttemptResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users/{userId}/worksheets/{worksheetId}/attempts")
public class WorksheetAttemptController {

  private final WorksheetAttemptService attemptService;

  public WorksheetAttemptController(WorksheetAttemptService attemptService) {
    this.attemptService = attemptService;
  }

  @PostMapping
  public WorksheetAttemptResponse submitAttempt(
      @PathVariable String userId,
      @PathVariable String worksheetId,
      @Valid @RequestBody WorksheetAttemptRequest request) {
    return attemptService.submitAttempt(userId, worksheetId, request);
  }

  @GetMapping
  public List<WorksheetAttemptResponse> listAttempts(
      @PathVariable String userId, @PathVariable String worksheetId) {
    return attemptService.listAttempts(userId, worksheetId);
  }
}
