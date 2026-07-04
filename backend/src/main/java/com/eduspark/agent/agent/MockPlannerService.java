package com.eduspark.agent.agent;

import java.util.List;
import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "agent.planner.mode", havingValue = "mock", matchIfMissing = true)
public class MockPlannerService implements PlannerService {

  @Override
  public TaskPlan plan(String instruction) {
    return new TaskPlan(
        List.of(
            new TaskStep(
                1,
                "textSummaryTool",
                Map.of("text", instruction),
                "Generate an objective summary of the educational input."),
            new TaskStep(
                2,
                "quizGeneratorTool",
                Map.of("source", "summary", "questionCount", 5),
                "Generate standardized practice questions with answers.")));
  }
}
