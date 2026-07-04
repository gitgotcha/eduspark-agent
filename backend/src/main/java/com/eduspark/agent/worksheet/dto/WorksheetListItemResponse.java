package com.eduspark.agent.worksheet.dto;

public record WorksheetListItemResponse(
    String id, String taskId, String title, String status, String createdAt, String updatedAt) {}
