# Qwen Worksheet Agent Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 接入千问 OpenAI-compatible 作为 EduSpark 练习卷 Agent，让模型基于资料半约束地生成题目、答案、解析和 AI 出题依据。

**Architecture:** 系统继续负责认证、userId 隔离、文件抽取、向量检索、存储和 Word 导出；千问 Agent 只负责教学理解与结构化生成。现有 `WorksheetGenerationStrategy` 保持为边界，升级 `SpringAiWorksheetGenerator`、`WorksheetPromptBuilder`、parser 和 DTO，并在 `edu_worksheet` 持久化 `generation_rationale_json`。

**Tech Stack:** Spring Boot 3.4, Spring AI `ChatClient`, DashScope OpenAI-compatible API, MyBatis-Plus, Flyway, React/Vite/Tailwind, Vitest.

---

## File Structure

- Modify `backend/src/main/resources/application.yml`: remove hardcoded API key, add DashScope OpenAI-compatible base URL and qwen defaults.
- Create `backend/src/main/resources/db/migration/V6__add_worksheet_generation_rationale.sql`: add `generation_rationale_json`.
- Modify `backend/src/main/java/com/eduspark/agent/worksheet/EduWorksheet.java`: map `generationRationaleJson`.
- Create `backend/src/main/java/com/eduspark/agent/worksheet/dto/WorksheetGenerationRationale.java`: rationale DTO.
- Modify `backend/src/main/java/com/eduspark/agent/worksheet/dto/WorksheetGenerationResult.java`: include `generationRationale`.
- Modify `backend/src/main/java/com/eduspark/agent/worksheet/dto/GeneratedWorksheetQuestion.java`: add `difficulty` and `sourceQuote`.
- Modify `backend/src/main/java/com/eduspark/agent/worksheet/dto/WorksheetDetailResponse.java`: expose `generationRationale`.
- Modify `backend/src/main/java/com/eduspark/agent/worksheet/WorksheetQuestionOutputParser.java`: validate rationale, semi-constrained question count, explanations.
- Modify `backend/src/main/java/com/eduspark/agent/worksheet/WorksheetPromptBuilder.java`: replace fixed schedule prompt with Agent prompt.
- Modify `backend/src/main/java/com/eduspark/agent/worksheet/SpringAiWorksheetGenerator.java`: use Qwen Agent system prompt and parse Agent result.
- Modify `backend/src/main/java/com/eduspark/agent/worksheet/WorksheetQuestionEnricher.java`: carry `difficulty` and source quote into final questions.
- Modify `backend/src/main/java/com/eduspark/agent/worksheet/MockWorksheetGenerator.java`: produce rationale-compatible deterministic data indirectly via service behavior.
- Modify `backend/src/main/java/com/eduspark/agent/worksheet/WorksheetService.java`: persist/read `generationRationaleJson`.
- Modify `backend/src/main/java/com/eduspark/agent/worksheet/WordExportService.java`: include AI rationale in Word export when present.
- Modify `contracts/task.contract.ts`: add rationale contract.
- Modify `docs/api.md`: document Qwen Agent result fields and setup.
- Modify `frontend/src/api/taskApi.ts`: add rationale types.
- Modify `frontend/src/features/task/components/WorksheetPanel.tsx`: show AI rationale.
- Modify `frontend/src/features/task/TaskConsolePage.test.tsx`: assert rationale display.

Note: this workspace is not a git repository. Omit commit steps during execution.

---

### Task 1: Safe Qwen Configuration

**Files:**
- Modify: `backend/src/main/resources/application.yml`
- Test: `backend/src/test/java/com/eduspark/agent/config/ApplicationConfigurationTest.java`

- [ ] **Step 1: Write the failing configuration test**

Create `backend/src/test/java/com/eduspark/agent/config/ApplicationConfigurationTest.java`:

