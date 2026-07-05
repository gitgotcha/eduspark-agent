import { LibraryBig } from "lucide-react";
import { EduDocument } from "../../../api/taskApi";
import { GlassPanel } from "../components/GlassPanel";
import { PageIntro } from "../components/PageIntro";
import { DocumentCenterPanel } from "../components/DocumentCenterPanel";

interface DocumentCenterPageProps {
  documents: EduDocument[];
  isLoading: boolean;
  error: string | null;
  onDelete: (documentId: string) => void;
  onReindex: (documentId: string) => void;
}

export function DocumentCenterPage({
  documents,
  isLoading,
  error,
  onDelete,
  onReindex
}: DocumentCenterPageProps) {
  const parsedCount = documents.filter((document) => document.parseStatus === "PARSED").length;

  return (
    <section className="space-y-5">
      <PageIntro
        eyebrow="资料"
        title="资料中心"
        description="这里集中管理上传资料、解析状态和文本预览。工作台首页负责任务流转，资料中心负责素材本身。"
      />

      <GlassPanel className="grid gap-3 p-4 sm:grid-cols-3">
        <div className="liquid-glass rounded-2xl p-4">
          <p className="text-xs font-semibold uppercase tracking-[0.16em] text-slate-400">资料总数</p>
          <p className="mt-2 text-2xl font-semibold text-slate-950">{documents.length}</p>
        </div>
        <div className="liquid-glass rounded-2xl p-4">
          <p className="text-xs font-semibold uppercase tracking-[0.16em] text-slate-400">已解析</p>
          <p className="mt-2 text-2xl font-semibold text-slate-950">{parsedCount}</p>
        </div>
        <div className="liquid-glass rounded-2xl p-4">
          <p className="text-xs font-semibold uppercase tracking-[0.16em] text-slate-400">管理方式</p>
          <p className="mt-2 text-sm leading-6 text-slate-600">
            删除、重建索引和预览都在这里完成，避免首页拥挤。
          </p>
        </div>
      </GlassPanel>

      <DocumentCenterPanel
        documents={documents}
        isLoading={isLoading}
        error={error}
        onDelete={onDelete}
        onReindex={onReindex}
      />

      <GlassPanel className="grid gap-3 p-4 sm:grid-cols-[minmax(0,1fr)_220px]">
        <div className="flex items-start gap-3">
          <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-xl bg-slate-950 text-white">
            <LibraryBig aria-hidden="true" size={18} />
          </div>
          <div>
            <p className="text-sm font-semibold text-slate-950">素材索引说明</p>
            <p className="mt-1 text-sm leading-6 text-slate-500">
              索引完成后，资料会继续服务于出题、知识图谱和错题再练流程。
            </p>
          </div>
        </div>
        <div className="liquid-glass rounded-2xl px-4 py-3 text-sm leading-6 text-slate-600">
          若需要继续上传新资料，可以回到工作台首页或练习卷工作区完成导入。
        </div>
      </GlassPanel>
    </section>
  );
}
