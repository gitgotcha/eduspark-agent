# EduSpark 前端全量重构 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将 EduSpark 前端重构为 GPT 式克制专业感为主、带少量液态艺术感的完整产品界面，覆盖欢迎页、工作台、练习页、历史页和个人中心，并加入柔和渐变主题切换。

**Architecture:** 保留现有 React + Tailwind + lucide-react 技术栈，不引入新 UI 框架。以 `TaskConsolePage` 作为会话协调层，逐步拆出页面级组件和共享视觉组件；主题、页头、简介、玻璃面板、进度反馈统一沉淀到一组可复用原子组件中。页面之间不改后端 API，只改前端信息架构、布局和视觉交互。

**Tech Stack:** React 18, TypeScript, Tailwind CSS, lucide-react, Vitest, Testing Library, Vite

---

## File Structure

### Modify

- `frontend/src/features/task/TaskConsolePage.tsx` - 页面协调层，负责 session、theme、page view state、页面切换与共享数据分发。
- `frontend/src/features/task/components/AuthPanel.tsx` - 重做欢迎 / 登录页布局、简介、验证码和主题入口。
- `frontend/src/features/task/components/WorkspaceShell.tsx` - 重做工作台壳层、顶部栏、主题切换和页面导航入口。
- `frontend/src/features/task/components/PromptComposer.tsx` - 调整为更像 AI composer 的任务输入区。
- `frontend/src/features/task/components/WorksheetPanel.tsx` - 拆分为练习工作区主视图，强化做题 / 解析 / 错题分区。
- `frontend/src/features/task/components/HistoryPanel.tsx` - 作为历史页或工作台历史区的基础组件重构。
- `frontend/src/features/task/components/DocumentCenterPanel.tsx` - 调整为更精致的资料卡片区和空状态。
- `frontend/src/features/task/components/KnowledgeGraphPanel.tsx` - 调整为更轻量的图谱展示区域。
- `frontend/src/features/task/components/TaskTimeline.tsx` - 保持可折叠日志流，但统一到新视觉系统。
- `frontend/src/features/task/components/ResultPanel.tsx` - 统一最终结果展示样式。
- `frontend/src/features/task/components/ArtifactGrid.tsx` - 统一产物展示样式。
- `frontend/src/features/task/TaskUiTypes.ts` - 如有需要补充页面视图、主题与展示状态类型。
- `frontend/src/styles/globals.css` - 统一背景、玻璃面板、渐变主题、动效和页面简介相关的全局样式。
- `frontend/src/features/task/TaskConsolePage.test.tsx` - 更新测试覆盖新页面切换、主题切换、简介展示和回归行为。

### Create

- `frontend/src/features/task/components/PageIntro.tsx` - 统一的页面简介组件。
- `frontend/src/features/task/components/ThemeToggle.tsx` - 统一的主题切换组件，支持液态渐变过渡入口。
- `frontend/src/features/task/components/GlassPanel.tsx` - 统一玻璃面板容器。
- `frontend/src/features/task/components/EnergyProgress.tsx` - 统一进度条 / 状态条。
- `frontend/src/features/task/pages/AuthPage.tsx` - 独立欢迎 / 登录页面。
- `frontend/src/features/task/pages/WorkspacePage.tsx` - 独立工作台首页。
- `frontend/src/features/task/pages/WorksheetStudioPage.tsx` - 独立练习卷工作区。
- `frontend/src/features/task/pages/HistoryPage.tsx` - 独立历史与回放页。
- `frontend/src/features/task/pages/ProfilePage.tsx` - 独立个人中心 / 设置页。

---

### Task 1: 建立全局页面骨架与共享视觉原子组件

**Files:**
- Create: `frontend/src/features/task/components/PageIntro.tsx`
- Create: `frontend/src/features/task/components/ThemeToggle.tsx`
- Create: `frontend/src/features/task/components/GlassPanel.tsx`
- Create: `frontend/src/features/task/components/EnergyProgress.tsx`
- Modify: `frontend/src/styles/globals.css`
- Modify: `frontend/src/features/task/TaskUiTypes.ts`

- [ ] **Step 1: Write the failing test**

Add a focused test in `frontend/src/features/task/TaskConsolePage.test.tsx` that mounts a page shell and verifies:
1. 页面顶部存在简介文本。
2. `GlassPanel` 应用统一边框与玻璃背景。
3. 主题切换按钮可见。
4. 进度条组件在 loading 状态下能渲染。

- [ ] **Step 2: Run test to verify it fails**