```java
package com.eduspark.agent.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class ApplicationConfigurationTest {

  @Test
  void applicationYamlDoesNotHardcodeProviderApiKeys() throws Exception {
    String yaml = Files.readString(Path.of("src/main/resources/application.yml"));

    assertThat(yaml).doesNotContain("sk-");
    assertThat(yaml).contains("api-key: ${DASHSCOPE_API_KEY:}");
    assertThat(yaml)
        .contains(
            "base-url: ${SPRING_AI_OPENAI_BASE_URL:https://dashscope.aliyuncs.com/compatible-mode}");
    assertThat(yaml).contains("model: ${SPRING_AI_OPENAI_CHAT_MODEL:qwen-plus}");
  }
}
```

- [ ] **Step 2: Run the test and verify it fails**

Run:

```powershell
C:\Users\27846\.cache\codex-runtimes\apache-maven-3.9.9\bin\mvn.cmd -Dtest=ApplicationConfigurationTest test
```

Expected: FAIL because `application.yml` currently contains a hardcoded `sk-...` key and does not use `DASHSCOPE_API_KEY`.

- [ ] **Step 3: Update `application.yml`**

Change the `spring.ai.openai` block to:

```yaml
    openai:
      base-url: ${SPRING_AI_OPENAI_BASE_URL:https://dashscope.aliyuncs.com/compatible-mode}
      api-key: ${DASHSCOPE_API_KEY:}
      chat:
        model: ${SPRING_AI_OPENAI_CHAT_MODEL:qwen-plus}
        temperature: 0.3
      embedding:
        model: ${SPRING_AI_OPENAI_EMBEDDING_MODEL:text-embedding-3-small}
```

Keep `spring.ai.model.chat: ${SPRING_AI_MODEL_CHAT:openai}` and the existing datasource/Hikari settings unchanged.

- [ ] **Step 4: Run the test and verify it passes**

Run:

```powershell
C:\Users\27846\.cache\codex-runtimes\apache-maven-3.9.9\bin\mvn.cmd -Dtest=ApplicationConfigurationTest test
```

Expected: PASS.

---

### Task 2: Persist Generation Rationale

**Files:**
- Create: `backend/src/main/resources/db/migration/V6__add_worksheet_generation_rationale.sql`
- Modify: `backend/src/main/java/com/eduspark/agent/worksheet/EduWorksheet.java`
- Modify: `backend/src/test/java/com/eduspark/agent/worksheet/WorksheetSchemaIntegrationTest.java`

- [ ] **Step 1: Extend the schema test first**

In `WorksheetSchemaIntegrationTest`, add:

```java
  @Test
  void worksheetTableHasGenerationRationaleColumn() throws Exception {
    try (Connection connection = dataSource.getConnection()) {
      DatabaseMetaData metaData = connection.getMetaData();

      assertThat(hasColumn(metaData, "edu_worksheet", "generation_rationale_json")).isTrue();
    }
  }

  private boolean hasColumn(DatabaseMetaData metaData, String tableName, String columnName)
      throws Exception {
    try (ResultSet resultSet = metaData.getColumns(null, null, tableName, columnName)) {
      return resultSet.next();
    }
  }
```

- [ ] **Step 2: Run the test and verify it fails**

Run:

```powershell
C:\Users\27846\.cache\codex-runtimes\apache-maven-3.9.9\bin\mvn.cmd -Dtest=WorksheetSchemaIntegrationTest test
```

Expected: FAIL because the column does not exist.

- [ ] **Step 3: Add the Flyway migration**

Create `backend/src/main/resources/db/migration/V6__add_worksheet_generation_rationale.sql`:

```sql
alter table edu_worksheet
  add column generation_rationale_json longtext null after config_json;
```

- [ ] **Step 4: Map the new column in `EduWorksheet`**

Add field and accessors:

```java
  private String generationRationaleJson;

  public String getGenerationRationaleJson() {
    return generationRationaleJson;
  }

  public void setGenerationRationaleJson(String generationRationaleJson) {
    this.generationRationaleJson = generationRationaleJson;
  }
```

