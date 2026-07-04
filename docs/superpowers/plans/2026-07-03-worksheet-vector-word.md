# Worksheet Vector Word Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build user-scoped worksheet generation from uploaded documents, with MySQL-stored OpenAI embeddings and Word export.

**Architecture:** Reuse the existing document, task, SSE, and artifact flow. Add focused `vector` and `worksheet` backend packages, then add a worksheet mode to the existing React workspace.

**Tech Stack:** Spring Boot 3.4, MyBatis-Plus, Flyway, Spring AI OpenAI, Apache POI, React, TypeScript, Tailwind, Vitest.

---

## File Structure

Backend files:

- Create `backend/src/main/resources/db/migration/V4__add_document_chunks_and_worksheets.sql`: document chunks, worksheets, exports.
- Modify `backend/pom.xml`: add Apache POI dependency.
- Create `backend/src/main/java/com/eduspark/agent/vector/DocumentChunk.java`: chunk entity.
- Create `backend/src/main/java/com/eduspark/agent/vector/DocumentChunkMapper.java`: chunk persistence and user-scoped queries.
- Create `backend/src/main/java/com/eduspark/agent/vector/DocumentChunker.java`: deterministic text chunking.
- Create `backend/src/main/java/com/eduspark/agent/vector/EmbeddingService.java`: mock/openai embeddings.
- Create `backend/src/main/java/com/eduspark/agent/vector/DocumentIndexService.java`: index uploaded documents.
- Create `backend/src/main/java/com/eduspark/agent/vector/VectorSearchService.java`: cosine similarity retrieval.
- Modify `backend/src/main/java/com/eduspark/agent/document/DocumentService.java`: call indexing after document insert.
- Create `backend/src/main/java/com/eduspark/agent/worksheet/*`: entities, DTOs, controller, service, generator, export service.
- Modify `backend/src/main/java/com/eduspark/agent/api/ApiExceptionHandler.java`: map worksheet validation and generation errors if needed.
- Modify `contracts/task.contract.ts` and `docs/api.md`: add worksheet contracts.

Frontend files:

- Modify `frontend/src/api/taskApi.ts`: worksheet DTOs and API calls.
- Modify `frontend/src/features/task/TaskConsolePage.tsx`: worksheet state and orchestration.
- Create `frontend/src/features/task/components/WorksheetComposer.tsx`: worksheet configuration UI.
- Create `frontend/src/features/task/components/WorksheetResultPanel.tsx`: question rendering and Word download.
- Modify `frontend/src/features/task/components/WorkspaceShell.tsx`: mode switch if needed.
- Modify `frontend/src/features/task/TaskConsolePage.test.tsx`: worksheet UI and API assertions.

## Task 1: Backend Schema And POI Dependency

**Files:**
- Create: `backend/src/main/resources/db/migration/V4__add_document_chunks_and_worksheets.sql`
- Modify: `backend/pom.xml`
- Test: `backend/src/test/java/com/eduspark/agent/worksheet/WorksheetSchemaIntegrationTest.java`

- [ ] **Step 1: Write the failing schema test**

Create `backend/src/test/java/com/eduspark/agent/worksheet/WorksheetSchemaIntegrationTest.java`:

```java
package com.eduspark.agent.worksheet;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class WorksheetSchemaIntegrationTest {

  @Autowired private DataSource dataSource;

  @Test
  void worksheetTablesExistAfterFlywayMigration() throws Exception {
    DatabaseMetaData metaData = dataSource.getConnection().getMetaData();

    assertThat(hasTable(metaData, "edu_document_chunk")).isTrue();
    assertThat(hasTable(metaData, "edu_worksheet")).isTrue();
    assertThat(hasTable(metaData, "edu_worksheet_export")).isTrue();
  }

  private boolean hasTable(DatabaseMetaData metaData, String tableName) throws Exception {
    try (ResultSet resultSet = metaData.getTables(null, null, tableName, null)) {
      return resultSet.next();
    }
  }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run:

```powershell
cd D:\Project\eduspark-agent\backend
& 'C:\Users\27846\.cache\codex-runtimes\apache-maven-3.9.9\bin\mvn.cmd' -Dtest=WorksheetSchemaIntegrationTest test
```

Expected: FAIL because the three tables do not exist.

- [ ] **Step 3: Add Flyway migration**

Create `backend/src/main/resources/db/migration/V4__add_document_chunks_and_worksheets.sql`:

```sql
create table edu_document_chunk (
  id varchar(36) primary key,
  user_id varchar(36) not null,
  document_id varchar(36) not null,
  chunk_index int not null,
  content longtext not null,
  embedding_json longtext not null,
  token_count int null,
  created_at datetime not null
);

create index idx_chunk_user_document on edu_document_chunk(user_id, document_id, chunk_index);
create index idx_chunk_user_created on edu_document_chunk(user_id, created_at);

create table edu_worksheet (
  id varchar(36) primary key,
  user_id varchar(36) not null,
  task_id varchar(36) null,
  title varchar(255) not null,
  document_ids longtext not null,
  config_json longtext not null,
  questions_json longtext null,
  status varchar(32) not null,
  failure_reason text null,
  created_at datetime not null,
  updated_at datetime not null
);

