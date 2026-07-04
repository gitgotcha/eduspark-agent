package com.eduspark.agent.worksheet.attempt.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record WorksheetAttemptRequest(@NotEmpty List<@Valid WorksheetAnswerInput> answers) {}
