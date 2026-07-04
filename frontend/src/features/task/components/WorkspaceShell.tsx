import { AuthSession } from "../../../api/taskApi";
import { LogOut, Sparkles, SquareActivity } from "lucide-react";
import { ReactNode } from "react";
import { ThemeToggle } from "./ThemeToggle";
import { WorkspaceView } from "../TaskUiTypes";

interface WorkspaceShellProps {
  session: AuthSession;
  activeTaskId: string | null;
  activeView: WorkspaceView;
  theme: "aurora" | "sunrise";
  onThemeChange: (theme: "aurora" | "sunrise") => void;
  onViewChange: (view: WorkspaceView) => void;
  onLogout: () => void;
  children: ReactNode;
}

export function WorkspaceShell({
  session,
  activeTaskId,
  activeView,
  theme,
  onThemeChange,
  onViewChange,
  onLogout,
  children
}: WorkspaceShellProps) {
  const views: Array<{ value: WorkspaceView; label: string }> = [
    { value: "workspace", label: "工作台首页" },
    { value: "knowledge-graph", label: "知识图谱" },
    { value: "worksheet", label: "练习卷工作区" },
    { value: "history", label: "历史与回放" },
    { value: "profile", label: "个人中心" }
  ];

  return (
    <main className="energy-shell min-h-screen text-slate-950">
      <section className="relative mx-auto flex min-h-screen w-full max-w-7xl flex-col gap-5 px-4 py-4 sm:px-6 lg:px-8">
        <header className="glass-panel flex flex-col gap-4 px-4 py-4 sm:flex-row sm:items-center sm:justify-between sm:px-5">
          <div className="flex items-center gap-3">
            <div className="flex h-11 w-11 shrink-0 items-center justify-center rounded-xl bg-slate-950 text-white shadow-lg shadow-slate-950/15">
              <SquareActivity aria-hidden="true" size={20} />
            </div>
            <div>
              <div className="flex flex-wrap items-center gap-2">
                <h1 className="text-xl font-semibold tracking-tight text-slate-950 sm:text-2xl">
                  AI 工作台
                </h1>
                <span className="inline-flex items-center gap-1 rounded-full bg-slate-950 px-2.5 py-1 text-xs font-semibold text-white shadow-sm">
                  <Sparkles aria-hidden="true" size={13} />
                  EduSpark
                </span>
              </div>
              <p className="mt-1 text-sm text-slate-500">
                <span className="font-medium text-slate-700">{session.username}</span>
                {activeTaskId ? (
                  <span className="inline-flex items-center gap-1">
                    <span className="mx-2 text-slate-300">•</span>
                    <span>任务 {activeTaskId}</span>
                  </span>
                ) : null}
              </p>
            </div>
          </div>
          <div className="flex flex-wrap items-center gap-2">
            <ThemeToggle theme={theme} onThemeChange={onThemeChange} />
            <button
              className="inline-flex h-10 items-center justify-center gap-2 rounded-full border border-white/80 bg-white/80 px-3 text-sm font-semibold text-slate-700 shadow-sm transition hover:-translate-y-0.5 hover:border-slate-300 hover:text-slate-950"
              type="button"
              onClick={onLogout}
            >
              <LogOut aria-hidden="true" size={16} />
              退出登录
            </button>
          </div>
        </header>
        <nav className="glass-panel flex flex-wrap gap-2 px-3 py-3">
          {views.map((view) => (
            <button
              className={`inline-flex h-10 items-center rounded-full px-4 text-sm font-semibold transition ${
                activeView === view.value
                  ? "bg-slate-950 text-white shadow-lg shadow-slate-950/10"
                  : "bg-white/70 text-slate-600 hover:bg-white hover:text-slate-950"
              }`}
              type="button"
              key={view.value}
              onClick={() => onViewChange(view.value)}
            >
              {view.label}
            </button>
          ))}
        </nav>
        {children}
      </section>
    </main>
  );
}