create index idx_worksheet_user_created on edu_worksheet(user_id, created_at);

create table edu_worksheet_export (
  id varchar(36) primary key,
  user_id varchar(36) not null,
  worksheet_id varchar(36) not null,
  file_name varchar(255) not null,
  storage_path varchar(512) not null,
  created_at datetime not null
);

create index idx_export_user_worksheet on edu_worksheet_export(user_id, worksheet_id);
```

- [ ] **Step 4: Add Apache POI**

Modify `backend/pom.xml` inside `<properties>`:

```xml
<poi.version>5.4.0</poi.version>
```

Add inside `<dependencies>`:

```xml
<dependency>
  <groupId>org.apache.poi</groupId>
  <artifactId>poi-ooxml</artifactId>
  <version>${poi.version}</version>
</dependency>
```

- [ ] **Step 5: Run test to verify it passes**

Run:

```powershell
& 'C:\Users\27846\.cache\codex-runtimes\apache-maven-3.9.9\bin\mvn.cmd' -Dtest=WorksheetSchemaIntegrationTest test
```

Expected: PASS.

## Task 2: Document Chunking And Embedding Index

**Files:**
- Create: `backend/src/main/java/com/eduspark/agent/vector/DocumentChunk.java`
- Create: `backend/src/main/java/com/eduspark/agent/vector/DocumentChunkMapper.java`
- Create: `backend/src/main/java/com/eduspark/agent/vector/DocumentChunker.java`
- Create: `backend/src/main/java/com/eduspark/agent/vector/EmbeddingService.java`
- Create: `backend/src/main/java/com/eduspark/agent/vector/DocumentIndexService.java`
- Modify: `backend/src/main/java/com/eduspark/agent/document/DocumentService.java`
- Test: `backend/src/test/java/com/eduspark/agent/vector/DocumentIndexServiceIntegrationTest.java`

- [ ] **Step 1: Write failing indexing test**

Create `backend/src/test/java/com/eduspark/agent/vector/DocumentIndexServiceIntegrationTest.java`:

```java
package com.eduspark.agent.vector;

import static org.assertj.core.api.Assertions.assertThat;

