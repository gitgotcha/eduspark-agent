# EduSpark Phase 7 Learning Loop + Phase 6 UI Redesign Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** First complete the education workflow loop and standalone knowledge graph experience, then rebuild the front-end into a unified glass/liquid workspace with stronger auth, theme, and layout polish.

**Architecture:** Keep the current user-scoped backend contract and existing React workspace coordinator, but tighten the data model first so worksheets, attempts, wrong questions, history replay, and knowledge graphs all speak the same shape. Phase 7 stabilizes the learning loop and graph surfaces; Phase 6 then reuses those stable contracts to redesign the UI, auth entry, and global motion system without changing core behavior.

**Tech Stack:** Spring Boot, MyBatis-Plus, Flyway, Spring AI / Qwen-compatible provider, React 18, TypeScript, Tailwind CSS, lucide-react, Vitest, Testing Library, Vite.

---

## File Structure

### Backend

- Create `backend/src/main/resources/db/migration/V10__add_knowledge_graph_metadata.sql` - add or normalize graph metadata needed for stable graph replay and export.
- Create `backend/src/main/resources/db/migration/V11__normalize_learning_loop.sql` - add any missing attempt/replay fields needed by the wrong-question loop and history replay.
- Modify `backend/src/main/java/com/eduspark/agent/worksheet/attempt/WorksheetAttemptService.java` - persist attempt grading results and wrong-question snapshots consistently.
- Modify `backend/src/main/java/com/eduspark/agent/worksheet/attempt/WorksheetAttemptController.java` - expose attempt list/create endpoints with stable response shapes.
- Modify `backend/src/main/java/com/eduspark/agent/worksheet/wrong/WrongQuestionService.java` - collect, retry, and resolve wrong questions.
- Modify `backend/src/main/java/com/eduspark/agent/worksheet/wrong/WrongQuestionController.java` - user-scoped wrong-question endpoints.
- Modify `backend/src/main/java/com/eduspark/agent/knowledge/KnowledgeGraphService.java` - generate, store, and reload graph JSON.
- Modify `backend/src/main/java/com/eduspark/agent/knowledge/KnowledgeGraphController.java` - list, fetch, and delete graph records.
- Modify `backend/src/main/java/com/eduspark/agent/worksheet/WorksheetController.java` - keep worksheet payloads aligned with practice / explanation / export flows.
- Modify `backend/src/main/java/com/eduspark/agent/worksheet/dto/GeneratedWorksheetQuestion.java` - keep generated question shape stable.
- Modify `backend/src/main/java/com/eduspark/agent/worksheet/dto/WorksheetCreateRequest.java` - keep request contract aligned with the frontend composer.
- Modify `backend/src/main/java/com/eduspark/agent/worksheet/dto/WorksheetCreateResponse.java` - keep create response scoped to task and worksheet ids.
- Modify `backend/src/main/java/com/eduspark/agent/worksheet/dto/WorksheetDetailResponse.java` - keep detail response aligned with practice / explanation / retry views.
- Modify `backend/src/main/java/com/eduspark/agent/worksheet/dto/WorksheetGenerationRationale.java` - keep rationale stable for explanation view.
- Modify `backend/src/main/java/com/eduspark/agent/worksheet/dto/WorksheetGenerationResult.java` - keep AI output parsing stable.
- Modify `backend/src/main/java/com/eduspark/agent/worksheet/dto/WorksheetQuestion.java` - keep rendered question shape stable.
- Modify `backend/src/main/java/com/eduspark/agent/worksheet/attempt/dto/WorksheetAnswerInput.java` - keep grading input aligned with answer form.
- Modify `backend/src/main/java/com/eduspark/agent/worksheet/attempt/dto/WorksheetAttemptRequest.java` - keep submit payload aligned with attempt form.
- Modify `backend/src/main/java/com/eduspark/agent/worksheet/attempt/dto/WorksheetAttemptResponse.java` - keep grading response aligned with result panel.
- Modify `backend/src/main/java/com/eduspark/agent/worksheet/attempt/dto/WorksheetGradingItem.java` - keep per-question grading data stable.
- Modify `backend/src/main/java/com/eduspark/agent/worksheet/wrong/dto/WrongQuestionResponse.java` - keep wrong-question list data stable.
- Modify `backend/src/main/java/com/eduspark/agent/worksheet/wrong/dto/WrongQuestionRetryRequest.java` - keep retry request shape stable.
- Modify `backend/src/main/java/com/eduspark/agent/knowledge/dto/KnowledgeGraphNode.java` - keep node fields renderable by Canvas.
- Modify `backend/src/main/java/com/eduspark/agent/knowledge/dto/KnowledgeGraphEdge.java` - keep edge fields renderable by Canvas.
- Modify `backend/src/main/java/com/eduspark/agent/knowledge/dto/KnowledgeGraphResponse.java` - keep graph response shape stable.
- Modify `backend/src/main/java/com/eduspark/agent/api/ApiExceptionHandler.java` - keep 401 / 403 / 404 / retryable failures consistent.
- Modify `backend/src/main/java/com/eduspark/agent/auth/AuthController.java` - add captcha-backed auth flow and keep auth responses stable.
- Modify `backend/src/main/java/com/eduspark/agent/auth/AuthRequest.java` - extend auth payload with captcha fields.
- Create `backend/src/main/java/com/eduspark/agent/auth/captcha/CaptchaChallenge.java` - captcha response DTO.
- Create `backend/src/main/java/com/eduspark/agent/auth/captcha/CaptchaService.java` - challenge generation and verification.
- Create `backend/src/main/java/com/eduspark/agent/auth/captcha/CaptchaStore.java` - captcha persistence interface.
- Create `backend/src/main/java/com/eduspark/agent/auth/captcha/InMemoryCaptchaStore.java` - single-node captcha store implementation.
- Create `backend/src/test/java/com/eduspark/agent/worksheet/attempt/WorksheetAttemptIntegrationTest.java` - verify grading persistence and replay shape.
- Create `backend/src/test/java/com/eduspark/agent/worksheet/wrong/WrongQuestionIntegrationTest.java` - verify wrong-question list, resolve, and retry flows.
- Create `backend/src/test/java/com/eduspark/agent/knowledge/KnowledgeGraphIntegrationTest.java` - verify graph generation and retrieval.
- Create `backend/src/test/java/com/eduspark/agent/auth/captcha/CaptchaServiceTest.java` - verify captcha challenge generation and verification.

