# Auth API Documents Workspace Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Fix production worksheet white-screen/API path issues, add password change and password length validation, and move document management into a first-level Document Center view with independent async refresh.

**Architecture:** Normalize frontend API URL joining so `/api` build bases do not produce `/api/api`; keep Nginx compatibility for old bundles. Add a user-scoped password change endpoint in the existing auth/user services. Promote document management from the workspace sidebar into a dedicated route-level React view while keeping workspace data refreshes independent.

**Tech Stack:** React, Vite, Vitest, Spring Boot, MyBatis-Plus, Spring Security, Nginx.

---

### Task 1: Frontend API URL Normalization

**Files:**
- Modify: `frontend/src/api/taskApi.ts`
- Modify: `frontend/src/api/taskApi.test.ts`
- Modify: `frontend/nginx.conf`

- [ ] Add Vitest coverage proving `VITE_API_BASE_URL=/api` creates `/api/auth/login`, not `/api/api/auth/login`.
- [ ] Add a small `apiUrl(path)` helper and route all `fetch`/`EventSource` URLs through it.
- [ ] Keep `/api/api/` Nginx compatibility rewrite for stale production bundles.
- [ ] Run `npm test -- --run taskApi.test.ts`.

### Task 2: Password Policy And Change Password API

**Files:**
- Create: `backend/src/main/java/com/eduspark/agent/auth/ChangePasswordRequest.java`
- Modify: `backend/src/main/java/com/eduspark/agent/auth/AuthController.java`
- Modify: `backend/src/main/java/com/eduspark/agent/user/UserService.java`
- Modify: `backend/src/main/java/com/eduspark/agent/user/UserMapper.java`
- Modify: `frontend/src/api/taskApi.ts`
- Modify: `frontend/src/features/task/TaskConsolePage.tsx`
- Modify: `frontend/src/features/task/pages/ProfilePage.tsx`
- Modify: frontend auth tests where needed

- [ ] Add backend test/compile coverage for min 6 password validation and old-password verification.
- [ ] Implement `POST /api/users/{userId}/auth/change-password`.
- [ ] Add frontend API function and profile form.
- [ ] Block login/register/change-password submissions shorter than 6 characters on the frontend.
- [ ] Run focused backend auth tests and frontend tests.

### Task 3: Document Center First-Level View

**Files:**
- Create: `frontend/src/features/task/pages/DocumentCenterPage.tsx`
- Modify: `frontend/src/features/task/TaskUiTypes.ts`
- Modify: `frontend/src/features/task/components/WorkspaceShell.tsx`
- Modify: `frontend/src/features/task/TaskConsolePage.tsx`

- [ ] Add `documents` as a top-level workspace view.
- [ ] Move `DocumentCenterPanel` rendering from workspace sidebar into `DocumentCenterPage`.
- [ ] Keep workspace homepage lightweight and independent from document/history/graph request completion.
- [ ] Run focused frontend page tests and `npm run build`.

### Task 4: Verification

**Files:**
- No production edits unless verification reveals a regression.

- [ ] Run `npm test -- --run taskApi.test.ts TaskConsolePage.test.tsx`.
- [ ] Run `npm run build`.
- [ ] Run focused Maven tests for auth/user changes.
- [ ] Summarize any local-only limitations.
