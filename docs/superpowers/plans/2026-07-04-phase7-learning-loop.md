# Phase 7 Learning Loop Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Strengthen the learning loop: answer questions, review explanations, collect wrong questions, generate retry practice, and replay task logs clearly.

**Architecture:** Keep the current worksheet and task modules. Add small, user-scoped attempt/wrong-question APIs instead of replacing the worksheet generator. The frontend should extend `WorksheetPanel`, `TaskTimeline`, and history flow without a full UI rewrite, because Phase 6 will handle global visual polish.

**Tech Stack:** Spring Boot, MyBatis Plus, Flyway, React, TypeScript, TailwindCSS, Vitest.

---

## File Structure

- Modify `backend/src/main/resources/db/migration/V9__add_wrong_questions.sql`: store wrong-question snapshots and retry worksheet linkage.
- Create `backend/src/main/java/com/eduspark/agent/worksheet/wrong/WrongQuestion.java`: wrong-question entity.
- Create `backend/src/main/java/com/eduspark/agent/worksheet/wrong/WrongQuestionMapper.java`: user-scoped wrong-question queries.
- Create `backend/src/main/java/com/eduspark/agent/worksheet/wrong/WrongQuestionService.java`: collect wrong items from attempts and create retry worksheet requests.
- Create `backend/src/main/java/com/eduspark/agent/worksheet/wrong/WrongQuestionController.java`: wrong-question list, delete, retry endpoints.
- Modify `backend/src/main/java/com/eduspark/agent/worksheet/attempt/WorksheetAttemptService.java`: persist wrong-question snapshots after grading.
- Modify `backend/src/main/java/com/eduspark/agent/task/TaskLogMapper.java`: add grouped log retrieval helper if needed.
- Modify `backend/src/main/java/com/eduspark/agent/task/TaskController.java`: keep `/logs`, but frontend grouping can happen client-side first.
- Modify `frontend/src/api/taskApi.ts`: add wrong-question and retry-practice contracts.
- Modify `frontend/src/features/task/components/WorksheetPanel.tsx`: add `错题` tab and retry action.
- Modify `frontend/src/features/task/components/TaskTimeline.tsx`: add collapsible grouped logs.
- Modify `frontend/src/features/task/components/HistoryPanel.tsx`: expose replay affordance more clearly.
- Modify `frontend/src/features/task/TaskConsolePage.tsx`: wire wrong-question state, retry flow, and log replay state.
- Test backend with `WorksheetAttemptIntegrationTest`, new `WrongQuestionIntegrationTest`.
- Test frontend with `TaskConsolePage.test.tsx`.

---

### Task 1: Wrong Question Schema

**Files:**
- Create: `backend/src/main/resources/db/migration/V9__add_wrong_questions.sql`

- [ ] **Step 1: Add migration**

```sql
create table edu_wrong_question (
  id varchar(64) primary key,
  user_id varchar(64) not null,
  worksheet_id varchar(64) not null,
  attempt_id varchar(64) not null,
  question_id varchar(128) not null,
  question_json longtext not null,
  submitted_answer text null,
  correct_answer text null,
  explanation text null,
  weakness_tag varchar(128) not null,
  retry_worksheet_id varchar(64) null,
  resolved tinyint not null default 0,
  created_at timestamp not null,
  updated_at timestamp not null,
  index idx_wrong_user_created (user_id, created_at),
  index idx_wrong_user_resolved (user_id, resolved),
  index idx_wrong_worksheet (user_id, worksheet_id)
);
```

- [ ] **Step 2: Verify migration compiles**

Run: `cd backend && mvn -DskipTests compile`

Expected: `BUILD SUCCESS`.

---

### Task 2: Wrong Question Backend Model

**Files:**
- Create: `backend/src/main/java/com/eduspark/agent/worksheet/wrong/WrongQuestion.java`
- Create: `backend/src/main/java/com/eduspark/agent/worksheet/wrong/WrongQuestionMapper.java`
- Create: `backend/src/main/java/com/eduspark/agent/worksheet/wrong/dto/WrongQuestionResponse.java`

- [ ] **Step 1: Create entity fields**

Use camelCase fields matching the migration: `id`, `userId`, `worksheetId`, `attemptId`, `questionId`, `questionJson`, `submittedAnswer`, `correctAnswer`, `explanation`, `weaknessTag`, `retryWorksheetId`, `resolved`, `createdAt`, `updatedAt`.