### Frontend

- Modify `frontend/src/api/taskApi.ts` - consolidate task, worksheet, wrong-question, replay, graph, and auth API calls.
- Modify `frontend/src/features/task/TaskConsolePage.tsx` - keep session, theme, page view, and shared state coordination.
- Modify `frontend/src/features/task/TaskConsolePage.test.tsx` - cover new page layout, graph view, wrong-question retry, and theme persistence.
- Modify `frontend/src/features/task/pages/AuthPage.test.tsx` - cover captcha-backed auth entry and theme controls.
- Modify `frontend/src/features/task/components/WorkspaceShell.tsx` - top navigation, page switching, and top-bar controls.
- Modify `frontend/src/features/task/components/WorksheetPanel.tsx` - practice / explanation / wrong-question tabs and retry actions.
- Modify `frontend/src/features/task/components/TaskTimeline.tsx` - collapsible replay groups and readable log formatting.
- Modify `frontend/src/features/task/components/HistoryPanel.tsx` - history and replay entry points.
- Modify `frontend/src/features/task/components/KnowledgeGraphPanel.tsx` - Canvas-oriented graph rendering and node detail state.
- Modify `frontend/src/features/task/components/AuthPanel.tsx` - auth entry, captcha, and clearer errors.
- Modify `frontend/src/features/task/pages/AuthPage.tsx` - branded welcome page wrapper.
- Modify `frontend/src/features/task/pages/WorkspacePage.tsx` - workspace home layout and current-work focus.
- Modify `frontend/src/features/task/pages/WorksheetStudioPage.tsx` - dedicated worksheet workspace wrapper.
- Modify `frontend/src/features/task/pages/HistoryPage.tsx` - dedicated history/replay wrapper.
- Modify `frontend/src/features/task/pages/KnowledgeGraphPage.tsx` - dedicated knowledge graph workspace.
- Modify `frontend/src/features/task/pages/ProfilePage.tsx` - theme/session preferences and logout cleanup.
- Modify `frontend/src/features/task/components/PageIntro.tsx` - page-level one-line introductions.
- Modify `frontend/src/features/task/components/ThemeToggle.tsx` - theme switching with clearer feedback.
- Modify `frontend/src/features/task/components/GlassPanel.tsx` - reusable glass shell with randomized motion vars.
- Modify `frontend/src/features/task/components/EnergyProgress.tsx` - slim progress and task status bars.
- Modify `frontend/src/styles/globals.css` - glass/liquid system, theme tokens, motion, and page shell styling.

