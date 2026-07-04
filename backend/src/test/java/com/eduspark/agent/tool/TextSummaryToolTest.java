package com.eduspark.agent.tool;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class TextSummaryToolTest {

  private final TextSummaryTool tool = new TextSummaryTool();

  @Test
  void summarizesTextWithDeterministicLengthLimit() {
    SummaryResponse response =
        tool.summarize(
            new SummaryRequest(
                "第一段介绍函数概念。第二段介绍一次函数图像。第三段要求学生完成练习并解释斜率。", 18));

    assertThat(response.summary()).isEqualTo("第一段介绍函数概念。第二段介绍...");
    assertThat(response.sourceLength()).isEqualTo(39);
  }
}
