package com.eduspark.agent.tool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class ToolRegistryIntegrationTest {

  @Autowired private ToolRegistry toolRegistry;

  @Test
  void exposesInitialEducationToolsByName() {
    assertThat(toolRegistry.names()).containsExactlyInAnyOrder("textSummaryTool", "quizGeneratorTool");
  }

  @Test
  void invokesTextSummaryToolFromMapInput() {
    ToolCallResult result =
        toolRegistry.call("textSummaryTool", Map.of("text", "本节课学习分数加减法。", "maxLength", 8));

    assertThat(result.toolName()).isEqualTo("textSummaryTool");
    assertThat(result.output()).isInstanceOf(SummaryResponse.class);
    assertThat(((SummaryResponse) result.output()).summary()).isEqualTo("本节课学习...");
  }

  @Test
  void rejectsUnknownToolName() {
    assertThatThrownBy(() -> toolRegistry.call("unknownTool", Map.of()))
        .isInstanceOf(ToolNotFoundException.class)
        .hasMessageContaining("unknownTool");
  }
}
