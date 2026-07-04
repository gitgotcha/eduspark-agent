package com.eduspark.agent.worksheet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.eduspark.agent.worksheet.dto.WorksheetCreateRequest;
import com.eduspark.agent.worksheet.dto.WorksheetGenerationResult;
import java.util.List;
import org.junit.jupiter.api.Test;

class WorksheetQuestionOutputParserTest {

  private final WorksheetQuestionOutputParser parser = new WorksheetQuestionOutputParser();

  @Test
  void parsesValidWorksheetJson() {
    WorksheetCreateRequest request = sampleRequest(2);

    WorksheetGenerationResult result =
        parser.parse(
            """
            {
              "generationRationale": {
                "summary": "材料讲解同分母分数加减法。",
                "keyPoints": ["分母不变", "分子相加减"],
                "difficultyPlan": "以中等难度为主，包含基础判断。",
                "typePlan": "选择题考查规则，判断题考查应用。",
                "deviationFromPreference": ""
              },
              "questions": [
                {
                  "type": "选择题",
                  "stem": "同分母分数相加时，分母应如何处理？",
                  "options": ["A. 相加", "B. 不变", "C. 相减", "D. 取平均"],
                  "answer": "B",
                  "explanation": "同分母分数相加，分母保持不变。",
                  "difficulty": "中等",
                  "sourceQuote": "同分母分数相加，分母保持不变。"
                },
                {
                  "type": "判断题",
                  "stem": "1/4 + 1/4 = 1/2。",
                  "options": ["A. 正确", "B. 错误"],
                  "answer": "A",
                  "explanation": "分子相加，分母不变。",
                  "difficulty": "基础",
                  "sourceQuote": "分子相加，分母不变。"
                }
              ]
            }
            """,
            request);

    assertThat(result.questions()).hasSize(2);
    assertThat(result.questions().get(0).stem()).contains("同分母分数");
  }

  @Test
  void rejectsMissingRationale() {
    WorksheetCreateRequest request = sampleRequest(1);

    assertThatThrownBy(
            () ->
                parser.parse(
                    """
                    {
                      "questions": [
                        {
                          "type": "选择题",
                          "stem": "题干",
                          "options": ["A. 1", "B. 2"],
                          "answer": "A",
                          "explanation": "解析",
                          "difficulty": "基础",
                          "sourceQuote": "材料"
                        }
                      ]
                    }
                    """,
                    request))
        .isInstanceOf(InvalidWorksheetGenerationException.class)
        .hasMessageContaining("generationRationale");
  }

  @Test
  void rejectsBlankRationaleSummary() {
    WorksheetCreateRequest request = sampleRequest(1);

    assertThatThrownBy(
            () ->
                parser.parse(
                    jsonWithRationale(
                        """
                        "summary": "",
                        "keyPoints": ["分母不变", "分子相加减"],
                        "difficultyPlan": "以中等难度为主，包含基础判断。",
                        "typePlan": "判断题考查应用。",
                        "deviationFromPreference": ""
                        """),
                    request))
        .isInstanceOf(InvalidWorksheetGenerationException.class)
        .hasMessageContaining("summary");
  }

  @Test
  void rejectsEmptyRationaleKeyPoints() {
    WorksheetCreateRequest request = sampleRequest(1);

    assertThatThrownBy(
            () ->
                parser.parse(
                    jsonWithRationale(
                        """
                        "summary": "材料讲解同分母分数加减法。",
                        "keyPoints": [],
                        "difficultyPlan": "以中等难度为主，包含基础判断。",
                        "typePlan": "判断题考查应用。",
                        "deviationFromPreference": ""
                        """),
                    request))
        .isInstanceOf(InvalidWorksheetGenerationException.class)
        .hasMessageContaining("keyPoints");
  }

  @Test
  void rejectsBlankRationaleDifficultyPlan() {
    WorksheetCreateRequest request = sampleRequest(1);

    assertThatThrownBy(
            () ->
                parser.parse(
                    jsonWithRationale(
                        """
                        "summary": "材料讲解同分母分数加减法。",
                        "keyPoints": ["分母不变", "分子相加减"],
                        "difficultyPlan": "",
                        "typePlan": "判断题考查应用。",
                        "deviationFromPreference": ""
                        """),
                    request))
        .isInstanceOf(InvalidWorksheetGenerationException.class)
        .hasMessageContaining("difficultyPlan");
  }

