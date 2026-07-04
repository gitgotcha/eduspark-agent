package com.eduspark.agent.agent;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class PlannerServiceIntegrationTest {

  @Autowired private PlannerService plannerService;

  @Test
  void mockPlannerReturnsDeterministicTaskPlan() {
    TaskPlan plan = plannerService.plan("总结课文并生成练习");

    assertThat(plan.steps()).hasSize(2);
    assertThat(plan.steps().get(0).stepNo()).isEqualTo(1);
    assertThat(plan.steps().get(0).toolName()).isEqualTo("textSummaryTool");
    assertThat(plan.steps().get(1).toolName()).isEqualTo("quizGeneratorTool");
  }
}
