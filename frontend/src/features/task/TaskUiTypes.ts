export interface ConsoleLine {
  id: string;
  text: string;
  tone: "info" | "success" | "error";
  group?: string;
}

export type WorkspaceView =
  | "workspace"
  | "knowledge-graph"
  | "worksheet"
  | "wrong-questions"
  | "history"
  | "profile";

export type ThemeMode = "aurora" | "sunrise";
