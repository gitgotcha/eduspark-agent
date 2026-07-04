# Phase 6 Auth Security And Liquid UI Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add local image captcha to authentication and upgrade the whole UI with themes, liquid-glass styling, progress feedback, and richer motion.

**Architecture:** Keep JWT auth unchanged, but extend auth requests with captcha fields. Introduce a `CaptchaService` with an in-memory store first and a store interface that can later be backed by Redis. On the frontend, add a theme provider and CSS variables before applying visual polish to existing components.

**Tech Stack:** Spring Boot, Java2D captcha generation, React, TypeScript, TailwindCSS, CSS variables, lucide-react, Vitest.

---

## File Structure

- Create `backend/src/main/java/com/eduspark/agent/auth/captcha/CaptchaChallenge.java`: captcha response DTO.
- Create `backend/src/main/java/com/eduspark/agent/auth/captcha/CaptchaStore.java`: storage interface.
- Create `backend/src/main/java/com/eduspark/agent/auth/captcha/InMemoryCaptchaStore.java`: single-node TTL store.
- Create `backend/src/main/java/com/eduspark/agent/auth/captcha/CaptchaService.java`: image generation and verification.
- Modify `backend/src/main/java/com/eduspark/agent/auth/AuthRequest.java`: add `captchaId`, `captchaCode`.
- Modify `backend/src/main/java/com/eduspark/agent/auth/AuthController.java`: add captcha endpoint and verify on login/register.
- Modify `backend/src/test/java/com/eduspark/agent/auth/AuthControllerIntegrationTest.java`: captcha auth tests.
- Modify `frontend/src/api/taskApi.ts`: captcha API and auth request payload.
- Modify `frontend/src/features/task/components/AuthPanel.tsx`: captcha UI and liquid login composition.
- Modify `frontend/src/features/task/TaskConsolePage.tsx`: captcha state and theme state.
- Create `frontend/src/features/task/components/ThemeSwitcher.tsx`: theme selector.
- Modify `frontend/src/features/task/components/WorkspaceShell.tsx`: top bar theme controls and progress indicator.
- Modify `frontend/src/styles/globals.css`: CSS variables, themes, liquid glass, animation utilities.
- Modify `frontend/src/features/task/components/TaskTimeline.tsx`: animated grouped timeline styling.
- Modify `frontend/src/features/task/components/PromptComposer.tsx`, `WorksheetPanel.tsx`, `DocumentCenterPanel.tsx`, `KnowledgeGraphPanel.tsx`: progress/loading polish.
- Test `frontend/src/features/task/TaskConsolePage.test.tsx`.

---

### Task 1: Backend Captcha Contract

**Files:**
- Create: `backend/src/main/java/com/eduspark/agent/auth/captcha/CaptchaChallenge.java`
- Modify: `backend/src/main/java/com/eduspark/agent/auth/AuthRequest.java`

- [ ] **Step 1: Add response DTO**

```java
package com.eduspark.agent.auth.captcha;

public record CaptchaChallenge(String captchaId, String imageBase64, int expiresInSeconds) {}
```

- [ ] **Step 2: Extend auth request**

`AuthRequest` should contain:

```java
@NotBlank @Size(max = 64) String username
@NotBlank @Size(min = 6, max = 128) String password
@NotBlank String captchaId
@NotBlank @Size(min = 4, max = 8) String captchaCode
```

- [ ] **Step 3: Run compile**

Run: `cd backend && mvn -DskipTests compile`

Expected: compile fails until controller/tests are updated. This is expected during this task.

---

### Task 2: Captcha Store And Generator

**Files:**
- Create: `backend/src/main/java/com/eduspark/agent/auth/captcha/CaptchaStore.java`
- Create: `backend/src/main/java/com/eduspark/agent/auth/captcha/InMemoryCaptchaStore.java`
- Create: `backend/src/main/java/com/eduspark/agent/auth/captcha/CaptchaService.java`
- Test: `backend/src/test/java/com/eduspark/agent/auth/captcha/CaptchaServiceTest.java`

- [ ] **Step 1: Write service test**

Test cases:

- `createChallenge` returns id, base64 PNG, and `expiresInSeconds=300`.
- correct code verifies once.
- same code cannot be reused.
- wrong code fails.

