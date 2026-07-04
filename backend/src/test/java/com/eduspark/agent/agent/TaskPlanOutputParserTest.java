package com.eduspark.agent.agent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class TaskPlanOutputParserTest {

  private final TaskPlanOutputParser parser = new TaskPlanOutputParser();

  @Test
  void parsesValidTaskPlanJson() {
    TaskPlan plan =
        parser.parse(
            """
            {"steps":[{"stepNo":1,"toolName":"textSummaryTool","input":{"text":"abc"},"expectedOutput":"摘要"}]}
            """);

    assertThat(plan.steps()).hasSize(1);
    assertThat(plan.steps().get(0).toolName()).isEqualTo("textSummaryTool");
    assertThat(plan.steps().get(0).input()).containsEntry("text", "abc");
  }

  @Test
  void rejectsJsonWithoutSteps() {
    assertThatThrownBy(() -> parser.parse("{\"foo\":[]}"))
        .isInstanceOf(InvalidTaskPlanException.class)
        .hasMessageContaining("steps");
  }
}
