import { ChangeEvent, FormEvent, useEffect, useRef, useState } from "react";
import { LogOut } from "lucide-react";
import {
  AuthSession,
  changePassword,
  deleteDocument,
  EduDocument,
  createWorksheet,
  createTask,
  createKnowledgeGraph,
  exportWorksheetWord,
  deleteKnowledgeGraph,
  getWorksheet,
  getTask,
  getTaskArtifacts,
  getKnowledgeGraph,
  KnowledgeGraphResponse,
  KnowledgeGraphRecord,
  login,
  listDocuments,
  listKnowledgeGraphs,
  listTasks,
  listWrongQuestions,
  listWorksheetAttempts,
  listWorksheetWrongQuestions,
  register,
  reindexDocument,
  resolveWrongQuestion,
  retryWrongQuestions,
  submitWorksheetAttempt,
  subscribeTaskStream,
  TaskArtifact,
  TaskDetail,
  TaskStreamPayload,
  uploadDocument,
  WorksheetAnswerInput,
  WorksheetAttemptResponse,
  WorksheetDetail,
  WrongQuestion
} from "../../api/taskApi";
import { ArtifactGrid } from "./components/ArtifactGrid";
import { GlassPanel } from "./components/GlassPanel";
import { HistoryPanel } from "./components/HistoryPanel";
import { KnowledgeGraphPanel } from "./components/KnowledgeGraphPanel";
import { PromptComposer } from "./components/PromptComposer";
import { ResultPanel } from "./components/ResultPanel";
import { TaskTimeline } from "./components/TaskTimeline";
import { WorkspaceShell } from "./components/WorkspaceShell";
import { WorksheetPanel } from "./components/WorksheetPanel";
import { ConsoleLine, WorkspaceView } from "./TaskUiTypes";
import { AuthPage } from "./pages/AuthPage";
import { DocumentCenterPage } from "./pages/DocumentCenterPage";
import { KnowledgeGraphPage } from "./pages/KnowledgeGraphPage";
import { HistoryPage } from "./pages/HistoryPage";
import { ProfilePage } from "./pages/ProfilePage";
import { WrongQuestionsPage } from "./pages/WrongQuestionsPage";
import { WorkspacePage } from "./pages/WorkspacePage";
import { WorksheetStudioPage } from "./pages/WorksheetStudioPage";

const AUTH_STORAGE_KEY = "eduspark.auth";
const THEME_STORAGE_KEY = "eduspark.theme";

