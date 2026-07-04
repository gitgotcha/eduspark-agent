package com.eduspark.agent.tool;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class QuizGeneratorToolTest {

  private final QuizGeneratorTool tool = new QuizGeneratorTool();

  @Test
  void generatesStandardizedQuestionsWithAnswers() {
    QuizResponse response = tool.generate(new QuizRequest("一次函数 y=kx+b 的图像与性质", 2));

    assertThat(response.questions()).hasSize(2);
    assertThat(response.questions().get(0).stem()).contains("一次函数");
    assertThat(response.questions().get(0).options()).containsExactly("A. 正确", "B. 错误");
    assertThat(response.questions().get(0).answer()).isEqualTo("A");
    assertThat(response.questions().get(0).explanation()).isNotBlank();
  }
}
