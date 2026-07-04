package com.eduspark.agent.worksheet.attempt.dto;

import jakarta.validation.constraints.NotBlank;

public record WorksheetAnswerInput(@NotBlank String questionId, String answer) {}