- [ ] **Step 5: Run schema test**

Run:

```powershell
C:\Users\27846\.cache\codex-runtimes\apache-maven-3.9.9\bin\mvn.cmd -Dtest=WorksheetSchemaIntegrationTest test
```

Expected: PASS and Flyway logs show migration version 6 applied.

---

### Task 3: Agent Result DTO and Parser Validation

**Files:**
- Create: `backend/src/main/java/com/eduspark/agent/worksheet/dto/WorksheetGenerationRationale.java`
- Modify: `backend/src/main/java/com/eduspark/agent/worksheet/dto/WorksheetGenerationResult.java`
- Modify: `backend/src/main/java/com/eduspark/agent/worksheet/dto/GeneratedWorksheetQuestion.java`
- Modify: `backend/src/main/java/com/eduspark/agent/worksheet/WorksheetQuestionOutputParser.java`
- Modify: `backend/src/test/java/com/eduspark/agent/worksheet/WorksheetQuestionOutputParserTest.java`

- [ ] **Step 1: Update parser tests first**

In valid JSON test, require `generationRationale`:

```java
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
```

Add tests:

```java
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
                          "type": "判断题",
                          "stem": "题干",
                          "options": ["A. 正确", "B. 错误"],
                          "answer": "A",
                          "explanation": "解析",
                          "difficulty": "基础"
                        }
                      ]
                    }
                    """,
                    request))
        .isInstanceOf(InvalidWorksheetGenerationException.class)
        .hasMessageContaining("generationRationale");
  }

  @Test
  void allowsQuestionCountWithinTwentyPercent() {
    WorksheetCreateRequest request = sampleRequest(10);

    WorksheetGenerationResult result =
        parser.parse(validJsonWithRepeatedQuestions(8), request);

    assertThat(result.questions()).hasSize(8);
  }
```

Implement `validJsonWithRepeatedQuestions(int count)` in the test using a `StringBuilder` to emit valid JSON with `generationRationale` and `count` questions.

- [ ] **Step 2: Run parser tests and verify failure**

Run:

```powershell
C:\Users\27846\.cache\codex-runtimes\apache-maven-3.9.9\bin\mvn.cmd -Dtest=WorksheetQuestionOutputParserTest test
```

Expected: FAIL because DTOs/parser do not yet require rationale and still require exact question count.

- [ ] **Step 3: Add rationale DTO**

Create `WorksheetGenerationRationale.java`:

```java
package com.eduspark.agent.worksheet.dto;

import java.util.List;

public record WorksheetGenerationRationale(
    String summary,
    List<String> keyPoints,
    String difficultyPlan,
    String typePlan,
    String deviationFromPreference) {}
```

- [ ] **Step 4: Update Agent DTOs**

Change `WorksheetGenerationResult` to:

```java
package com.eduspark.agent.worksheet.dto;

import java.util.List;

public record WorksheetGenerationResult(
    WorksheetGenerationRationale generationRationale,
    List<GeneratedWorksheetQuestion> questions) {}
```

Change `GeneratedWorksheetQuestion` to:

```java
package com.eduspark.agent.worksheet.dto;

import java.util.List;

public record GeneratedWorksheetQuestion(
    String type,
    String stem,
    List<String> options,
    String answer,
    String explanation,
    String difficulty,
    String sourceQuote) {}
```

- [ ] **Step 5: Update parser validation**

In `WorksheetQuestionOutputParser`, replace exact-count validation with range validation:

```java
    validateRationale(result.generationRationale());
    validateQuestionCount(result.questions().size(), request.questionCount());
```

Add helper methods:

