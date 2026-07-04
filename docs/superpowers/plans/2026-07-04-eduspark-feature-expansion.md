# EduSpark Feature Expansion Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Expand EduSpark from a worksheet generator into a complete learning workflow with document management, practice/grading, history replay, and knowledge graphs.

**Architecture:** Keep the existing React + Spring Boot + MyBatis-Plus + MySQL architecture. Every new backend API remains user-scoped under `/api/users/{userId}/...`, and every frontend API call must include the existing JWT session.

**Tech Stack:** Spring Boot 3.4, MyBatis-Plus, Flyway, React, TypeScript, Tailwind, lucide-react, Vitest.

---

## File Structure Map

### Backend

- `backend/src/main/resources/db/migration/`
  - Add Flyway migrations for document metadata, worksheet attempts, history deletion support, and knowledge graph tables.
- `backend/src/main/java/com/eduspark/agent/document/`
  - Extend document list/delete/reindex APIs and document parse/index status.
- `backend/src/main/java/com/eduspark/agent/worksheet/`
  - Add practice/explanation workflow, attempts, grading, retry mistakes.
- `backend/src/main/java/com/eduspark/agent/history/`
  - New package for user-scoped history aggregation DTOs/services if cross-domain listing becomes messy.
- `backend/src/main/java/com/eduspark/agent/graph/`
  - New knowledge graph model, mapper, service, controller, generator/parser.
- `contracts/task.contract.ts`
  - Source contract for frontend/backend DTO alignment.
- `docs/api.md`
  - API contract documentation.

### Frontend

- `frontend/src/api/taskApi.ts`
  - Add typed API functions for documents, attempts, history, knowledge graphs.
- `frontend/src/features/task/components/DocumentCenterPanel.tsx`
  - New document management panel.
- `frontend/src/features/task/components/WorksheetTabs.tsx`
  - Practice/explanation tabs.
- `frontend/src/features/task/components/PracticeTab.tsx`
  - Student answer UI.
- `frontend/src/features/task/components/AttemptResultPanel.tsx`
  - Grading result display.
- `frontend/src/features/task/components/HistoryPanel.tsx`
  - Lightweight task/document/worksheet history.
- `frontend/src/features/task/components/KnowledgeGraphPanel.tsx`
  - Graph list and creation entry.
- `frontend/src/features/task/components/KnowledgeGraphCanvas.tsx`
  - Interactive SVG graph rendering.
- `frontend/src/features/task/TaskConsolePage.tsx`
  - Compose new panels without changing auth/session behavior.

---

## Phase 1: Document Center

### Task 1.1: Persist Document Parse/Index Status

**Files:**
- Create: `backend/src/main/resources/db/migration/V7__add_document_status_fields.sql`
- Modify: `backend/src/main/java/com/eduspark/agent/document/EduDocument.java`
- Modify: `backend/src/main/java/com/eduspark/agent/document/DocumentService.java`
- Test: `backend/src/test/java/com/eduspark/agent/document/DocumentServiceIntegrationTest.java`

- [ ] **Step 1: Add failing schema/status test**

Add assertions that uploaded documents persist:

```java
assertThat(persisted.getParseStatus()).isEqualTo("PARSED");
assertThat(persisted.getTextPreview()).contains("上传成功");
```

- [ ] **Step 2: Run test to verify failure**

Run:

```powershell
C:\Users\27846\.cache\codex-runtimes\apache-maven-3.9.9\bin\mvn.cmd '-Dtest=DocumentServiceIntegrationTest' test
```

Expected: compile or assertion failure because fields do not exist.

- [ ] **Step 3: Add migration**

Create:

```sql
alter table edu_document
  add column parse_status varchar(32) not null default 'PARSED',
  add column parse_error text null,
  add column indexed_at timestamp null,
  add column text_preview text null;

create index idx_edu_document_user_created on edu_document(user_id, created_at);
```

- [ ] **Step 4: Add fields to `EduDocument`**

