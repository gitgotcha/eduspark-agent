export type ErrorCode =
  | "TASK_NOT_FOUND"
  | "INVALID_TASK_STATUS"
  | "MODEL_OUTPUT_PARSE_FAILED"
  | "TOOL_NOT_FOUND"
  | "DOCUMENT_PARSE_FAILED"
  | "MILVUS_SEARCH_FAILED"
  | "CRITIC_RETRY_EXCEEDED"
  | "VALIDATION_FAILED";

export interface ApiError {
  code: ErrorCode;
  message: string;
  details?: Record<string, unknown>;
}

export interface ApiResponse<T> {
  success: boolean;
  data?: T;
  error?: ApiError;
}
