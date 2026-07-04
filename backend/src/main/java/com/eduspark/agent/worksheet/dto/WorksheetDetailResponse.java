package com.eduspark.agent.worksheet.dto;

import java.util.List;

public record WorksheetDetailResponse(
    String id,
    String userId,
    String taskId,
    String title,
    List<String> documentIds,
    WorksheetCreateRequest config,
    WorksheetGenerationRationale generationRationale,
    List<WorksheetQuestion> questions,
    String status,
    String createdAt,
    String updatedAt) {}
