# 错题本独立页面 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a standalone错题本页面，支持全局错题与按练习卷切换查看，能查看详情、标记已解决，并从错题一键发起再练。

**Architecture:** 复用现有的 user-scoped 错题数据和再练接口，只补一个很轻的“我的练习卷列表”后端接口，方便独立页面选择 scope。前端继续沿用当前 shell 内部视图切换，不引入新的路由库；把错题本的数据加载与操作收进专用 hook，页面层只负责布局和交互呈现。

**Tech Stack:** Spring Boot 3, MyBatis-Plus, MySQL, React 18, TypeScript, Vite, Tailwind CSS, lucide-react.

---

## File Map

- `backend/src/main/java/com/eduspark/agent/worksheet/WorksheetController.java`: 新增当前用户练习卷列表接口。
- `backend/src/main/java/com/eduspark/agent/worksheet/WorksheetService.java`: 查询最近练习卷并映射为轻量 DTO。
- `backend/src/main/java/com/eduspark/agent/worksheet/WorksheetMapper.java`: 增加按用户倒序分页查询 SQL。
- `backend/src/main/java/com/eduspark/agent/worksheet/dto/WorksheetListItemResponse.java`: 新增练习卷列表行 DTO。
- `backend/src/test/java/com/eduspark/agent/worksheet/WorksheetControllerIntegrationTest.java`: 覆盖练习卷列表接口的用户隔离与排序。
- `frontend/src/api/taskApi.ts`: 新增 `listWorksheets` API helper 与列表 DTO。
- `frontend/src/features/task/TaskUiTypes.ts`: 新增错题本视图类型与页面内状态类型。
- `frontend/src/features/task/components/WorkspaceShell.tsx`: 在顶部导航中增加“错题本”入口。
- `frontend/src/features/task/TaskConsolePage.tsx`: 接入新的视图分支。
- `frontend/src/features/task/hooks/useWrongQuestionCenter.ts`: 聚合错题本页面的数据加载、筛选和操作。
- `frontend/src/features/task/pages/WrongQuestionCenterPage.tsx`: 错题本独立页面。
- `frontend/src/features/task/components/WrongQuestionScopeTabs.tsx`: scope 切换。
- `frontend/src/features/task/components/WrongQuestionSummaryStrip.tsx`: 顶部摘要条。
- `frontend/src/features/task/components/WrongQuestionList.tsx`: 错题列表。
- `frontend/src/features/task/components/WrongQuestionDetailPanel.tsx`: 详情与再练面板。
- `frontend/src/features/task/pages/WrongQuestionCenterPage.test.tsx`: 新页面回归测试。
- `frontend/src/features/task/TaskConsolePage.test.tsx`: 增加导航入口回归断言。

## Task 1: 后端练习卷列表接口

**Files:**
- Modify: `backend/src/main/java/com/eduspark/agent/worksheet/WorksheetController.java`
- Modify: `backend/src/main/java/com/eduspark/agent/worksheet/WorksheetService.java`
- Modify: `backend/src/main/java/com/eduspark/agent/worksheet/WorksheetMapper.java`
- Create: `backend/src/main/java/com/eduspark/agent/worksheet/dto/WorksheetListItemResponse.java`
- Modify: `backend/src/test/java/com/eduspark/agent/worksheet/WorksheetControllerIntegrationTest.java`

- [ ] **Step 1: Write the failing test**

```java
@Test
void listsWorksheetsForUser() throws Exception {
  AuthSession userA = register();
  AuthSession userB = register();

  String worksheetA = createWorksheet(userA, uploadDocument(userA));
  createWorksheet(userB, uploadDocument(userB));

  mockMvc.perform(get("/api/users/{userId}/worksheets", userA.userId())
      .header("Authorization", "Bearer " + userA.token())
      .param("limit", "5"))
    .andExpect(status().isOk())
    .andExpect(jsonPath("$[0].id").value(worksheetA))
    .andExpect(jsonPath("$[0].title").value("Fraction practice"));
}
```

- [ ] **Step 2: Run the failing test**

Run: `mvn -Dtest=WorksheetControllerIntegrationTest#listsWorksheetsForUser test`

