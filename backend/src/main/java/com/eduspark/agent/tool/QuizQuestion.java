package com.eduspark.agent.tool;

import java.util.List;

public record QuizQuestion(String stem, List<String> options, String answer, String explanation) {}