- [ ] **Step 2: Implement store**

Store methods:

```java
void save(String captchaId, String code, Instant expiresAt);
boolean consume(String captchaId, String code);
void removeExpired();
```

Use `ConcurrentHashMap<String, CaptchaEntry>`.

- [ ] **Step 3: Implement image generator**

Generate a 120x44 PNG with:

- white/translucent background
- 5 random uppercase alphanumeric characters excluding confusing characters
- light interference lines
- base64 prefix: `data:image/png;base64,`

- [ ] **Step 4: Run captcha tests**

Run: `cd backend && mvn -Dtest=CaptchaServiceTest test`

Expected: PASS.

---

### Task 3: Captcha Auth Endpoint And Login Validation

**Files:**
- Modify: `backend/src/main/java/com/eduspark/agent/auth/AuthController.java`
- Modify: `backend/src/test/java/com/eduspark/agent/auth/AuthControllerIntegrationTest.java`

- [ ] **Step 1: Add integration tests**

Add tests:

- `captchaEndpointReturnsChallenge`
- `loginRejectsMissingCaptcha`
- `loginRejectsWrongCaptcha`
- `loginAcceptsCorrectCaptcha`
- `captchaCannotBeReused`

- [ ] **Step 2: Add endpoint**

```java
@GetMapping("/captcha")
public CaptchaChallenge captcha() {
  return captchaService.createChallenge();
}
```

- [ ] **Step 3: Verify before register/login**

Before `userService.register` and `userService.authenticate`, call:

```java
captchaService.verifyOrThrow(request.captchaId(), request.captchaCode());
```

Throw `ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid or expired captcha")` on failure.

- [ ] **Step 4: Run auth tests**

Run: `cd backend && mvn -Dtest=AuthControllerIntegrationTest,CaptchaServiceTest test`

Expected: PASS.

---

### Task 4: Frontend Captcha API And Auth Flow

**Files:**
- Modify: `frontend/src/api/taskApi.ts`
- Modify: `frontend/src/features/task/TaskConsolePage.tsx`
- Modify: `frontend/src/features/task/components/AuthPanel.tsx`
- Test: `frontend/src/features/task/TaskConsolePage.test.tsx`

- [ ] **Step 1: Add API types**

```ts
export interface CaptchaChallenge {
  captchaId: string;
  imageBase64: string;
  expiresInSeconds: number;
}
```

- [ ] **Step 2: Add API functions**

```ts
getCaptcha(): Promise<CaptchaChallenge>
login(username: string, password: string, captchaId: string, captchaCode: string): Promise<AuthSession>
register(username: string, password: string, captchaId: string, captchaCode: string): Promise<AuthSession>
```

- [ ] **Step 3: Add captcha state**

In `TaskConsolePage`:

```ts
const [captcha, setCaptcha] = useState<CaptchaChallenge | null>(null);
const [captchaCode, setCaptchaCode] = useState("");
```

On unauthenticated mount, fetch captcha. On auth failure, refresh captcha and clear captchaCode.

- [ ] **Step 4: Add captcha UI**

In `AuthPanel`, show image, input, refresh button. Disable submit unless username, password, captcha, and captchaCode exist.

- [ ] **Step 5: Run frontend auth tests**

Run: `cd frontend && node_modules\.bin\vitest.cmd run`

Expected: PASS.

---

### Task 5: Theme System Foundation

**Files:**
- Create: `frontend/src/features/task/components/ThemeSwitcher.tsx`
- Modify: `frontend/src/features/task/TaskConsolePage.tsx`
- Modify: `frontend/src/features/task/components/WorkspaceShell.tsx`
- Modify: `frontend/src/styles/globals.css`
- Test: `frontend/src/features/task/TaskConsolePage.test.tsx`

- [ ] **Step 1: Add theme type**

```ts
type ThemeMode = "light" | "dark" | "energy";
```

- [ ] **Step 2: Add CSS variables**

Define:

- `--app-bg`
- `--panel-bg`
- `--panel-border`
- `--text-main`
- `--text-muted`
- `--accent-blue`
- `--accent-green`
- `--accent-yellow`
- `--accent-red`

Apply via `[data-theme="light"]`, `[data-theme="dark"]`, `[data-theme="energy"]`.

- [ ] **Step 3: Persist theme**

