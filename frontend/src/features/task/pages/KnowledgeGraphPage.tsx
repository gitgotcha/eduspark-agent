import { useMemo } from "react";
import { KnowledgeGraphPanel } from "../components/KnowledgeGraphPanel";
import { KnowledgeGraphRecordsPanel } from "../components/KnowledgeGraphRecordsPanel";
import { PageIntro } from "../components/PageIntro";
import { KnowledgeGraphRecord, KnowledgeGraphResponse } from "../../../api/taskApi";

interface KnowledgeGraphPageProps {
  graph: KnowledgeGraphResponse | null;
  records: KnowledgeGraphRecord[];
  selectedRecordId: string | null;
  recordTitle: string;
  isLoadingRecords: boolean;
  recordsError: string | null;
  isLoading: boolean;
  error: string | null;
  onRefresh: () => void;
  onRefreshRecords: () => void;
  onRecordTitleChange: (value: string) => void;
  onCreateRecord: () => void;
  onClearSelection: () => void;
  onSelectRecord: (graphId: string) => void;
  onDeleteRecord: (graphId: string) => void;
}

export function KnowledgeGraphPage({
  graph,
  records,
  selectedRecordId,
  recordTitle,
  isLoadingRecords,
  recordsError,
  isLoading,
  error,
  onRefresh,
  onRefreshRecords,
  onRecordTitleChange,
  onCreateRecord,
  onClearSelection,
  onSelectRecord,
  onDeleteRecord
}: KnowledgeGraphPageProps) {
  const nodes = graph?.nodes ?? [];
  const edges = graph?.edges ?? [];
  const selectedRecord = records.find((record) => record.id === selectedRecordId) ?? null;
  const previewGraph = selectedRecord?.graphJson ?? graph;
  const canCreateRecord = (graph?.nodes.length ?? 0) > 0;

  const summary = useMemo(
    () => [
      { label: "资料节点", value: nodes.filter((node) => node.type === "document").length },
      { label: "概念节点", value: nodes.filter((node) => node.type === "concept").length },
      { label: "关联关系", value: edges.length },
      { label: "记录数量", value: records.length }
    ],
    [edges.length, nodes, records.length]
  );

  return (
    <section className="space-y-5">
      <PageIntro
        eyebrow="图谱"
        title="知识图谱专栏"
        description="专注展示资料、概念和关系的流动结构。Canvas 是主界面，右侧提供节点细节和局部说明。"
      />

      <div className="flex flex-wrap gap-2">
        {summary.map((item) => (
          <span key={item.label} className="liquid-glass rounded-full px-3 py-1 text-xs font-semibold text-slate-600">
            {item.label} {item.value}
          </span>
        ))}
        <span className="liquid-glass rounded-full px-3 py-1 text-xs font-semibold text-blue-700">
          Canvas 优先
        </span>
      </div>

      <div className="grid gap-5 xl:grid-cols-[minmax(0,1.05fr)_minmax(320px,0.78fr)]">
        <div className="space-y-3">
        <p className="text-sm leading-6 text-slate-500">
          点击节点查看关联细节，画布负责主要阅读路径，旁侧卡片只承接补充信息。
        </p>
          {selectedRecord ? (
            <div className="liquid-glass rounded-2xl px-4 py-3 text-sm leading-6 text-slate-600">
              正在预览历史记录「{selectedRecord.title}」，可以切回实时图谱或继续浏览其他记录。
            </div>
          ) : null}
          <KnowledgeGraphPanel graph={previewGraph} isLoading={isLoading} error={error} onRefresh={onRefresh} />
        </div>

        <div className="space-y-5">
          <KnowledgeGraphRecordsPanel
            records={records}
            isLoading={isLoadingRecords}
            error={recordsError}
            selectedGraphId={selectedRecordId}
            draftTitle={recordTitle}
            canCreate={canCreateRecord}
            onTitleChange={onRecordTitleChange}
            onCreate={onCreateRecord}
            onRefresh={onRefreshRecords}
            onClearSelection={onClearSelection}
            onSelectRecord={onSelectRecord}
            onDeleteRecord={onDeleteRecord}
          />

          <div className="liquid-glass rounded-2xl p-4">
            <p className="text-xs font-semibold uppercase tracking-[0.14em] text-slate-400">使用说明</p>
            <p className="mt-2 text-sm leading-6 text-slate-600">
              记录面板保存的是当前资料快照。选中记录后，画布会切换到那次保存时的图谱结构，方便对比资料变化。
            </p>
          </div>
        </div>
      </div>
    </section>
  );
}
