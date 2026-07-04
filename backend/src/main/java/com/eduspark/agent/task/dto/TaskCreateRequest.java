package com.eduspark.agent.task.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.List;

public record TaskCreateRequest(@NotBlank String instruction, List<String> documentIds) {

  public TaskCreateRequest(String instruction) {
    this(instruction, List.of());
  }
}