Use `localStorage` key `eduspark.theme`. Default to `energy`.

- [ ] **Step 4: Add switcher**

`ThemeSwitcher` renders three buttons: `浅色`, `深色`, `能量`.

- [ ] **Step 5: Test persistence**

Click `深色`, assert `localStorage.getItem("eduspark.theme") === "dark"` and shell has `data-theme="dark"`.

---

### Task 6: Liquid Glass Visual Upgrade

**Files:**
- Modify: `frontend/src/styles/globals.css`
- Modify: `frontend/src/features/task/components/AuthPanel.tsx`
- Modify: `frontend/src/features/task/components/WorkspaceShell.tsx`
- Modify: `frontend/src/features/task/components/PromptComposer.tsx`
- Modify: `frontend/src/features/task/components/WorksheetPanel.tsx`
- Modify: `frontend/src/features/task/components/DocumentCenterPanel.tsx`
- Modify: `frontend/src/features/task/components/HistoryPanel.tsx`
- Modify: `frontend/src/features/task/components/KnowledgeGraphPanel.tsx`

- [ ] **Step 1: Add liquid utility classes**

Create classes:

- `.liquid-shell`
- `.liquid-panel`
- `.liquid-highlight`
- `.energy-ribbon`
- `.state-pulse`

Use CSS only. No animation dependency.

- [ ] **Step 2: Update AuthPanel**

Make first viewport visually stronger:

- larger brand mark
- translucent panel
- captcha row
- dynamic energy ribbon background
- no marketing-only extra page

- [ ] **Step 3: Update workspace panels**

Replace old `glass-panel` usages with `liquid-panel` where appropriate. Keep 8px radius. Do not nest visual cards inside cards.

- [ ] **Step 4: Run responsive check**

Open `http://localhost:5173/` manually after build/dev run and check:

- login form no overlap
- buttons do not overflow
- right column remains readable on narrow viewport
- theme switcher does not wrap awkwardly

---

### Task 7: Progress And Motion Feedback

**Files:**
- Create: `frontend/src/features/task/components/ProgressRail.tsx`
- Modify: `frontend/src/features/task/components/TaskTimeline.tsx`
- Modify: `frontend/src/features/task/components/PromptComposer.tsx`
- Modify: `frontend/src/features/task/components/WorksheetPanel.tsx`
- Modify: `frontend/src/features/task/TaskConsolePage.tsx`
- Test: `frontend/src/features/task/TaskConsolePage.test.tsx`

- [ ] **Step 1: Add progress component**

Props:

```ts
interface ProgressRailProps {
  label: string;
  value: number;
  tone: "blue" | "green" | "yellow" | "red";
}
```

- [ ] **Step 2: Map task status to progress**

Use:

- `PENDING`: 8
- `PLANNING`: 25
- `EXECUTING`: 55
- `REVIEWING`: 78
- `COMPLETED`: 100
- `FAILED`: 100 with red tone

- [ ] **Step 3: Add upload/generation progress**

First version is state-based, not byte-accurate:

- selecting files: 10
- uploading: 35
- creating worksheet/task: 60
- fetching result: 85
- done: 100

- [ ] **Step 4: Test progress rendering**

After task creation and fake SSE completed event, assert progress text reaches `100%`.

---

### Task 8: Phase 6 Full Verification

**Files:**
- Modify: `contracts/task.contract.ts`
- Modify: `docs/api.md`

- [ ] **Step 1: Update contracts**

Add:

- `CaptchaChallenge`
- `AuthRequest.captchaId`
- `AuthRequest.captchaCode`
- `GET /api/auth/captcha`

- [ ] **Step 2: Run backend package**

Run: `cd backend && mvn package`

Expected: `BUILD SUCCESS`.

- [ ] **Step 3: Run frontend validation**

Run:

```powershell
cd frontend
node_modules\.bin\vitest.cmd run
node_modules\.bin\tsc.cmd -b
node_modules\.bin\vite.cmd build
```

Expected: all PASS.

- [ ] **Step 4: Manual smoke test**

Run backend and frontend. Verify:

- captcha loads on login page
- wrong captcha blocks login
- correct captcha logs in
- theme switching persists after refresh
- task progress reaches completed
- mobile width has no overlapping text

