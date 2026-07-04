package com.eduspark.agent.worksheet;

import com.eduspark.agent.vector.DocumentChunk;
import com.eduspark.agent.worksheet.dto.WorksheetCreateRequest;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class WorksheetPromptBuilder {

  public String buildUserPrompt(
      WorksheetCreateRequest request, List<DocumentChunk> chunks, String outputFormat) {
    return """
        Generate a worksheet based on the following reference material.

        Teacher preferences:
        - Title: %s
        - Grade level: %s
        - Target question count: %d
        - Preferred question types: %s
        - Target difficulty: %s
        - Include explanations: %s

        Semi-constrained rules:
        - The number of questions may vary within 20%% of the target, but generate at least 1 question.
        - Prefer the requested question types, but adjust reasonably based on the material.
        - Grade level is a hard constraint. Do not exceed the target grade.
        - If you deviate from teacher preferences, explain it in generationRationale.deviationFromPreference.
        - All questions must be based on the reference material. Do not invent facts outside the material.
        - All generated question stems, options, answers, explanations, and generationRationale fields must be written in English.

        Reference material chunks:
        %s

        Please complete:
        1. Extract the core knowledge points from the material.
        2. Plan the question type and difficulty distribution.
        3. Generate questions, answers, and explanations.
        4. Output strict JSON with generationRationale and questions. All values must be in English.

        %s
        """
        .formatted(
            request.title(),
            request.gradeLevel(),
            request.questionCount(),
            String.join(", ", request.questionTypes()),
            request.difficulty(),
            request.includeExplanation(),
            formatMaterial(chunks),
            outputFormat);
  }

  private String formatMaterial(List<DocumentChunk> chunks) {
    if (chunks == null || chunks.isEmpty()) {
      return "(No available reference material)";
    }

    return java.util.stream.IntStream.range(0, chunks.size())
        .mapToObj(index -> "[chunk-" + (index + 1) + "]\n" + chunks.get(index).getContent())
        .collect(Collectors.joining("\n\n---\n\n"));
  }
}
