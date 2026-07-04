package com.eduspark.agent.agent;

import java.util.Map;

public record TaskStep(
    Integer stepNo, String toolName, Map<String, Object> input, String expectedOutput) {}
