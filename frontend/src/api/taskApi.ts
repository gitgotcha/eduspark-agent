export type TaskStatus =
  | "PENDING"
  | "PLANNING"
  | "EXECUTING"
  | "REVIEWING"
  | "COMPLETED"
  | "FAILED";

export interface TaskCreateResponse {
  taskId: string;
  status: "PENDING";
}

export interface AuthSession {
  userId: string;
  username: string;
  token: string;
}

export interface TaskDetail {
  id: string;
  userId: string;
  userInstruction: string;
  status: TaskStatus;
  planJson?: string | null;
  finalAnswer?: string | null;
  failureReason?: string | null;
}

export interface TaskArtifact {
  id: string;
  taskId: string;
  artifactType: string;
  contentJson: string;
  createdAt: string;
}

export interface EduDocument {
  id: string;
  userId: string;
  taskId?: string | null;
  fileName: string;
  mimeType: string;
  storagePath?: string;
  extractedText?: string | null;
  parseStatus?: "PARSED" | "INDEX_FAILED" | string;
  parseError?: string | null;
  indexedAt?: string | null;
  textPreview?: string | null;
  createdAt: string;
}

export interface WorksheetCreateRequest {
  title: string;
  documentIds: string[];
  questionCount: number;
  gradeLevel: string;
  difficulty: string;
  questionTypes: string[];
  includeExplanation: boolean;
}

export interface WorksheetCreateResponse {
  worksheetId: string;
  taskId: string;
  status: "PENDING" | "COMPLETED" | "FAILED";
}

export interface WorksheetQuestion {
  id: string;
  type: string;
  stem: string;
  options: string[];
  answer: unknown;
  explanation?: string;
  difficulty: string;
  sourceChunkIds: string[];
}

export interface WorksheetGenerationRationale {
  summary: string;
  keyPoints: string[];
  difficultyPlan: string;
  typePlan: string;
  deviationFromPreference?: string | null;
}

export interface WorksheetDetail {
  id: string;
  userId: string;
  taskId: string;
  title: string;
  documentIds: string[];
  config: WorksheetCreateRequest;
  generationRationale?: WorksheetGenerationRationale | null;
  questions: WorksheetQuestion[];
  status: "PENDING" | "COMPLETED" | "FAILED";
  createdAt: string;
  updatedAt: string;
}

export interface WorksheetListItem {
  id: string;
  taskId: string;
  title: string;
  status: "PENDING" | "COMPLETED" | "FAILED";
  createdAt: string;
  updatedAt: string;
}

export interface WorksheetAnswerInput {
  questionId: string;
  answer: string;
}

export interface WorksheetGradingItem {
  questionId: string;
  stem: string;
  submittedAnswer: string;
  correctAnswer: string;
  correct: boolean;
  explanation?: string;
}

export interface WorksheetAttemptResponse {
  attemptId: string;
  worksheetId: string;
  score: number;
  items: WorksheetGradingItem[];
  weaknessSummary: string;
  remediationSuggestion: string;
  createdAt: string;
}

export interface KnowledgeGraphNode {
  id: string;
  label: string;
  type: "document" | "concept" | string;
  weight: number;
}

export interface KnowledgeGraphEdge {
  source: string;
  target: string;
  label: string;
  weight: number;
}

export interface KnowledgeGraphResponse {
  nodes: KnowledgeGraphNode[];
  edges: KnowledgeGraphEdge[];
}

export type KnowledgeGraphStatus = "PENDING" | "COMPLETED" | "FAILED";

export interface KnowledgeGraphCreateRequest {
  title: string;
  documentIds: string[];
}

export interface KnowledgeGraphCreateResponse {
  graphId: string;
  taskId: string;
  status: KnowledgeGraphStatus;
}