### Contracts and Docs

- Modify `contracts/task.contract.ts` - keep shared DTOs aligned with backend and frontend.
- Modify `docs/api.md` - document user-scoped endpoints, graph payloads, wrong-question flows, and auth behavior.

---

### Task 1: Contract-First Sync for Learning Loop and Graph

**Files:**
- Modify: `contracts/task.contract.ts`
- Modify: `docs/api.md`
- Modify: `frontend/src/api/taskApi.ts`
- Modify: `frontend/src/features/task/TaskConsolePage.test.tsx`

- [ ] **Step 1: Add contract assertions in tests**

Add or extend frontend test coverage so the expected shapes are explicit before implementation:

```ts
expect(graph.nodes[0]).toMatchObject({
  id: expect.any(String),
  label: expect.any(String),
  type: expect.any(String),
  weight: expect.any(Number)
});

expect(attempt.items[0]).toMatchObject({
  questionId: expect.any(String),
  correctAnswer: expect.any(String),
  correct: expect.any(Boolean)
});
```

- [ ] **Step 2: Run the contract-focused tests and confirm they fail**

Run:

```powershell
cd frontend
npm exec vitest run src/features/task/TaskConsolePage.test.tsx
```

Expected: fail until the new DTO shapes and API helpers are aligned.

- [ ] **Step 3: Update the shared contract layer**

Add or refine:

- knowledge graph node / edge response fields for Canvas rendering
- worksheet attempt response fields for replay and wrong-question generation
- wrong-question response fields for retry and resolve flows
- user-scoped endpoints in `docs/api.md`

- [ ] **Step 4: Re-run typecheck and targeted tests**

Run:

```powershell
cd frontend
npx tsc -b
npm exec vitest run src/features/task/TaskConsolePage.test.tsx
```

Expected: both pass.

---

### Task 2: Backend Learning Loop and Knowledge Graph Hardening

**Files:**
- Modify: `backend/src/main/resources/db/migration/V10__add_knowledge_graph_metadata.sql`
- Modify: `backend/src/main/resources/db/migration/V11__normalize_learning_loop.sql`
- Modify: `backend/src/main/java/com/eduspark/agent/worksheet/attempt/WorksheetAttemptService.java`
- Modify: `backend/src/main/java/com/eduspark/agent/worksheet/attempt/WorksheetAttemptController.java`
- Modify: `backend/src/main/java/com/eduspark/agent/worksheet/wrong/WrongQuestionService.java`
- Modify: `backend/src/main/java/com/eduspark/agent/worksheet/wrong/WrongQuestionController.java`
- Modify: `backend/src/main/java/com/eduspark/agent/knowledge/KnowledgeGraphService.java`
- Modify: `backend/src/main/java/com/eduspark/agent/knowledge/KnowledgeGraphController.java`
- Test: `backend/src/test/java/com/eduspark/agent/worksheet/attempt/WorksheetAttemptIntegrationTest.java`
- Test: `backend/src/test/java/com/eduspark/agent/worksheet/wrong/WrongQuestionIntegrationTest.java`
- Test: `backend/src/test/java/com/eduspark/agent/knowledge/KnowledgeGraphIntegrationTest.java`

- [ ] **Step 1: Write the failing backend integration tests**