Expected: fail because `GET /api/users/{userId}/worksheets` does not exist yet.

- [ ] **Step 3: Implement the endpoint**
  1. Add `WorksheetListItemResponse` with `id`, `taskId`, `title`, `status`, `createdAt`, `updatedAt`.
  2. Add `WorksheetMapper.selectRecentByUserId(userId, limit)` ordered by `created_at desc`.
  3. Add `WorksheetService.listRecent(userId, limit)` and clamp `limit` to a small safe range.
  4. Add `GET /api/users/{userId}/worksheets?limit=20` in `WorksheetController`.

- [ ] **Step 4: Rerun the test**

Run: `mvn -Dtest=WorksheetControllerIntegrationTest#listsWorksheetsForUser test`

Expected: PASS with a single user-scoped worksheet row in descending order.

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/java/com/eduspark/agent/worksheet/WorksheetController.java \
  backend/src/main/java/com/eduspark/agent/worksheet/WorksheetService.java \
  backend/src/main/java/com/eduspark/agent/worksheet/WorksheetMapper.java \
  backend/src/main/java/com/eduspark/agent/worksheet/dto/WorksheetListItemResponse.java \
  backend/src/test/java/com/eduspark/agent/worksheet/WorksheetControllerIntegrationTest.java
git commit -m "feat: add worksheet list endpoint for wrong question center"
```

## Task 2: 前端视图入口与 API plumbing

**Files:**
- Modify: `frontend/src/api/taskApi.ts`
- Modify: `frontend/src/features/task/TaskUiTypes.ts`
- Modify: `frontend/src/features/task/components/WorkspaceShell.tsx`
- Modify: `frontend/src/features/task/TaskConsolePage.tsx`
- Modify: `frontend/src/features/task/TaskConsolePage.test.tsx`

- [ ] **Step 1: Write the failing test**

Add a navigation assertion that the shell exposes a new button labeled `错题本`, and that clicking it renders a placeholder for the new page.

```tsx
expect(screen.getByRole("button", { name: "错题本" })).toBeInTheDocument();
await user.click(screen.getByRole("button", { name: "错题本" }));
expect(await screen.findByText("错题本")).toBeInTheDocument();
```

- [ ] **Step 2: Run the failing test**

Run: `npm exec vitest run src/features/task/TaskConsolePage.test.tsx`

Expected: fail until the new view exists.

- [ ] **Step 3: Implement the plumbing**
  1. Extend `WorkspaceView` with `"wrong-questions"`.
  2. Add `WorksheetListItem` and `listWorksheets(session, limit = 12)` to `taskApi.ts`.
  3. Add `错题本` to `WorkspaceShell` nav.
  4. Add a `wrong-questions` branch in `TaskConsolePage.tsx` that renders the new page.

- [ ] **Step 4: Rerun the test**

Run: `npm exec vitest run src/features/task/TaskConsolePage.test.tsx`

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add frontend/src/api/taskApi.ts \
  frontend/src/features/task/TaskUiTypes.ts \
  frontend/src/features/task/components/WorkspaceShell.tsx \
  frontend/src/features/task/TaskConsolePage.tsx \
  frontend/src/features/task/TaskConsolePage.test.tsx
git commit -m "feat: add wrong question center navigation"
```

## Task 3: 错题本数据 hook 与页面骨架

**Files:**
- Create: `frontend/src/features/task/hooks/useWrongQuestionCenter.ts`
- Create: `frontend/src/features/task/pages/WrongQuestionCenterPage.tsx`
- Create: `frontend/src/features/task/components/WrongQuestionSummaryStrip.tsx`
- Create: `frontend/src/features/task/components/WrongQuestionScopeTabs.tsx`
- Modify: `frontend/src/features/task/TaskUiTypes.ts`
- Modify: `frontend/src/features/task/pages/WrongQuestionCenterPage.test.tsx`

- [ ] **Step 1: Write the failing test**

```tsx
render(<WrongQuestionCenterPage session={session} />);
expect(await screen.findByText("全局错题")).toBeInTheDocument();
expect(screen.getByRole("button", { name: "本卷错题" })).toBeInTheDocument();
expect(screen.getByText("暂无错题")).toBeInTheDocument();
```