```java
  private void validateRationale(WorksheetGenerationRationale rationale) {
    if (rationale == null) {
      throw new InvalidWorksheetGenerationException("generationRationale is required");
    }
    if (rationale.summary() == null || rationale.summary().isBlank()) {
      throw new InvalidWorksheetGenerationException("generationRationale.summary is required");
    }
    if (rationale.keyPoints() == null || rationale.keyPoints().isEmpty()) {
      throw new InvalidWorksheetGenerationException("generationRationale.keyPoints is required");
    }
    if (rationale.difficultyPlan() == null || rationale.difficultyPlan().isBlank()) {
      throw new InvalidWorksheetGenerationException("generationRationale.difficultyPlan is required");
    }
    if (rationale.typePlan() == null || rationale.typePlan().isBlank()) {
      throw new InvalidWorksheetGenerationException("generationRationale.typePlan is required");
    }
  }

  private void validateQuestionCount(int actualCount, int requestedCount) {
    int minimum = Math.max(1, (int) Math.floor(requestedCount * 0.8));
    int maximum = Math.max(minimum, (int) Math.ceil(requestedCount * 1.2));
    if (actualCount < minimum || actualCount > maximum) {
      throw new InvalidWorksheetGenerationException(
          "Expected question count between "
              + minimum
              + " and "
              + maximum
              + " but received "
              + actualCount);
    }
  }
```

In `validateQuestion`, add:

```java
    if (question.difficulty() == null || question.difficulty().isBlank()) {
      throw new InvalidWorksheetGenerationException("Question " + index + " must contain difficulty");
    }
```

For explanation:

```java
    if (question.explanation() == null || question.explanation().isBlank()) {
      throw new InvalidWorksheetGenerationException("Question " + index + " must contain explanation");
    }
```

- [ ] **Step 6: Run parser tests**

Run:

```powershell
C:\Users\27846\.cache\codex-runtimes\apache-maven-3.9.9\bin\mvn.cmd -Dtest=WorksheetQuestionOutputParserTest test
```

Expected: PASS.

---

### Task 4: Agent Prompt Builder

**Files:**
- Modify: `backend/src/main/java/com/eduspark/agent/worksheet/WorksheetPromptBuilder.java`
- Modify: `backend/src/test/java/com/eduspark/agent/worksheet/WorksheetPromptBuilderTest.java`

- [ ] **Step 1: Update prompt tests first**

In `WorksheetPromptBuilderTest`, assert these strings:

```java
assertThat(prompt).contains("教师偏好：");
assertThat(prompt).contains("半约束规则：");
assertThat(prompt).contains("题量允许在目标值上下 20% 内浮动");
assertThat(prompt).contains("[chunk-1]");
assertThat(prompt).contains("输出严格 JSON");
assertThat(prompt).contains("generationRationale");
```

- [ ] **Step 2: Run prompt tests and verify failure**

Run:

```powershell
C:\Users\27846\.cache\codex-runtimes\apache-maven-3.9.9\bin\mvn.cmd -Dtest=WorksheetPromptBuilderTest test
```

Expected: FAIL if current prompt still uses fixed question schedule and does not include `generationRationale`.

- [ ] **Step 3: Rewrite `buildUserPrompt`**

Replace the returned prompt with:

```java
    return """
        请基于以下资料生成练习卷。

        教师偏好：
        - 标题：%s
        - 年级：%s
        - 目标题量：%d
        - 偏好题型：%s
        - 目标难度：%s
        - 是否需要解析：%s

        半约束规则：
        - 题量允许在目标值上下 20%% 内浮动，但至少生成 1 题。
        - 题型优先遵循偏好，但可根据资料内容合理调整。
        - 年级是硬约束，不得超纲。
        - 如果偏离偏好，必须在 generationRationale.deviationFromPreference 说明。
        - 所有题目必须基于参考材料，不允许编造资料外事实。

        参考材料片段：
        %s

        请完成：
        1. 抽取资料中的核心知识点。
        2. 规划题型和难度分布。
        3. 生成题目、答案和解析。
        4. 输出严格 JSON，必须包含 generationRationale 和 questions。

        %s
        """
        .formatted(
            request.title(),
            request.gradeLevel(),
            request.questionCount(),
            String.join("、", request.questionTypes()),
            request.difficulty(),
            request.includeExplanation(),
            material,
            outputFormat);
```

