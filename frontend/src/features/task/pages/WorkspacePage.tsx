import { ReactNode } from "react";
import { EnergyProgress } from "../components/EnergyProgress";
import { GlassPanel } from "../components/GlassPanel";
import { PageIntro } from "../components/PageIntro";

interface WorkspacePageProps {
  children: ReactNode;
  documentsCount: number;
  artifactCount: number;
  knowledgeGraphCount: number;
  activeTaskId: string | null;
  selectedKnowledgeGraphRecordId: string | null;
  canSaveKnowledgeGraph: boolean;
  onOpenKnowledgeGraph: () => void;
  onSaveKnowledgeGraph: () => void;
  onDeleteKnowledgeGraph: () => void;
}

export function WorkspacePage({
  children,
  documentsCount,
  artifactCount,
  knowledgeGraphCount,
  activeTaskId,
  selectedKnowledgeGraphRecordId,
  canSaveKnowledgeGraph,
  onOpenKnowledgeGraph,
  onSaveKnowledgeGraph,
  onDeleteKnowledgeGraph
}: WorkspacePageProps) {
  const progressValue = Math.min(
    100,
    documentsCount * 16 + artifactCount * 12 + knowledgeGraphCount * 10 + (activeTaskId ? 16 : 0)
  );

  return (
    <section className="space-y-5">
      <PageIntro
        eyebrow="工作台"
        title="工作台首页"
        description="把上传、出题、批改、回放与产物展示放在同一视野里，保持流程清晰。"
      />
      <GlassPanel className="space-y-4 p-4">
        <div className="flex flex-wrap items-end justify-between gap-3">
          <div>
            <h2 className="text-lg font-semibold text-slate-950">工作台概览</h2>
            <p className="mt-1 text-sm leading-6 text-slate-500">
              以资料、任务、产物和练习进度为线索，快速看见当前学习流转。
            </p>
          </div>
          <div className="rounded-full border border-sky-200 bg-sky-50 px-3 py-1 text-xs font-semibold text-sky-700">
            {activeTaskId ? `当前任务 ${activeTaskId}` : "当前空闲"}
          </div>
        </div>
        <EnergyProgress label="工作台流转" value={progressValue} loading={Boolean(activeTaskId)} />
        <div className="grid gap-3 sm:grid-cols-3">
          <div className="liquid-glass rounded-2xl px-4 py-3">
            <p className="text-xs font-semibold uppercase tracking-[0.12em] text-slate-400">
              当前资料
            </p>
            <p className="mt-2 text-2xl font-semibold text-slate-950">{documentsCount}</p>
          </div>
          <div className="liquid-glass rounded-2xl px-4 py-3">
            <p className="text-xs font-semibold uppercase tracking-[0.12em] text-slate-400">
              当前任务
            </p>
            <p className="mt-2 text-sm font-semibold text-slate-950">
              {activeTaskId ? activeTaskId : "等待创建"}
            </p>
          </div>
          <div className="liquid-glass rounded-2xl px-4 py-3">
            <p className="text-xs font-semibold uppercase tracking-[0.12em] text-slate-400">
              当前结果
            </p>
            <p className="mt-2 text-sm font-semibold text-slate-950">
              {artifactCount > 0 ? "已生成" : "待生成"}
            </p>
          </div>
          <div className="liquid-glass rounded-2xl px-4 py-3">
            <p className="text-xs font-semibold uppercase tracking-[0.12em] text-slate-400">
              图谱快照
            </p>
            <p className="mt-2 text-sm font-semibold text-slate-950">
              {knowledgeGraphCount > 0 ? `${knowledgeGraphCount} 条记录` : "尚未保存"}
            </p>
            <div className="mt-3 flex flex-wrap gap-2">
              <button
                className="inline-flex h-8 items-center justify-center rounded-full border border-slate-950 bg-slate-950 px-3 text-xs font-semibold text-white transition hover:-translate-y-0.5 disabled:cursor-not-allowed disabled:opacity-50"
                type="button"
                onClick={onOpenKnowledgeGraph}
              >
                查看图谱
              </button>
              <button
                className="inline-flex h-8 items-center justify-center rounded-full border border-white/80 bg-white/80 px-3 text-xs font-semibold text-slate-700 transition hover:border-blue-200 hover:text-blue-700 disabled:cursor-not-allowed disabled:opacity-50"
                type="button"
                onClick={onSaveKnowledgeGraph}
                disabled={!canSaveKnowledgeGraph}
              >
                保存快照
              </button>
              <button
                className="inline-flex h-8 items-center justify-center rounded-full border border-red-100 bg-red-50 px-3 text-xs font-semibold text-red-700 transition hover:border-red-200 hover:bg-red-100 disabled:cursor-not-allowed disabled:opacity-50"
                type="button"
                onClick={onDeleteKnowledgeGraph}
                disabled={!selectedKnowledgeGraphRecordId}
              >
                删除快照
              </button>
            </div>
          </div>
        </div>
      </GlassPanel>
      {children}
    </section>
  );
}
