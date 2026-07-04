import { FileText, Plus, RefreshCcw, Trash2 } from "lucide-react";
import { ChangeEvent, FormEvent } from "react";
import { KnowledgeGraphRecord } from "../../../api/taskApi";
import { GlassPanel } from "./GlassPanel";

interface KnowledgeGraphRecordsPanelProps {
  records: KnowledgeGraphRecord[];
  isLoading: boolean;
  error: string | null;
  selectedGraphId: string | null;
  draftTitle: string;
  canCreate: boolean;
  onTitleChange: (value: string) => void;
  onCreate: () => void;
  onRefresh: () => void;
  onClearSelection: () => void;
  onSelectRecord: (graphId: string) => void;
  onDeleteRecord: (graphId: string) => void;
}

export function KnowledgeGraphRecordsPanel({
  records,
  isLoading,
  error,
  selectedGraphId,
  draftTitle,
  canCreate,
  onTitleChange,
  onCreate,
  onRefresh,
  onClearSelection,
  onSelectRecord,
  onDeleteRecord
}: KnowledgeGraphRecordsPanelProps) {
  function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    onCreate();
  }

  return (
    <GlassPanel className="space-y-4 p-4 sm:p-5">
      <div className="flex flex-col gap-3 sm:flex-row sm:items-start sm:justify-between">
        <div>
          <p className="text-sm font-semibold text-slate-950">图谱记录</p>
          <p className="mt-1 text-sm leading-6 text-slate-500">
            保存当前图谱快照，回看历史关系，并随时删除不需要的记录。
          </p>
        </div>
        <button
          className="inline-flex h-9 items-center justify-center gap-2 rounded-full border border-white/80 bg-white/80 px-3 text-sm font-semibold text-slate-700 transition hover:-translate-y-0.5 hover:border-blue-200 hover:text-blue-700"
          type="button"
          onClick={onRefresh}
        >
          <RefreshCcw aria-hidden="true" size={15} className={isLoading ? "animate-spin" : ""} />
          刷新记录
        </button>
      </div>

      <button
        className="inline-flex h-9 items-center justify-center gap-2 rounded-full border border-slate-200 bg-white/75 px-3 text-xs font-semibold text-slate-600 transition hover:border-slate-300 hover:text-slate-950"
        type="button"
        onClick={onClearSelection}
      >
        查看实时图谱
      </button>

      {error ? <div className="rounded-xl bg-red-50 px-3 py-2 text-sm font-medium text-red-700">{error}</div> : null}

      <form className="space-y-3" onSubmit={handleSubmit}>
        <label className="grid gap-2 text-sm font-medium text-slate-700">
          <span>记录标题</span>
          <input
            className="h-11 rounded-xl border border-white/80 bg-white/80 px-3 text-sm text-slate-950 outline-none transition placeholder:text-slate-400 focus:border-blue-300 focus:ring-2 focus:ring-blue-200/60"
            type="text"
            value={draftTitle}
            onChange={(event: ChangeEvent<HTMLInputElement>) => onTitleChange(event.target.value)}
            placeholder="例如：第一次知识图谱快照"
          />
        </label>

        <button
          className="inline-flex h-10 items-center justify-center gap-2 rounded-full border border-slate-950 bg-slate-950 px-4 text-sm font-semibold text-white shadow-lg shadow-slate-950/10 transition hover:-translate-y-0.5 disabled:cursor-not-allowed disabled:opacity-50"
          type="submit"
          disabled={!canCreate || isLoading}
        >
          <Plus aria-hidden="true" size={16} />
          保存当前图谱
        </button>
      </form>

      <div className="space-y-2">
        {records.length === 0 ? (
          <div className="rounded-2xl border border-dashed border-white/70 bg-white/45 px-4 py-6 text-sm leading-6 text-slate-500">
            还没有保存过图谱记录。先生成并保存一次，后续就可以在这里快速回看。
          </div>
        ) : (
          records.map((record) => {
            const selected = record.id === selectedGraphId;
            const nodes = record.graphJson.nodes.length;
            const edges = record.graphJson.edges.length;
            return (
              <div
                key={record.id}
                className={`rounded-2xl border px-4 py-3 transition ${
                  selected
                    ? "border-blue-200 bg-white/90 shadow-[0_18px_40px_rgba(59,130,246,0.12)]"
                    : "border-white/70 bg-white/60 hover:border-slate-200 hover:bg-white/80"
                }`}
              >
                <button
                  className="flex w-full items-start justify-between gap-3 text-left"
                  type="button"
                  onClick={() => onSelectRecord(record.id)}
                >
                  <div className="min-w-0 space-y-1">
                    <div className="flex items-center gap-2">
                      <FileText aria-hidden="true" size={15} className="text-blue-600" />
                      <p className="truncate text-sm font-semibold text-slate-950">{record.title}</p>
                    </div>
                    <p className="text-xs leading-5 text-slate-500">
                      {record.status} · {record.documentIds.length} 份资料 · {nodes} 个节点 · {edges} 条关系
                    </p>
                    <p className="text-xs leading-5 text-slate-400">
                      {record.createdAt} {record.taskId ? `· 任务 ${record.taskId}` : ""}
                    </p>
                  </div>
                  <span className="rounded-full bg-slate-950 px-2.5 py-1 text-[11px] font-semibold text-white">
                    查看
                  </span>
                </button>

                <div className="mt-3 flex items-center justify-between gap-3">
                  <span
                    className={`rounded-full px-2.5 py-1 text-[11px] font-semibold ${
                      selected ? "bg-blue-50 text-blue-700" : "bg-slate-100 text-slate-500"
                    }`}
                  >
                    {selected ? "当前预览" : "历史记录"}
                  </span>
                  <button
                    className="inline-flex h-8 items-center justify-center gap-1 rounded-full border border-white/80 bg-white/80 px-2.5 text-xs font-semibold text-slate-600 transition hover:border-red-200 hover:text-red-700"
                    type="button"
                    onClick={() => onDeleteRecord(record.id)}
                  >
                    <Trash2 aria-hidden="true" size={14} />
                    删除
                  </button>
                </div>
              </div>
            );
          })
        )}
      </div>
    </GlassPanel>
  );
}