Add `parseStatus`, `parseError`, `indexedAt`, `textPreview` with getters/setters.

- [ ] **Step 5: Set fields in `DocumentService.upload`**

After extraction:

```java
String extractedText = extractText(file);
document.setExtractedText(extractedText);
document.setTextPreview(extractedText.length() <= 500 ? extractedText : extractedText.substring(0, 500));
document.setParseStatus("PARSED");
```

On indexing success set `indexedAt`; on indexing failure keep upload successful and set `parseStatus` to `INDEX_FAILED`, `parseError` to exception message.

- [ ] **Step 6: Verify**

Run the same test. Expected: PASS.

### Task 1.2: Document List/Delete/Reindex API

**Files:**
- Modify: `backend/src/main/java/com/eduspark/agent/document/DocumentMapper.java`
- Modify: `backend/src/main/java/com/eduspark/agent/document/DocumentService.java`
- Modify: `backend/src/main/java/com/eduspark/agent/document/DocumentController.java`
- Test: `backend/src/test/java/com/eduspark/agent/document/DocumentControllerIntegrationTest.java`

- [ ] **Step 1: Add failing integration tests**

Cover:

- `GET /api/users/{userId}/documents` returns only current user's documents.
- `DELETE /api/users/{userId}/documents/{documentId}` removes only current user's document.
- `POST /api/users/{userId}/documents/{documentId}/reindex` updates index status.

- [ ] **Step 2: Add mapper methods**

Use MyBatis-Plus wrappers or mapper methods:

```java
List<EduDocument> selectByUserIdOrderByCreatedAtDesc(String userId);
int deleteByIdAndUserId(String documentId, String userId);
```

- [ ] **Step 3: Add service methods**

```java
public List<EduDocument> listDocuments(String userId)
public void deleteDocument(String userId, String documentId)
public EduDocument reindex(String userId, String documentId)
```

- [ ] **Step 4: Add controller endpoints**

```java
@GetMapping("/api/users/{userId}/documents")
@DeleteMapping("/api/users/{userId}/documents/{documentId}")
@PostMapping("/api/users/{userId}/documents/{documentId}/reindex")
```

- [ ] **Step 5: Verify**

Run:

```powershell
C:\Users\27846\.cache\codex-runtimes\apache-maven-3.9.9\bin\mvn.cmd '-Dtest=DocumentControllerIntegrationTest' test
```

Expected: PASS.

### Task 1.3: Frontend Document Center Panel

**Files:**
- Modify: `frontend/src/api/taskApi.ts`
- Create: `frontend/src/features/task/components/DocumentCenterPanel.tsx`
- Modify: `frontend/src/features/task/TaskConsolePage.tsx`
- Test: `frontend/src/features/task/TaskConsolePage.test.tsx`

- [ ] **Step 1: Add API types/functions**

Add:

```ts
export async function listDocuments(session: AuthSession): Promise<EduDocument[]>
export async function deleteDocument(session: AuthSession, documentId: string): Promise<void>
export async function reindexDocument(session: AuthSession, documentId: string): Promise<EduDocument>
```

- [ ] **Step 2: Add failing frontend test**

Assert that after login document center shows uploaded document preview and can delete a document.

- [ ] **Step 3: Build `DocumentCenterPanel`**

Props:

```ts
interface DocumentCenterPanelProps {
  documents: EduDocument[];
  onDelete: (documentId: string) => void;
  onReindex: (documentId: string) => void;
}
```

Render file name, status, preview, delete/reindex buttons.

- [ ] **Step 4: Wire into `TaskConsolePage`**

Load documents after login and after upload.

- [ ] **Step 5: Verify**

Run:

```powershell
D:\Project\eduspark-agent\frontend\node_modules\.bin\vitest.cmd run
D:\Project\eduspark-agent\frontend\node_modules\.bin\tsc.cmd -b
```

Expected: PASS.

---

## Phase 2: Practice and Grading Loop

### Task 2.1: Worksheet Attempt Persistence

