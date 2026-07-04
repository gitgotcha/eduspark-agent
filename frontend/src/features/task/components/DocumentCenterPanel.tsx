import { DatabaseZap, FileText, RefreshCw, Trash2 } from "lucide-react";
import { EduDocument } from "../../../api/taskApi";

interface DocumentCenterPanelProps {
  documents: EduDocument[];
  isLoading: boolean;
  error: string | null;
  onDelete: (documentId: string) => void;
  onReindex: (documentId: string) => void;
}

export function DocumentCenterPanel({
  documents,
  isLoading,
  error,
  onDelete,
  onReindex
}: DocumentCenterPanelProps) {
  const parsedCount = documents.filter((document) => document.parseStatus === "PARSED").length;

  return (
    <section className="glass-panel p-4 sm:p-5">
      <div className="mb-4 flex items-center justify-between gap-3">
        <div>
          <div className="flex items-center gap-2">
            <DatabaseZap aria-hidden="true" size={18} className="text-blue-600" />
            <h2 className="text-base font-semibold text-slate-950">资料中心</h2>
          </div>
          <p className="mt-1 text-sm text-slate-500">
            查看已解析资料，重新索引或删除无用文档，素材会继续服务出题和图谱。
          </p>
        </div>
        <div className="flex flex-col items-end gap-1 text-right">
          <span className="rounded-full border border-blue-100 bg-blue-50 px-3 py-1 text-xs font-semibold text-blue-700">
            {isLoading ? "同步中" : `${documents.length} 份资料`}
          </span>
          <span className="text-[11px] font-medium text-slate-400">{parsedCount} 份已解析</span>
        </div>
      </div>

      {error ? (
        <div className="mb-3 rounded-lg border border-red-100 bg-red-50 px-3 py-2 text-sm font-medium text-red-700">
          {error}
        </div>
      ) : null}

      {documents.length === 0 ? (
        <div className="liquid-glass rounded-2xl px-4 py-5 text-center text-sm text-slate-500">
          暂无已上传资料。
        </div>
      ) : (
        <div className="max-h-80 space-y-3 overflow-auto pr-1">
          {documents.map((document) => (
            <article className="liquid-glass rounded-2xl p-3" key={document.id}>
              <div className="flex items-start justify-between gap-3">
                <div className="min-w-0">
                  <div className="flex items-center gap-2">
                    <FileText aria-hidden="true" size={15} className="shrink-0 text-slate-500" />
                    <h3 className="truncate text-sm font-semibold text-slate-950">{document.fileName}</h3>
                  </div>
                  <p className="mt-1 text-xs font-medium text-slate-500">
                    {document.mimeType} · {document.parseStatus ?? "PARSED"}
                  </p>
                </div>
                <div className="flex shrink-0 gap-1">
                  <button
                    className="inline-flex h-8 w-8 items-center justify-center rounded-lg border border-slate-200 bg-white text-slate-600 transition hover:border-blue-200 hover:text-blue-700"
                    type="button"
                    aria-label={`重新索引 ${document.fileName}`}
                    onClick={() => onReindex(document.id)}
                  >
                    <RefreshCw aria-hidden="true" size={14} />
                  </button>
                  <button
                    className="inline-flex h-8 w-8 items-center justify-center rounded-lg border border-slate-200 bg-white text-slate-600 transition hover:border-red-200 hover:text-red-700"
                    type="button"
                    aria-label={`删除 ${document.fileName}`}
                    onClick={() => onDelete(document.id)}
                  >
                    <Trash2 aria-hidden="true" size={14} />
                  </button>
                </div>
              </div>
              {document.textPreview || document.extractedText ? (
                <p className="mt-2 line-clamp-3 text-sm leading-6 text-slate-600">
                  {document.textPreview ?? document.extractedText}
                </p>
              ) : null}
              {document.parseError ? (
                <p className="mt-2 text-xs font-medium text-red-600">{document.parseError}</p>
              ) : null}
            </article>
          ))}
        </div>
      )}
    </section>
  );
}
