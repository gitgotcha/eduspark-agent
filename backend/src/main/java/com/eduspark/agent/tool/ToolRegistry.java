package com.eduspark.agent.tool;

import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class ToolRegistry {

  public static final String TEXT_SUMMARY_TOOL = "textSummaryTool";
  public static final String QUIZ_GENERATOR_TOOL = "quizGeneratorTool";

  private final TextSummaryTool textSummaryTool;
  private final QuizGeneratorTool quizGeneratorTool;

  public ToolRegistry(TextSummaryTool textSummaryTool, QuizGeneratorTool quizGeneratorTool) {
    this.textSummaryTool = textSummaryTool;
    this.quizGeneratorTool = quizGeneratorTool;
  }

  public Set<String> names() {
    return Set.of(TEXT_SUMMARY_TOOL, QUIZ_GENERATOR_TOOL);
  }

  public ToolCallResult call(String toolName, Map<String, Object> input) {
    Map<String, Object> safeInput = input == null ? Map.of() : input;
    return switch (toolName) {
      case TEXT_SUMMARY_TOOL -> new ToolCallResult(toolName, textSummaryTool.summarize(toSummaryRequest(safeInput)));
      case QUIZ_GENERATOR_TOOL -> new ToolCallResult(toolName, quizGeneratorTool.generate(toQuizRequest(safeInput)));
      default -> throw new ToolNotFoundException(toolName);
    };
  }

  private SummaryRequest toSummaryRequest(Map<String, Object> input) {
    return new SummaryRequest(asString(input.get("text")), asInt(input.get("maxLength"), 120));
  }

  private QuizRequest toQuizRequest(Map<String, Object> input) {
    Object topic = input.containsKey("topic") ? input.get("topic") : input.get("source");
    Object questionCount = input.containsKey("questionCount") ? input.get("questionCount") : input.get("count");
    return new QuizRequest(asString(topic), asInt(questionCount, 5));
  }

  private String asString(Object value) {
    return value == null ? "" : String.valueOf(value);
  }

  private int asInt(Object value, int fallback) {
    if (value instanceof Number number) {
      return number.intValue();
    }
    if (value instanceof String text) {
      try {
        return Integer.parseInt(text);
      } catch (NumberFormatException ignored) {
        return fallback;
      }
    }
    return fallback;
  }
}
