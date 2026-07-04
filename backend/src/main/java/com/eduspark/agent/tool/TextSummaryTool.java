package com.eduspark.agent.tool;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

@Component
public class TextSummaryTool {

  @Tool(description = "Summarize teaching text with a deterministic character limit.")
  public SummaryResponse summarize(SummaryRequest request) {
    String text = request == null || request.text() == null ? "" : request.text();
    int maxLength = request == null ? 120 : Math.max(1, request.maxLength());
    String summary = text.length() <= maxLength ? text : abbreviate(text, maxLength);
    return new SummaryResponse(summary, text.length());
  }

  private String abbreviate(String text, int maxLength) {
    if (maxLength <= 3) {
      return text.substring(0, Math.min(text.length(), maxLength));
    }
    return text.substring(0, Math.min(text.length(), maxLength - 3)) + "...";
  }
}