  @Test
  void rejectsBlankRationaleTypePlan() {
    WorksheetCreateRequest request = sampleRequest(1);

    assertThatThrownBy(
            () ->
                parser.parse(
                    jsonWithRationale(
                        """
                        "summary": "材料讲解同分母分数加减法。",
                        "keyPoints": ["分母不变", "分子相加减"],
                        "difficultyPlan": "以中等难度为主，包含基础判断。",
                        "typePlan": "",
                        "deviationFromPreference": ""
                        """),
                    request))
        .isInstanceOf(InvalidWorksheetGenerationException.class)
        .hasMessageContaining("typePlan");
  }

  @Test
  void allowsQuestionCountWithinTwentyPercent() {
    WorksheetCreateRequest request = sampleRequest(10);

    WorksheetGenerationResult result = parser.parse(validJsonWithRepeatedQuestions(8), request);

    assertThat(result.questions()).hasSize(8);
  }

  @Test
  void rejectsQuestionCountOutsideTwentyPercent() {
    WorksheetCreateRequest request = sampleRequest(10);

    assertThatThrownBy(() -> parser.parse(validJsonWithRepeatedQuestions(7), request))
        .isInstanceOf(InvalidWorksheetGenerationException.class)
        .hasMessageContaining("question count between");
  }

  @Test
  void allowsQuestionCountAtUpperTwentyPercentBoundary() {
    WorksheetCreateRequest request = sampleRequest(10);

    WorksheetGenerationResult result = parser.parse(validJsonWithRepeatedQuestions(12), request);

    assertThat(result.questions()).hasSize(12);
  }

  @Test
  void rejectsQuestionCountAboveUpperTwentyPercentBoundary() {
    WorksheetCreateRequest request = sampleRequest(10);

    assertThatThrownBy(() -> parser.parse(validJsonWithRepeatedQuestions(13), request))
        .isInstanceOf(InvalidWorksheetGenerationException.class)
        .hasMessageContaining("question count between");
  }

  @Test
  void rejectsTwoQuestionsWhenOneRequested() {
    WorksheetCreateRequest request = sampleRequest(1);

    assertThatThrownBy(() -> parser.parse(validJsonWithRepeatedQuestions(2), request))
        .isInstanceOf(InvalidWorksheetGenerationException.class)
        .hasMessageContaining("question count between");
  }

  @Test
  void rejectsMissingStem() {
    WorksheetCreateRequest request = sampleRequest(1);

    assertThatThrownBy(
            () ->
                parser.parse(
                    """
                    {
                      "generationRationale": {
                        "summary": "材料讲解同分母分数加减法。",
                        "keyPoints": ["分母不变", "分子相加减"],
                        "difficultyPlan": "以中等难度为主，包含基础判断。",
                        "typePlan": "选择题考查规则，判断题考查应用。",
                        "deviationFromPreference": ""
                      },
                      "questions": [
                        {
                          "type": "选择题",
                          "stem": "",
                          "options": ["A. 1", "B. 2"],
                          "answer": "A",
                          "explanation": "解析",
                          "difficulty": "基础",
                          "sourceQuote": "材料"
                        }
                      ]
                    }
                    """,
                    request))
        .isInstanceOf(InvalidWorksheetGenerationException.class)
        .hasMessageContaining("stem");
  }

  @Test
  void rejectsMissingType() {
    WorksheetCreateRequest request = sampleRequest(1);

    assertThatThrownBy(
            () ->
                parser.parse(
                    jsonWithSingleQuestion(
                        """
                        {
                          "type": "",
                          "stem": "1/4 + 1/4 = 1/2。",
                          "options": ["A. 正确", "B. 错误"],
                          "answer": "A",
                          "explanation": "分子相加，分母不变。",
                          "difficulty": "基础",
                          "sourceQuote": "材料"
                        }
                        """),
                    request))
        .isInstanceOf(InvalidWorksheetGenerationException.class)
        .hasMessageContaining("type");
  }

