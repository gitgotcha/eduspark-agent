export type TaskStatus =
  | "PENDING"
  | "PLANNING"
  | "EXECUTING"
  | "REVIEWING"
  | "COMPLETED"
  | "FAILED";

export type TaskLogStage = TaskStatus | "TOOL_CALL" | "TOOL_RESULT" | "CRITIC_FEEDBACK";

export interface TaskStep {
  stepNo: number;
  toolName: string;
  input: Record<string, unknown>;
  expectedOutput: string;
}

export interface TaskPlan {
  steps: TaskStep[];
}

export interface AuthRegisterRequest {
  username: string;
  password: string;
}

export interface AuthLoginRequest {
  username: string;
  password: string;
}

export interface AuthResponse {
  userId: string;
  username: string;
  token: string;
}

export interface EduTask {
  id: string;
  userId: string;
  userInstruction: string;
  status: TaskStatus;
  planJson?: TaskPlan;
  finalAnswer?: string;
  failureReason?: string;
  retryCount: number;
  createdAt: string;
  updatedAt: string;
}

export interface EduTaskLog {
  id: string;
  taskId: string;
  stage: TaskLogStage;
  message: string;
  payload?: unknown;
  createdAt: string;
}

export interface EduTaskArtifact {
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
  storagePath: string;
  extractedText?: string | null;
  parseStatus?: "PARSED" | "INDEX_FAILED" | string;
  parseError?: string | null;
  indexedAt?: string | null;
  textPreview?: string | null;
  createdAt: string;
}

export interface TaskCreateRequest {
  instruction: string;
  documentIds?: string[];
}

export interface TaskCreateResponse {
  taskId: string;
  status: "PENDING";
}

export type WorksheetStatus = "PENDING" | "COMPLETED" | "FAILED";

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
  status: WorksheetStatus;
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

export interface WorksheetDetailResponse {
  id: string;
  userId: string;
  taskId: string;
  title: string;
  documentIds: string[];
  config: WorksheetCreateRequest;
  generationRationale?: WorksheetGenerationRationale | null;
  questions: WorksheetQuestion[];
  status: WorksheetStatus;
  createdAt: string;
  updatedAt: string;
}

export interface WorksheetAnswerInput {
  questionId: string;
  answer: string;
}

export interface WorksheetAttemptRequest {
  answers: WorksheetAnswerInput[];
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

export interface WrongQuestion {
  id: string;
  worksheetId: string;
  attemptId: string;
  questionId: string;
  questionStem: string;
  submittedAnswer: string;
  correctAnswer: string;
  explanation?: string;
  weaknessTag: string;
  retryWorksheetId?: string | null;
  resolved: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface WrongQuestionRetryRequest {
  wrongQuestionIds: string[];
  title: string;
}

export interface WrongQuestionRetryResponse {
  worksheetId: string;
  taskId: string;
  status: WorksheetStatus;
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

export interface AuthErrorResponse {
  code: string;
  message: string;
}

export interface TaskStreamEvent {
  event: "task.status.changed" | "task.log.appended" | "task.completed" | "task.failed";
  data: EduTask | EduTaskLog;
}
