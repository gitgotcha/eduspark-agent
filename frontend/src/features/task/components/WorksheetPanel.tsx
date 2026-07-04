import { ChangeEvent, FormEvent, useMemo, useState } from "react";
import { CheckCircle2, Download, FileQuestion, Loader2, WandSparkles, XCircle } from "lucide-react";
import {
  WorksheetAnswerInput,
  WorksheetAttemptResponse,
  WorksheetDetail,
  WrongQuestion
} from "../../../api/taskApi";

interface WorksheetPanelProps {
  title: string;
  gradeLevel: string;
  difficulty: string;
  questionCount: number;
  questionTypes: string[];
  includeExplanation: boolean;
  worksheet: WorksheetDetail | null;
  attempts: WorksheetAttemptResponse[];
  wrongQuestions: WrongQuestion[];
  isGenerating: boolean;
  isExporting: boolean;
  isSubmittingAttempt: boolean;
  isRetryingWrongQuestions: boolean;
  error: string | null;
  wrongQuestionError: string | null;
  canGenerate: boolean;
  onTitleChange: (value: string) => void;
  onGradeLevelChange: (value: string) => void;
  onDifficultyChange: (value: string) => void;
  onQuestionCountChange: (value: number) => void;
  onQuestionTypesChange: (value: string[]) => void;
  onIncludeExplanationChange: (value: boolean) => void;
  onSubmit: (event: FormEvent<HTMLFormElement>) => void;
  onExport: () => void;
  onAttemptSubmit: (answers: WorksheetAnswerInput[]) => void;
  onLoadAttempts: () => void;
  onRetryWrongQuestions: (wrongQuestionIds: string[], title: string) => void;
  onResolveWrongQuestion: (wrongQuestionId: string) => void;
}

const QUESTION_TYPES = ["选择题", "判断题", "简答题"];

