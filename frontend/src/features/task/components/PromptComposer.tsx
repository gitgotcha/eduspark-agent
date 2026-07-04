import { FileText, Paperclip, Send, X } from "lucide-react";
import { ChangeEvent, FormEvent } from "react";

interface PromptComposerProps {
  instruction: string;
  selectedFiles: File[];
  isSubmitting: boolean;
  onInstructionChange: (value: string) => void;
  onFileChange: (event: ChangeEvent<HTMLInputElement>) => void;
  onSubmit: (event: FormEvent<HTMLFormElement>) => void;
}

export function PromptComposer({
  instruction,
  selectedFiles,
  isSubmitting,
  onInstructionChange,
  onFileChange,
  onSubmit
}: PromptComposerProps) {
  return (
    <form className="glass-panel grid gap-4 p-4 sm:p-5" onSubmit={onSubmit}>
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div className="space-y-1">
          <label className="text-sm font-semibold text-slate-700" htmlFor="task-instruction">
            教育任务指令
          </label>
          <p className="text-sm text-slate-500">
            输入目标，资料会自动并入 Agent 规划上下文，出题与批改会顺着这条主线展开。
          </p>
        </div>
        <span className="inline-flex items-center rounded-full border border-emerald-100 bg-emerald-50 px-3 py-1 text-xs font-semibold text-emerald-700">
          {isSubmitting ? "正在调度" : "Ready"}
        </span>
      </div>

      <div className="liquid-glass rounded-2xl p-2">
        <textarea
          id="task-instruction"
          className="min-h-32 w-full resize-y rounded-xl border-0 bg-transparent px-3 py-2 text-base leading-7 text-slate-900 outline-none placeholder:text-slate-400"
          value={instruction}
          onChange={(event) => onInstructionChange(event.target.value)}
          placeholder="例如：基于上传资料生成一份适合初二学生的课堂练习"
        />
        <div className="flex flex-col gap-3 border-t border-slate-100 pt-3 sm:flex-row sm:items-center sm:justify-between">
          <label className="inline-flex h-10 cursor-pointer items-center justify-center gap-2 rounded-lg border border-slate-200 bg-white px-3 text-sm font-semibold text-slate-700 transition hover:-translate-y-0.5 hover:border-blue-200 hover:text-blue-700">
            <Paperclip aria-hidden="true" size={16} />
            参考资料
            <input className="sr-only" type="file" multiple onChange={onFileChange} />
          </label>
          <button
            className="inline-flex h-10 items-center justify-center gap-2 rounded-lg bg-blue-600 px-4 text-sm font-semibold text-white shadow-lg shadow-blue-600/20 transition hover:-translate-y-0.5 hover:bg-blue-700 disabled:translate-y-0 disabled:cursor-not-allowed disabled:bg-slate-400 disabled:shadow-none"
            type="submit"
            disabled={isSubmitting || !instruction.trim()}
          >
            <Send aria-hidden="true" size={16} />
            创建任务
          </button>
        </div>
      </div>

      {selectedFiles.length > 0 ? (
        <div className="flex flex-wrap items-center gap-2">
          <span className="text-xs font-semibold uppercase tracking-[0.16em] text-slate-400">
            已选资料
          </span>
          {selectedFiles.map((file) => (
            <span
            className="liquid-glass inline-flex max-w-full items-center gap-2 rounded-full px-3 py-1.5 text-sm font-medium text-blue-800"
            key={`${file.name}-${file.size}`}
          >
              <FileText aria-hidden="true" size={15} />
              <span className="truncate">{file.name}</span>
              <X aria-hidden="true" size={14} className="text-blue-400" />
            </span>
          ))}
        </div>
      ) : null}
    </form>
  );
}
