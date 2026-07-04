package com.eduspark.agent.worksheet.attempt.dto;

public record WorksheetGradingItem(
    String questionId,
    String stem,
    String submittedAnswer,
    String correctAnswer,
    boolean correct,
    String explanation) {}