export interface KnowledgeGraphRecord {
  id: string;
  userId: string;
  title: string;
  documentIds: string[];
  graphJson: KnowledgeGraphResponse;
  status: KnowledgeGraphStatus;
  taskId?: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface WrongQuestion {
  id: string;
  worksheetId: string;
  attemptId: string;
  questionId: string;
  questionStem: string;
  submittedAnswer?: string | null;
  correctAnswer?: string | null;
  explanation?: string | null;
  weaknessTag: string;
  retryWorksheetId?: string | null;
  resolved: boolean;
  createdAt: string;
  updatedAt: string;
}

interface ApiError {
  code: string;
  message: string;
  details?: Record<string, unknown>;
}

export interface TaskStreamPayload {
  stage?: string;
  status?: TaskStatus;
  message?: string;
}

const API_BASE_URL = resolveApiBaseUrl(import.meta.env.VITE_API_BASE_URL);

export function resolveApiBaseUrl(value: string | undefined) {
  const rawValue = value?.trim();
  if (!rawValue) {
    return "http://localhost:8080";
  }

  const withoutTrailingSlash = rawValue.replace(/\/+$/, "");
  return withoutTrailingSlash === "/api" ? "" : withoutTrailingSlash;
}

export async function login(username: string, password: string): Promise<AuthSession> {
  return authenticate("/api/auth/login", username, password);
}

export async function register(username: string, password: string): Promise<AuthSession> {
  return authenticate("/api/auth/register", username, password);
}

export async function changePassword(
  session: AuthSession,
  oldPassword: string,
  newPassword: string
): Promise<void> {
  const response = await fetch(`${API_BASE_URL}/api/users/${session.userId}/auth/change-password`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      ...authHeader(session)
    },
    body: JSON.stringify({ oldPassword, newPassword })
  });

  if (!response.ok) {
    throw new Error(await readErrorMessage(response, `Change password failed: ${response.status}`));
  }
}

export async function createTask(
  session: AuthSession,
  instruction: string,
  documentIds: string[] = []
): Promise<TaskCreateResponse> {
  const payload = documentIds.length > 0 ? { instruction, documentIds } : { instruction };
  const response = await fetch(`${API_BASE_URL}/api/users/${session.userId}/tasks`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      ...authHeader(session)
    },
    body: JSON.stringify(payload)
  });

  if (!response.ok) {
    throw new Error(await readErrorMessage(response, `Create task failed: ${response.status}`));
  }

  return response.json() as Promise<TaskCreateResponse>;
}

export async function listTasks(session: AuthSession, limit = 20): Promise<TaskDetail[]> {
  const response = await fetch(`${API_BASE_URL}/api/users/${session.userId}/tasks?limit=${limit}`, {
    headers: authHeader(session)
  });

  if (!response.ok) {
    throw new Error(await readErrorMessage(response, `List tasks failed: ${response.status}`));
  }

  return response.json() as Promise<TaskDetail[]>;
}

export async function uploadDocument(session: AuthSession, file: File): Promise<EduDocument> {
  const formData = new FormData();
  formData.append("file", file);

  const response = await fetch(`${API_BASE_URL}/api/users/${session.userId}/documents`, {
    method: "POST",
    headers: authHeader(session),
    body: formData
  });

  if (!response.ok) {
    throw new Error(await readErrorMessage(response, `Upload document failed: ${response.status}`));
  }

  return response.json() as Promise<EduDocument>;
}

export async function listDocuments(session: AuthSession): Promise<EduDocument[]> {
  const response = await fetch(`${API_BASE_URL}/api/users/${session.userId}/documents`, {
    headers: authHeader(session)
  });

  if (!response.ok) {
    throw new Error(await readErrorMessage(response, `List documents failed: ${response.status}`));
  }

  return response.json() as Promise<EduDocument[]>;
}

export async function getKnowledgeGraph(session: AuthSession): Promise<KnowledgeGraphResponse> {
  const response = await fetch(`${API_BASE_URL}/api/users/${session.userId}/knowledge-graph?limit=12`, {
    headers: authHeader(session)
  });

  if (!response.ok) {
    throw new Error(await readErrorMessage(response, `Get knowledge graph failed: ${response.status}`));
  }

  return response.json() as Promise<KnowledgeGraphResponse>;
}

export async function createKnowledgeGraph(
  session: AuthSession,
  request: KnowledgeGraphCreateRequest
): Promise<KnowledgeGraphCreateResponse> {
  const response = await fetch(`${API_BASE_URL}/api/users/${session.userId}/knowledge-graphs`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      ...authHeader(session)
    },
    body: JSON.stringify(request)
  });

  if (!response.ok) {
    throw new Error(await readErrorMessage(response, `Create knowledge graph failed: ${response.status}`));
  }

  return response.json() as Promise<KnowledgeGraphCreateResponse>;
}

export async function listKnowledgeGraphs(session: AuthSession): Promise<KnowledgeGraphRecord[]> {
  const response = await fetch(`${API_BASE_URL}/api/users/${session.userId}/knowledge-graphs`, {
    headers: authHeader(session)
  });

  if (!response.ok) {
    throw new Error(await readErrorMessage(response, `List knowledge graphs failed: ${response.status}`));
  }

  return response.json() as Promise<KnowledgeGraphRecord[]>;
}