Build chunk labels with:

```java
  private String formatMaterial(List<DocumentChunk> chunks) {
    if (chunks == null || chunks.isEmpty()) {
      return "（无可用参考材料）";
    }
    return java.util.stream.IntStream.range(0, chunks.size())
        .mapToObj(index -> "[chunk-" + (index + 1) + "]\n" + chunks.get(index).getContent())
        .collect(Collectors.joining("\n\n---\n\n"));
  }
```

- [ ] **Step 4: Run prompt tests**

Run:

```powershell
C:\Users\27846\.cache\codex-runtimes\apache-maven-3.9.9\bin\mvn.cmd -Dtest=WorksheetPromptBuilderTest test
```

Expected: PASS.

---

### Task 5: Spring AI Qwen Agent Generator

**Files:**
- Modify: `backend/src/main/java/com/eduspark/agent/worksheet/SpringAiWorksheetGenerator.java`
- Modify: `backend/src/main/java/com/eduspark/agent/worksheet/WorksheetQuestionEnricher.java`
- Modify: `backend/src/test/java/com/eduspark/agent/worksheet/SpringAiWorksheetGeneratorTest.java`

- [ ] **Step 1: Update generator test first**

Ensure the mocked model output includes `generationRationale`, `difficulty`, and `sourceQuote`. Assert the user prompt contains semi-constrained language:

```java
assertThat(capturedUserPrompt).contains("半约束规则");
assertThat(capturedUserPrompt).contains("generationRationale");
assertThat(questions.get(0).difficulty()).isEqualTo("中等");
assertThat(questions.get(0).sourceChunkIds()).contains("chunk-1");
```

- [ ] **Step 2: Run generator test and verify failure**

Run:

```powershell
C:\Users\27846\.cache\codex-runtimes\apache-maven-3.9.9\bin\mvn.cmd -Dtest=SpringAiWorksheetGeneratorTest test
```

Expected: FAIL until DTO/parser/enricher changes are complete.

- [ ] **Step 3: Update system prompt**

In `SpringAiWorksheetGenerator`, replace `SYSTEM_PROMPT` with:

```java
  private static final String SYSTEM_PROMPT =
      """
      你是 EduSpark 的教育出题 Agent，擅长根据教师上传的课堂资料生成练习题。

      你的职责：
      1. 阅读参考材料，抽取核心知识点、易错点和适合考查的能力点。
      2. 根据教师给出的年级、难度、题量和题型偏好，生成适合学生练习的题目。
      3. 题型和题量是偏好，不是死命令；如果材料更适合调整题型或题量，你可以调整，但必须说明原因。
      4. 每道题必须给出答案和解析。
      5. 所有题目必须基于参考材料，不允许编造材料外事实。
      6. 如果材料不足以支撑指定题量，应生成较少题目，并在 deviationFromPreference 中说明。
      7. 只输出 JSON，不输出 Markdown，不输出额外说明。
      """;
```

- [ ] **Step 4: Update enricher**

In `WorksheetQuestionEnricher`, when converting `GeneratedWorksheetQuestion` to `WorksheetQuestion`, use:

```java
generated.difficulty() == null || generated.difficulty().isBlank()
    ? request.difficulty()
    : generated.difficulty()
```

Keep source chunk ids from retrieved chunks:

```java
List<String> sourceChunkIds =
    chunks == null ? List.of() : chunks.stream().map(DocumentChunk::getId).toList();
```

- [ ] **Step 5: Run generator test**

Run:

```powershell
C:\Users\27846\.cache\codex-runtimes\apache-maven-3.9.9\bin\mvn.cmd -Dtest=SpringAiWorksheetGeneratorTest test
```

Expected: PASS.

---

### Task 6: Save and Return Generation Rationale

