package com.eduspark.agent.worksheet.dto;

import java.util.List;

public record WorksheetGenerationResult(
    WorksheetGenerationRationale generationRationale,
    List<GeneratedWorksheetQuestion> questions) {}