- [ ] **Step 2: Create mapper queries**

Add:

```java
List<WrongQuestion> selectOpenByUserId(String userId);
List<WrongQuestion> selectByWorksheetId(String userId, String worksheetId);
int markResolved(String userId, String wrongQuestionId, LocalDateTime updatedAt);
int attachRetryWorksheet(String userId, List<String> wrongQuestionIds, String retryWorksheetId, LocalDateTime updatedAt);
```

- [ ] **Step 3: Run compile**

Run: `cd backend && mvn -DskipTests compile`

Expected: `BUILD SUCCESS`.

---

### Task 3: Collect Wrong Questions After Grading

**Files:**
- Modify: `backend/src/main/java/com/eduspark/agent/worksheet/attempt/WorksheetAttemptService.java`
- Create: `backend/src/main/java/com/eduspark/agent/worksheet/wrong/WrongQuestionService.java`
- Test: `backend/src/test/java/com/eduspark/agent/worksheet/wrong/WrongQuestionIntegrationTest.java`

- [ ] **Step 1: Write integration test**

Test flow:

1. Register user.
2. Upload a text document.
3. Create worksheet.
4. Submit intentionally wrong answer.
5. Call `GET /api/users/{userId}/wrong-questions`.
6. Assert one item exists with `resolved=false`, `submittedAnswer`, `correctAnswer`, and `weaknessTag`.

- [ ] **Step 2: Run failing test**

Run: `cd backend && mvn -Dtest=WrongQuestionIntegrationTest test`

Expected: FAIL because endpoint/service does not exist.

- [ ] **Step 3: Implement wrong-question collection**

After `WorksheetAttemptService.submitAttempt` creates the attempt, call:

```java
wrongQuestionService.collectFromAttempt(userId, worksheet, response);
```

Collection rule:

- Only persist `WorksheetGradingItem` where `correct == false`.
- `weaknessTag` first version uses deterministic text: `"Needs review: " + item.stem().substring(0, Math.min(24, item.stem().length()))`.
- Store the original question snapshot as JSON so future worksheet edits do not mutate history.

- [ ] **Step 4: Add controller**

Routes:

- `GET /api/users/{userId}/wrong-questions`
- `GET /api/users/{userId}/worksheets/{worksheetId}/wrong-questions`
- `DELETE /api/users/{userId}/wrong-questions/{wrongQuestionId}` marks resolved.

- [ ] **Step 5: Run test**

Run: `cd backend && mvn -Dtest=WrongQuestionIntegrationTest test`

Expected: PASS.

---

### Task 4: Wrong Question Retry Practice

**Files:**
- Modify: `backend/src/main/java/com/eduspark/agent/worksheet/wrong/WrongQuestionService.java`
- Modify: `backend/src/main/java/com/eduspark/agent/worksheet/wrong/WrongQuestionController.java`
- Test: `backend/src/test/java/com/eduspark/agent/worksheet/wrong/WrongQuestionRetryIntegrationTest.java`

- [ ] **Step 1: Add retry endpoint test**

Route:

`POST /api/users/{userId}/wrong-questions/retry`

Request:

```json
{ "wrongQuestionIds": ["wrong-id"], "title": "错题再练" }
```

Expected response:

```json
{ "worksheetId": "string", "taskId": "string", "status": "PENDING" }
```

- [ ] **Step 2: Implement retry endpoint**

Build a `WorksheetCreateRequest` using the source worksheet document IDs and wrong-question stems as context in the title:

```java
new WorksheetCreateRequest(
  request.title(),
  sourceWorksheet.documentIds(),
  Math.max(3, wrongQuestionIds.size() * 2),
  sourceWorksheet.config().gradeLevel(),
  sourceWorksheet.config().difficulty(),
  sourceWorksheet.config().questionTypes(),
  true
)
```

- [ ] **Step 3: Mark retry worksheet linkage**

After creating the retry worksheet, call mapper `attachRetryWorksheet`.

- [ ] **Step 4: Run retry tests**

Run: `cd backend && mvn -Dtest=WrongQuestionRetryIntegrationTest test`

Expected: PASS.

---

### Task 5: Frontend Wrong Question Contracts

