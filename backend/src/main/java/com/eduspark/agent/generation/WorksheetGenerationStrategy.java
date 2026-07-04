package com.eduspark.agent.generation;

import com.eduspark.agent.vector.DocumentChunk;
import com.eduspark.agent.worksheet.dto.WorksheetCreateRequest;
import com.eduspark.agent.worksheet.dto.WorksheetGenerationResult;
import com.eduspark.agent.worksheet.dto.WorksheetQuestion;
import java.util.List;

public interface WorksheetGenerationStrategy {

  WorksheetGenerationResult generateResult(WorksheetCreateRequest request, List<DocumentChunk> chunks);

  default List<WorksheetQuestion> generate(WorksheetCreateRequest request, List<DocumentChunk> chunks) {
    throw new UnsupportedOperationException("Use generateResult for worksheet generation");
  }
}
