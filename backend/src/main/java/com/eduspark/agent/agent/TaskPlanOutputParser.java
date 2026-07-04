package com.eduspark.agent.agent;

import java.util.List;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.stereotype.Component;

@Component
public class TaskPlanOutputParser {

  private final BeanOutputConverter<TaskPlan> converter = new BeanOutputConverter<>(TaskPlan.class);

  public String format() {
    return converter.getFormat();
  }

  public TaskPlan parse(String rawOutput) {
    try {
      TaskPlan plan = converter.convert(rawOutput);
      validate(plan);
      return plan;
    } catch (InvalidTaskPlanException ex) {
      throw ex;
    } catch (RuntimeException ex) {
      throw new InvalidTaskPlanException("Model output cannot be parsed as TaskPlan", ex);
    }
  }

  private void validate(TaskPlan plan) {
    if (plan == null || plan.steps() == null || plan.steps().isEmpty()) {
      throw new InvalidTaskPlanException("TaskPlan must contain non-empty steps");
    }
    List<TaskStep> steps = plan.steps();
    for (TaskStep step : steps) {
      if (step.stepNo() == null || step.toolName() == null || step.toolName().isBlank()) {
        throw new InvalidTaskPlanException("Each step must contain stepNo and toolName");
      }
    }
  }
}
