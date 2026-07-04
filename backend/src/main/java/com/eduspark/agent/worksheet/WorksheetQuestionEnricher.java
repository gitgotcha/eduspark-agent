package com.eduspark.agent.worksheet;

import com.eduspark.agent.vector.DocumentChunk;
import com.eduspark.agent.worksheet.dto.GeneratedWorksheetQuestion;
import com.eduspark.agent.worksheet.dto.WorksheetCreateRequest;
import com.eduspark.agent.worksheet.dto.WorksheetGenerationResult;
import com.eduspark.agent.worksheet.dto.WorksheetQuestion;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class WorksheetQuestionEnricher {

  public List<WorksheetQuestion> enrich(
      WorksheetCreateRequest request, WorksheetGenerationResult result, List<DocumentChunk> chunks) {
    List<GeneratedWorksheetQuestion> questions = result.questions();
    return java.util.stream.IntStream.range(0, questions.size())
        .mapToObj(index -> enrichQuestion(request, questions.get(index), index, chunks))
        .toList();
  }

  private WorksheetQuestion enrichQuestion(
      WorksheetCreateRequest request,
      GeneratedWorksheetQuestion question,
      int index,
      List<DocumentChunk> chunks) {
    String explanation =
        request.includeExplanation() ? blankToNull(question.explanation()) : null;
    return new WorksheetQuestion(
        deterministicId(request, index),
        question.type(),
        question.stem(),
        question.options() == null ? List.of() : question.options(),
        question.answer(),
        explanation,
        blankToDefault(question.difficulty(), request.difficulty()),
        sourceChunkIds(chunks));
  }

  private List<String> sourceChunkIds(List<DocumentChunk> chunks) {
    if (chunks == null || chunks.isEmpty()) {
      return List.of();
    }
    return chunks.stream().map(DocumentChunk::getId).toList();
  }

  private String deterministicId(WorksheetCreateRequest request, int index) {
    return UUID.nameUUIDFromBytes((request.title() + ":" + index).getBytes(StandardCharsets.UTF_8))
        .toString();
  }

  private String blankToNull(String value) {
    return value == null || value.isBlank() ? null : value;
  }

  private String blankToDefault(String value, String defaultValue) {
    return value == null || value.isBlank() ? defaultValue : value;
  }
}
