import { Clock3, RotateCcw } from "lucide-react";
import { TaskDetail } from "../../../api/taskApi";

interface HistoryPanelProps {
  tasks: TaskDetail[];
  isLoading: boolean;
  error: string | null;
  onRefresh: () => void;
  onOpenTask: (taskId: string) => void;
}

export function HistoryPanel({ tasks, isLoading, error, onRefresh, onOpenTask }: HistoryPanelProps) {
  return (
    <section className="glass-panel p-4 sm:p-5">
      <div className="mb-4 flex items-center justify-between gap-3">
        <div className="flex items-center gap-2">
          <Clock3 aria-hidden="true" size={18} className="text-blue-600" />
          <h2 className="text-base font-semibold text-slate-950">历史任务</h2>
        </div>
        <button
          className="liquid-glass inline-flex h-9 items-center justify-center gap-2 rounded-lg px-3 text-sm font-semibold text-slate-700 transition hover:border-blue-200 hover:text-blue-700"
          type="button"
          onClick={onRefresh}
        >
          <RotateCcw aria-hidden="true" size={15} className={isLoading ? "animate-spin" : ""} />
          刷新
        </button>
      </div>
      {error ? <div className="mb-3 rounded-lg bg-red-50 px-3 py-2 text-sm font-medium text-red-700">{error}</div> : null}
      <div className="max-h-72 space-y-2 overflow-auto pr-1">
        {tasks.length ? (
          tasks.map((task) => (
            <button
              className="liquid-glass w-full rounded-lg p-3 text-left transition hover:-translate-y-0.5 hover:border-blue-200 hover:bg-white"
              key={task.id}
              type="button"
              onClick={() => onOpenTask(task.id)}
            >
              <div className="flex items-center justify-between gap-3">
                <span className="min-w-0 truncate text-sm font-semibold text-slate-900">{task.userInstruction}</span>
                <span className="rounded bg-slate-100 px-2 py-0.5 text-xs font-semibold text-slate-600">{task.status}</span>
              </div>
              <div className="mt-2 flex items-center justify-between gap-3">
                <p className="text-xs text-slate-500">{task.id}</p>
                <span className="inline-flex items-center gap-1 rounded-full bg-blue-50 px-2 py-0.5 text-xs font-semibold text-blue-700">
                  回放
                </span>
              </div>
            </button>
          ))
        ) : (
          <div className="liquid-glass rounded-lg px-4 py-5 text-center text-sm text-slate-500">
            暂无历史任务。
          </div>
        )}
      </div>
    </section>
  );
}