- [ ] **Step 2: Run the failing test**

Run: `npm exec vitest run src/features/task/pages/WrongQuestionCenterPage.test.tsx`

Expected: fail because the page and hook do not exist yet.

- [ ] **Step 3: Implement the hook and shell**
  1. In `useWrongQuestionCenter.ts`, load `listWorksheets`, `listWrongQuestions`, and `listWorksheetWrongQuestions`.
  2. Keep selected scope in local component state so the page can switch between global and worksheet views without leaving the shell.
  3. In `WrongQuestionCenterPage.tsx`, add the top summary strip and scope tabs, and render a compact empty state when there are no rows.

- [ ] **Step 4: Rerun the page test**

Run: `npm exec vitest run src/features/task/pages/WrongQuestionCenterPage.test.tsx`

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add frontend/src/features/task/hooks/useWrongQuestionCenter.ts \
  frontend/src/features/task/pages/WrongQuestionCenterPage.tsx \
  frontend/src/features/task/components/WrongQuestionSummaryStrip.tsx \
  frontend/src/features/task/components/WrongQuestionScopeTabs.tsx \
  frontend/src/features/task/pages/WrongQuestionCenterPage.test.tsx \
  frontend/src/features/task/TaskUiTypes.ts
git commit -m "feat: scaffold wrong question center"
```

## Task 4: 错题列表、详情与再练动作

**Files:**
- Create: `frontend/src/features/task/components/WrongQuestionList.tsx`
- Create: `frontend/src/features/task/components/WrongQuestionDetailPanel.tsx`
- Modify: `frontend/src/features/task/pages/WrongQuestionCenterPage.tsx`
- Modify: `frontend/src/features/task/pages/WrongQuestionCenterPage.test.tsx`

- [ ] **Step 1: Extend the page test**

Add selection and action assertions:

```tsx
await user.click(screen.getByRole("button", { name: "开始再练" }));
expect(await screen.findByText("再练已创建")).toBeInTheDocument();
await user.click(screen.getByRole("button", { name: "标记已解决" }));
expect(screen.getByText("已解决")).toBeInTheDocument();
```

- [ ] **Step 2: Implement the list and detail panels**
  1. `WrongQuestionList.tsx` renders stem, weakness tag, source worksheet label, and retry status.
  2. `WrongQuestionDetailPanel.tsx` renders the full stem, submitted answer, correct answer, explanation, and action buttons.
  3. Wire the page to keep a selected wrong question id and open the matching detail panel.

- [ ] **Step 3: Wire retry and resolve**
  1. Call `resolveWrongQuestion(session, wrongQuestionId)` from the detail panel.
  2. Call `retryWrongQuestions(session, selectedIds, title)` from the page, then refresh the current scope.
  3. After retry succeeds, keep the user on the same page and highlight the new retry worksheet id in the detail view.

- [ ] **Step 4: Rerun the page test and build**

Run:

```bash
npm exec vitest run src/features/task/pages/WrongQuestionCenterPage.test.tsx
npm run build
```

Expected: both commands PASS.

- [ ] **Step 5: Commit**

```bash
git add frontend/src/features/task/components/WrongQuestionList.tsx \
  frontend/src/features/task/components/WrongQuestionDetailPanel.tsx \
  frontend/src/features/task/pages/WrongQuestionCenterPage.tsx \
  frontend/src/features/task/pages/WrongQuestionCenterPage.test.tsx
git commit -m "feat: add wrong question detail and retry flow"
```

## Task 5: Final regression sweep

**Files:**
- Modify only if any suite exposes a regression.

- [ ] **Step 1: Run the full backend and frontend suites**

Run:

```bash
mvn test
npm exec vitest run
npm run build
```

Expected: all suites PASS, with no new backend integration failures and no new frontend TypeScript/build errors.

- [ ] **Step 2: Fix any regression in the smallest possible file**

If a regression appears, patch the exact file that owns the broken behavior and rerun only the affected suite before widening back to the full sweep.

- [ ] **Step 3: Commit the final slice**

```bash
git add backend frontend
git commit -m "feat: ship standalone wrong question center"
```