export function WorksheetPanel({
  title,
  gradeLevel,
  difficulty,
  questionCount,
  questionTypes,
  includeExplanation,
  worksheet,
  attempts,
  wrongQuestions,
  isGenerating,
  isExporting,
  isSubmittingAttempt,
  isRetryingWrongQuestions,
  error,
  wrongQuestionError,
  canGenerate,
  onTitleChange,
  onGradeLevelChange,
  onDifficultyChange,
  onQuestionCountChange,
  onQuestionTypesChange,
  onIncludeExplanationChange,
  onSubmit,
  onExport,
  onAttemptSubmit,
  onLoadAttempts,
  onRetryWrongQuestions,
  onResolveWrongQuestion
}: WorksheetPanelProps) {
  const [activeTab, setActiveTab] = useState<"practice" | "answers" | "wrong" | "attempts">("practice");
  const [answers, setAnswers] = useState<Record<string, string>>({});
  const [selectedWrongQuestionIds, setSelectedWrongQuestionIds] = useState<string[]>([]);
  const latestAttempt = attempts[0];
  const gradingByQuestionId = useMemo(
    () => new Map(latestAttempt?.items.map((item) => [item.questionId, item]) ?? []),
    [latestAttempt]
  );

  function handleTypeChange(event: ChangeEvent<HTMLInputElement>) {
    const type = event.target.value;
    const nextTypes = event.target.checked
      ? [...questionTypes, type]
      : questionTypes.filter((item) => item !== type);
    onQuestionTypesChange(nextTypes);
  }

  function submitAttempt() {
    if (!worksheet) {
      return;
    }
    onAttemptSubmit(
      worksheet.questions.map((question) => ({
        questionId: question.id,
        answer: answers[question.id] ?? ""
      }))
    );
  }

  function retrySelectedWrongQuestions() {
    if (!worksheet || selectedWrongQuestionIds.length === 0) {
      return;
    }
    onRetryWrongQuestions(selectedWrongQuestionIds, `${worksheet.title} - 错题再练`);
  }

  return (
    <section className="glass-panel liquid-glass p-4 sm:p-5">
      <div className="mb-4 flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <div className="flex items-center gap-2">
            <FileQuestion aria-hidden="true" size={18} className="text-green-600" />
            <h2 className="text-base font-semibold text-slate-950">智能练习卷</h2>
          </div>
          <p className="mt-1 text-sm text-slate-500">
            基于已选择的参考资料生成题目，并导出为 Word。做题、解析、错题再练与记录回看都在这里完成。
          </p>
        </div>
        {worksheet ? (
          <span className="inline-flex w-fit rounded-lg bg-green-50 px-2.5 py-1 text-xs font-semibold text-green-700">
            {worksheet.status}
          </span>
        ) : null}
      </div>

      <form className="grid gap-4" onSubmit={onSubmit}>
        <div className="grid gap-3 sm:grid-cols-[minmax(0,1fr)_120px]">
          <label className="grid gap-1.5 text-sm font-semibold text-slate-700">
            练习标题
            <input
              className="h-10 rounded-lg border border-slate-200 bg-white/85 px-3 text-sm font-medium text-slate-900 outline-none transition focus:border-blue-300 focus:ring-4 focus:ring-blue-100"
              value={title}
              onChange={(event) => onTitleChange(event.target.value)}
              placeholder="例如：分数加减法巩固练习"
            />
          </label>
          <label className="grid gap-1.5 text-sm font-semibold text-slate-700">
            题量
            <input
              className="h-10 rounded-lg border border-slate-200 bg-white/85 px-3 text-sm font-medium text-slate-900 outline-none transition focus:border-blue-300 focus:ring-4 focus:ring-blue-100"
              type="number"
              min={1}
              max={30}
              value={questionCount}
              onChange={(event) => onQuestionCountChange(Number(event.target.value))}
            />
          </label>
        </div>

        <div className="grid gap-3 sm:grid-cols-2">
          <label className="grid gap-1.5 text-sm font-semibold text-slate-700">
            年级
            <input
              className="h-10 rounded-lg border border-slate-200 bg-white/85 px-3 text-sm font-medium text-slate-900 outline-none transition focus:border-blue-300 focus:ring-4 focus:ring-blue-100"
              value={gradeLevel}
              onChange={(event) => onGradeLevelChange(event.target.value)}
              placeholder="五年级"
            />
          </label>
          <label className="grid gap-1.5 text-sm font-semibold text-slate-700">
            难度
            <select
              className="h-10 rounded-lg border border-slate-200 bg-white/85 px-3 text-sm font-medium text-slate-900 outline-none transition focus:border-blue-300 focus:ring-4 focus:ring-blue-100"
              value={difficulty}
              onChange={(event) => onDifficultyChange(event.target.value)}
            >
              <option>基础</option>
              <option>中等</option>
              <option>提高</option>
            </select>
          </label>
        </div>

        <div className="flex flex-wrap gap-2">
          {QUESTION_TYPES.map((type) => (
            <label
              className="inline-flex h-9 cursor-pointer items-center gap-2 rounded-lg border border-slate-200 bg-white/80 px-3 text-sm font-semibold text-slate-700 transition hover:border-blue-200 hover:text-blue-700"
              key={type}
            >
              <input
                className="h-4 w-4 accent-blue-600"
                type="checkbox"
                value={type}
                checked={questionTypes.includes(type)}
                onChange={handleTypeChange}
              />
              {type}
            </label>
          ))}
        </div>

        <label className="inline-flex w-fit cursor-pointer items-center gap-2 text-sm font-semibold text-slate-700">
          <input
            className="h-4 w-4 accent-blue-600"
            type="checkbox"
            checked={includeExplanation}
            onChange={(event) => onIncludeExplanationChange(event.target.checked)}
          />
          包含答案解析
        </label>

        {error ? (
          <div className="rounded-lg border border-red-100 bg-red-50 px-3 py-2 text-sm font-medium text-red-700">
            {error}
          </div>
        ) : null}

        <div className="flex flex-col gap-2 sm:flex-row">
          <button
            className="inline-flex h-10 items-center justify-center gap-2 rounded-lg bg-slate-950 px-4 text-sm font-semibold text-white shadow-lg shadow-slate-950/15 transition hover:-translate-y-0.5 disabled:translate-y-0 disabled:cursor-not-allowed disabled:bg-slate-400 disabled:shadow-none"
            type="submit"
            disabled={isGenerating || !canGenerate}
          >
            {isGenerating ? <Loader2 aria-hidden="true" size={16} className="animate-spin" /> : <WandSparkles aria-hidden="true" size={16} />}
            生成练习卷
          </button>
          <button
            className="inline-flex h-10 items-center justify-center gap-2 rounded-lg border border-slate-200 bg-white px-4 text-sm font-semibold text-slate-700 transition hover:-translate-y-0.5 hover:border-green-200 hover:text-green-700 disabled:translate-y-0 disabled:cursor-not-allowed disabled:text-slate-400"
            type="button"
            disabled={!worksheet || isExporting}
            onClick={onExport}
          >
            {isExporting ? <Loader2 aria-hidden="true" size={16} className="animate-spin" /> : <Download aria-hidden="true" size={16} />}
            导出 Word
          </button>
        </div>
      </form>

      {worksheet ? (
        <div className="mt-5 space-y-3">
          {worksheet.generationRationale ? (
            <section className="liquid-glass rounded-lg p-3">
              <div className="mb-2 flex items-center gap-2">
                <WandSparkles aria-hidden="true" size={15} className="text-blue-600" />
                <h3 className="text-sm font-semibold text-slate-950">AI 出题依据</h3>
              </div>
              <p className="text-sm leading-6 text-slate-700">{worksheet.generationRationale.summary}</p>
              {worksheet.generationRationale.keyPoints?.length ? (
                <div className="mt-2 flex flex-wrap gap-1.5">
                  {worksheet.generationRationale.keyPoints.map((point) => (
                    <span className="rounded bg-white/80 px-2 py-0.5 text-xs font-semibold text-blue-700" key={point}>
                      {point}
                    </span>
                  ))}
                </div>
              ) : null}
              <div className="mt-3 grid gap-1.5 text-xs font-medium text-slate-600 sm:grid-cols-2">
                <span>难度规划：{worksheet.generationRationale.difficultyPlan}</span>
                <span>题型规划：{worksheet.generationRationale.typePlan}</span>
                {worksheet.generationRationale.deviationFromPreference ? (
                  <span className="sm:col-span-2">调整说明：{worksheet.generationRationale.deviationFromPreference}</span>
                ) : null}
              </div>
            </section>
          ) : null}
          <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
            <h3 className="text-sm font-semibold text-slate-950">题目预览</h3>
            <div className="liquid-glass inline-flex w-fit rounded-lg p-1">
              {[
                ["practice", "做题"],
                ["answers", "解析"],
                ["wrong", "错题"],
                ["attempts", "记录"]
              ].map(([value, label]) => (
                <button
                  className={`h-8 rounded-md px-3 text-sm font-semibold transition ${
                    activeTab === value ? "bg-slate-950 text-white" : "text-slate-600 hover:text-blue-700"
                  }`}
                  key={value}
                  type="button"
                  onClick={() => {
                    setActiveTab(value as "practice" | "answers" | "wrong" | "attempts");
                    if (value === "attempts") {
                      onLoadAttempts();
                    }
                    if (value === "wrong") {
                      setSelectedWrongQuestionIds([]);
                    }
                  }}
                >
                  {label}
                </button>
              ))}
            </div>
            <span className="text-xs font-medium text-slate-500">{worksheet.questions.length} 题</span>
          </div>
          <div className="max-h-96 space-y-3 overflow-auto pr-1">
            {activeTab === "wrong" ? (
              <div className="space-y-3">
                {wrongQuestionError ? (
                  <div className="rounded-lg border border-red-100 bg-red-50 px-3 py-2 text-sm font-medium text-red-700">
                    {wrongQuestionError}
                  </div>
                ) : null}
                {wrongQuestions.length ? (
                  wrongQuestions.map((wrongQuestion) => {
                    const checked = selectedWrongQuestionIds.includes(wrongQuestion.id);
                    return (
                      <article className="liquid-glass rounded-lg p-3" key={wrongQuestion.id}>
                        <label className="flex items-start gap-3">
                          <input
                            className="mt-1 h-4 w-4 accent-blue-600"
                            type="checkbox"
                            aria-label={`选择错题 ${wrongQuestion.questionId}`}
                            checked={checked}
                            onChange={(event) =>
                              setSelectedWrongQuestionIds((current) =>
                                event.target.checked
                                  ? [...current, wrongQuestion.id]
                                  : current.filter((id) => id !== wrongQuestion.id)
                              )
                            }
                          />
                          <div className="min-w-0 flex-1">
                            <div className="flex flex-wrap items-center gap-2">
                              <span className="rounded bg-red-50 px-2 py-0.5 text-xs font-semibold text-red-700">
                                错题
                              </span>
                              <span className="rounded bg-slate-100 px-2 py-0.5 text-xs font-medium text-slate-600">
                                {wrongQuestion.weaknessTag}
                              </span>
                            </div>
                            <p className="mt-2 text-sm font-semibold leading-6 text-slate-900">
                              {wrongQuestion.questionStem}
                            </p>
                            <div className="mt-2 grid gap-1 text-sm text-slate-600">
                              <span>你的答案：{wrongQuestion.submittedAnswer || "未作答"}</span>
                              <span>正确答案：{wrongQuestion.correctAnswer || "未知"}</span>
                            </div>
                            {wrongQuestion.explanation ? (
                              <p className="mt-2 text-sm leading-6 text-slate-500">
                                解析：{wrongQuestion.explanation}
                              </p>
                            ) : null}
                            <div className="mt-3 flex flex-wrap gap-2">
                              <button
                                className="h-8 rounded-md border border-slate-200 bg-white px-3 text-xs font-semibold text-slate-700 transition hover:border-green-200 hover:text-green-700"
                                type="button"
                                onClick={() => onResolveWrongQuestion(wrongQuestion.id)}
                              >
                                标记已掌握
                              </button>
                              {wrongQuestion.retryWorksheetId ? (
                                <span className="rounded-md bg-blue-50 px-3 py-1 text-xs font-semibold text-blue-700">
                                  已生成再练
                                </span>
                              ) : null}
                            </div>
                          </div>
                        </label>
                      </article>
                    );
                  })
                ) : (
                  <div className="liquid-glass rounded-lg p-4 text-center text-sm text-slate-500">
                    暂无错题记录。
                  </div>
                )}
                <button
                  className="inline-flex h-10 w-full items-center justify-center gap-2 rounded-lg bg-blue-600 px-4 text-sm font-semibold text-white shadow-lg shadow-blue-600/15 transition hover:-translate-y-0.5 disabled:translate-y-0 disabled:cursor-not-allowed disabled:bg-slate-400 disabled:shadow-none sm:w-fit"
                  type="button"
                  disabled={isRetryingWrongQuestions || selectedWrongQuestionIds.length === 0}
                  onClick={retrySelectedWrongQuestions}
                >
                  {isRetryingWrongQuestions ? (
                    <Loader2 aria-hidden="true" size={16} className="animate-spin" />
                  ) : (
                    <WandSparkles aria-hidden="true" size={16} />
                  )}
                  错题再练
                </button>
              </div>
              ) : activeTab === "attempts" ? (
              <div className="space-y-3">
                <div className="liquid-glass rounded-lg px-3 py-2 text-sm leading-6 text-slate-600">
                  历史记录会保留最近一次批改和复盘建议，方便你从这里继续下一轮练习。
                </div>
                {attempts.length ? (
                  attempts.map((attempt) => (
                    <article className="liquid-glass rounded-lg p-3" key={attempt.attemptId}>
                      <div className="flex items-center justify-between gap-3">
                        <span className="text-sm font-semibold text-slate-950">得分 {attempt.score}</span>
                        <span className="text-xs font-medium text-slate-500">{attempt.createdAt}</span>
                      </div>
                      <p className="mt-2 text-sm text-slate-600">{attempt.weaknessSummary}</p>
                      <p className="mt-1 text-sm text-blue-700">{attempt.remediationSuggestion}</p>
                    </article>
                  ))
                ) : (
                  <div className="liquid-glass rounded-lg p-4 text-center text-sm text-slate-500">
                    暂无答题记录。
                  </div>
                )}
              </div>
            ) : (
            worksheet.questions.map((question, index) => {
              const grading = gradingByQuestionId.get(question.id);
              return (
              <article className="liquid-glass rounded-lg p-3" key={question.id}>
                <div className="mb-2 flex flex-wrap items-center gap-2">
                  <span className="rounded bg-blue-50 px-2 py-0.5 text-xs font-semibold text-blue-700">
                    {question.type}
                  </span>
                  <span className="text-xs font-medium text-slate-500">{question.difficulty}</span>
                </div>
                <p className="text-sm font-semibold leading-6 text-slate-900">
                  {index + 1}. {question.stem}
                </p>
                {question.options?.length ? (
                  <div className="mt-2 grid gap-1 text-sm text-slate-600">
                    {question.options.map((option) => (
                      <label className="liquid-glass flex items-center gap-2 rounded-lg px-3 py-2" key={option}>
                        {activeTab === "practice" ? (
                          <input
                            className="h-4 w-4 accent-blue-600"
                            type="radio"
                            name={question.id}
                            value={option.split(".")[0]}
                            checked={answers[question.id] === option.split(".")[0]}
                            onChange={(event) => setAnswers((current) => ({ ...current, [question.id]: event.target.value }))}
                          />
                        ) : null}
                        <span>{option}</span>
                      </label>
                    ))}
                  </div>
                ) : null}
                {!question.options?.length && activeTab === "practice" ? (
                  <textarea
                    className="liquid-glass mt-2 min-h-20 w-full rounded-lg px-3 py-2 text-sm outline-none transition focus:border-blue-300 focus:ring-4 focus:ring-blue-100"
                    value={answers[question.id] ?? ""}
                    onChange={(event) => setAnswers((current) => ({ ...current, [question.id]: event.target.value }))}
                    placeholder="填写你的答案"
                  />
                ) : null}
                {activeTab === "answers" ? (
                  <p className="mt-2 text-sm font-medium text-green-700">答案：{String(question.answer)}</p>
                ) : null}
                {activeTab === "answers" && includeExplanation && question.explanation ? (
                  <p className="mt-1 text-sm leading-6 text-slate-500">解析：{question.explanation}</p>
                ) : null}
                {grading ? (
                  <div className={`mt-3 flex items-start gap-2 rounded-lg px-3 py-2 text-sm ${grading.correct ? "bg-green-50 text-green-700" : "bg-red-50 text-red-700"}`}>
                    {grading.correct ? <CheckCircle2 aria-hidden="true" size={16} /> : <XCircle aria-hidden="true" size={16} />}
                    <span>
                      你的答案：{grading.submittedAnswer || "未作答"}；正确答案：{grading.correctAnswer}
                    </span>
                  </div>
                ) : null}
              </article>
              );
            })
            )}
          </div>
          {activeTab === "practice" ? (
            <button
              className="inline-flex h-10 w-full items-center justify-center gap-2 rounded-lg bg-blue-600 px-4 text-sm font-semibold text-white shadow-lg shadow-blue-600/15 transition hover:-translate-y-0.5 disabled:translate-y-0 disabled:cursor-not-allowed disabled:bg-slate-400 disabled:shadow-none sm:w-fit"
              type="button"
              disabled={isSubmittingAttempt || worksheet.questions.length === 0}
              onClick={submitAttempt}
            >
              {isSubmittingAttempt ? <Loader2 aria-hidden="true" size={16} className="animate-spin" /> : <CheckCircle2 aria-hidden="true" size={16} />}
              提交批改
            </button>
          ) : null}
        </div>
      ) : (
        <div className="liquid-glass mt-5 rounded-lg px-4 py-5 text-center text-sm text-slate-500">
          选择参考资料后，可以在这里生成可导出的练习卷。
        </div>
      )}
    </section>
  );
}