  @Test
  void rejectsMissingAnswer() {
    WorksheetCreateRequest request = sampleRequest(1);

    assertThatThrownBy(
            () ->
                parser.parse(
                    jsonWithSingleQuestion(
                        """
                        {
                          "type": "判断题",
                          "stem": "1/4 + 1/4 = 1/2。",
                          "options": ["A. 正确", "B. 错误"],
                          "answer": "",
                          "explanation": "分子相加，分母不变。",
                          "difficulty": "基础",
                          "sourceQuote": "材料"
                        }
                        """),
                    request))
        .isInstanceOf(InvalidWorksheetGenerationException.class)
        .hasMessageContaining("answer");
  }

  @Test
  void rejectsChoiceQuestionWithoutOptions() {
    WorksheetCreateRequest request = sampleRequest(1);

    assertThatThrownBy(
            () ->
                parser.parse(
                    """
                    {
                      "generationRationale": {
                        "summary": "材料讲解同分母分数加减法。",
                        "keyPoints": ["分母不变", "分子相加减"],
                        "difficultyPlan": "以中等难度为主，包含基础判断。",
                        "typePlan": "选择题考查规则，判断题考查应用。",
                        "deviationFromPreference": ""
                      },
                      "questions": [
                        {
                          "type": "选择题",
                          "stem": "题干",
                          "options": [],
                          "answer": "A",
                          "explanation": "解析",
                          "difficulty": "基础",
                          "sourceQuote": "材料"
                        }
                      ]
                    }
                    """,
                    request))
        .isInstanceOf(InvalidWorksheetGenerationException.class)
        .hasMessageContaining("options");
  }

  @Test
  void rejectsMissingExplanation() {
    WorksheetCreateRequest request = sampleRequest(1);

    assertThatThrownBy(
            () ->
                parser.parse(
                    """
                    {
                      "generationRationale": {
                        "summary": "材料讲解同分母分数加减法。",
                        "keyPoints": ["分母不变", "分子相加减"],
                        "difficultyPlan": "以中等难度为主，包含基础判断。",
                        "typePlan": "选择题考查规则，判断题考查应用。",
                        "deviationFromPreference": ""
                      },
                      "questions": [
                        {
                          "type": "判断题",
                          "stem": "1/4 + 1/4 = 1/2。",
                          "options": ["A. 正确", "B. 错误"],
                          "answer": "A",
                          "explanation": "",
                          "difficulty": "基础",
                          "sourceQuote": "材料"
                        }
                      ]
                    }
                    """,
                    request))
        .isInstanceOf(InvalidWorksheetGenerationException.class)
        .hasMessageContaining("explanation");
  }

  @Test
  void allowsMissingExplanationWhenExplanationNotRequested() {
    WorksheetCreateRequest request = sampleRequest(1, false);

    WorksheetGenerationResult result =
        parser.parse(
            """
            {
              "generationRationale": {
                "summary": "材料讲解同分母分数加减法。",
                "keyPoints": ["分母不变", "分子相加减"],
                "difficultyPlan": "以中等难度为主，包含基础判断。",
                "typePlan": "判断题考查应用。",
                "deviationFromPreference": ""
              },
              "questions": [
                {
                  "type": "判断题",
                  "stem": "1/4 + 1/4 = 1/2。",
                  "options": ["A. 正确", "B. 错误"],
                  "answer": "A",
                  "explanation": "",
                  "difficulty": "基础",
                  "sourceQuote": "材料"
                }
              ]
            }
            """,
            request);

    assertThat(result.questions()).hasSize(1);
  }

  @Test
  void rejectsMissingDifficulty() {
    WorksheetCreateRequest request = sampleRequest(1);

    assertThatThrownBy(
            () ->
                parser.parse(
                    """
                    {
                      "generationRationale": {
                        "summary": "材料讲解同分母分数加减法。",
                        "keyPoints": ["分母不变", "分子相加减"],
                        "difficultyPlan": "以中等难度为主，包含基础判断。",
                        "typePlan": "选择题考查规则，判断题考查应用。",
                        "deviationFromPreference": ""
                      },
                      "questions": [
                        {
                          "type": "判断题",
                          "stem": "1/4 + 1/4 = 1/2。",
                          "options": ["A. 正确", "B. 错误"],
                          "answer": "A",
                          "explanation": "分子相加，分母不变。",
                          "difficulty": "",
                          "sourceQuote": "材料"
                        }
                      ]
                    }
                    """,
                    request))
        .isInstanceOf(InvalidWorksheetGenerationException.class)
        .hasMessageContaining("difficulty");
  }