Run from `frontend/`: `npm exec vitest run src/features/task/TaskConsolePage.test.tsx`
Expected: FAIL because the shared page primitives do not exist yet.

- [ ] **Step 3: Write minimal implementation**

Implement the four shared components and global styles:
- `PageIntro` receives `title` and `description`.
- `ThemeToggle` receives current theme and callback.
- `GlassPanel` is a thin wrapper around the existing card style.
- `EnergyProgress` renders a slim animated progress bar with `aria-label`.

Wire the new gradient theme tokens into `globals.css`:
- `html[data-theme="aurora"]`
- `html[data-theme="sunrise"]`
- `energy-shell`
- `glass-panel`

- [ ] **Step 4: Run test to verify it passes**

Run from `frontend/`: `npm exec vitest run src/features/task/TaskConsolePage.test.tsx`
Expected: PASS.

---

### Task 2: 拆出页面协调层与内部分区导航

**Files:**
- Modify: `frontend/src/features/task/TaskConsolePage.tsx`
- Modify: `frontend/src/features/task/components/WorkspaceShell.tsx`
- Create: `frontend/src/features/task/pages/WorkspacePage.tsx`
- Create: `frontend/src/features/task/pages/WorksheetStudioPage.tsx`
- Create: `frontend/src/features/task/pages/HistoryPage.tsx`
- Create: `frontend/src/features/task/pages/ProfilePage.tsx`

- [ ] **Step 1: Write the failing test**

Add tests that verify the authenticated app can switch between:
1. 工作台首页
2. 练习卷工作区
3. 历史与回放页
4. 个人中心 / 设置页

The tests should assert the visible page title changes without losing the session.

- [ ] **Step 2: Run test to verify it fails**

Run from `frontend/`: `npm exec vitest run src/features/task/TaskConsolePage.test.tsx`
Expected: FAIL because page-level views and the navigation state do not exist yet.

- [ ] **Step 3: Write minimal implementation**

Add a page view state in `TaskConsolePage` such as:
- `workspace`
- `worksheet`
- `history`
- `profile`

Render the correct page component based on the active view, while keeping session, theme, and shared data in the coordinator layer.

- [ ] **Step 4: Run test to verify it passes**

Run from `frontend/`: `npm exec vitest run src/features/task/TaskConsolePage.test.tsx`
Expected: PASS.

---

### Task 3: 重做登录 / 欢迎页

**Files:**
- Modify: `frontend/src/features/task/components/AuthPanel.tsx`
- Create: `frontend/src/features/task/pages/AuthPage.tsx`
- Modify: `frontend/src/features/task/TaskConsolePage.test.tsx`

- [ ] **Step 1: Write the failing test**

Add a test that verifies the welcome page shows:
1. 短简介
2. 登录 / 注册切换
3. 图形验证码
4. 主题切换按钮

The test should also verify that an incorrect captcha blocks submission.

- [ ] **Step 2: Run test to verify it fails**

Run from `frontend/`: `npm exec vitest run src/features/task/TaskConsolePage.test.tsx`
Expected: FAIL because the redesigned welcome page behavior is not fully implemented yet.

- [ ] **Step 3: Write minimal implementation**

Refactor `AuthPanel` into a branded welcome page:
- 左侧品牌叙事区
- 右侧登录卡片
- 简短页面简介
- 图形验证码
- 温和错误提示
- 主题切换入口

Keep the existing auth API contract unchanged.

- [ ] **Step 4: Run test to verify it passes**

Run from `frontend/`: `npm exec vitest run src/features/task/TaskConsolePage.test.tsx`
Expected: PASS.

---

### Task 4: 重做工作台首页

**Files:**
- Modify: `frontend/src/features/task/components/PromptComposer.tsx`
- Modify: `frontend/src/features/task/components/DocumentCenterPanel.tsx`
- Modify: `frontend/src/features/task/components/TaskTimeline.tsx`
- Modify: `frontend/src/features/task/components/ResultPanel.tsx`
- Modify: `frontend/src/features/task/components/ArtifactGrid.tsx`
- Modify: `frontend/src/features/task/components/KnowledgeGraphPanel.tsx`
- Create: `frontend/src/features/task/pages/WorkspacePage.tsx`

- [ ] **Step 1: Write the failing test**

Add a workspace test that verifies the authenticated首页 renders:
1. 任务输入区
2. 资料中心
3. 任务时间线
4. 结果区
5. 产物区
6. 知识图谱入口

The test should also check that the page includes a one-line intro and that the timeline groups log lines by section.

- [ ] **Step 2: Run test to verify it fails**

