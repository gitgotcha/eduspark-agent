package com.eduspark.agent.worksheet.dto;

import java.util.List;

public record GeneratedWorksheetQuestion(
    String type,
    String stem,
    List<String> options,
    String answer,
    String explanation,
    String difficulty,
    String sourceQuote) {}