  @Test
  void rejectsChoiceQuestionWithFewerThanFourOptions() {
    WorksheetCreateRequest request = sampleRequest(1);

    assertThatThrownBy(
            () ->
                parser.parse(
                    jsonWithSingleQuestion(
                        """
                        {
                          "type": "选择题",
                          "stem": "同分母分数相加时，分母应如何处理？",
                          "options": ["A. 相加", "B. 不变", "C. 相减"],
                          "answer": "B",
                          "explanation": "同分母分数相加，分母保持不变。",
                          "difficulty": "中等",
                          "sourceQuote": "材料"
                        }
                        """),
                    request))
        .isInstanceOf(InvalidWorksheetGenerationException.class)
        .hasMessageContaining("options");
  }

  @Test
  void rejectsChoiceQuestionWithInvalidAnswer() {
    WorksheetCreateRequest request = sampleRequest(1);

    assertThatThrownBy(
            () ->
                parser.parse(
                    jsonWithSingleQuestion(
                        """
                        {
                          "type": "选择题",
                          "stem": "同分母分数相加时，分母应如何处理？",
                          "options": ["A. 相加", "B. 不变", "C. 相减", "D. 取平均"],
                          "answer": "E",
                          "explanation": "同分母分数相加，分母保持不变。",
                          "difficulty": "中等",
                          "sourceQuote": "材料"
                        }
                        """),
                    request))
        .isInstanceOf(InvalidWorksheetGenerationException.class)
        .hasMessageContaining("answer");
  }

  @Test
  void rejectsJudgmentQuestionWithNonStandardOptions() {
    WorksheetCreateRequest request = sampleRequest(1);

    assertThatThrownBy(
            () ->
                parser.parse(
                    jsonWithSingleQuestion(
                        """
                        {
                          "type": "判断题",
                          "stem": "1/4 + 1/4 = 1/2。",
                          "options": ["A. 对", "B. 错"],
                          "answer": "A",
                          "explanation": "分子相加，分母不变。",
                          "difficulty": "基础",
                          "sourceQuote": "材料"
                        }
                        """),
                    request))
        .isInstanceOf(InvalidWorksheetGenerationException.class)
        .hasMessageContaining("options");
  }

  @Test
  void rejectsJudgmentQuestionWithInvalidAnswer() {
    WorksheetCreateRequest request = sampleRequest(1);

    assertThatThrownBy(
            () ->
                parser.parse(
                    jsonWithSingleQuestion(
                        """
                        {
                          "type": "判断题",
                          "stem": "1/4 + 1/4 = 1/2。",
                          "options": ["A. 正确", "B. 错误"],
                          "answer": "C",
                          "explanation": "分子相加，分母不变。",
                          "difficulty": "基础",
                          "sourceQuote": "材料"
                        }
                        """),
                    request))
        .isInstanceOf(InvalidWorksheetGenerationException.class)
        .hasMessageContaining("answer");
  }

  @Test
  void rejectsBlankOption() {
    WorksheetCreateRequest request = sampleRequest(1);

    assertThatThrownBy(
            () ->
                parser.parse(
                    jsonWithSingleQuestion(
                        """
                        {
                          "type": "选择题",
                          "stem": "同分母分数相加时，分母应如何处理？",
                          "options": ["A. 相加", "", "C. 相减", "D. 取平均"],
                          "answer": "A",
                          "explanation": "同分母分数相加，分母保持不变。",
                          "difficulty": "中等",
                          "sourceQuote": "材料"
                        }
                        """),
                    request))
        .isInstanceOf(InvalidWorksheetGenerationException.class)
        .hasMessageContaining("option");
  }

  @Test
  void allowsOpenEndedQuestionWithoutOptions() {
    WorksheetCreateRequest request = sampleRequest(1);

    WorksheetGenerationResult result =
        parser.parse(
            jsonWithSingleQuestion(
                """
                {
                  "type": "简答题",
                  "stem": "请说明同分母分数相加的方法。",
                  "options": [],
                  "answer": "分母不变，分子相加。",
                  "explanation": "同分母分数相加时只处理分子。",
                  "difficulty": "中等",
                  "sourceQuote": "同分母分数相加，分母保持不变。"
                }
                """),
            request);

    assertThat(result.questions()).hasSize(1);
  }

