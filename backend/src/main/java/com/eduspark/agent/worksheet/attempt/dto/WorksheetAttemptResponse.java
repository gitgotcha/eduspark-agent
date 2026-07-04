package com.eduspark.agent.worksheet.attempt.dto;

import java.util.List;

public record WorksheetAttemptResponse(
    String attemptId,
    String worksheetId,
    double score,
    List<WorksheetGradingItem> items,
    String weaknessSummary,
    String remediationSuggestion,
    String createdAt) {}
