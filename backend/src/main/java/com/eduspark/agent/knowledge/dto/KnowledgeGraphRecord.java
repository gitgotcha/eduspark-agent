package com.eduspark.agent.knowledge.dto;

import java.util.List;

public record KnowledgeGraphRecord(
    String id,
    String userId,
    String title,
    List<String> documentIds,
    KnowledgeGraphResponse graphJson,
    String status,
    String taskId,
    String createdAt,
    String updatedAt) {}