Run from `frontend/`: `npm exec vitest run src/features/task/TaskConsolePage.test.tsx`
Expected: FAIL because the new page composition is not yet in place.

- [ ] **Step 3: Write minimal implementation**

Adjust the workspace layout to a two-column dashboard:
- left: composer, worksheet teaser, final answer
- right: timeline, history, document center, knowledge graph, artifacts

Keep the current data fetching and session behavior. Only move layout and presentation.

- [ ] **Step 4: Run test to verify it passes**

Run from `frontend/`: `npm exec vitest run src/features/task/TaskConsolePage.test.tsx`
Expected: PASS.

---

### Task 5: 重做练习卷工作区

**Files:**
- Modify: `frontend/src/features/task/components/WorksheetPanel.tsx`
- Create: `frontend/src/features/task/pages/WorksheetStudioPage.tsx`
- Modify: `frontend/src/features/task/TaskConsolePage.test.tsx`

- [ ] **Step 1: Write the failing test**

Add a test that verifies the worksheet area contains:
1. 页面简介
2. `做题 / 解析` 双标签页
3. `错题` 独立页签
4. `错题再练` 按钮
5. `导出 Word` 按钮

The test should also verify that switching tabs does not clear already entered answers.

- [ ] **Step 2: Run test to verify it fails**

Run: `npm exec vitest run frontend/src/features/task/TaskConsolePage.test.tsx`
Expected: FAIL until the studio layout is split cleanly.

- [ ] **Step 3: Write minimal implementation**

Refactor the worksheet area into a dedicated studio view:
- top intro
- configuration panel
- question preview
- answer / explanation / wrong-question tab group
- export and retry actions

Preserve all current API calls and wrong-question behavior.

- [ ] **Step 4: Run test to verify it passes**

Run: `npm exec vitest run frontend/src/features/task/TaskConsolePage.test.tsx`
Expected: PASS.

---

### Task 6: 重做历史页与个人中心

**Files:**
- Modify: `frontend/src/features/task/components/HistoryPanel.tsx`
- Create: `frontend/src/features/task/pages/HistoryPage.tsx`
- Create: `frontend/src/features/task/pages/ProfilePage.tsx`
- Modify: `frontend/src/features/task/TaskConsolePage.test.tsx`

- [ ] **Step 1: Write the failing test**

Add tests that verify:
1. 历史页显示任务历史和回放入口
2. 历史日志可折叠展开
3. 个人中心可切换主题并清理本地状态

- [ ] **Step 2: Run test to verify it fails**

Run: `npm exec vitest run frontend/src/features/task/TaskConsolePage.test.tsx`
Expected: FAIL because the page-level wrappers are not present yet.

- [ ] **Step 3: Write minimal implementation**

Keep `HistoryPanel` as the data view, but wrap it in a dedicated `HistoryPage` with an intro, filter strip, and a replay area. Add a lightweight `ProfilePage` for theme and session preferences.

- [ ] **Step 4: Run test to verify it passes**

Run: `npm exec vitest run frontend/src/features/task/TaskConsolePage.test.tsx`
Expected: PASS.

---

### Task 7: 统一渐变主题、动效与视觉回归

**Files:**
- Modify: `frontend/src/styles/globals.css`
- Modify: `frontend/src/features/task/TaskConsolePage.tsx`
- Modify: `frontend/src/features/task/TaskConsolePage.test.tsx`

- [ ] **Step 1: Write the failing test**

Add a test that checks:
1. `aurora` 和 `sunrise` 主题都能切换
2. 主题值写回 `localStorage`
3. 切换时页面主容器类名保持稳定

- [ ] **Step 2: Run test to verify it fails**

Run: `npm exec vitest run frontend/src/features/task/TaskConsolePage.test.tsx`
Expected: FAIL if the theme transition behavior is incomplete.

- [ ] **Step 3: Write minimal implementation**

Finalize the theme system:
- 柔和液态渐变背景
- 主题切换过渡
- 全局统一阴影与玻璃质感
- 页面简介、标题、空状态、错误态风格统一

- [ ] **Step 4: Run test to verify it passes**

Run: `npm exec vitest run frontend/src/features/task/TaskConsolePage.test.tsx`
Expected: PASS.

---

## Verification

After all tasks:

- From `frontend/`, run `npm exec vitest run`
- From `frontend/`, run `npx tsc -b`
- From `frontend/`, run `npm exec vite build`

Expected:

- All tests pass
- TypeScript build passes
- Vite production build passes
- Login, workspace, worksheet, history, and profile pages all render with the new visual system
