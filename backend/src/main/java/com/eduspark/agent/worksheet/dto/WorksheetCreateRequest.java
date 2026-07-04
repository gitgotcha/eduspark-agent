package com.eduspark.agent.worksheet.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record WorksheetCreateRequest(
    @NotBlank String title,
    @NotEmpty List<String> documentIds,
    @Min(1) @Max(30) int questionCount,
    @NotBlank String gradeLevel,
    @NotBlank String difficulty,
    @NotEmpty List<@NotBlank String> questionTypes,
    boolean includeExplanation) {}