Add integration tests that lock the intended behavior:

- a worksheet attempt stores grading items and wrong-question snapshots
- wrong questions can be listed, resolved, and retried per user
- a knowledge graph can be generated from selected documents and fetched back

- [ ] **Step 2: Run backend tests and confirm the current gaps**

Run:

```powershell
cd backend
mvn -Dtest=WorksheetAttemptIntegrationTest,WrongQuestionIntegrationTest,KnowledgeGraphIntegrationTest test
```

Expected: fail until the backend persistence and controller contracts are aligned.

- [ ] **Step 3: Implement the storage and controller updates**

Wire the following together:

- attempt submission persists per-question grading output
- wrong-question collection stores resolved state and retry linkage
- knowledge graph generation stores graph JSON plus source document references
- user-scoped fetch and delete endpoints guard against cross-user access

- [ ] **Step 4: Re-run backend tests**

Run:

```powershell
cd backend
mvn -Dtest=WorksheetAttemptIntegrationTest,WrongQuestionIntegrationTest,KnowledgeGraphIntegrationTest test
```

Expected: pass.

---

### Task 3: Frontend Phase 7 Surfaces

**Files:**
- Modify: `frontend/src/api/taskApi.ts`
- Modify: `frontend/src/features/task/components/WorksheetPanel.tsx`
- Modify: `frontend/src/features/task/components/TaskTimeline.tsx`
- Modify: `frontend/src/features/task/components/HistoryPanel.tsx`
- Modify: `frontend/src/features/task/components/KnowledgeGraphPanel.tsx`
- Modify: `frontend/src/features/task/pages/WorksheetStudioPage.tsx`
- Modify: `frontend/src/features/task/pages/HistoryPage.tsx`
- Modify: `frontend/src/features/task/pages/KnowledgeGraphPage.tsx`
- Modify: `frontend/src/features/task/TaskConsolePage.tsx`
- Test: `frontend/src/features/task/TaskConsolePage.test.tsx`

- [ ] **Step 1: Write the failing frontend interaction tests**

Add tests that cover:

- practice and explanation tab switching
- wrong-question tab and retry action
- collapsible replay groups in the timeline
- knowledge graph Canvas page loads with node detail affordance

- [ ] **Step 2: Run the tests and confirm they fail before wiring**

Run:

```powershell
cd frontend
npm exec vitest run src/features/task/TaskConsolePage.test.tsx
```

Expected: fail until the page-specific wrappers and UI state are connected.

- [ ] **Step 3: Implement the page-specific views**

Keep the coordinator in `TaskConsolePage`, but move rendering into:

- `WorkspacePage` for current work
- `WorksheetStudioPage` for practice and explanation
- `HistoryPage` for task replay and history
- `KnowledgeGraphPage` for Canvas graph interaction

Keep the graph rendering Canvas-first and treat Mermaid as secondary export / preview only.

- [ ] **Step 4: Re-run frontend tests**

Run:

```powershell
cd frontend
npm exec vitest run src/features/task/TaskConsolePage.test.tsx
```

Expected: pass.

---

### Task 4: Phase 6 Auth Entry, Theme, and Shell Redesign

**Files:**
- Modify: `backend/src/main/java/com/eduspark/agent/auth/AuthController.java`
- Modify: `backend/src/main/java/com/eduspark/agent/auth/AuthRequest.java`
- Create: `backend/src/main/java/com/eduspark/agent/auth/captcha/CaptchaChallenge.java`
- Create: `backend/src/main/java/com/eduspark/agent/auth/captcha/CaptchaService.java`
- Create: `backend/src/main/java/com/eduspark/agent/auth/captcha/CaptchaStore.java`
- Create: `backend/src/main/java/com/eduspark/agent/auth/captcha/InMemoryCaptchaStore.java`
- Modify: `frontend/src/api/taskApi.ts`
- Modify: `frontend/src/features/task/components/AuthPanel.tsx`
- Modify: `frontend/src/features/task/pages/AuthPage.tsx`
- Modify: `frontend/src/features/task/components/WorkspaceShell.tsx`
- Modify: `frontend/src/features/task/components/ThemeToggle.tsx`
- Modify: `frontend/src/features/task/TaskConsolePage.tsx`
- Test: `frontend/src/features/task/pages/AuthPage.test.tsx`