**Files:**
- Modify: `backend/src/main/java/com/eduspark/agent/worksheet/WorksheetService.java`
- Modify: `backend/src/main/java/com/eduspark/agent/worksheet/dto/WorksheetDetailResponse.java`
- Modify: `backend/src/main/java/com/eduspark/agent/worksheet/MockWorksheetGenerator.java`
- Modify: `backend/src/test/java/com/eduspark/agent/worksheet/WorksheetControllerIntegrationTest.java`

- [ ] **Step 1: Write controller integration assertion first**

In `WorksheetControllerIntegrationTest`, after getting worksheet detail:

```java
mockMvc
    .perform(
        get("/api/users/{userId}/worksheets/{worksheetId}", user.userId(), worksheetId)
            .header("Authorization", "Bearer " + user.token()))
    .andExpect(status().isOk())
    .andExpect(jsonPath("$.generationRationale.summary").isNotEmpty())
    .andExpect(jsonPath("$.generationRationale.keyPoints").isArray())
    .andExpect(jsonPath("$.questions").isArray());
```

- [ ] **Step 2: Run worksheet controller test and verify failure**

Run:

```powershell
C:\Users\27846\.cache\codex-runtimes\apache-maven-3.9.9\bin\mvn.cmd -Dtest=WorksheetControllerIntegrationTest test
```

Expected: FAIL because detail response does not include `generationRationale`.

- [ ] **Step 3: Extend response DTO**

Change `WorksheetDetailResponse` to include rationale before questions:

```java
    WorksheetGenerationRationale generationRationale,
    List<WorksheetQuestion> questions,
```

- [ ] **Step 4: Persist rationale in service**

After generation:

```java
WorksheetGenerationResult result = worksheetGenerationStrategy.generateResult(request, chunks);
worksheet.setGenerationRationaleJson(writeJson(result.generationRationale()));
List<WorksheetQuestion> questions = worksheetQuestionEnricher.enrich(request, result, chunks);
```

If keeping the existing `generate(...)` interface, add a second interface method:

```java
default WorksheetGenerationResult generateResult(WorksheetCreateRequest request, List<DocumentChunk> chunks) {
  throw new UnsupportedOperationException("generateResult is not implemented");
}
```

Then update both Spring and mock strategies to implement `generateResult(...)`, while `generate(...)` can delegate to `generateResult(...)` and return enriched questions only where needed.

- [ ] **Step 5: Read rationale in `findDetail`**

Add helper:

```java
  private WorksheetGenerationRationale readRationale(String rationaleJson) {
    if (rationaleJson == null || rationaleJson.isBlank()) {
      return null;
    }
    return readJson(rationaleJson, WorksheetGenerationRationale.class);
  }
```

Pass it into `WorksheetDetailResponse`:

```java
readRationale(worksheet.getGenerationRationaleJson()),
readQuestions(worksheet.getQuestionsJson()),
```

- [ ] **Step 6: Run worksheet tests**

Run:

```powershell
C:\Users\27846\.cache\codex-runtimes\apache-maven-3.9.9\bin\mvn.cmd -Dtest=WorksheetControllerIntegrationTest,WorksheetExportIntegrationTest test
```

Expected: PASS.

---

### Task 7: Word Export, Contracts, and API Docs

**Files:**
- Modify: `backend/src/main/java/com/eduspark/agent/worksheet/WordExportService.java`
- Modify: `backend/src/test/java/com/eduspark/agent/worksheet/WorksheetExportIntegrationTest.java`
- Modify: `contracts/task.contract.ts`
- Modify: `docs/api.md`

- [ ] **Step 1: Update export test first**

In `WorksheetExportIntegrationTest`, assert exported text contains rationale heading:

```java
assertThat(readDocumentText(exportedBytes))
    .contains("Word练习", "AI 出题依据", "答案与解析");
```

- [ ] **Step 2: Run export test and verify failure**

Run:

