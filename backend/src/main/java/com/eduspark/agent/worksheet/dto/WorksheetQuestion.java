package com.eduspark.agent.worksheet.dto;

import java.util.List;

public record WorksheetQuestion(
    String id,
    String type,
    String stem,
    List<String> options,
    Object answer,
    String explanation,
    String difficulty,
    List<String> sourceChunkIds) {}
