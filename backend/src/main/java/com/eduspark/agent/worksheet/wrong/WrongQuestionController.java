package com.eduspark.agent.worksheet.wrong;

import com.eduspark.agent.worksheet.attempt.dto.WorksheetAttemptResponse;
import com.eduspark.agent.worksheet.wrong.dto.WrongQuestionResponse;
import com.eduspark.agent.worksheet.wrong.dto.WrongQuestionRetryRequest;
import com.eduspark.agent.worksheet.dto.WorksheetCreateResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users/{userId}")
public class WrongQuestionController {

  private final WrongQuestionService wrongQuestionService;

  public WrongQuestionController(WrongQuestionService wrongQuestionService) {
    this.wrongQuestionService = wrongQuestionService;
  }

  @GetMapping("/wrong-questions")
  public List<WrongQuestionResponse> listOpen(@PathVariable String userId) {
    return wrongQuestionService.listOpen(userId);
  }

  @GetMapping("/worksheets/{worksheetId}/wrong-questions")
  public List<WrongQuestionResponse> listByWorksheet(
      @PathVariable String userId, @PathVariable String worksheetId) {
    return wrongQuestionService.listByWorksheet(userId, worksheetId);
  }

  @DeleteMapping("/wrong-questions/{wrongQuestionId}")
  public void resolve(@PathVariable String userId, @PathVariable String wrongQuestionId) {
    wrongQuestionService.resolve(userId, wrongQuestionId);
  }

  @PostMapping("/wrong-questions/retry")
  public WorksheetCreateResponse retry(
      @PathVariable String userId, @Valid @RequestBody WrongQuestionRetryRequest request) {
    return wrongQuestionService.retry(userId, request);
  }
}