export async function getKnowledgeGraphRecord(
  session: AuthSession,
  graphId: string
): Promise<KnowledgeGraphRecord> {
  const response = await fetch(`${API_BASE_URL}/api/users/${session.userId}/knowledge-graphs/${graphId}`, {
    headers: authHeader(session)
  });

  if (!response.ok) {
    throw new Error(await readErrorMessage(response, `Get knowledge graph failed: ${response.status}`));
  }

  return response.json() as Promise<KnowledgeGraphRecord>;
}

export async function deleteKnowledgeGraph(session: AuthSession, graphId: string): Promise<void> {
  const response = await fetch(`${API_BASE_URL}/api/users/${session.userId}/knowledge-graphs/${graphId}`, {
    method: "DELETE",
    headers: authHeader(session)
  });

  if (!response.ok) {
    throw new Error(await readErrorMessage(response, `Delete knowledge graph failed: ${response.status}`));
  }
}

export async function deleteDocument(session: AuthSession, documentId: string): Promise<void> {
  const response = await fetch(`${API_BASE_URL}/api/users/${session.userId}/documents/${documentId}`, {
    method: "DELETE",
    headers: authHeader(session)
  });

  if (!response.ok) {
    throw new Error(await readErrorMessage(response, `Delete document failed: ${response.status}`));
  }
}

export async function reindexDocument(session: AuthSession, documentId: string): Promise<EduDocument> {
  const response = await fetch(`${API_BASE_URL}/api/users/${session.userId}/documents/${documentId}/reindex`, {
    method: "POST",
    headers: authHeader(session)
  });

  if (!response.ok) {
    throw new Error(await readErrorMessage(response, `Reindex document failed: ${response.status}`));
  }

  return response.json() as Promise<EduDocument>;
}

export async function getTask(session: AuthSession, taskId: string): Promise<TaskDetail> {
  const response = await fetch(`${API_BASE_URL}/api/users/${session.userId}/tasks/${taskId}`, {
    headers: authHeader(session)
  });

  if (!response.ok) {
    throw new Error(await readErrorMessage(response, `Get task failed: ${response.status}`));
  }

  return response.json() as Promise<TaskDetail>;
}

export async function getTaskArtifacts(session: AuthSession, taskId: string): Promise<TaskArtifact[]> {
  const response = await fetch(`${API_BASE_URL}/api/users/${session.userId}/tasks/${taskId}/artifacts`, {
    headers: authHeader(session)
  });

  if (!response.ok) {
    throw new Error(await readErrorMessage(response, `Get task artifacts failed: ${response.status}`));
  }

  return response.json() as Promise<TaskArtifact[]>;
}

export async function createWorksheet(
  session: AuthSession,
  request: WorksheetCreateRequest
): Promise<WorksheetCreateResponse> {
  const response = await fetch(`${API_BASE_URL}/api/users/${session.userId}/worksheets`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      ...authHeader(session)
    },
    body: JSON.stringify(request)
  });

  if (!response.ok) {
    throw new Error(await readErrorMessage(response, `Create worksheet failed: ${response.status}`));
  }

  return response.json() as Promise<WorksheetCreateResponse>;
}

export async function listWorksheets(session: AuthSession, limit = 12): Promise<WorksheetListItem[]> {
  const response = await fetch(`${API_BASE_URL}/api/users/${session.userId}/worksheets?limit=${limit}`, {
    headers: authHeader(session)
  });

  if (!response.ok) {
    throw new Error(await readErrorMessage(response, `List worksheets failed: ${response.status}`));
  }

  return response.json() as Promise<WorksheetListItem[]>;
}

export async function getWorksheet(
  session: AuthSession,
  worksheetId: string
): Promise<WorksheetDetail> {
  const response = await fetch(
    `${API_BASE_URL}/api/users/${session.userId}/worksheets/${worksheetId}`,
    {
      headers: authHeader(session)
    }
  );

  if (!response.ok) {
    throw new Error(await readErrorMessage(response, `Get worksheet failed: ${response.status}`));
  }

  return response.json() as Promise<WorksheetDetail>;
}

export async function exportWorksheetWord(session: AuthSession, worksheetId: string): Promise<Blob> {
  const response = await fetch(
    `${API_BASE_URL}/api/users/${session.userId}/worksheets/${worksheetId}/export.docx`,
    {
      headers: authHeader(session)
    }
  );

  if (!response.ok) {
    throw new Error(await readErrorMessage(response, `Export worksheet failed: ${response.status}`));
  }

  return response.blob();
}