**Files:**
- Create: `backend/src/main/resources/db/migration/V8__add_worksheet_attempts.sql`
- Create: `backend/src/main/java/com/eduspark/agent/worksheet/attempt/EduWorksheetAttempt.java`
- Create: `backend/src/main/java/com/eduspark/agent/worksheet/attempt/WorksheetAttemptMapper.java`
- Create: `backend/src/main/java/com/eduspark/agent/worksheet/attempt/dto/WorksheetAttemptRequest.java`
- Create: `backend/src/main/java/com/eduspark/agent/worksheet/attempt/dto/WorksheetAttemptResponse.java`
- Test: `backend/src/test/java/com/eduspark/agent/worksheet/WorksheetAttemptIntegrationTest.java`

- [ ] **Step 1: Write failing migration/integration test**

Test creates worksheet, posts answers, expects attempt id and score.

- [ ] **Step 2: Add migration**

```sql
create table edu_worksheet_attempt (
  id varchar(64) primary key,
  user_id varchar(64) not null,
  worksheet_id varchar(64) not null,
  answers_json longtext not null,
  grading_result_json longtext not null,
  score decimal(5,2) not null,
  created_at timestamp not null,
  index idx_attempt_user_worksheet (user_id, worksheet_id),
  index idx_attempt_user_created (user_id, created_at)
);
```

- [ ] **Step 3: Add DTOs**

Request:

```java
public record WorksheetAttemptRequest(List<WorksheetAnswerInput> answers) {}
public record WorksheetAnswerInput(String questionId, String answer) {}
```

Response includes `attemptId`, `score`, `items`, `weaknessSummary`, `remediationSuggestion`.

- [ ] **Step 4: Verify migration compiles**

Run backend compile.

### Task 2.2: Deterministic Grading Service

**Files:**
- Create: `backend/src/main/java/com/eduspark/agent/worksheet/attempt/AnswerGradingService.java`
- Create: `backend/src/main/java/com/eduspark/agent/worksheet/attempt/DeterministicAnswerGradingService.java`
- Test: `backend/src/test/java/com/eduspark/agent/worksheet/AnswerGradingServiceTest.java`

- [ ] **Step 1: Add grading test**

Choice and true/false exact answers are correct; short answer is correct when normalized text equals expected answer.

- [ ] **Step 2: Implement service**

Normalize answers with trim and case-insensitive comparison.

- [ ] **Step 3: Verify**

Run:

```powershell
C:\Users\27846\.cache\codex-runtimes\apache-maven-3.9.9\bin\mvn.cmd '-Dtest=AnswerGradingServiceTest' test
```

Expected: PASS.

### Task 2.3: Attempt Controller API

**Files:**
- Create: `backend/src/main/java/com/eduspark/agent/worksheet/attempt/WorksheetAttemptService.java`
- Create: `backend/src/main/java/com/eduspark/agent/worksheet/attempt/WorksheetAttemptController.java`
- Test: `backend/src/test/java/com/eduspark/agent/worksheet/WorksheetAttemptIntegrationTest.java`

- [ ] **Step 1: Add endpoints**

```java
POST /api/users/{userId}/worksheets/{worksheetId}/attempts
GET /api/users/{userId}/worksheets/{worksheetId}/attempts
```

- [ ] **Step 2: Enforce user scope**

Load worksheet by `worksheetId + userId`, return `404` when not visible.

- [ ] **Step 3: Persist attempt**

Save request answers and grading result JSON.

- [ ] **Step 4: Verify**

Run attempt integration tests. Expected: PASS.

### Task 2.4: Frontend Practice/Explanation Tabs

**Files:**
- Modify: `frontend/src/api/taskApi.ts`
- Create: `frontend/src/features/task/components/WorksheetTabs.tsx`
- Create: `frontend/src/features/task/components/PracticeTab.tsx`
- Create: `frontend/src/features/task/components/AttemptResultPanel.tsx`
- Modify: `frontend/src/features/task/components/WorksheetPanel.tsx`
- Test: `frontend/src/features/task/TaskConsolePage.test.tsx`

