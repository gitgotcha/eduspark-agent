package com.eduspark.agent.tool;

import java.util.ArrayList;
import java.util.List;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

@Component
public class QuizGeneratorTool {

  private static final List<String> TRUE_FALSE_OPTIONS = List.of("A. 正确", "B. 错误");

  @Tool(description = "Generate deterministic true/false quiz questions for a teaching topic.")
  public QuizResponse generate(QuizRequest request) {
    String topic = request == null || request.topic() == null || request.topic().isBlank() ? "本节内容" : request.topic();
    int questionCount = request == null ? 1 : Math.max(1, Math.min(request.questionCount(), 10));
    List<QuizQuestion> questions = new ArrayList<>();

    for (int index = 0; index < questionCount; index++) {
      questions.add(
          new QuizQuestion(
              "判断题 " + (index + 1) + "：" + topic + " 的相关表述是否正确？",
              TRUE_FALSE_OPTIONS,
              "A",
              "该题用于检查学生是否理解 " + topic + " 的核心概念。"));
    }

    return new QuizResponse(List.copyOf(questions));
  }
}