export async function submitWorksheetAttempt(
  session: AuthSession,
  worksheetId: string,
  answers: WorksheetAnswerInput[]
): Promise<WorksheetAttemptResponse> {
  const response = await fetch(`${API_BASE_URL}/api/users/${session.userId}/worksheets/${worksheetId}/attempts`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      ...authHeader(session)
    },
    body: JSON.stringify({ answers })
  });

  if (!response.ok) {
    throw new Error(await readErrorMessage(response, `Submit worksheet attempt failed: ${response.status}`));
  }

  return response.json() as Promise<WorksheetAttemptResponse>;
}

export async function listWorksheetAttempts(
  session: AuthSession,
  worksheetId: string
): Promise<WorksheetAttemptResponse[]> {
  const response = await fetch(`${API_BASE_URL}/api/users/${session.userId}/worksheets/${worksheetId}/attempts`, {
    headers: authHeader(session)
  });

  if (!response.ok) {
    throw new Error(await readErrorMessage(response, `List worksheet attempts failed: ${response.status}`));
  }

  return response.json() as Promise<WorksheetAttemptResponse[]>;
}

export async function listWrongQuestions(session: AuthSession): Promise<WrongQuestion[]> {
  const response = await fetch(`${API_BASE_URL}/api/users/${session.userId}/wrong-questions`, {
    headers: authHeader(session)
  });

  if (!response.ok) {
    throw new Error(await readErrorMessage(response, `List wrong questions failed: ${response.status}`));
  }

  return response.json() as Promise<WrongQuestion[]>;
}

export async function listWorksheetWrongQuestions(
  session: AuthSession,
  worksheetId: string
): Promise<WrongQuestion[]> {
  const response = await fetch(
    `${API_BASE_URL}/api/users/${session.userId}/worksheets/${worksheetId}/wrong-questions`,
    {
      headers: authHeader(session)
    }
  );

  if (!response.ok) {
    throw new Error(
      await readErrorMessage(response, `List worksheet wrong questions failed: ${response.status}`)
    );
  }

  return response.json() as Promise<WrongQuestion[]>;
}

export async function resolveWrongQuestion(session: AuthSession, wrongQuestionId: string): Promise<void> {
  const response = await fetch(`${API_BASE_URL}/api/users/${session.userId}/wrong-questions/${wrongQuestionId}`, {
    method: "DELETE",
    headers: authHeader(session)
  });

  if (!response.ok) {
    throw new Error(await readErrorMessage(response, `Resolve wrong question failed: ${response.status}`));
  }
}

export async function retryWrongQuestions(
  session: AuthSession,
  wrongQuestionIds: string[],
  title: string
): Promise<WorksheetCreateResponse> {
  const response = await fetch(`${API_BASE_URL}/api/users/${session.userId}/wrong-questions/retry`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      ...authHeader(session)
    },
    body: JSON.stringify({ wrongQuestionIds, title })
  });

  if (!response.ok) {
    throw new Error(await readErrorMessage(response, `Retry wrong questions failed: ${response.status}`));
  }

  return response.json() as Promise<WorksheetCreateResponse>;
}

export function subscribeTaskStream(
  session: AuthSession,
  taskId: string,
  onPayload: (payload: TaskStreamPayload) => void,
  onError: () => void
) {
  const source = new EventSource(
    `${API_BASE_URL}/api/users/${session.userId}/tasks/${taskId}/stream?token=${encodeURIComponent(session.token)}`
  );
  const handleMessage = (event: MessageEvent) => {
    onPayload(JSON.parse(event.data) as TaskStreamPayload);
  };

  source.onmessage = handleMessage;
  source.onerror = onError;

  if ("addEventListener" in source) {
    ["task.status.changed", "task.log.appended", "task.completed", "task.failed"].forEach(
      (eventName) => source.addEventListener(eventName, handleMessage as EventListener)
    );
  }

  return () => source.close();
}

async function authenticate(path: string, username: string, password: string): Promise<AuthSession> {
  const response = await fetch(`${API_BASE_URL}${path}`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json"
    },
    body: JSON.stringify({ username, password })
  });

  if (!response.ok) {
    throw new Error(await readErrorMessage(response, `Authentication failed: ${response.status}`));
  }

  return response.json() as Promise<AuthSession>;
}

function authHeader(session: AuthSession) {
  return {
    Authorization: `Bearer ${session.token}`
  };
}

async function readErrorMessage(response: Response, fallback: string) {
  try {
    const error = (await response.json()) as Partial<ApiError>;
    return typeof error.message === "string" && error.message.trim() ? error.message : fallback;
  } catch {
    return fallback;
  }
}