- [ ] **Step 1: Add frontend contracts**

Add `WorksheetAttemptRequest`, `WorksheetAttemptResponse`, `submitWorksheetAttempt`.

- [ ] **Step 2: Add failing tests**

Assert practice tab hides answers, explanation tab shows answers, submit displays score.

- [ ] **Step 3: Implement tabs**

Use internal selected tab state: `"practice" | "explanation"`.

- [ ] **Step 4: Verify**

Run frontend tests, tsc, build.

---

## Phase 3: History Center and Replay

### Task 3.1: List/Delete Task and Worksheet APIs

**Files:**
- Modify: `backend/src/main/java/com/eduspark/agent/task/TaskController.java`
- Modify: `backend/src/main/java/com/eduspark/agent/task/TaskService.java`
- Modify: `backend/src/main/java/com/eduspark/agent/worksheet/WorksheetController.java`
- Modify: `backend/src/main/java/com/eduspark/agent/worksheet/WorksheetService.java`
- Test: `backend/src/test/java/com/eduspark/agent/auth/UserScopedApiIntegrationTest.java`

- [ ] **Step 1: Add tests**

User A cannot list/delete User B resources.

- [ ] **Step 2: Add task list/delete**

```http
GET /api/users/{userId}/tasks
DELETE /api/users/{userId}/tasks/{taskId}
```

- [ ] **Step 3: Add worksheet list/delete**

```http
GET /api/users/{userId}/worksheets
DELETE /api/users/{userId}/worksheets/{worksheetId}
```

- [ ] **Step 4: Verify**

Run user-scoped tests.

### Task 3.2: Frontend History Panel

**Files:**
- Modify: `frontend/src/api/taskApi.ts`
- Create: `frontend/src/features/task/components/HistoryPanel.tsx`
- Modify: `frontend/src/features/task/components/WorkspaceShell.tsx`
- Modify: `frontend/src/features/task/TaskConsolePage.tsx`
- Test: `frontend/src/features/task/TaskConsolePage.test.tsx`

- [ ] **Step 1: Add API functions**

`listTasks`, `deleteTask`, `listWorksheets`, `deleteWorksheet`.

- [ ] **Step 2: Add History panel test**

History displays task and worksheet rows and delete buttons.

- [ ] **Step 3: Implement navigation**

Workspace tabs: Console, Documents, History, Knowledge Graph.

- [ ] **Step 4: Verify**

Run frontend tests and build.

---

## Phase 4: Knowledge Graph

### Task 4.1: Graph Schema and Contracts

**Files:**
- Create: `backend/src/main/resources/db/migration/V9__add_knowledge_graphs.sql`
- Create: `backend/src/main/java/com/eduspark/agent/graph/EduKnowledgeGraph.java`
- Create: `backend/src/main/java/com/eduspark/agent/graph/KnowledgeGraphMapper.java`
- Create: `backend/src/main/java/com/eduspark/agent/graph/dto/KnowledgeGraphDtos.java`
- Modify: `contracts/task.contract.ts`
- Modify: `docs/api.md`
- Test: `backend/src/test/java/com/eduspark/agent/graph/KnowledgeGraphSchemaIntegrationTest.java`

- [ ] **Step 1: Add migration**

```sql
create table edu_knowledge_graph (
  id varchar(64) primary key,
  user_id varchar(64) not null,
  title varchar(255) not null,
  document_ids_json longtext not null,
  graph_json longtext null,
  status varchar(32) not null,
  created_at timestamp not null,
  updated_at timestamp not null,
  index idx_graph_user_created (user_id, created_at)
);
```

- [ ] **Step 2: Add DTO contract**

`KnowledgeGraphNode`, `KnowledgeGraphEdge`, `KnowledgeGraphDetailResponse`.

- [ ] **Step 3: Verify**

Run schema test and compile.

### Task 4.2: Graph Generator and API