```powershell
C:\Users\27846\.cache\codex-runtimes\apache-maven-3.9.9\bin\mvn.cmd -Dtest=WorksheetExportIntegrationTest test
```

Expected: FAIL because Word export does not yet write rationale.

- [ ] **Step 3: Add rationale to Word export**

In `WordExportService.export`, after `writeMeta(...)`, call:

```java
writeRationale(document, worksheet);
```

Add:

```java
  private void writeRationale(XWPFDocument document, WorksheetDetailResponse worksheet) {
    if (worksheet.generationRationale() == null) {
      return;
    }
    writeHeading(document, "AI 出题依据");
    writeLine(document, "资料理解：" + worksheet.generationRationale().summary());
    writeLine(document, "知识点：" + String.join("、", worksheet.generationRationale().keyPoints()));
    writeLine(document, "难度安排：" + worksheet.generationRationale().difficultyPlan());
    writeLine(document, "题型安排：" + worksheet.generationRationale().typePlan());
    if (worksheet.generationRationale().deviationFromPreference() != null
        && !worksheet.generationRationale().deviationFromPreference().isBlank()) {
      writeLine(document, "偏好调整：" + worksheet.generationRationale().deviationFromPreference());
    }
    writeSpacer(document);
  }
```

- [ ] **Step 4: Update TypeScript contract**

In `contracts/task.contract.ts`, add:

```ts
export interface WorksheetGenerationRationale {
  summary: string;
  keyPoints: string[];
  difficultyPlan: string;
  typePlan: string;
  deviationFromPreference?: string;
}
```

Add to `WorksheetDetailResponse`:

```ts
generationRationale?: WorksheetGenerationRationale | null;
```

- [ ] **Step 5: Update API docs**

In `docs/api.md`, add `generationRationale` to worksheet detail example:

```json
"generationRationale": {
  "summary": "模型对资料的理解",
  "keyPoints": ["知识点一", "知识点二"],
  "difficultyPlan": "难度安排说明",
  "typePlan": "题型安排说明",
  "deviationFromPreference": "如有偏离，在这里说明"
}
```

Add setup note:

```powershell
$env:DASHSCOPE_API_KEY="<dashscope-api-key>"
$env:SPRING_AI_OPENAI_BASE_URL="https://dashscope.aliyuncs.com/compatible-mode"
$env:SPRING_AI_OPENAI_CHAT_MODEL="qwen-plus"
$env:AGENT_PLANNER_MODE="spring-ai"
```

- [ ] **Step 6: Run export test**

Run:

```powershell
C:\Users\27846\.cache\codex-runtimes\apache-maven-3.9.9\bin\mvn.cmd -Dtest=WorksheetExportIntegrationTest test
```

Expected: PASS.

---

### Task 8: Frontend Rationale Display

**Files:**
- Modify: `frontend/src/api/taskApi.ts`
- Modify: `frontend/src/features/task/components/WorksheetPanel.tsx`
- Modify: `frontend/src/features/task/TaskConsolePage.test.tsx`

- [ ] **Step 1: Update frontend test first**

In mock worksheet detail response, add:

```ts
generationRationale: {
  summary: "资料围绕同分母分数加减法展开。",
  keyPoints: ["分母不变", "分子相加减"],
  difficultyPlan: "基础题和中等题结合。",
  typePlan: "选择题考查概念，判断题考查应用。",
  deviationFromPreference: ""
},
```

Assert:

```ts
expect(await screen.findByText("AI 出题依据")).toBeInTheDocument();
expect(screen.getByText("资料围绕同分母分数加减法展开。")).toBeInTheDocument();
expect(screen.getByText("分母不变")).toBeInTheDocument();
```

- [ ] **Step 2: Run Vitest and verify failure**

Run:

```powershell
D:\Project\eduspark-agent\frontend\node_modules\.bin\vitest.cmd run
```

Expected: FAIL because UI does not render rationale yet.

- [ ] **Step 3: Add API types**

In `frontend/src/api/taskApi.ts`, add:

