package com.eduspark.agent.vector;

import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class DocumentChunker {

  private static final int CHUNK_SIZE = 1000;
  private static final int OVERLAP = 120;

  public List<String> chunk(String text) {
    if (text == null || text.isBlank()) {
      return List.of();
    }

    List<String> chunks = new ArrayList<>();
    int start = 0;
    while (start < text.length()) {
      int end = Math.min(start + CHUNK_SIZE, text.length());
      chunks.add(text.substring(start, end));
      if (end == text.length()) {
        break;
      }
      start = Math.max(end - OVERLAP, start + 1);
    }
    return chunks;
  }
}
