package com.eduspark.agent.worksheet;

import com.eduspark.agent.generation.WorksheetGenerationStrategy;
import com.eduspark.agent.vector.DocumentChunk;
import com.eduspark.agent.worksheet.dto.GeneratedWorksheetQuestion;
import com.eduspark.agent.worksheet.dto.WorksheetCreateRequest;
import com.eduspark.agent.worksheet.dto.WorksheetGenerationRationale;
import com.eduspark.agent.worksheet.dto.WorksheetGenerationResult;
import com.eduspark.agent.worksheet.dto.WorksheetQuestion;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "agent.planner.mode", havingValue = "mock", matchIfMissing = true)
public class MockWorksheetGenerator implements WorksheetGenerationStrategy {

  private static final List<String> DEFAULT_OPTIONS = List.of("A. 正确", "B. 错误");

  @Override
  public WorksheetGenerationResult generateResult(
      WorksheetCreateRequest request, List<DocumentChunk> chunks) {
    WorksheetGenerationRationale rationale =
        new WorksheetGenerationRationale(
            "根据《" + request.title() + "》生成" + request.questionCount() + "道练习题。",
            List.of(request.title(), request.gradeLevel(), request.difficulty()),
            "按" + request.gradeLevel() + "和" + request.difficulty() + "难度组织题目。",
            "按题型偏好轮换生成：" + String.join("、", request.questionTypes()) + "。",
            "无偏离。");
    List<GeneratedWorksheetQuestion> questions =
        java.util.stream.IntStream.range(0, request.questionCount())
            .mapToObj(
                index -> {
                  String type = request.questionTypes().get(index % request.questionTypes().size());
                  return new GeneratedWorksheetQuestion(
                      type,
                      "第" + (index + 1) + "题：请根据《" + request.title() + "》完成这道" + type + "。",
                      DEFAULT_OPTIONS,
                      "A",
                      request.includeExplanation() ? "参考材料中的关键知识点可支持答案 A。" : null,
                      request.difficulty(),
                      "课堂材料");
                })
            .toList();
    return new WorksheetGenerationResult(rationale, questions);
  }

  @Override
  public List<WorksheetQuestion> generate(WorksheetCreateRequest request, List<DocumentChunk> chunks) {
    List<String> sourceChunkIds = firstChunkId(chunks);
    return java.util.stream.IntStream.range(0, request.questionCount())
        .mapToObj(
            index -> {
              String type = request.questionTypes().get(index % request.questionTypes().size());
              return new WorksheetQuestion(
                  deterministicId(request, index),
                  type,
                  "第" + (index + 1) + "题：请根据《" + request.title() + "》完成这道" + type + "。",
                  DEFAULT_OPTIONS,
                  "A",
                  request.includeExplanation() ? "参考材料中的关键知识点可支持答案 A。" : null,
                  request.difficulty(),
                  sourceChunkIds);
            })
        .toList();
  }

  private List<String> firstChunkId(List<DocumentChunk> chunks) {
    if (chunks == null || chunks.isEmpty()) {
      return List.of();
    }
    return List.of(chunks.get(0).getId());
  }

  private String deterministicId(WorksheetCreateRequest request, int index) {
    return UUID.nameUUIDFromBytes((request.title() + ":" + index).getBytes(StandardCharsets.UTF_8))
        .toString();
  }
}
