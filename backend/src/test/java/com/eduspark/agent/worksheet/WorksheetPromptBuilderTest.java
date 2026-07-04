package com.eduspark.agent.worksheet;

import static org.assertj.core.api.Assertions.assertThat;

import com.eduspark.agent.vector.DocumentChunk;
import com.eduspark.agent.worksheet.dto.WorksheetCreateRequest;
import java.util.List;
import org.junit.jupiter.api.Test;

class WorksheetPromptBuilderTest {

  private final WorksheetPromptBuilder promptBuilder = new WorksheetPromptBuilder();

  @Test
  void includesChunkContentAndQuestionConfiguration() {
    DocumentChunk chunk = new DocumentChunk();
    chunk.setId("chunk-1");
    chunk.setContent("课堂材料：同分母分数加减法。");

    WorksheetCreateRequest request =
        new WorksheetCreateRequest(
            "分数加减法练习",
            List.of("doc-1"),
            2,
            "五年级",
            "中等",
            List.of("选择题", "判断题"),
            true);

    String prompt = promptBuilder.buildUserPrompt(request, List.of(chunk), "{\"questions\":[]}");

    assertThat(prompt).contains("分数加减法练习");
    assertThat(prompt).contains("五年级");
    assertThat(prompt).contains("同分母分数加减法");
    assertThat(prompt).contains("Teacher preferences:");
    assertThat(prompt).contains("Semi-constrained rules:");
    assertThat(prompt).contains("The number of questions may vary within 20% of the target");
    assertThat(prompt).contains("[chunk-1]");
    assertThat(prompt).contains("Output strict JSON");
    assertThat(prompt).contains("generationRationale");
    assertThat(prompt).contains("选择题, 判断题");
    assertThat(prompt).contains("All values must be in English");
    assertThat(prompt).contains("{\"questions\":[]}");
  }

  @Test
  void usesPlaceholderWhenChunksAreEmpty() {
    WorksheetCreateRequest request =
        new WorksheetCreateRequest(
            "分数练习",
            List.of("doc-1"),
            1,
            "五年级",
            "中等",
            List.of("选择题"),
            false);

    String prompt = promptBuilder.buildUserPrompt(request, List.of(), "format");

    assertThat(prompt).contains("(No available reference material)");
  }
}