export function TaskConsolePage() {
  const [session, setSession] = useState<AuthSession | null>(() => readStoredSession());
  const [theme, setTheme] = useState<"aurora" | "sunrise">(() => readStoredTheme());
  const [activeView, setActiveView] = useState<WorkspaceView>("workspace");
  const [authMode, setAuthMode] = useState<"login" | "register">("login");
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [oldPassword, setOldPassword] = useState("");
  const [newPassword, setNewPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [passwordChangeMessage, setPasswordChangeMessage] = useState<string | null>(null);
  const [passwordChangeError, setPasswordChangeError] = useState<string | null>(null);
  const [isChangingPassword, setIsChangingPassword] = useState(false);
  const [captchaCode, setCaptchaCode] = useState(() => createCaptchaCode());
  const [captchaInput, setCaptchaInput] = useState("");
  const [authError, setAuthError] = useState<string | null>(null);
  const [isAuthenticating, setIsAuthenticating] = useState(false);
  const [instruction, setInstruction] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [activeTaskId, setActiveTaskId] = useState<string | null>(null);
  const [finalAnswer, setFinalAnswer] = useState<string | null>(null);
  const [artifacts, setArtifacts] = useState<TaskArtifact[]>([]);
  const [selectedFiles, setSelectedFiles] = useState<File[]>([]);
  const [documents, setDocuments] = useState<EduDocument[]>([]);
  const [documentsError, setDocumentsError] = useState<string | null>(null);
  const [isLoadingDocuments, setIsLoadingDocuments] = useState(false);
  const [historyTasks, setHistoryTasks] = useState<TaskDetail[]>([]);
  const [historyError, setHistoryError] = useState<string | null>(null);
  const [isLoadingHistory, setIsLoadingHistory] = useState(false);
  const [knowledgeGraph, setKnowledgeGraph] = useState<KnowledgeGraphResponse | null>(null);
  const [knowledgeGraphRecords, setKnowledgeGraphRecords] = useState<KnowledgeGraphRecord[]>([]);
  const [knowledgeGraphRecordTitle, setKnowledgeGraphRecordTitle] = useState("当前图谱快照");
  const [selectedKnowledgeGraphRecordId, setSelectedKnowledgeGraphRecordId] = useState<string | null>(null);
  const [knowledgeGraphError, setKnowledgeGraphError] = useState<string | null>(null);
  const [knowledgeGraphRecordsError, setKnowledgeGraphRecordsError] = useState<string | null>(null);
  const [isLoadingKnowledgeGraph, setIsLoadingKnowledgeGraph] = useState(false);
  const [isLoadingKnowledgeGraphRecords, setIsLoadingKnowledgeGraphRecords] = useState(false);
  const [lines, setLines] = useState<ConsoleLine[]>([]);
  const [worksheetTitle, setWorksheetTitle] = useState("课堂资料巩固练习");
  const [worksheetGradeLevel, setWorksheetGradeLevel] = useState("五年级");
  const [worksheetDifficulty, setWorksheetDifficulty] = useState("中等");
  const [worksheetQuestionCount, setWorksheetQuestionCount] = useState(8);
  const [worksheetQuestionTypes, setWorksheetQuestionTypes] = useState(["选择题", "判断题"]);
  const [worksheetIncludeExplanation, setWorksheetIncludeExplanation] = useState(true);
  const [worksheet, setWorksheet] = useState<WorksheetDetail | null>(null);
  const [worksheetAttempts, setWorksheetAttempts] = useState<WorksheetAttemptResponse[]>([]);
  const [wrongQuestions, setWrongQuestions] = useState<WrongQuestion[]>([]);
  const [wrongQuestionBank, setWrongQuestionBank] = useState<WrongQuestion[]>([]);
  const [isLoadingWrongQuestions, setIsLoadingWrongQuestions] = useState(false);
  const [isLoadingWrongQuestionBank, setIsLoadingWrongQuestionBank] = useState(false);
  const [wrongQuestionError, setWrongQuestionError] = useState<string | null>(null);
  const [wrongQuestionBankError, setWrongQuestionBankError] = useState<string | null>(null);
  const [wrongQuestionRetryError, setWrongQuestionRetryError] = useState<string | null>(null);
  const [wrongQuestionRetryTitle, setWrongQuestionRetryTitle] = useState("错题再练");
  const [worksheetError, setWorksheetError] = useState<string | null>(null);
  const [isGeneratingWorksheet, setIsGeneratingWorksheet] = useState(false);
  const [isExportingWorksheet, setIsExportingWorksheet] = useState(false);
  const [isSubmittingAttempt, setIsSubmittingAttempt] = useState(false);
  const [isRetryingWrongQuestions, setIsRetryingWrongQuestions] = useState(false);
  const unsubscribeRef = useRef<(() => void) | null>(null);

  useEffect(() => {
    document.documentElement.dataset.theme = theme;
    localStorage.setItem(THEME_STORAGE_KEY, theme);
  }, [theme]);

  useEffect(() => {
    if (session) {
      startWorkspaceRefresh(session);
    }
  }, [session]);

  useEffect(() => {
    if (session && worksheet) {
      void refreshWrongQuestions(session, worksheet.id);
    } else {
      setWrongQuestions([]);
      setWrongQuestionError(null);
    }
  }, [session, worksheet?.id]);

  useEffect(() => {
    if (session && activeView === "wrong-questions") {
      void refreshWrongQuestionBank(session);
    }
  }, [session, activeView]);

  useEffect(() => {
    if (knowledgeGraphRecords.length === 0) {
      setSelectedKnowledgeGraphRecordId(null);
      return;
    }

    if (
      selectedKnowledgeGraphRecordId === null ||
      !knowledgeGraphRecords.some((record) => record.id === selectedKnowledgeGraphRecordId)
    ) {
      setSelectedKnowledgeGraphRecordId(knowledgeGraphRecords[0].id);
    }
  }, [knowledgeGraphRecords, selectedKnowledgeGraphRecordId]);

  async function handleAuthSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const trimmedUsername = username.trim();
    if (!trimmedUsername || !password) {
      return;
    }
    if (password.length < 6) {
      setAuthError("密码至少需要 6 位。");
      return;
    }
    if (captchaInput.trim().toUpperCase() !== captchaCode) {
      setAuthError("验证码不正确，请重试。");
      setCaptchaCode(createCaptchaCode());
      setCaptchaInput("");
      return;
    }

    setIsAuthenticating(true);
    setAuthError(null);
    try {
      const nextSession =
        authMode === "login"
          ? await login(trimmedUsername, password)
          : await register(trimmedUsername, password);
      localStorage.setItem(AUTH_STORAGE_KEY, JSON.stringify(nextSession));
      setSession(nextSession);
      setActiveView("workspace");
      startWorkspaceRefresh(nextSession);
      setPassword("");
      setCaptchaCode(createCaptchaCode());
      setCaptchaInput("");
    } catch (error) {
      setAuthError(error instanceof Error ? error.message : "认证失败");
      setCaptchaCode(createCaptchaCode());
      setCaptchaInput("");
    } finally {
      setIsAuthenticating(false);
    }
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!session) {
      return;
    }
    const trimmedInstruction = instruction.trim();
    if (!trimmedInstruction) {
      return;
    }

    setIsSubmitting(true);
    setFinalAnswer(null);
    setArtifacts([]);
    setLines([{ id: crypto.randomUUID(), text: "正在创建任务...", tone: "info", group: "任务" }]);

    try {
      unsubscribeRef.current?.();
      const uploadedDocuments = await uploadSelectedDocuments(session);
      mergeDocuments(uploadedDocuments);
      void refreshKnowledgeGraph(session);
      uploadedDocuments.forEach((document) => appendLine(`已上传 ${document.fileName}`, "success", "上传资料"));

      const task = await createTask(
        session,
        trimmedInstruction,
        uploadedDocuments.map((document) => document.id)
      );
      setActiveTaskId(task.taskId);
      appendLine(`任务 ${task.taskId} 已创建，状态 ${task.status}`, "success", "任务");
      void refreshHistory(session);
      unsubscribeRef.current = subscribeTaskStream(
        session,
        task.taskId,
        (payload) => handleStreamPayload(session, task.taskId, payload),
        () => appendLine("实时连接已中断", "error")
      );
    } catch (error) {
      appendLine(error instanceof Error ? error.message : "Create task failed", "error");
    } finally {
      setIsSubmitting(false);
    }
  }

  async function handleWorksheetSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!session) {
      return;
    }

    const title = worksheetTitle.trim();
    const gradeLevel = worksheetGradeLevel.trim();
    if (!title || !gradeLevel || selectedFiles.length === 0 || worksheetQuestionTypes.length === 0) {
      setWorksheetError("请填写标题、年级，并至少选择一份参考资料和一种题型。");
      return;
    }

    setIsGeneratingWorksheet(true);
    setWorksheetError(null);
    setWorksheet(null);
    setWorksheetAttempts([]);
    appendLine("正在生成练习卷...", "info", "练习卷");

    try {
      const uploadedDocuments = await uploadSelectedDocuments(session);
      mergeDocuments(uploadedDocuments);
      void refreshKnowledgeGraph(session);
      uploadedDocuments.forEach((document) =>
        appendLine(`练习卷资料已上传 ${document.fileName}`, "success", "练习卷")
      );
      const created = await createWorksheet(session, {
        title,
        documentIds: uploadedDocuments.map((document) => document.id),
        questionCount: Math.min(Math.max(worksheetQuestionCount, 1), 30),
        gradeLevel,
        difficulty: worksheetDifficulty,
        questionTypes: worksheetQuestionTypes,
        includeExplanation: worksheetIncludeExplanation
      });
      const detail = await getWorksheet(session, created.worksheetId);
      setWorksheet(detail);
      setWorksheetAttempts([]);
      setWrongQuestions([]);
      appendLine(`练习卷 ${created.worksheetId} 已生成`, "success", "练习卷");
    } catch (error) {
      const message = error instanceof Error ? error.message : "生成练习卷失败";
      setWorksheetError(message);
      appendLine(message, "error");
    } finally {
      setIsGeneratingWorksheet(false);
    }
  }

  async function handleWorksheetExport() {
    if (!session || !worksheet) {
      return;
    }

    setIsExportingWorksheet(true);
    setWorksheetError(null);
    try {
      const blob = await exportWorksheetWord(session, worksheet.id);
      downloadBlob(blob, `${worksheet.title}.docx`);
      appendLine("练习卷 Word 已开始下载", "success", "练习卷");
    } catch (error) {
      const message = error instanceof Error ? error.message : "导出 Word 失败";
      setWorksheetError(message);
      appendLine(message, "error");
    } finally {
      setIsExportingWorksheet(false);
    }
  }

  async function handleWorksheetAttemptSubmit(answers: WorksheetAnswerInput[]) {
    if (!session || !worksheet) {
      return;
    }
    setIsSubmittingAttempt(true);
    setWorksheetError(null);
    try {
      const attempt = await submitWorksheetAttempt(session, worksheet.id, answers);
      setWorksheetAttempts((current) => [attempt, ...current]);
      void refreshWrongQuestions(session, worksheet.id);
      appendLine(`练习批改完成，得分 ${attempt.score}`, "success", "练习卷");
    } catch (error) {
      const message = error instanceof Error ? error.message : "提交练习答案失败";
      setWorksheetError(message);
      appendLine(message, "error");
    } finally {
      setIsSubmittingAttempt(false);
    }
  }

  async function handleLoadWorksheetAttempts() {
    if (!session || !worksheet) {
      return;
    }
    try {
      setWorksheetAttempts(await listWorksheetAttempts(session, worksheet.id));
    } catch (error) {
      setWorksheetError(error instanceof Error ? error.message : "加载练习记录失败");
    }
  }

  async function handleRetryWrongQuestions(wrongQuestionIds: string[], title: string) {
    if (!session) {
      return;
    }
    setIsRetryingWrongQuestions(true);
    setWrongQuestionError(null);
    setWrongQuestionRetryError(null);
    try {
      const created = await retryWrongQuestions(session, wrongQuestionIds, title);
      appendLine(`错题再练已创建 ${created.worksheetId}`, "success", "错题");
      setWorksheetError(null);
      setWorksheet(null);
      setWorksheetAttempts([]);
      setWrongQuestions([]);
      void refreshWrongQuestionBank(session);
      const detail = await getWorksheet(session, created.worksheetId);
      setWorksheet(detail);
      setActiveView("worksheet");
    } catch (error) {
      const message = error instanceof Error ? error.message : "错题再练失败";
      setWrongQuestionError(message);
      setWrongQuestionRetryError(message);
      appendLine(message, "error");
    } finally {
      setIsRetryingWrongQuestions(false);
    }
  }

  async function handleResolveWrongQuestion(wrongQuestionId: string) {
    if (!session) {
      return;
    }
    setWrongQuestionError(null);
    try {
      await resolveWrongQuestion(session, wrongQuestionId);
      if (worksheet) {
        void refreshWrongQuestions(session, worksheet.id);
      }
      setWrongQuestionBank((current) => current.filter((item) => item.id !== wrongQuestionId));
      void refreshWrongQuestionBank(session);
      appendLine("错题已标记为已解决", "success", "错题");
    } catch (error) {
      const message = error instanceof Error ? error.message : "标记错题失败";
      setWrongQuestionError(message);
    }
  }

  async function handleOpenHistoryTask(taskId: string) {
    if (!session) {
      return;
    }
    setActiveTaskId(taskId);
    setLines([{ id: crypto.randomUUID(), text: `正在打开历史任务 ${taskId}`, tone: "info", group: "历史" }]);
    try {
      const [task, taskArtifacts] = await Promise.all([
        getTask(session, taskId),
        getTaskArtifacts(session, taskId)
      ]);
      setFinalAnswer(task.finalAnswer ?? null);
      setArtifacts(taskArtifacts);
      appendLine(`历史任务已加载，状态 ${task.status}`, "success", "历史");
    } catch (error) {
      appendLine(error instanceof Error ? error.message : "加载历史任务失败", "error");
    }
  }

  function handleLogout() {
    unsubscribeRef.current?.();
    localStorage.removeItem(AUTH_STORAGE_KEY);
    setSession(null);
    setActiveView("workspace");
    setInstruction("");
    setActiveTaskId(null);
    setFinalAnswer(null);
    setArtifacts([]);
    setSelectedFiles([]);
    setDocuments([]);
    setDocumentsError(null);
    setHistoryTasks([]);
    setHistoryError(null);
    setKnowledgeGraph(null);
    setKnowledgeGraphRecords([]);
    setKnowledgeGraphRecordsError(null);
    setSelectedKnowledgeGraphRecordId(null);
    setKnowledgeGraphError(null);
    setWrongQuestions([]);
    setWrongQuestionBank([]);
    setWrongQuestionError(null);
    setWrongQuestionBankError(null);
    setWrongQuestionRetryError(null);
    setWrongQuestionRetryTitle("错题再练");
    setLines([]);
    setWorksheet(null);
    setWorksheetAttempts([]);
    setWorksheetError(null);
    setIsLoadingWrongQuestions(false);
    setIsRetryingWrongQuestions(false);
    setCaptchaInput("");
    setOldPassword("");
    setNewPassword("");
    setConfirmPassword("");
    setPasswordChangeMessage(null);
    setPasswordChangeError(null);
    setKnowledgeGraphRecordTitle("当前图谱快照");
  }

  async function handleChangePassword(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!session) {
      return;
    }
    if (oldPassword.length < 6 || newPassword.length < 6) {
      setPasswordChangeError("密码至少需要 6 位。");
      setPasswordChangeMessage(null);
      return;
    }
    if (newPassword !== confirmPassword) {
      setPasswordChangeError("两次输入的新密码不一致。");
      setPasswordChangeMessage(null);
      return;
    }

    setIsChangingPassword(true);
    setPasswordChangeError(null);
    setPasswordChangeMessage(null);
    try {
      await changePassword(session, oldPassword, newPassword);
      setOldPassword("");
      setNewPassword("");
      setConfirmPassword("");
      setPasswordChangeMessage("密码已更新，下次登录请使用新密码。");
    } catch (error) {
      setPasswordChangeError(error instanceof Error ? error.message : "修改密码失败");
    } finally {
      setIsChangingPassword(false);
    }
  }

  function handleFileChange(event: ChangeEvent<HTMLInputElement>) {
    setSelectedFiles(Array.from(event.target.files ?? []));
  }

  function handleStreamPayload(
    currentSession: AuthSession,
    taskId: string,
    payload: TaskStreamPayload
  ) {
    if (payload.message) {
      appendLine(
        payload.message,
        payload.stage === "COMPLETED" ? "success" : "info",
        payload.stage ?? payload.status ?? "任务"
      );
      if (payload.stage === "COMPLETED") {
        void loadTaskResult(currentSession, taskId);
      }
      return;
    }

    if (payload.status) {
      appendLine(
        `状态更新为 ${payload.status}`,
        payload.status === "COMPLETED" ? "success" : "info",
        payload.status
      );
      if (payload.status === "COMPLETED") {
        void loadTaskResult(currentSession, taskId);
      }
    }
  }

  async function loadTaskResult(currentSession: AuthSession, taskId: string) {
    try {
      const [task, taskArtifacts] = await Promise.all([
        getTask(currentSession, taskId),
        getTaskArtifacts(currentSession, taskId)
      ]);
      setFinalAnswer(task.finalAnswer ? formatJson(task.finalAnswer) : "No final answer returned.");
      setArtifacts(taskArtifacts);
    } catch (error) {
      appendLine(error instanceof Error ? error.message : "Load task result failed", "error", "任务");
    }
  }

  function appendLine(text: string, tone: ConsoleLine["tone"], group = "日志") {
    setLines((current) => [...current, { id: crypto.randomUUID(), text, tone, group }]);
  }

  function uploadSelectedDocuments(currentSession: AuthSession) {
    return Promise.all(selectedFiles.map((file) => uploadDocument(currentSession, file)));
  }

  async function refreshDocuments(currentSession: AuthSession) {
    setIsLoadingDocuments(true);
    setDocumentsError(null);
    try {
      setDocuments(await listDocuments(currentSession));
    } catch (error) {
      setDocumentsError(error instanceof Error ? error.message : "加载资料失败");
    } finally {
      setIsLoadingDocuments(false);
    }
  }

  async function refreshHistory(currentSession: AuthSession) {
    setIsLoadingHistory(true);
    setHistoryError(null);
    try {
      setHistoryTasks(await listTasks(currentSession, 20));
    } catch (error) {
      setHistoryError(error instanceof Error ? error.message : "加载历史任务失败");
    } finally {
      setIsLoadingHistory(false);
    }
  }

  async function refreshKnowledgeGraph(currentSession: AuthSession) {
    setIsLoadingKnowledgeGraph(true);
    setKnowledgeGraphError(null);
    try {
      setKnowledgeGraph(await getKnowledgeGraph(currentSession));
    } catch (error) {
      setKnowledgeGraphError(error instanceof Error ? error.message : "加载知识图谱失败");
    } finally {
      setIsLoadingKnowledgeGraph(false);
    }
  }

  async function refreshKnowledgeGraphRecords(currentSession: AuthSession) {
    setIsLoadingKnowledgeGraphRecords(true);
    setKnowledgeGraphRecordsError(null);
    try {
      setKnowledgeGraphRecords(await listKnowledgeGraphs(currentSession));
    } catch (error) {
      setKnowledgeGraphRecordsError(error instanceof Error ? error.message : "加载图谱记录失败");
    } finally {
      setIsLoadingKnowledgeGraphRecords(false);
    }
  }

  function startWorkspaceRefresh(currentSession: AuthSession) {
    void refreshDocuments(currentSession);
    void refreshHistory(currentSession);
    void refreshKnowledgeGraph(currentSession);
    void refreshKnowledgeGraphRecords(currentSession);
  }

  async function handleCreateKnowledgeGraphRecord() {
    if (!session) {
      return;
    }

    const documentIds = documents.map((document) => document.id);
    if (documentIds.length === 0) {
      setKnowledgeGraphRecordsError("请先上传资料，再保存图谱记录。");
      return;
    }

    setKnowledgeGraphRecordsError(null);
    try {
      const created = await createKnowledgeGraph(session, {
        title: knowledgeGraphRecordTitle.trim() || "当前图谱快照",
        documentIds
      });
      setSelectedKnowledgeGraphRecordId(created.graphId);
      void refreshKnowledgeGraphRecords(session);
      appendLine(`图谱记录已保存 ${created.graphId}`, "success", "图谱");
    } catch (error) {
      setKnowledgeGraphRecordsError(error instanceof Error ? error.message : "保存图谱记录失败");
    }
  }

  async function handleDeleteKnowledgeGraphRecord(graphId: string) {
    if (!session) {
      return;
    }

    setKnowledgeGraphRecordsError(null);
    try {
      await deleteKnowledgeGraph(session, graphId);
      setKnowledgeGraphRecords((current) => current.filter((record) => record.id !== graphId));
      if (selectedKnowledgeGraphRecordId === graphId) {
        setSelectedKnowledgeGraphRecordId(null);
      }
      void refreshKnowledgeGraphRecords(session);
      appendLine(`图谱记录已删除 ${graphId}`, "success", "图谱");
    } catch (error) {
      setKnowledgeGraphRecordsError(error instanceof Error ? error.message : "删除图谱记录失败");
    }
  }

  function handleClearKnowledgeGraphSelection() {
    setSelectedKnowledgeGraphRecordId(null);
  }

  async function refreshWrongQuestions(currentSession: AuthSession, worksheetId: string) {
    setIsLoadingWrongQuestions(true);
    setWrongQuestionError(null);
    try {
      setWrongQuestions(await listWorksheetWrongQuestions(currentSession, worksheetId));
    } catch (error) {
      setWrongQuestionError(error instanceof Error ? error.message : "加载错题失败");
    } finally {
      setIsLoadingWrongQuestions(false);
    }
  }

  async function refreshWrongQuestionBank(currentSession: AuthSession) {
    setIsLoadingWrongQuestionBank(true);
    setWrongQuestionBankError(null);
    try {
      setWrongQuestionBank(await listWrongQuestions(currentSession));
    } catch (error) {
      setWrongQuestionBankError(error instanceof Error ? error.message : "加载错题本失败");
    } finally {
      setIsLoadingWrongQuestionBank(false);
    }
  }

  function mergeDocuments(nextDocuments: EduDocument[]) {
    setDocuments((current) => {
      const byId = new Map(current.map((document) => [document.id, document]));
      nextDocuments.forEach((document) => byId.set(document.id, document));
      return Array.from(byId.values()).sort((left, right) =>
        right.createdAt.localeCompare(left.createdAt)
      );
    });
  }

  async function handleDeleteDocument(documentId: string) {
    if (!session) {
      return;
    }
    setDocumentsError(null);
    try {
      await deleteDocument(session, documentId);
      setDocuments((current) => current.filter((document) => document.id !== documentId));
      void refreshDocuments(session);
      void refreshKnowledgeGraph(session);
      void refreshKnowledgeGraphRecords(session);
    } catch (error) {
      setDocumentsError(error instanceof Error ? error.message : "删除资料失败");
    }
  }

  async function handleReindexDocument(documentId: string) {
    if (!session) {
      return;
    }
    setDocumentsError(null);
    try {
      const nextDocument = await reindexDocument(session, documentId);
      setDocuments((current) =>
        current.map((document) => (document.id === documentId ? nextDocument : document))
      );
      void refreshKnowledgeGraph(session);
    } catch (error) {
      setDocumentsError(error instanceof Error ? error.message : "重建索引失败");
    }
  }

  function formatJson(value: string) {
    try {
      return JSON.stringify(JSON.parse(value), null, 2);
    } catch {
      return value;
    }
  }

  if (!session) {
    return (
      <AuthPage
        authMode={authMode}
        username={username}
        password={password}
        captchaCode={captchaCode}
        captchaInput={captchaInput}
        authError={authError}
        isAuthenticating={isAuthenticating}
        theme={theme}
        onAuthModeChange={(mode) => {
          setAuthError(null);
          setAuthMode(mode);
          setCaptchaCode(createCaptchaCode());
          setCaptchaInput("");
        }}
        onUsernameChange={setUsername}
        onPasswordChange={setPassword}
        onCaptchaInputChange={setCaptchaInput}
        onThemeChange={setTheme}
        onSubmit={handleAuthSubmit}
      />
    );
  }

  return (
      <WorkspaceShell
        session={session}
        activeTaskId={activeTaskId}
        activeView={activeView}
        theme={theme}
      onThemeChange={setTheme}
      onViewChange={setActiveView}
      onLogout={handleLogout}
      >
      {activeView === "workspace" ? (
        <WorkspacePage
          documentsCount={documents.length}
          artifactCount={artifacts.length}
          knowledgeGraphCount={knowledgeGraphRecords.length}
          activeTaskId={activeTaskId}
          selectedKnowledgeGraphRecordId={selectedKnowledgeGraphRecordId}
          canSaveKnowledgeGraph={documents.length > 0 && Boolean(session)}
          onOpenKnowledgeGraph={() => setActiveView("knowledge-graph")}
          onSaveKnowledgeGraph={handleCreateKnowledgeGraphRecord}
          onDeleteKnowledgeGraph={() => {
            if (selectedKnowledgeGraphRecordId) {
              void handleDeleteKnowledgeGraphRecord(selectedKnowledgeGraphRecordId);
            }
          }}
        >
          <div className="grid gap-5 lg:grid-cols-[minmax(0,1.05fr)_minmax(320px,0.95fr)]">
            <div className="space-y-5">
              <PromptComposer
                instruction={instruction}
                selectedFiles={selectedFiles}
                isSubmitting={isSubmitting}
                onInstructionChange={setInstruction}
                onFileChange={handleFileChange}
                onSubmit={handleSubmit}
              />
              <WorksheetPanel
                title={worksheetTitle}
                gradeLevel={worksheetGradeLevel}
                difficulty={worksheetDifficulty}
                questionCount={worksheetQuestionCount}
                questionTypes={worksheetQuestionTypes}
                includeExplanation={worksheetIncludeExplanation}
                worksheet={worksheet}
                attempts={worksheetAttempts}
                wrongQuestions={wrongQuestions}
                isGenerating={isGeneratingWorksheet}
                isExporting={isExportingWorksheet}
                isSubmittingAttempt={isSubmittingAttempt}
                isRetryingWrongQuestions={isRetryingWrongQuestions}
                error={worksheetError}
                wrongQuestionError={wrongQuestionError}
                canGenerate={
                  selectedFiles.length > 0 &&
                  worksheetTitle.trim().length > 0 &&
                  worksheetGradeLevel.trim().length > 0 &&
                  worksheetQuestionTypes.length > 0
                }
                onTitleChange={setWorksheetTitle}
                onGradeLevelChange={setWorksheetGradeLevel}
                onDifficultyChange={setWorksheetDifficulty}
                onQuestionCountChange={setWorksheetQuestionCount}
                onQuestionTypesChange={setWorksheetQuestionTypes}
                onIncludeExplanationChange={setWorksheetIncludeExplanation}
                onSubmit={handleWorksheetSubmit}
                onExport={handleWorksheetExport}
                onAttemptSubmit={handleWorksheetAttemptSubmit}
                onLoadAttempts={handleLoadWorksheetAttempts}
                onRetryWrongQuestions={handleRetryWrongQuestions}
                onResolveWrongQuestion={handleResolveWrongQuestion}
              />
              <ResultPanel finalAnswer={finalAnswer} />
            </div>
            <div className="space-y-5">
              <GlassPanel className="space-y-3 p-4 sm:p-5">
                <div className="flex items-center justify-between gap-3">
                  <div>
                    <p className="text-sm font-semibold text-slate-950">当前状态</p>
                    <p className="mt-1 text-sm leading-6 text-slate-500">
                      {activeTaskId ? `正在跟踪任务 ${activeTaskId}` : "还没有开始新的任务。"}
                    </p>
                  </div>
                  <button
                    className="rounded-full border border-slate-200 bg-white/80 px-3 py-1 text-xs font-semibold text-slate-700 transition hover:border-blue-200 hover:text-blue-700"
                    type="button"
                    onClick={() => setActiveView("history")}
                  >
                    打开历史与回放
                  </button>
                </div>
                <div className="liquid-glass rounded-2xl p-3">
                  <p className="text-xs font-semibold uppercase tracking-[0.14em] text-slate-400">
                    最新日志
                  </p>
                  <p className="mt-2 line-clamp-3 text-sm leading-6 text-slate-600">
                    {lines[lines.length - 1]?.text ?? "任务执行后，日志会在这里出现。"}
                  </p>
                </div>
                <div className="grid gap-3 sm:grid-cols-2">
                  <div className="liquid-glass rounded-2xl p-3">
                    <p className="text-xs font-semibold uppercase tracking-[0.14em] text-slate-400">
                      当前任务
                    </p>
                    <p className="mt-2 text-sm font-semibold text-slate-950">
                      {activeTaskId ? activeTaskId : "等待创建"}
                    </p>
                  </div>
                  <div className="liquid-glass rounded-2xl p-3">
                    <p className="text-xs font-semibold uppercase tracking-[0.14em] text-slate-400">
                      当前结果
                    </p>
                    <p className="mt-2 text-sm font-semibold text-slate-950">
                      {finalAnswer ? "已生成" : "待生成"}
                    </p>
                  </div>
                </div>
              </GlassPanel>
            </div>
          </div>
        </WorkspacePage>
      ) : activeView === "documents" ? (
        <DocumentCenterPage
          documents={documents}
          isLoading={isLoadingDocuments}
          error={documentsError}
          onDelete={handleDeleteDocument}
          onReindex={handleReindexDocument}
        />
      ) : activeView === "knowledge-graph" ? (
        <KnowledgeGraphPage
        graph={knowledgeGraph}
        records={knowledgeGraphRecords}
        selectedRecordId={selectedKnowledgeGraphRecordId}
        recordTitle={knowledgeGraphRecordTitle}
        isLoadingRecords={isLoadingKnowledgeGraphRecords}
        recordsError={knowledgeGraphRecordsError}
        isLoading={isLoadingKnowledgeGraph}
        error={knowledgeGraphError}
        onRefresh={() => {
          if (session) {
            void refreshKnowledgeGraph(session);
          }
        }}
        onRefreshRecords={() => {
          if (session) {
            void refreshKnowledgeGraphRecords(session);
          }
        }}
        onRecordTitleChange={setKnowledgeGraphRecordTitle}
        onCreateRecord={handleCreateKnowledgeGraphRecord}
        onClearSelection={handleClearKnowledgeGraphSelection}
        onSelectRecord={setSelectedKnowledgeGraphRecordId}
        onDeleteRecord={handleDeleteKnowledgeGraphRecord}
      />
      ) : activeView === "worksheet" ? (
        <WorksheetStudioPage>
          <div className="grid gap-5 lg:grid-cols-[minmax(0,1fr)_360px]">
            <WorksheetPanel
              title={worksheetTitle}
              gradeLevel={worksheetGradeLevel}
              difficulty={worksheetDifficulty}
              questionCount={worksheetQuestionCount}
              questionTypes={worksheetQuestionTypes}
              includeExplanation={worksheetIncludeExplanation}
              worksheet={worksheet}
              attempts={worksheetAttempts}
              wrongQuestions={wrongQuestions}
              isGenerating={isGeneratingWorksheet}
              isExporting={isExportingWorksheet}
              isSubmittingAttempt={isSubmittingAttempt}
              isRetryingWrongQuestions={isRetryingWrongQuestions}
              error={worksheetError}
              wrongQuestionError={wrongQuestionError}
              canGenerate={
                selectedFiles.length > 0 &&
                worksheetTitle.trim().length > 0 &&
                worksheetGradeLevel.trim().length > 0 &&
                worksheetQuestionTypes.length > 0
              }
              onTitleChange={setWorksheetTitle}
              onGradeLevelChange={setWorksheetGradeLevel}
              onDifficultyChange={setWorksheetDifficulty}
              onQuestionCountChange={setWorksheetQuestionCount}
              onQuestionTypesChange={setWorksheetQuestionTypes}
              onIncludeExplanationChange={setWorksheetIncludeExplanation}
              onSubmit={handleWorksheetSubmit}
              onExport={handleWorksheetExport}
              onAttemptSubmit={handleWorksheetAttemptSubmit}
              onLoadAttempts={handleLoadWorksheetAttempts}
              onRetryWrongQuestions={handleRetryWrongQuestions}
              onResolveWrongQuestion={handleResolveWrongQuestion}
            />
            <div className="space-y-5">
              <GlassPanel className="p-4">
                <p className="text-sm font-semibold text-slate-950">工作区提示</p>
                <p className="mt-2 text-sm leading-6 text-slate-500">
                  这里会保留题目、解析和错题操作的完整链路，后续可在此继续细分做题和解析双标签页。
                </p>
              </GlassPanel>
              <ResultPanel finalAnswer={finalAnswer} />
            </div>
          </div>
        </WorksheetStudioPage>
      ) : activeView === "wrong-questions" ? (
        <WrongQuestionsPage
          wrongQuestions={wrongQuestionBank}
          isLoading={isLoadingWrongQuestionBank}
          error={wrongQuestionBankError}
          retryError={wrongQuestionRetryError}
          retryTitle={wrongQuestionRetryTitle}
          isRetrying={isRetryingWrongQuestions}
          onRefresh={() => {
            if (session) {
              void refreshWrongQuestionBank(session);
            }
          }}
          onRetryTitleChange={setWrongQuestionRetryTitle}
          onRetry={handleRetryWrongQuestions}
          onResolve={handleResolveWrongQuestion}
        />
      ) : activeView === "history" ? (
        <HistoryPage>
          <div className="grid gap-5 xl:grid-cols-[minmax(0,1fr)_minmax(360px,0.9fr)]">
            <TaskTimeline lines={lines} />
            <div className="space-y-5">
              <HistoryPanel
                tasks={historyTasks}
                isLoading={isLoadingHistory}
                error={historyError}
                onRefresh={() => {
                  if (session) {
                    void refreshHistory(session);
                  }
                }}
                onOpenTask={handleOpenHistoryTask}
              />
              <ArtifactGrid artifacts={artifacts} formatJson={formatJson} />
            </div>
          </div>
        </HistoryPage>
      ) : (
        <ProfilePage>
          <div className="grid gap-5 lg:grid-cols-[minmax(0,1fr)_360px]">
            <GlassPanel className="p-4">
              <p className="text-sm font-semibold text-slate-950">账户信息</p>
              <p className="mt-2 text-sm leading-6 text-slate-500">当前账号：{session.username}</p>
              <p className="mt-1 text-sm leading-6 text-slate-500">
                会话状态：{activeTaskId ? `任务 ${activeTaskId}` : "空闲"}
              </p>
            </GlassPanel>
            <GlassPanel className="p-4">
              <p className="text-sm font-semibold text-slate-950">主题与偏好</p>
              <p className="mt-2 text-sm leading-6 text-slate-500">
                当前主题：{theme === "aurora" ? "晨蓝" : "晨光"}。可以在顶部快速切换。
              </p>
              <div className="mt-4">
                <button
                  className="inline-flex h-10 items-center justify-center gap-2 rounded-lg border border-white/80 bg-white/80 px-3 text-sm font-semibold text-slate-700 shadow-sm transition hover:-translate-y-0.5 hover:border-blue-200 hover:text-blue-700"
                  type="button"
                  onClick={handleLogout}
                >
                  <LogOut aria-hidden="true" size={16} />
                  退出登录
                </button>
              </div>
            </GlassPanel>
            <GlassPanel className="p-4">
              <form className="grid gap-3" onSubmit={handleChangePassword}>
                <div>
                  <p className="text-sm font-semibold text-slate-950">修改密码</p>
                  <p className="mt-1 text-sm leading-6 text-slate-500">
                    新密码至少 6 位，修改后当前会话仍可继续使用。
                  </p>
                </div>
                <label className="grid gap-1.5 text-sm font-medium text-slate-700">
                  当前密码
                  <input
                    className="liquid-glass h-10 rounded-lg px-3 text-sm outline-none transition focus:border-blue-300 focus:ring-4 focus:ring-blue-100"
                    type="password"
                    value={oldPassword}
                    onChange={(event) => setOldPassword(event.target.value)}
                    autoComplete="current-password"
                  />
                </label>
                <label className="grid gap-1.5 text-sm font-medium text-slate-700">
                  新密码
                  <input
                    className="liquid-glass h-10 rounded-lg px-3 text-sm outline-none transition focus:border-blue-300 focus:ring-4 focus:ring-blue-100"
                    type="password"
                    value={newPassword}
                    onChange={(event) => setNewPassword(event.target.value)}
                    autoComplete="new-password"
                  />
                </label>
                <label className="grid gap-1.5 text-sm font-medium text-slate-700">
                  确认新密码
                  <input
                    className="liquid-glass h-10 rounded-lg px-3 text-sm outline-none transition focus:border-blue-300 focus:ring-4 focus:ring-blue-100"
                    type="password"
                    value={confirmPassword}
                    onChange={(event) => setConfirmPassword(event.target.value)}
                    autoComplete="new-password"
                  />
                </label>
                {passwordChangeError ? (
                  <p className="rounded-lg border border-red-100 bg-red-50 px-3 py-2 text-sm text-red-700">
                    {passwordChangeError}
                  </p>
                ) : null}
                {passwordChangeMessage ? (
                  <p className="rounded-lg border border-green-100 bg-green-50 px-3 py-2 text-sm text-green-700">
                    {passwordChangeMessage}
                  </p>
                ) : null}
                <button
                  className="inline-flex h-10 items-center justify-center rounded-lg bg-slate-950 px-4 text-sm font-semibold text-white shadow-lg shadow-slate-950/10 transition hover:-translate-y-0.5 disabled:translate-y-0 disabled:cursor-not-allowed disabled:bg-slate-400"
                  type="submit"
                  disabled={isChangingPassword || !oldPassword || !newPassword || !confirmPassword}
                >
                  {isChangingPassword ? "更新中..." : "更新密码"}
                </button>
              </form>
            </GlassPanel>
          </div>
        </ProfilePage>
      )}
    </WorkspaceShell>
  );
}

function readStoredSession(): AuthSession | null {
  try {
    const value = localStorage.getItem(AUTH_STORAGE_KEY);
    return value ? (JSON.parse(value) as AuthSession) : null;
  } catch {
    return null;
  }
}

function readStoredTheme(): "aurora" | "sunrise" {
  const value = localStorage.getItem(THEME_STORAGE_KEY);
  return value === "sunrise" ? "sunrise" : "aurora";
}

function createCaptchaCode() {
  return Math.random().toString(36).slice(2, 6).toUpperCase();
}

function downloadBlob(blob: Blob, fileName: string) {
  const url = URL.createObjectURL(blob);
  const anchor = document.createElement("a");
  anchor.href = url;
  anchor.download = fileName.replace(/[\\/:*?"<>|]/g, "_");
  document.body.appendChild(anchor);
  anchor.click();
  anchor.remove();
  URL.revokeObjectURL(url);
}
