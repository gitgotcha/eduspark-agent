package com.eduspark.agent.knowledge.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotBlank;
import java.util.List;

public record KnowledgeGraphCreateRequest(
    @NotBlank String title, @NotEmpty List<@NotBlank String> documentIds) {}
