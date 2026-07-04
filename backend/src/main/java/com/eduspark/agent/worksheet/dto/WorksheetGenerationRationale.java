package com.eduspark.agent.worksheet.dto;

import java.util.List;

public record WorksheetGenerationRationale(
    String summary,
    List<String> keyPoints,
    String difficultyPlan,
    String typePlan,
    String deviationFromPreference) {}