```ts
export interface WorksheetGenerationRationale {
  summary: string;
  keyPoints: string[];
  difficultyPlan: string;
  typePlan: string;
  deviationFromPreference?: string | null;
}
```

Add to `WorksheetDetail`:

```ts
generationRationale?: WorksheetGenerationRationale | null;
```

- [ ] **Step 4: Render rationale in `WorksheetPanel`**

Before question preview, add:

```tsx
{worksheet?.generationRationale ? (
  <div className="mt-5 rounded-lg border border-blue-100 bg-blue-50/70 p-3">
    <h3 className="text-sm font-semibold text-slate-950">AI 出题依据</h3>
    <p className="mt-2 text-sm leading-6 text-slate-600">{worksheet.generationRationale.summary}</p>
    <div className="mt-3 flex flex-wrap gap-2">
      {worksheet.generationRationale.keyPoints.map((point) => (
        <span
          className="rounded-lg bg-white/80 px-2.5 py-1 text-xs font-semibold text-blue-700"
          key={point}
        >
          {point}
        </span>
      ))}
    </div>
    <div className="mt-3 grid gap-2 text-sm leading-6 text-slate-600">
      <p>难度安排：{worksheet.generationRationale.difficultyPlan}</p>
      <p>题型安排：{worksheet.generationRationale.typePlan}</p>
      {worksheet.generationRationale.deviationFromPreference ? (
        <p>偏好调整：{worksheet.generationRationale.deviationFromPreference}</p>
      ) : null}
    </div>
  </div>
) : null}
```

- [ ] **Step 5: Run frontend tests**

Run:

```powershell
D:\Project\eduspark-agent\frontend\node_modules\.bin\vitest.cmd run
```

Expected: PASS.

- [ ] **Step 6: Run TypeScript check**

Run:

```powershell
D:\Project\eduspark-agent\frontend\node_modules\.bin\tsc.cmd -b
```

Expected: PASS.

---

### Task 9: Final Verification

**Files:**
- No code changes.

- [ ] **Step 1: Backend full package**

Run:

```powershell
C:\Users\27846\.cache\codex-runtimes\apache-maven-3.9.9\bin\mvn.cmd package
```

Expected: BUILD SUCCESS; all backend tests pass.

- [ ] **Step 2: Frontend tests**

Run:

```powershell
D:\Project\eduspark-agent\frontend\node_modules\.bin\vitest.cmd run
```

Expected: all frontend tests pass.

- [ ] **Step 3: Frontend production build**

Run:

```powershell
D:\Project\eduspark-agent\frontend\node_modules\.bin\vite.cmd build
```

Expected: build completes successfully and writes `frontend/dist`.

- [ ] **Step 4: Manual runtime configuration check**

Start backend with:

```powershell
$env:DASHSCOPE_API_KEY="<dashscope-api-key>"
$env:SPRING_AI_OPENAI_BASE_URL="https://dashscope.aliyuncs.com/compatible-mode"
$env:SPRING_AI_OPENAI_CHAT_MODEL="qwen-plus"
$env:AGENT_PLANNER_MODE="spring-ai"
C:\Users\27846\.cache\codex-runtimes\apache-maven-3.9.9\bin\mvn.cmd spring-boot:run
```

Expected: app starts without missing API key configuration resolution errors. Creating a worksheet with uploaded material calls the Spring AI generator and returns `generationRationale` in worksheet detail.

---

## Self-Review

- Spec coverage: configuration security, DashScope OpenAI-compatible setup, Agent prompt, semi-constrained output, rationale persistence, frontend display, Word export, and tests are covered.
- Scope: one coherent feature; no unrelated dashboard/history work included.
- Type consistency: `WorksheetGenerationRationale`, `generationRationale`, `generation_rationale_json`, and `generationRationaleJson` are consistently named across API, DB, and Java bean mapping.
- Execution note: workspace is not a git repo, so commit steps are intentionally omitted.
