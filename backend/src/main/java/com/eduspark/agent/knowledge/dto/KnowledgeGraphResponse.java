package com.eduspark.agent.knowledge.dto;

import java.util.List;

public record KnowledgeGraphResponse(
    List<KnowledgeGraphNode> nodes, List<KnowledgeGraphEdge> edges) {}