**Files:**
- Create: `backend/src/main/java/com/eduspark/agent/graph/KnowledgeGraphGenerator.java`
- Create: `backend/src/main/java/com/eduspark/agent/graph/MockKnowledgeGraphGenerator.java`
- Create: `backend/src/main/java/com/eduspark/agent/graph/KnowledgeGraphService.java`
- Create: `backend/src/main/java/com/eduspark/agent/graph/KnowledgeGraphController.java`
- Test: `backend/src/test/java/com/eduspark/agent/graph/KnowledgeGraphControllerIntegrationTest.java`

- [ ] **Step 1: Add integration tests**

Create graph from selected document, list graph, get graph detail, delete graph.

- [ ] **Step 2: Implement mock generator**

Extract first 3 chunks as concept nodes and connect them sequentially.

- [ ] **Step 3: Implement service/controller**

Endpoints:

```http
POST /api/users/{userId}/knowledge-graphs
GET /api/users/{userId}/knowledge-graphs
GET /api/users/{userId}/knowledge-graphs/{graphId}
DELETE /api/users/{userId}/knowledge-graphs/{graphId}
```

- [ ] **Step 4: Verify**

Run graph tests.

### Task 4.3: Frontend Graph View

**Files:**
- Modify: `frontend/src/api/taskApi.ts`
- Create: `frontend/src/features/task/components/KnowledgeGraphPanel.tsx`
- Create: `frontend/src/features/task/components/KnowledgeGraphCanvas.tsx`
- Test: `frontend/src/features/task/TaskConsolePage.test.tsx`

- [ ] **Step 1: Add frontend graph API**

`createKnowledgeGraph`, `listKnowledgeGraphs`, `getKnowledgeGraph`, `deleteKnowledgeGraph`.

- [ ] **Step 2: Add render test**

Assert graph nodes render as buttons/text and clicking node shows detail.

- [ ] **Step 3: Implement SVG canvas**

Use SVG circles/lines/text. Support click-to-detail first; drag/zoom can follow after stable render.

- [ ] **Step 4: Verify**

Run frontend tests and build.

---

## Phase 5: Final Polish and Verification

### Task 5.1: Empty/Error/Loading States

**Files:**
- Modify frontend component files touched in Phases 1-4.

- [ ] **Step 1: Audit all panels**

Ensure every panel has:

- Empty state
- Loading state
- Error state
- Disabled state for in-flight actions

- [ ] **Step 2: Run frontend verification**

```powershell
D:\Project\eduspark-agent\frontend\node_modules\.bin\vitest.cmd run
D:\Project\eduspark-agent\frontend\node_modules\.bin\tsc.cmd -b
D:\Project\eduspark-agent\frontend\node_modules\.bin\vite.cmd build
```

Expected: all pass.

### Task 5.2: Full Backend Verification

**Files:** all backend changes.

- [ ] **Step 1: Run full package**

```powershell
C:\Users\27846\.cache\codex-runtimes\apache-maven-3.9.9\bin\mvn.cmd package
```

Expected: BUILD SUCCESS.

- [ ] **Step 2: Manual smoke test**

In browser:

1. Register/login.
2. Upload readable TXT/PDF.
3. Preview document.
4. Generate worksheet.
5. Practice and submit answers.
6. View grading result.
7. Open history and reopen worksheet.
8. Generate knowledge graph and click node.

Expected: no 401/403/500; UI remains responsive.

---

## Self-Review

Spec coverage:

- Document center: Phase 1.
- Practice/grading loop: Phase 2.
- Agent/history replay: Phase 3.
- Knowledge graph: Phase 4.
- Polish/performance-lite: Phase 5.
- Production Docker, OCR, and chunked upload are explicitly out of scope.

Placeholder scan:

- No TODO/TBD placeholders.

Type consistency:

- All new APIs remain under `/api/users/{userId}/...`.
- Frontend API additions are centralized in `frontend/src/api/taskApi.ts`.
- Backend additions follow existing controller/service/mapper package style.

