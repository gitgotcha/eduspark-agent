package com.eduspark.agent.worksheet;

import com.eduspark.agent.generation.WorksheetGenerationStrategy;
import com.eduspark.agent.vector.DocumentChunk;
import com.eduspark.agent.worksheet.dto.GeneratedWorksheetQuestion;
import com.eduspark.agent.worksheet.dto.WorksheetCreateRequest;
import com.eduspark.agent.worksheet.dto.WorksheetGenerationRationale;
import com.eduspark.agent.worksheet.dto.WorksheetGenerationResult;
import com.eduspark.agent.worksheet.dto.WorksheetQuestion;
import java.util.ArrayList;
import java.util.List;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "agent.planner.mode", havingValue = "spring-ai")
public class SpringAiWorksheetGenerator implements WorksheetGenerationStrategy {

  private static final String SYSTEM_PROMPT =
      """
      You are EduSpark's educational worksheet generation agent.
      Your responsibilities:
      - Read the uploaded teaching material and extract knowledge points, common mistakes, and skills.
      - Generate worksheet questions based on grade level, difficulty, target count, and type preferences.
      - Question type and count are preferences. Adjust reasonably when the material requires it, and explain why.
      - Provide an answer and explanation for every question.
      - Use only the uploaded material. Do not invent facts outside the material.
      - If the material is insufficient, generate fewer questions and explain why.
      - All question stems, options, answers, explanations, and rationale fields must be written in English.
      - Output JSON only. Do not include extra prose.
      """;

  private final ChatClient chatClient;
  private final WorksheetPromptBuilder promptBuilder;
  private final WorksheetQuestionOutputParser parser;
  private final WorksheetQuestionEnricher enricher;

  public SpringAiWorksheetGenerator(
      ChatClient.Builder chatClientBuilder,
      WorksheetPromptBuilder promptBuilder,
      WorksheetQuestionOutputParser parser,
      WorksheetQuestionEnricher enricher) {
    this.chatClient = chatClientBuilder.defaultSystem(SYSTEM_PROMPT).build();
    this.promptBuilder = promptBuilder;
    this.parser = parser;
    this.enricher = enricher;
  }

  @Override
  public WorksheetGenerationResult generateResult(
      WorksheetCreateRequest request, List<DocumentChunk> chunks) {
    String rawOutput =
        chatClient
            .prompt()
            .user(promptBuilder.buildUserPrompt(request, chunks, parser.format()))
            .call()
            .content();

    try {
      return parser.parse(rawOutput, request);
    } catch (InvalidWorksheetGenerationException exception) {
      return fallbackFromMaterial(request, chunks, exception);
    }
  }

  private WorksheetGenerationResult fallbackFromMaterial(
      WorksheetCreateRequest request, List<DocumentChunk> chunks, RuntimeException exception) {
    List<DocumentChunk> availableChunks =
        chunks == null
            ? List.of()
            : chunks.stream()
                .filter(chunk -> chunk.getContent() != null && !chunk.getContent().isBlank())
                .toList();
    if (availableChunks.isEmpty()) {
      throw exception;
    }

    String primaryType =
        request.questionTypes() == null || request.questionTypes().isEmpty()
            ? "Short Answer"
            : request.questionTypes().get(0);
    int count = Math.max(1, Math.min(request.questionCount(), Math.min(availableChunks.size(), 3)));
    List<GeneratedWorksheetQuestion> questions = new ArrayList<>();
    for (int index = 0; index < count; index++) {
      String content = normalizeSnippet(availableChunks.get(index).getContent());
      questions.add(buildFallbackQuestion(primaryType, request.difficulty(), content));
    }

    WorksheetGenerationRationale rationale =
        new WorksheetGenerationRationale(
            "The model did not return valid questions, so the system generated fallback questions from the uploaded material.",
            availableChunks.stream().map(chunk -> normalizeSnippet(chunk.getContent())).limit(3).toList(),
            "Use the teacher-selected difficulty: " + request.difficulty(),
            "Prefer the teacher-selected question type: " + primaryType,
            "The original model output contained an empty questions array, so material-based fallback generation was used.");
    return new WorksheetGenerationResult(rationale, questions);
  }

  private GeneratedWorksheetQuestion buildFallbackQuestion(
      String type, String difficulty, String content) {
    if (type.contains("选择")) {
      return new GeneratedWorksheetQuestion(
          "Multiple Choice",
          "According to the material, which option best matches the reference content?",
          List.of("A. " + content, "B. A statement unrelated to the material", "C. A conclusion not mentioned in the material", "D. A statement opposite to the material"),
          "A",
          "Option A is directly based on the uploaded material. The other options do not match the reference content.",
          difficulty,
          content);
    }
    if (type.contains("判断")) {
      return new GeneratedWorksheetQuestion(
          "True/False",
          "The material mentions the following idea: " + content,
          List.of("A. True", "B. False"),
          "A",
          "This statement is based on the uploaded reference material.",
          difficulty,
          content);
    }
    return new GeneratedWorksheetQuestion(
        "Short Answer",
        "Summarize this knowledge point based on the material: " + content,
        List.of(),
        content,
        "The answer should focus on the core idea in the reference snippet.",
        difficulty,
        content);
  }

  private String normalizeSnippet(String text) {
    String normalized = text.replaceAll("\\s+", " ").trim();
    if (normalized.length() <= 80) {
      return normalized;
    }
    return normalized.substring(0, 80);
  }

  @Override
  public List<WorksheetQuestion> generate(WorksheetCreateRequest request, List<DocumentChunk> chunks) {
    return enricher.enrich(request, generateResult(request, chunks), chunks);
  }
}
