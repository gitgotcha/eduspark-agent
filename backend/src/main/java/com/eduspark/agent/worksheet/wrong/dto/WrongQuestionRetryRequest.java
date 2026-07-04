package com.eduspark.agent.worksheet.wrong.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record WrongQuestionRetryRequest(@NotEmpty List<@NotBlank String> wrongQuestionIds, @NotBlank String title) {}