- [ ] **Step 1: Write the failing auth and captcha tests**

Lock the intended login flow:

- auth page shows branded intro and two-mode auth
- captcha is required before login/register submit
- refreshing captcha resets the challenge
- successful auth stores `{ userId, username, token }`

- [ ] **Step 2: Run the auth tests and confirm they fail**

Run:

```powershell
cd frontend
npm exec vitest run src/features/task/pages/AuthPage.test.tsx
```

Expected: fail until captcha and theme controls are fully wired.

- [ ] **Step 3: Implement captcha-backed auth and theme persistence**

Wire the backend auth endpoint, captcha generation, and frontend state so the login entry becomes stable and explicit.

The theme switch should persist via `localStorage` and animate the transition without changing the current workspace state.

- [ ] **Step 4: Re-run auth tests**

Run:

```powershell
cd frontend
npm exec vitest run src/features/task/pages/AuthPage.test.tsx
```

Expected: pass.

---

### Task 5: Global Glass / Liquid Visual System

**Files:**
- Modify: `frontend/src/styles/globals.css`
- Modify: `frontend/src/features/task/components/GlassPanel.tsx`
- Modify: `frontend/src/features/task/components/PageIntro.tsx`
- Modify: `frontend/src/features/task/components/EnergyProgress.tsx`
- Modify: `frontend/src/features/task/components/PromptComposer.tsx`
- Modify: `frontend/src/features/task/components/DocumentCenterPanel.tsx`
- Modify: `frontend/src/features/task/components/ResultPanel.tsx`
- Modify: `frontend/src/features/task/components/ArtifactGrid.tsx`
- Modify: `frontend/src/features/task/components/KnowledgeGraphPanel.tsx`
- Modify: `frontend/src/features/task/components/HistoryPanel.tsx`
- Modify: `frontend/src/features/task/components/WorksheetPanel.tsx`
- Modify: `frontend/src/features/task/TaskConsolePage.test.tsx`

- [ ] **Step 1: Add a visual regression test for the shell**

Add assertions that the main shell:

- keeps page titles and intro text readable
- preserves button and label containment on narrow width
- maintains theme switching after rerender

- [ ] **Step 2: Run the visual test and confirm it fails on the target deltas**

Run:

```powershell
cd frontend
npm exec vitest run src/features/task/TaskConsolePage.test.tsx
```

Expected: fail until the new shell styles are in place.

- [ ] **Step 3: Implement the shared motion system**

Update the global CSS to establish:

- transparent white / deep black theme tokens
- glass panels with visible outline and shadow depth
- continuous liquid-flow backgrounds
- stronger color accents without losing readability
- consistent page intro spacing and content hierarchy

- [ ] **Step 4: Re-run frontend typecheck and build**

Run:

```powershell
cd frontend
npx tsc -b
npm exec vite build
```

Expected: pass.

---

### Task 6: Full Verification and Documentation Sync

**Files:**
- Modify: `docs/api.md`
- Modify: `contracts/task.contract.ts`

- [ ] **Step 1: Update API documentation**

Document the final user-scoped endpoints and response shapes for:

- documents
- tasks and task stream
- worksheets and attempts
- wrong questions and retry
- knowledge graph
- auth and captcha

- [ ] **Step 2: Run backend package build**

Run:

```powershell
cd backend
mvn package
```

Expected: `BUILD SUCCESS`.

- [ ] **Step 3: Run full frontend verification**

Run:

```powershell
cd frontend
npm exec vitest run
npx tsc -b
npm exec vite build
```

Expected: all pass.

- [ ] **Step 4: Do one manual smoke pass**

Verify in the browser:

- login / captcha works
- theme switching persists
- worksheet practice / explanation / wrong-question tabs render
- history replay collapses cleanly
- knowledge graph loads in Canvas mode
- narrow-width layout does not overlap