**Files:**
- Modify: `frontend/src/api/taskApi.ts`

- [ ] **Step 1: Add types**

Add:

```ts
export interface WrongQuestion {
  id: string;
  userId: string;
  worksheetId: string;
  attemptId: string;
  questionId: string;
  submittedAnswer?: string | null;
  correctAnswer?: string | null;
  explanation?: string | null;
  weaknessTag: string;
  retryWorksheetId?: string | null;
  resolved: boolean;
  createdAt: string;
  updatedAt: string;
}
```

- [ ] **Step 2: Add API functions**

Add:

```ts
listWrongQuestions(session: AuthSession): Promise<WrongQuestion[]>
listWorksheetWrongQuestions(session: AuthSession, worksheetId: string): Promise<WrongQuestion[]>
resolveWrongQuestion(session: AuthSession, wrongQuestionId: string): Promise<void>
retryWrongQuestions(session: AuthSession, wrongQuestionIds: string[], title: string): Promise<WorksheetCreateResponse>
```

- [ ] **Step 3: Run frontend typecheck**

Run: `cd frontend && node_modules\.bin\tsc.cmd -b`

Expected: PASS.

---

### Task 6: Worksheet Wrong Question UI

**Files:**
- Modify: `frontend/src/features/task/components/WorksheetPanel.tsx`
- Modify: `frontend/src/features/task/TaskConsolePage.tsx`
- Test: `frontend/src/features/task/TaskConsolePage.test.tsx`

- [ ] **Step 1: Add failing frontend test**

Extend worksheet test:

1. Generate worksheet.
2. Submit wrong answer.
3. Click `错题` tab.
4. Assert wrong-question suggestion is visible.
5. Click `错题再练`.
6. Assert `POST /wrong-questions/retry` called with selected wrong-question IDs.

- [ ] **Step 2: Add `错题` tab**

Tabs become:

- `答题`
- `解析`
- `错题`
- `记录`

Show wrong items with:

- submitted answer
- correct answer
- explanation
- weakness tag
- resolve button
- retry selected button

- [ ] **Step 3: Wire state in `TaskConsolePage`**

State:

```ts
const [wrongQuestions, setWrongQuestions] = useState<WrongQuestion[]>([]);
const [isRetryingWrongQuestions, setIsRetryingWrongQuestions] = useState(false);
```

On attempt submit: refresh worksheet wrong questions.

- [ ] **Step 4: Run frontend tests**

Run: `cd frontend && node_modules\.bin\vitest.cmd run`

Expected: PASS.

---

### Task 7: Collapsible Task Timeline Replay

**Files:**
- Modify: `frontend/src/features/task/components/TaskTimeline.tsx`
- Modify: `frontend/src/features/task/TaskConsolePage.tsx`
- Test: `frontend/src/features/task/TaskConsolePage.test.tsx`

- [ ] **Step 1: Add timeline grouping behavior**

Group by `stage/status`:

- `PENDING`: 创建
- `PLANNING`: 规划
- `EXECUTING`: 执行
- `REVIEWING`: 复核
- `COMPLETED`: 完成
- `FAILED`: 失败

- [ ] **Step 2: Add collapsible UI**

Each group has:

- title
- count
- expand/collapse button
- latest message preview when collapsed

- [ ] **Step 3: Add test**

Render completed task replay, collapse `执行`, assert detailed message hidden, expand it, assert message visible.

- [ ] **Step 4: Run frontend validation**

Run:

```powershell
cd frontend
node_modules\.bin\vitest.cmd run
node_modules\.bin\tsc.cmd -b
node_modules\.bin\vite.cmd build
```

Expected: all PASS.

---

### Task 8: Phase 7 Full Verification

**Files:**
- Modify: `contracts/task.contract.ts`
- Modify: `docs/api.md`

- [ ] **Step 1: Update contracts**

Add wrong-question DTOs and endpoints to both files.

- [ ] **Step 2: Run backend package**

Run: `cd backend && mvn package`

Expected: `Tests run: ... Failures: 0, Errors: 0`, `BUILD SUCCESS`.

- [ ] **Step 3: Run frontend validation**

Run:

```powershell
cd frontend
node_modules\.bin\vitest.cmd run
node_modules\.bin\tsc.cmd -b
node_modules\.bin\vite.cmd build
```

Expected: all PASS.

