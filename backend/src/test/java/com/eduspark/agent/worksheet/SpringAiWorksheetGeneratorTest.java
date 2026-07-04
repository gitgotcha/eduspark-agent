package com.eduspark.agent.worksheet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.eduspark.agent.vector.DocumentChunk;
import com.eduspark.agent.worksheet.dto.WorksheetCreateRequest;
import com.eduspark.agent.worksheet.dto.WorksheetQuestion;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;

@ExtendWith(MockitoExtension.class)
class SpringAiWorksheetGeneratorTest {

  @Test
  void generatesEnrichedQuestionsFromLlmOutput() {
    ChatClient.Builder chatClientBuilder = mock(ChatClient.Builder.class);
    ChatClient chatClient = mock(ChatClient.class);
    ChatClient.ChatClientRequestSpec requestSpec = mock(ChatClient.ChatClientRequestSpec.class);
    ChatClient.CallResponseSpec callSpec = mock(ChatClient.CallResponseSpec.class);

    when(chatClientBuilder.defaultSystem(anyString())).thenReturn(chatClientBuilder);
    when(chatClientBuilder.build()).thenReturn(chatClient);
    when(chatClient.prompt()).thenReturn(requestSpec);
    when(requestSpec.user(anyString())).thenReturn(requestSpec);
    when(callSpec.content())
        .thenReturn(
            """
            {
              "generationRationale": {
                "summary": "围绕同分母分数加法生成基础练习。",
                "keyPoints": ["同分母分数相加"],
                "difficultyPlan": "按中等难度考查计算理解。",
                "typePlan": "遵循选择题偏好。",
                "deviationFromPreference": ""
              },
              "questions": [
                {
                  "type": "选择题",
                  "stem": "1/2 + 1/2 等于多少？",
                  "options": ["A. 1/4", "B. 1", "C. 2", "D. 1/2"],
                  "answer": "B",
                  "explanation": "两个二分之一相加等于一。",
                  "difficulty": "中等",
                  "sourceQuote": "分数加法基础"
                }
              ]
            }
            """);
    when(requestSpec.call()).thenReturn(callSpec);

    SpringAiWorksheetGenerator generator =
        new SpringAiWorksheetGenerator(
            chatClientBuilder,
            new WorksheetPromptBuilder(),
            new WorksheetQuestionOutputParser(),
            new WorksheetQuestionEnricher());

    DocumentChunk chunk = new DocumentChunk();
    chunk.setId("chunk-1");
    chunk.setContent("分数加法基础");

    WorksheetCreateRequest request =
        new WorksheetCreateRequest(
            "分数练习",
            List.of("doc-1"),
            1,
            "五年级",
            "简单",
            List.of("选择题"),
            true);

    List<WorksheetQuestion> questions = generator.generate(request, List.of(chunk));

    ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);
    org.mockito.Mockito.verify(requestSpec).user(promptCaptor.capture());
    assertThat(promptCaptor.getValue()).contains("Semi-constrained rules", "generationRationale");

    assertThat(questions).hasSize(1);
    assertThat(questions.get(0).stem()).contains("1/2");
    assertThat(questions.get(0).id()).isNotBlank();
    assertThat(questions.get(0).difficulty()).isEqualTo("中等");
    assertThat(questions.get(0).sourceChunkIds()).containsExactly("chunk-1");
    assertThat(questions.get(0).explanation()).isNotBlank();
  }

  @Test
  void fallsBackToMaterialQuestionWhenLlmReturnsNoQuestions() {
    ChatClient.Builder chatClientBuilder = mock(ChatClient.Builder.class);
    ChatClient chatClient = mock(ChatClient.class);
    ChatClient.ChatClientRequestSpec requestSpec = mock(ChatClient.ChatClientRequestSpec.class);
    ChatClient.CallResponseSpec callSpec = mock(ChatClient.CallResponseSpec.class);

    when(chatClientBuilder.defaultSystem(anyString())).thenReturn(chatClientBuilder);
    when(chatClientBuilder.build()).thenReturn(chatClient);
    when(chatClient.prompt()).thenReturn(requestSpec);
    when(requestSpec.user(anyString())).thenReturn(requestSpec);
    when(callSpec.content())
        .thenReturn(
            """
            {
              "generationRationale": {
                "summary": "资料不足。",
                "keyPoints": ["分数加法"],
                "difficultyPlan": "基础",
                "typePlan": "选择题",
                "deviationFromPreference": "未生成题目"
              },
              "questions": []
            }
            """);
    when(requestSpec.call()).thenReturn(callSpec);

    SpringAiWorksheetGenerator generator =
        new SpringAiWorksheetGenerator(
            chatClientBuilder,
            new WorksheetPromptBuilder(),
            new WorksheetQuestionOutputParser(),
            new WorksheetQuestionEnricher());

    DocumentChunk chunk = new DocumentChunk();
    chunk.setId("chunk-1");
    chunk.setContent("同分母分数相加时，分母不变，分子相加。");

    WorksheetCreateRequest request =
        new WorksheetCreateRequest(
            "分数练习",
            List.of("doc-1"),
            1,
            "五年级",
            "中等",
            List.of("选择题"),
            true);

    List<WorksheetQuestion> questions = generator.generate(request, List.of(chunk));

    assertThat(questions).hasSize(1);
    assertThat(questions.get(0).stem()).contains("reference content");
    assertThat(questions.get(0).answer()).isEqualTo("A");
    assertThat(questions.get(0).sourceChunkIds()).containsExactly("chunk-1");
  }
}