  @Test
  void rejectsOpenEndedQuestionWithBlankOption() {
    WorksheetCreateRequest request = sampleRequest(1);

    assertThatThrownBy(
            () ->
                parser.parse(
                    jsonWithSingleQuestion(
                        """
                        {
                          "type": "简答题",
                          "stem": "请说明同分母分数相加的方法。",
                          "options": ["参考提示", " "],
                          "answer": "分母不变，分子相加。",
                          "explanation": "同分母分数相加时只处理分子。",
                          "difficulty": "中等",
                          "sourceQuote": "同分母分数相加，分母保持不变。"
                        }
                        """),
                    request))
        .isInstanceOf(InvalidWorksheetGenerationException.class)
        .hasMessageContaining("options");
  }

  @Test
  void rejectsChoiceQuestionWithInvalidOptionLabels() {
    WorksheetCreateRequest request = sampleRequest(1);

    assertThatThrownBy(
            () ->
                parser.parse(
                    jsonWithSingleQuestion(
                        """
                        {
                          "type": "选择题",
                          "stem": "同分母分数相加时，分母应如何处理？",
                          "options": ["A 选项", "C. 不变", "C. 相减", "D. 取平均"],
                          "answer": "B",
                          "explanation": "同分母分数相加，分母保持不变。",
                          "difficulty": "中等",
                          "sourceQuote": "材料"
                        }
                        """),
                    request))
        .isInstanceOf(InvalidWorksheetGenerationException.class)
        .hasMessageContaining("options");
  }

  private String jsonWithRationale(String rationaleFields) {
    return """
        {
          "generationRationale": {
        """
        + rationaleFields
        + """
          },
          "questions": [
            {
              "type": "判断题",
              "stem": "1/4 + 1/4 = 1/2。",
              "options": ["A. 正确", "B. 错误"],
              "answer": "A",
              "explanation": "分子相加，分母不变。",
              "difficulty": "基础",
              "sourceQuote": "材料"
            }
          ]
        }
        """;
  }

  private String jsonWithSingleQuestion(String questionJson) {
    return """
        {
          "generationRationale": {
            "summary": "材料讲解同分母分数加减法。",
            "keyPoints": ["分母不变", "分子相加减"],
            "difficultyPlan": "以中等难度为主，包含基础判断。",
            "typePlan": "选择题考查规则，判断题考查应用。",
            "deviationFromPreference": ""
          },
          "questions": [
        """
        + questionJson
        + """
          ]
        }
        """;
  }

  private String validJsonWithRepeatedQuestions(int count) {
    StringBuilder json = new StringBuilder();
    json.append(
        """
        {
          "generationRationale": {
            "summary": "材料讲解同分母分数加减法。",
            "keyPoints": ["分母不变", "分子相加减"],
            "difficultyPlan": "以中等难度为主，包含基础判断。",
            "typePlan": "判断题考查应用。",
            "deviationFromPreference": ""
          },
          "questions": [
        """);

    for (int index = 0; index < count; index++) {
      if (index > 0) {
        json.append(",");
      }
      json.append(
          """
              {
                "type": "判断题",
                "stem": "1/4 + 1/4 = 1/2。",
                "options": ["A. 正确", "B. 错误"],
                "answer": "A",
                "explanation": "分子相加，分母不变。",
                "difficulty": "基础",
                "sourceQuote": "分子相加，分母不变。"
              }
          """);
    }

    json.append(
        """
          ]
        }
        """);
    return json.toString();
  }

  private WorksheetCreateRequest sampleRequest(int questionCount) {
    return sampleRequest(questionCount, true);
  }

  private WorksheetCreateRequest sampleRequest(int questionCount, boolean includeExplanation) {
    return new WorksheetCreateRequest(
        "分数练习",
        List.of("doc-1"),
        questionCount,
        "五年级",
        "中等",
        List.of("选择题", "判断题"),
        includeExplanation);
  }
}
