package com.eduspark.agent.worksheet.wrong.dto;

public record WrongQuestionResponse(
    String id,
    String worksheetId,
    String attemptId,
    String questionId,
    String questionStem,
    String submittedAnswer,
    String correctAnswer,
    String explanation,
    String weaknessTag,
    String retryWorksheetId,
    boolean resolved,
    String createdAt,
    String updatedAt) {}
