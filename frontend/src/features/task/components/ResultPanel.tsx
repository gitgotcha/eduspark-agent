import { Sparkles } from "lucide-react";

interface ResultPanelProps {
  finalAnswer: string | null;
}

export function ResultPanel({ finalAnswer }: ResultPanelProps) {
  if (!finalAnswer) {
    return (
      <section className="glass-panel p-4 sm:p-5">
        <div className="liquid-glass flex min-h-52 items-center justify-center rounded-2xl px-4 text-center text-sm text-slate-500">
          任务完成后，最终结果会在这里展开。
        </div>
      </section>
    );
  }

  return (
    <section className="glass-panel p-4 sm:p-5">
      <div className="mb-4 flex items-center gap-2">
        <Sparkles aria-hidden="true" size={18} className="text-yellow-500" />
        <h2 className="text-base font-semibold text-slate-950">最终结果</h2>
      </div>
      <pre className="liquid-glass max-h-96 overflow-auto whitespace-pre-wrap break-words rounded-2xl bg-slate-950 p-4 text-sm leading-6 text-slate-100 shadow-inner">
        {finalAnswer}
      </pre>
    </section>
  );
}