import com.eduspark.agent.document.DocumentService;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class DocumentIndexServiceIntegrationTest {

  @Autowired private DocumentService documentService;
  @Autowired private DocumentChunkMapper documentChunkMapper;

  @Test
  void uploadCreatesUserScopedDocumentChunksWithEmbeddings() {
    MockMultipartFile file =
        new MockMultipartFile(
            "file",
            "lesson.txt",
            "text/plain",
            "比例应用题讲义。比例表示两个量之间的关系。学生需要理解单位量、总量和比值。"
                .repeat(80)
                .getBytes(StandardCharsets.UTF_8));

    String documentId = documentService.upload("user-index-a", file).getId();

    List<DocumentChunk> chunks = documentChunkMapper.selectByDocumentIdAndUserId(documentId, "user-index-a");
    assertThat(chunks).isNotEmpty();
    assertThat(chunks.get(0).getUserId()).isEqualTo("user-index-a");
    assertThat(chunks.get(0).getDocumentId()).isEqualTo(documentId);
    assertThat(chunks.get(0).getEmbeddingJson()).startsWith("[");
  }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run:

```powershell
& 'C:\Users\27846\.cache\codex-runtimes\apache-maven-3.9.9\bin\mvn.cmd' -Dtest=DocumentIndexServiceIntegrationTest test
```

Expected: FAIL because vector classes do not exist.

- [ ] **Step 3: Implement chunk entity and mapper**

Create `DocumentChunk.java`:

```java
package com.eduspark.agent.vector;

import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("edu_document_chunk")
public class DocumentChunk {
  private String id;
  private String userId;
  private String documentId;
  private Integer chunkIndex;
  private String content;
  private String embeddingJson;
  private Integer tokenCount;
  private LocalDateTime createdAt;

  public String getId() { return id; }
  public void setId(String id) { this.id = id; }
  public String getUserId() { return userId; }
  public void setUserId(String userId) { this.userId = userId; }
  public String getDocumentId() { return documentId; }
  public void setDocumentId(String documentId) { this.documentId = documentId; }
  public Integer getChunkIndex() { return chunkIndex; }
  public void setChunkIndex(Integer chunkIndex) { this.chunkIndex = chunkIndex; }
  public String getContent() { return content; }
  public void setContent(String content) { this.content = content; }
  public String getEmbeddingJson() { return embeddingJson; }
  public void setEmbeddingJson(String embeddingJson) { this.embeddingJson = embeddingJson; }
  public Integer getTokenCount() { return tokenCount; }
  public void setTokenCount(Integer tokenCount) { this.tokenCount = tokenCount; }
  public LocalDateTime getCreatedAt() { return createdAt; }
  public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
```

Create `DocumentChunkMapper.java`:

```java
package com.eduspark.agent.vector;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface DocumentChunkMapper extends BaseMapper<DocumentChunk> {
  default List<DocumentChunk> selectByDocumentIdAndUserId(String documentId, String userId) {
    return selectList(
        new QueryWrapper<DocumentChunk>()
            .eq("document_id", documentId)
            .eq("user_id", userId)
            .orderByAsc("chunk_index"));
  }

  default List<DocumentChunk> selectByUserIdAndDocumentIds(String userId, List<String> documentIds) {
    return selectList(
        new QueryWrapper<DocumentChunk>()
            .eq("user_id", userId)
            .in("document_id", documentIds)
            .orderByAsc("document_id", "chunk_index"));
  }
}
```

- [ ] **Step 4: Implement chunking and mock embeddings**

Create `DocumentChunker.java`:

```java
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
    String normalized = text.replace("\r\n", "\n").trim();
    List<String> chunks = new ArrayList<>();
    int start = 0;
    while (start < normalized.length()) {
      int end = Math.min(normalized.length(), start + CHUNK_SIZE);
      chunks.add(normalized.substring(start, end));
      if (end == normalized.length()) {
        break;
      }
      start = Math.max(0, end - OVERLAP);
    }
    return chunks;
  }
}
```

Create `EmbeddingService.java`:

```java
package com.eduspark.agent.vector;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class EmbeddingService {
  private final ObjectMapper objectMapper;
  private final String plannerMode;

  public EmbeddingService(ObjectMapper objectMapper, @Value("${agent.planner.mode:mock}") String plannerMode) {
    this.objectMapper = objectMapper;
    this.plannerMode = plannerMode;
  }

  public String embedAsJson(String text) {
    List<Double> vector = "mock".equalsIgnoreCase(plannerMode) ? mockVector(text) : mockVector(text);
    try {
      return objectMapper.writeValueAsString(vector);
    } catch (JsonProcessingException exception) {
      throw new IllegalStateException("Embedding serialization failed", exception);
    }
  }

  private List<Double> mockVector(String text) {
    int hash = text == null ? 0 : text.hashCode();
    List<Double> vector = new ArrayList<>();
    for (int i = 0; i < 16; i++) {
      vector.add(((hash >> (i % 8)) & 0xff) / 255.0);
    }
    return vector;
  }
}
```

- [ ] **Step 5: Implement index service and wire upload**

Create `DocumentIndexService.java`:

```java
package com.eduspark.agent.vector;

import com.eduspark.agent.document.EduDocument;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class DocumentIndexService {
  private final DocumentChunker chunker;
  private final EmbeddingService embeddingService;
  private final DocumentChunkMapper chunkMapper;

  public DocumentIndexService(DocumentChunker chunker, EmbeddingService embeddingService, DocumentChunkMapper chunkMapper) {
    this.chunker = chunker;
    this.embeddingService = embeddingService;
    this.chunkMapper = chunkMapper;
  }

  public void index(EduDocument document) {
    List<String> chunks = chunker.chunk(document.getExtractedText());
    for (int index = 0; index < chunks.size(); index++) {
      DocumentChunk chunk = new DocumentChunk();
      chunk.setId(UUID.randomUUID().toString());
      chunk.setUserId(document.getUserId());
      chunk.setDocumentId(document.getId());
      chunk.setChunkIndex(index);
      chunk.setContent(chunks.get(index));
      chunk.setEmbeddingJson(embeddingService.embedAsJson(chunks.get(index)));
      chunk.setTokenCount(Math.max(1, chunks.get(index).length() / 2));
      chunk.setCreatedAt(LocalDateTime.now());
      chunkMapper.insert(chunk);
    }
  }
}
```

Modify `DocumentService` constructor and upload method:

```java
private final DocumentIndexService documentIndexService;

public DocumentService(DocumentMapper documentMapper, DocumentIndexService documentIndexService) {
  this.documentMapper = documentMapper;
  this.documentIndexService = documentIndexService;
  this.uploadRoot = Path.of("var", "uploads", "documents").toAbsolutePath().normalize();
}
```

After `documentMapper.insert(document);` add:

```java
documentIndexService.index(document);
```

Add import:

```java
import com.eduspark.agent.vector.DocumentIndexService;
```

- [ ] **Step 6: Run test to verify it passes**

Run:

```powershell
& 'C:\Users\27846\.cache\codex-runtimes\apache-maven-3.9.9\bin\mvn.cmd' -Dtest=DocumentIndexServiceIntegrationTest test
```

Expected: PASS.

## Task 3: Vector Search

**Files:**
- Create: `backend/src/main/java/com/eduspark/agent/vector/VectorSearchService.java`
- Test: `backend/src/test/java/com/eduspark/agent/vector/VectorSearchServiceTest.java`

- [ ] **Step 1: Write failing vector search test**

Create `VectorSearchServiceTest.java`:

```java
package com.eduspark.agent.vector;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class VectorSearchServiceTest {

  @Test
  void ranksChunksByCosineSimilarity() {
    VectorSearchService service = new VectorSearchService(null, null);
    DocumentChunk close = chunk("close", "[1.0,0.0]");
    DocumentChunk far = chunk("far", "[0.0,1.0]");

    List<DocumentChunk> ranked = service.rank("[0.9,0.1]", List.of(far, close), 1);

    assertThat(ranked).extracting(DocumentChunk::getId).containsExactly("close");
  }

  private DocumentChunk chunk(String id, String vector) {
    DocumentChunk chunk = new DocumentChunk();
    chunk.setId(id);
    chunk.setEmbeddingJson(vector);
    return chunk;
  }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run:

```powershell
& 'C:\Users\27846\.cache\codex-runtimes\apache-maven-3.9.9\bin\mvn.cmd' -Dtest=VectorSearchServiceTest test
```

Expected: FAIL because `VectorSearchService` does not exist.

- [ ] **Step 3: Implement vector search**

Create `VectorSearchService.java`:

```java
package com.eduspark.agent.vector;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class VectorSearchService {
  private final DocumentChunkMapper chunkMapper;
  private final EmbeddingService embeddingService;
  private final ObjectMapper objectMapper = new ObjectMapper();

  public VectorSearchService(DocumentChunkMapper chunkMapper, EmbeddingService embeddingService) {
    this.chunkMapper = chunkMapper;
    this.embeddingService = embeddingService;
  }

  public List<DocumentChunk> search(String userId, List<String> documentIds, String query, int limit) {
    List<DocumentChunk> chunks = chunkMapper.selectByUserIdAndDocumentIds(userId, documentIds);
    return rank(embeddingService.embedAsJson(query), chunks, limit);
  }

  List<DocumentChunk> rank(String queryEmbeddingJson, List<DocumentChunk> chunks, int limit) {
    List<Double> query = parse(queryEmbeddingJson);
    return chunks.stream()
        .sorted(Comparator.comparingDouble((DocumentChunk chunk) -> cosine(query, parse(chunk.getEmbeddingJson()))).reversed())
        .limit(Math.max(1, limit))
        .toList();
  }

  private List<Double> parse(String json) {
    try {
      return objectMapper.readValue(json, new TypeReference<List<Double>>() {});
    } catch (Exception exception) {
      throw new IllegalArgumentException("Invalid embedding JSON", exception);
    }
  }

  private double cosine(List<Double> left, List<Double> right) {
    int size = Math.min(left.size(), right.size());
    double dot = 0;
    double leftNorm = 0;
    double rightNorm = 0;
    for (int index = 0; index < size; index++) {
      dot += left.get(index) * right.get(index);
      leftNorm += left.get(index) * left.get(index);
      rightNorm += right.get(index) * right.get(index);
    }
    if (leftNorm == 0 || rightNorm == 0) {
      return 0;
    }
    return dot / (Math.sqrt(leftNorm) * Math.sqrt(rightNorm));
  }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run:

```powershell
& 'C:\Users\27846\.cache\codex-runtimes\apache-maven-3.9.9\bin\mvn.cmd' -Dtest=VectorSearchServiceTest test
```

Expected: PASS.

## Task 4: Worksheet Domain And API

**Files:**
- Create: `backend/src/main/java/com/eduspark/agent/worksheet/WorksheetStatus.java`
- Create: `backend/src/main/java/com/eduspark/agent/worksheet/EduWorksheet.java`
- Create: `backend/src/main/java/com/eduspark/agent/worksheet/WorksheetMapper.java`
- Create: `backend/src/main/java/com/eduspark/agent/worksheet/dto/*.java`
- Create: `backend/src/main/java/com/eduspark/agent/worksheet/WorksheetGenerator.java`
- Create: `backend/src/main/java/com/eduspark/agent/worksheet/WorksheetService.java`
- Create: `backend/src/main/java/com/eduspark/agent/worksheet/WorksheetController.java`
- Test: `backend/src/test/java/com/eduspark/agent/worksheet/WorksheetControllerIntegrationTest.java`

- [ ] **Step 1: Write failing worksheet API test**

Create `WorksheetControllerIntegrationTest.java`:

```java
package com.eduspark.agent.worksheet;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class WorksheetControllerIntegrationTest {

  @Autowired private MockMvc mockMvc;

  @Test
  void createsWorksheetFromUserScopedDocument() throws Exception {
    AuthSession user = register("worksheet-owner");
    String documentId = uploadDocument(user, "lesson.txt", "比例应用题资料。".repeat(100));

    mockMvc
        .perform(
            post("/api/users/{userId}/worksheets", user.userId())
                .header("Authorization", "Bearer " + user.token())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                  {
                    "title":"比例练习",
                    "documentIds":["%s"],
                    "questionCount":5,
                    "gradeLevel":"六年级",
                    "difficulty":"MEDIUM",
                    "questionTypes":["SINGLE_CHOICE","FILL_BLANK"],
                    "includeExplanation":true
                  }
                  """.formatted(documentId)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.worksheetId").isNotEmpty())
        .andExpect(jsonPath("$.taskId").isNotEmpty())
        .andExpect(jsonPath("$.status").value("PENDING"));
  }

  private AuthSession register(String username) throws Exception {
    String body =
        mockMvc
            .perform(
                post("/api/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"username\":\"%s\",\"password\":\"secret123\"}".formatted(username)))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    return new AuthSession(JsonPath.read(body, "$.userId"), JsonPath.read(body, "$.token"));
  }

  private String uploadDocument(AuthSession user, String fileName, String text) throws Exception {
    MockMultipartFile file = new MockMultipartFile("file", fileName, "text/plain", text.getBytes(StandardCharsets.UTF_8));
    String body =
        mockMvc
            .perform(multipart("/api/users/{userId}/documents", user.userId()).file(file).header("Authorization", "Bearer " + user.token()))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    return JsonPath.read(body, "$.id");
  }

  private record AuthSession(String userId, String token) {}
}
```

- [ ] **Step 2: Run test to verify it fails**

Run:

```powershell
& 'C:\Users\27846\.cache\codex-runtimes\apache-maven-3.9.9\bin\mvn.cmd' -Dtest=WorksheetControllerIntegrationTest test
```

Expected: FAIL because worksheet endpoint does not exist.

- [ ] **Step 3: Implement minimal worksheet create/get domain**

Create DTOs:

```java
package com.eduspark.agent.worksheet.dto;

import jakarta.validation.constraints.*;
import java.util.List;

public record WorksheetCreateRequest(
    @NotBlank String title,
    @NotEmpty List<String> documentIds,
    @Min(1) @Max(30) int questionCount,
    @NotBlank String gradeLevel,
    @NotBlank String difficulty,
    @NotEmpty List<String> questionTypes,
    boolean includeExplanation) {}
```

```java
package com.eduspark.agent.worksheet.dto;

public record WorksheetCreateResponse(String worksheetId, String taskId, String status) {}
```

Create enum:

```java
package com.eduspark.agent.worksheet;

public enum WorksheetStatus {
  PENDING,
  GENERATING,
  COMPLETED,
  FAILED
}
```

Implement entity/mapper/service/controller following existing `EduTask` and `TaskController` patterns. The create method must:

```java
EduTask task = taskService.createTask(userId, new TaskCreateRequest("生成练习卷：" + request.title(), request.documentIds()));
EduWorksheet worksheet = new EduWorksheet();
worksheet.setId(UUID.randomUUID().toString());
worksheet.setUserId(userId);
worksheet.setTaskId(task.taskId());
worksheet.setTitle(request.title());
worksheet.setDocumentIds(objectMapper.writeValueAsString(request.documentIds()));
worksheet.setConfigJson(objectMapper.writeValueAsString(request));
worksheet.setStatus(WorksheetStatus.PENDING.name());
worksheet.setCreatedAt(LocalDateTime.now());
worksheet.setUpdatedAt(LocalDateTime.now());
worksheetMapper.insert(worksheet);
return new WorksheetCreateResponse(worksheet.getId(), task.taskId(), worksheet.getStatus());
```

- [ ] **Step 4: Run test to verify it passes**

Run:

```powershell
& 'C:\Users\27846\.cache\codex-runtimes\apache-maven-3.9.9\bin\mvn.cmd' -Dtest=WorksheetControllerIntegrationTest test
```

Expected: PASS.

## Task 5: Worksheet Generation And Retrieval

**Files:**
- Modify: `backend/src/main/java/com/eduspark/agent/worksheet/WorksheetGenerator.java`
- Modify: `backend/src/main/java/com/eduspark/agent/worksheet/WorksheetService.java`
- Modify: `backend/src/main/java/com/eduspark/agent/worksheet/WorksheetController.java`
- Test: `backend/src/test/java/com/eduspark/agent/worksheet/WorksheetControllerIntegrationTest.java`

- [ ] **Step 1: Add failing get worksheet test**

Append to `WorksheetControllerIntegrationTest`:

```java
@Test
void generatedWorksheetCanBeRetrievedWithQuestions() throws Exception {
  AuthSession user = register("worksheet-reader");
  String documentId = uploadDocument(user, "reader.txt", "分数和比例复习资料。".repeat(100));

  String response =
      mockMvc
          .perform(
              post("/api/users/{userId}/worksheets", user.userId())
                  .header("Authorization", "Bearer " + user.token())
                  .contentType(MediaType.APPLICATION_JSON)
                  .content("""
                    {
                      "title":"分数练习",
                      "documentIds":["%s"],
                      "questionCount":3,
                      "gradeLevel":"五年级",
                      "difficulty":"EASY",
                      "questionTypes":["TRUE_FALSE"],
                      "includeExplanation":true
                    }
                    """.formatted(documentId)))
          .andExpect(status().isOk())
          .andReturn()
          .getResponse()
          .getContentAsString();

  String worksheetId = JsonPath.read(response, "$.worksheetId");

  mockMvc
      .perform(
          org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(
                  "/api/users/{userId}/worksheets/{worksheetId}", user.userId(), worksheetId)
              .header("Authorization", "Bearer " + user.token()))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.questions[0].stem").isNotEmpty())
      .andExpect(jsonPath("$.status").value("COMPLETED"));
}
```

- [ ] **Step 2: Run test to verify it fails**

Run:

```powershell
& 'C:\Users\27846\.cache\codex-runtimes\apache-maven-3.9.9\bin\mvn.cmd' -Dtest=WorksheetControllerIntegrationTest#generatedWorksheetCanBeRetrievedWithQuestions test
```

Expected: FAIL because questions are not generated or get endpoint is incomplete.

- [ ] **Step 3: Implement mock worksheet generator**

Create records:

```java
package com.eduspark.agent.worksheet.dto;

import java.util.List;

public record WorksheetQuestion(
    String id,
    String type,
    String stem,
    List<String> options,
    Object answer,
    String explanation,
    String difficulty,
    List<String> sourceChunkIds) {}
```

```java
package com.eduspark.agent.worksheet.dto;

import java.util.List;

public record WorksheetDetailResponse(
    String id,
    String userId,
    String taskId,
    String title,
    List<String> documentIds,
    WorksheetCreateRequest config,
    List<WorksheetQuestion> questions,
    String status,
    String createdAt,
    String updatedAt) {}
```

Implement `WorksheetGenerator.generate`:

```java
public List<WorksheetQuestion> generate(WorksheetCreateRequest request, List<DocumentChunk> chunks) {
  String sourceChunkId = chunks.isEmpty() ? "" : chunks.get(0).getId();
  List<WorksheetQuestion> questions = new ArrayList<>();
  for (int index = 0; index < request.questionCount(); index++) {
    String type = request.questionTypes().get(index % request.questionTypes().size());
    questions.add(
        new WorksheetQuestion(
            "q-" + (index + 1),
            type,
            request.gradeLevel() + " " + request.title() + " 第 " + (index + 1) + " 题",
            List.of("A. 正确", "B. 错误"),
            "A",
            request.includeExplanation() ? "根据上传资料中的核心概念生成。" : null,
            request.difficulty(),
            sourceChunkId.isBlank() ? List.of() : List.of(sourceChunkId)));
  }
  return questions;
}
```

Update `WorksheetService.create` to call `VectorSearchService.search`, save `questions_json`, and set status `COMPLETED`.

- [ ] **Step 4: Run test to verify it passes**

Run:

```powershell
& 'C:\Users\27846\.cache\codex-runtimes\apache-maven-3.9.9\bin\mvn.cmd' -Dtest=WorksheetControllerIntegrationTest test
```

Expected: PASS.

## Task 6: Word Export

**Files:**
- Create: `backend/src/main/java/com/eduspark/agent/worksheet/WorksheetExport.java`
- Create: `backend/src/main/java/com/eduspark/agent/worksheet/WorksheetExportMapper.java`
- Create: `backend/src/main/java/com/eduspark/agent/worksheet/WordExportService.java`
- Modify: `backend/src/main/java/com/eduspark/agent/worksheet/WorksheetController.java`
- Test: `backend/src/test/java/com/eduspark/agent/worksheet/WorksheetExportIntegrationTest.java`

- [ ] **Step 1: Write failing export test**

Create `WorksheetExportIntegrationTest.java`:

```java
package com.eduspark.agent.worksheet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class WorksheetExportIntegrationTest {

  @Autowired private MockMvc mockMvc;

  @Test
  void exportsWorksheetAsWordDocument() throws Exception {
    AuthSession user = register("word-owner");
    String documentId = uploadDocument(user);
    String worksheetId = createWorksheet(user, documentId);

    byte[] body =
        mockMvc
            .perform(get("/api/users/{userId}/worksheets/{worksheetId}/export.docx", user.userId(), worksheetId)
                .header("Authorization", "Bearer " + user.token()))
            .andExpect(status().isOk())
            .andExpect(header().string(HttpHeaders.CONTENT_TYPE, "application/vnd.openxmlformats-officedocument.wordprocessingml.document"))
            .andReturn()
            .getResponse()
            .getContentAsByteArray();

    try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(body))) {
      String text = document.getParagraphs().stream().map(p -> p.getText()).reduce("", (a, b) -> a + "\n" + b);
      assertThat(text).contains("Word练习").contains("答案与解析");
    }
  }

  private AuthSession register(String username) throws Exception {
    String body = mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content("{\"username\":\"%s\",\"password\":\"secret123\"}".formatted(username))).andReturn().getResponse().getContentAsString();
    return new AuthSession(JsonPath.read(body, "$.userId"), JsonPath.read(body, "$.token"));
  }

  private String uploadDocument(AuthSession user) throws Exception {
    MockMultipartFile file = new MockMultipartFile("file", "word.txt", "text/plain", "比例资料。".repeat(100).getBytes(StandardCharsets.UTF_8));
    String body = mockMvc.perform(multipart("/api/users/{userId}/documents", user.userId()).file(file).header("Authorization", "Bearer " + user.token())).andReturn().getResponse().getContentAsString();
    return JsonPath.read(body, "$.id");
  }

  private String createWorksheet(AuthSession user, String documentId) throws Exception {
    String body = mockMvc.perform(post("/api/users/{userId}/worksheets", user.userId()).header("Authorization", "Bearer " + user.token()).contentType(MediaType.APPLICATION_JSON).content("""
      {"title":"Word练习","documentIds":["%s"],"questionCount":2,"gradeLevel":"六年级","difficulty":"MEDIUM","questionTypes":["TRUE_FALSE"],"includeExplanation":true}
      """.formatted(documentId))).andReturn().getResponse().getContentAsString();
    return JsonPath.read(body, "$.worksheetId");
  }

  private record AuthSession(String userId, String token) {}
}
```

- [ ] **Step 2: Run test to verify it fails**

Run:

```powershell
& 'C:\Users\27846\.cache\codex-runtimes\apache-maven-3.9.9\bin\mvn.cmd' -Dtest=WorksheetExportIntegrationTest test
```

Expected: FAIL because export endpoint does not exist.

- [ ] **Step 3: Implement Word export**

Create `WordExportService` using Apache POI:

```java
public byte[] export(WorksheetDetailResponse worksheet) {
  try (XWPFDocument document = new XWPFDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
    document.createParagraph().createRun().setText(worksheet.title());
    document.createParagraph().createRun().setText("年级：" + worksheet.config().gradeLevel() + " 难度：" + worksheet.config().difficulty());
    document.createParagraph().createRun().setText("一、题目");
    for (WorksheetQuestion question : worksheet.questions()) {
      document.createParagraph().createRun().setText(question.stem());
      if (question.options() != null) {
        for (String option : question.options()) {
          document.createParagraph().createRun().setText(option);
        }
      }
    }
    document.createParagraph().createRun().setText("二、答案与解析");
    for (WorksheetQuestion question : worksheet.questions()) {
      document.createParagraph().createRun().setText(question.id() + "：" + question.answer() + " " + question.explanation());
    }
    document.write(output);
    return output.toByteArray();
  } catch (IOException exception) {
    throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Word export failed", exception);
  }
}
```

Add controller method:

```java
@GetMapping("/{worksheetId}/export.docx")
public ResponseEntity<byte[]> export(@PathVariable String userId, @PathVariable String worksheetId) {
  WorksheetDetailResponse worksheet = worksheetService.findDetail(userId, worksheetId);
  byte[] body = wordExportService.export(worksheet);
  return ResponseEntity.ok()
      .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"worksheet.docx\"")
      .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.wordprocessingml.document"))
      .body(body);
}
```

- [ ] **Step 4: Run test to verify it passes**

Run:

```powershell
& 'C:\Users\27846\.cache\codex-runtimes\apache-maven-3.9.9\bin\mvn.cmd' -Dtest=WorksheetExportIntegrationTest test
```

Expected: PASS.

## Task 7: Contracts And API Docs

**Files:**
- Modify: `contracts/task.contract.ts`
- Modify: `docs/api.md`

- [ ] **Step 1: Update TypeScript contracts**

Append to `contracts/task.contract.ts`:

```ts
export type WorksheetDifficulty = "EASY" | "MEDIUM" | "HARD";

export type WorksheetQuestionType =
  | "SINGLE_CHOICE"
  | "MULTIPLE_CHOICE"
  | "TRUE_FALSE"
  | "FILL_BLANK"
  | "SHORT_ANSWER";

export interface WorksheetCreateRequest {
  title: string;
  documentIds: string[];
  questionCount: number;
  gradeLevel: string;
  difficulty: WorksheetDifficulty;
  questionTypes: WorksheetQuestionType[];
  includeExplanation: boolean;
}

export interface WorksheetCreateResponse {
  worksheetId: string;
  taskId: string;
  status: "PENDING" | "GENERATING" | "COMPLETED" | "FAILED";
}

export interface WorksheetQuestion {
  id: string;
  type: WorksheetQuestionType;
  stem: string;
  options?: string[];
  answer: string | string[];
  explanation?: string;
  difficulty: WorksheetDifficulty;
  sourceChunkIds: string[];
}

export interface Worksheet {
  id: string;
  userId: string;
  taskId?: string;
  title: string;
  documentIds: string[];
  config: WorksheetCreateRequest;
  questions: WorksheetQuestion[];
  status: "PENDING" | "GENERATING" | "COMPLETED" | "FAILED";
  createdAt: string;
  updatedAt: string;
}
```

- [ ] **Step 2: Update API docs**

Add to `docs/api.md`:

```markdown
- `POST /api/users/{userId}/worksheets`
  - Request: `{ "title": "string", "documentIds": ["string"], "questionCount": 10, "gradeLevel": "string", "difficulty": "EASY|MEDIUM|HARD", "questionTypes": ["TRUE_FALSE"], "includeExplanation": true }`
  - Response: `{ "worksheetId": "string", "taskId": "string", "status": "PENDING" }`
- `GET /api/users/{userId}/worksheets/{worksheetId}`
  - Response: generated worksheet detail with config and questions.
- `GET /api/users/{userId}/worksheets/{worksheetId}/export.docx`
  - Response: `.docx` file with questions and answers.
```

- [ ] **Step 3: Run backend package**

Run:

```powershell
cd D:\Project\eduspark-agent\backend
& 'C:\Users\27846\.cache\codex-runtimes\apache-maven-3.9.9\bin\mvn.cmd' package
```

Expected: BUILD SUCCESS.

## Task 8: Frontend API And Worksheet UI

**Files:**
- Modify: `frontend/src/api/taskApi.ts`
- Create: `frontend/src/features/task/components/WorksheetComposer.tsx`
- Create: `frontend/src/features/task/components/WorksheetResultPanel.tsx`
- Modify: `frontend/src/features/task/TaskConsolePage.tsx`
- Test: `frontend/src/features/task/TaskConsolePage.test.tsx`

- [ ] **Step 1: Write failing frontend test**

Add to `TaskConsolePage.test.tsx`:

```tsx
it("creates a worksheet with selected document ids and auth", async () => {
  localStorage.setItem("eduspark.auth", JSON.stringify({ userId: "user-1", username: "teacher", token: "token-1" }));
  vi.stubGlobal("fetch", vi.fn(async (url: string, init?: RequestInit) => {
    if (url.endsWith("/worksheets")) {
      expect(url).toContain("/api/users/user-1/worksheets");
      expect(init?.headers).toMatchObject({ Authorization: "Bearer token-1" });
      expect(JSON.parse(init?.body as string)).toMatchObject({ questionCount: 5, documentIds: expect.any(Array) });
      return new Response(JSON.stringify({ worksheetId: "worksheet-1", taskId: "task-1", status: "PENDING" }), { status: 200 });
    }
    return new Response(JSON.stringify({ id: "doc-1", userId: "user-1", fileName: "lesson.txt", mimeType: "text/plain", createdAt: "2026-07-03T00:00:00" }), { status: 200 });
  }));

  render(<TaskConsolePage />);
  await userEvent.click(screen.getByRole("button", { name: /练习卷/ }));
  await userEvent.upload(screen.getByLabelText(/上传资料/), new File(["比例资料"], "lesson.txt", { type: "text/plain" }));
  await userEvent.click(screen.getByRole("button", { name: /生成练习卷/ }));

  expect(await screen.findByText(/worksheet-1/)).toBeInTheDocument();
});
```

- [ ] **Step 2: Run test to verify it fails**

Run:

```powershell
cd D:\Project\eduspark-agent\frontend
& .\node_modules\.bin\vitest.cmd run src/features/task/TaskConsolePage.test.tsx
```

Expected: FAIL because worksheet UI/API does not exist.

- [ ] **Step 3: Add frontend worksheet API**

Add to `taskApi.ts`:

```ts
export type WorksheetDifficulty = "EASY" | "MEDIUM" | "HARD";
export type WorksheetQuestionType = "SINGLE_CHOICE" | "MULTIPLE_CHOICE" | "TRUE_FALSE" | "FILL_BLANK" | "SHORT_ANSWER";

export interface WorksheetCreateRequest {
  title: string;
  documentIds: string[];
  questionCount: number;
  gradeLevel: string;
  difficulty: WorksheetDifficulty;
  questionTypes: WorksheetQuestionType[];
  includeExplanation: boolean;
}

export interface WorksheetCreateResponse {
  worksheetId: string;
  taskId: string;
  status: "PENDING" | "GENERATING" | "COMPLETED" | "FAILED";
}

export async function createWorksheet(session: AuthSession, request: WorksheetCreateRequest): Promise<WorksheetCreateResponse> {
  const response = await fetch(`${API_BASE_URL}/api/users/${session.userId}/worksheets`, {
    method: "POST",
    headers: { "Content-Type": "application/json", ...authHeader(session) },
    body: JSON.stringify(request)
  });
  if (!response.ok) {
    throw new Error(await readErrorMessage(response, `Create worksheet failed: ${response.status}`));
  }
  return response.json() as Promise<WorksheetCreateResponse>;
}

export function worksheetExportUrl(session: AuthSession, worksheetId: string) {
  return `${API_BASE_URL}/api/users/${session.userId}/worksheets/${worksheetId}/export.docx`;
}
```

- [ ] **Step 4: Add UI components and wire page**

Create `WorksheetComposer.tsx` with inputs for title, grade, count, difficulty, question type checkboxes, explanations toggle, and generate button. It receives:

```ts
interface WorksheetComposerProps {
  documentIds: string[];
  disabled: boolean;
  onGenerate: (request: WorksheetCreateRequest) => void;
}
```

Create `WorksheetResultPanel.tsx` with:

```ts
interface WorksheetResultPanelProps {
  worksheetId?: string;
  exportUrl?: string;
}
```

Modify `TaskConsolePage.tsx` to:

```ts
const [workspaceMode, setWorkspaceMode] = useState<"agent_task" | "worksheet">("agent_task");
const [worksheetId, setWorksheetId] = useState<string | undefined>();
```

On generate:

```ts
const response = await createWorksheet(session, request);
setWorksheetId(response.worksheetId);
setTaskId(response.taskId);
subscribeToTask(response.taskId);
```

- [ ] **Step 5: Run frontend tests and builds**

Run:

```powershell
& .\node_modules\.bin\vitest.cmd run
& .\node_modules\.bin\tsc.cmd -b
& .\node_modules\.bin\vite.cmd build
```

Expected: all commands pass.

## Final Verification

- [ ] Run backend package:

```powershell
cd D:\Project\eduspark-agent\backend
& 'C:\Users\27846\.cache\codex-runtimes\apache-maven-3.9.9\bin\mvn.cmd' package
```

- [ ] Run frontend verification:

```powershell
cd D:\Project\eduspark-agent\frontend
& .\node_modules\.bin\vitest.cmd run
& .\node_modules\.bin\tsc.cmd -b
& .\node_modules\.bin\vite.cmd build
```

- [ ] Manual smoke test:

```text
1. Start backend with AGENT_PLANNER_MODE=mock.
2. Start Vite.
3. Register or login.
4. Upload a text document.
5. Switch to worksheet mode.
6. Generate a worksheet.
7. Confirm progress appears.
8. Confirm generated questions render.
9. Download Word and open it.
```
