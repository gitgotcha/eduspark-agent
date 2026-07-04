import { CheckCircle2, Loader2, RotateCcw, WandSparkles } from "lucide-react";
import { useEffect, useMemo, useState } from "react";
import { GlassPanel } from "../components/GlassPanel";
import { PageIntro } from "../components/PageIntro";
import { WrongQuestion } from "../../../api/taskApi";

interface WrongQuestionsPageProps {
  wrongQuestions: WrongQuestion[];
  isLoading: boolean;
  error: string | null;
  retryError: string | null;
  retryTitle: string;
  isRetrying: boolean;
  onRefresh: () => void;
  onRetryTitleChange: (value: string) => void;
  onRetry: (wrongQuestionIds: string[], title: string) => void;
  onResolve: (wrongQuestionId: string) => void;
}

export function WrongQuestionsPage({
  wrongQuestions,
  isLoading,
  error,
  retryError,
  retryTitle,
  isRetrying,
  onRefresh,
  onRetryTitleChange,
  onRetry,
  onResolve
}: WrongQuestionsPageProps) {
  const [selectedIds, setSelectedIds] = useState<string[]>([]);

  useEffect(() => {
    setSelectedIds((current) => current.filter((id) => wrongQuestions.some((item) => item.id === id)));
  }, [wrongQuestions]);

  const summary = useMemo(
    () => ({
      total: wrongQuestions.length,
      resolved: wrongQuestions.filter((item) => item.resolved).length,
      retryable: wrongQuestions.filter((item) => !item.resolved).length
    }),
    [wrongQuestions]
  );

  function handleRetrySelected() {
    if (selectedIds.length === 0) {
      return;
    }
    onRetry(selectedIds, retryTitle.trim() || "错题再练");
    setSelectedIds([]);
  }

  return (
    <section className="space-y-5">
      <PageIntro
        eyebrow="练习"
        title="错题本"
        description="这里会集中显示全局错题，支持快速重练、单题掌握和状态回看。"
      />

      <GlassPanel className="space-y-4 p-4 sm:p-5">
        <div className="flex flex-wrap items-center justify-between gap-3">
          <div>
            <h2 className="text-lg font-semibold text-slate-950">错题概览</h2>
            <p className="mt-1 text-sm leading-6 text-slate-500">
              全局错题会从练习流转中汇聚到这里，后续可以继续扩展统计和知识点标签。
            </p>
          </div>
          <button
            className="inline-flex h-9 items-center justify-center gap-2 rounded-full border border-slate-200 bg-white/80 px-3 text-sm font-semibold text-slate-700 transition hover:border-blue-200 hover:text-blue-700"
            type="button"
            onClick={onRefresh}
          >
            <RotateCcw aria-hidden="true" size={15} className={isLoading ? "animate-spin" : ""} />
            刷新错题
          </button>
        </div>

        <div className="grid gap-3 sm:grid-cols-3">
          <div className="liquid-glass rounded-2xl px-4 py-3">
            <p className="text-xs font-semibold uppercase tracking-[0.12em] text-slate-400">总错题</p>
            <p className="mt-2 text-2xl font-semibold text-slate-950">{summary.total}</p>
          </div>
          <div className="liquid-glass rounded-2xl px-4 py-3">
            <p className="text-xs font-semibold uppercase tracking-[0.12em] text-slate-400">可再练</p>
            <p className="mt-2 text-2xl font-semibold text-slate-950">{summary.retryable}</p>
          </div>
          <div className="liquid-glass rounded-2xl px-4 py-3">
            <p className="text-xs font-semibold uppercase tracking-[0.12em] text-slate-400">已掌握</p>
            <p className="mt-2 text-2xl font-semibold text-slate-950">{summary.resolved}</p>
          </div>
        </div>

        <div className="grid gap-3 sm:grid-cols-[minmax(0,1fr)_220px]">
          <label className="grid gap-1.5 text-sm font-semibold text-slate-700">
            再练标题
            <input
              className="h-10 rounded-lg border border-slate-200 bg-white/85 px-3 text-sm font-medium text-slate-900 outline-none transition focus:border-blue-300 focus:ring-4 focus:ring-blue-100"
              value={retryTitle}
              onChange={(event) => onRetryTitleChange(event.target.value)}
              placeholder="错题再练"
            />
          </label>
          <div className="flex items-end gap-2">
              <button
                className="inline-flex h-10 flex-1 items-center justify-center gap-2 rounded-lg bg-blue-600 px-4 text-sm font-semibold text-white shadow-lg shadow-blue-600/15 transition hover:-translate-y-0.5 disabled:translate-y-0 disabled:cursor-not-allowed disabled:bg-slate-400 disabled:shadow-none"
                type="button"
                onClick={handleRetrySelected}
                disabled={isRetrying || selectedIds.length === 0}
            >
              {isRetrying ? <Loader2 aria-hidden="true" size={16} className="animate-spin" /> : <WandSparkles aria-hidden="true" size={16} />}
              错题再练
            </button>
          </div>
        </div>

        {retryError ? (
          <div className="rounded-lg border border-red-100 bg-red-50 px-3 py-2 text-sm font-medium text-red-700">
            {retryError}
          </div>
        ) : null}
      </GlassPanel>

      {error ? (
        <div className="rounded-lg border border-red-100 bg-red-50 px-3 py-2 text-sm font-medium text-red-700">
          {error}
        </div>
      ) : null}

      <div className="space-y-3">
        {isLoading ? (
          <GlassPanel className="p-4 text-sm text-slate-500">正在加载错题本...</GlassPanel>
        ) : wrongQuestions.length > 0 ? (
          wrongQuestions.map((wrongQuestion) => {
            const checked = selectedIds.includes(wrongQuestion.id);
            return (
              <GlassPanel className="space-y-3 p-4 sm:p-5" key={wrongQuestion.id}>
                <label className="flex items-start gap-3">
                  <input
                    className="mt-1 h-4 w-4 accent-blue-600"
                    type="checkbox"
                    checked={checked}
                    aria-label={`选择错题 ${wrongQuestion.questionId}`}
                    onChange={(event) =>
                      setSelectedIds((current) =>
                        event.target.checked
                          ? [...current, wrongQuestion.id]
                          : current.filter((id) => id !== wrongQuestion.id)
                      )
                    }
                  />
                  <div className="min-w-0 flex-1 space-y-3">
                    <div className="flex flex-wrap items-center gap-2">
                      <span className="rounded-full bg-red-50 px-2.5 py-1 text-xs font-semibold text-red-700">
                        错题
                      </span>
                      <span className="rounded-full bg-slate-100 px-2.5 py-1 text-xs font-medium text-slate-600">
                        {wrongQuestion.weaknessTag}
                      </span>
                      {wrongQuestion.retryWorksheetId ? (
                        <span className="rounded-full bg-blue-50 px-2.5 py-1 text-xs font-semibold text-blue-700">
                          已生成再练
                        </span>
                      ) : null}
                    </div>

                    <p className="text-base font-semibold leading-7 text-slate-950">
                      {wrongQuestion.questionStem}
                    </p>

                    <div className="grid gap-1 text-sm leading-6 text-slate-600">
                      <span>你的答案：{wrongQuestion.submittedAnswer || "未作答"}</span>
                      <span>正确答案：{wrongQuestion.correctAnswer || "未知"}</span>
                    </div>

                    {wrongQuestion.explanation ? (
                      <p className="text-sm leading-6 text-slate-500">解析：{wrongQuestion.explanation}</p>
                    ) : null}

                    <div className="flex flex-wrap items-center gap-2">
                      <button
                        className="inline-flex h-9 items-center justify-center gap-2 rounded-lg border border-slate-200 bg-white/85 px-3 text-sm font-semibold text-slate-700 transition hover:border-green-200 hover:text-green-700"
                        type="button"
                        onClick={() => onResolve(wrongQuestion.id)}
                      >
                        <CheckCircle2 aria-hidden="true" size={15} />
                        删除并标记已掌握
                      </button>
                    </div>
                  </div>
                </label>
              </GlassPanel>
            );
          })
        ) : (
          <GlassPanel className="p-6 text-center text-sm text-slate-500">
            这里暂时还没有错题。完成一次批改后，系统会把错题汇总到这里。
          </GlassPanel>
        )}
      </div>
    </section>
  );
}
