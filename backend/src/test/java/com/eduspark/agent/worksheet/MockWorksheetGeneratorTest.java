package com.eduspark.agent.worksheet;

import static org.assertj.core.api.Assertions.assertThat;

import com.eduspark.agent.vector.DocumentChunk;
import com.eduspark.agent.worksheet.dto.WorksheetCreateRequest;
import com.eduspark.agent.worksheet.dto.WorksheetQuestion;
import java.util.List;
import org.junit.jupiter.api.Test;

class MockWorksheetGeneratorTest {

  private final MockWorksheetGenerator generator = new MockWorksheetGenerator();

  @Test
  void generatesStableMockQuestions() {
    DocumentChunk chunk = new DocumentChunk();
    chunk.setId("chunk-1");
    chunk.setContent("课堂材料");

    WorksheetCreateRequest request =
        new WorksheetCreateRequest(
            "分数练习",
            List.of("doc-1"),
            2,
            "五年级",
            "中等",
            List.of("选择题", "判断题"),
            true);

    List<WorksheetQuestion> questions = generator.generate(request, List.of(chunk));

    assertThat(questions).hasSize(2);
    assertThat(questions.get(0).type()).isEqualTo("选择题");
    assertThat(questions.get(1).type()).isEqualTo("判断题");
    assertThat(questions.get(0).sourceChunkIds()).containsExactly("chunk-1");
    assertThat(questions.get(0).id()).isNotEqualTo(questions.get(1).id());
    assertThat(questions.get(0).stem()).contains("分数练习");
  }
}
