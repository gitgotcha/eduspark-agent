import { Boxes } from "lucide-react";
import { TaskArtifact } from "../../../api/taskApi";

interface ArtifactGridProps {
  artifacts: TaskArtifact[];
  formatJson: (value: string) => string;
}

export function ArtifactGrid({ artifacts, formatJson }: ArtifactGridProps) {
  if (artifacts.length === 0) {
    return null;
  }

  return (
    <section className="glass-panel liquid-glass p-4 sm:p-5">
      <div className="mb-4 flex items-center gap-2">
        <Boxes aria-hidden="true" size={18} className="text-green-600" />
        <h2 className="text-base font-semibold text-slate-950">工具产物</h2>
      </div>
      <div className="grid gap-3 lg:grid-cols-2">
        {artifacts.map((artifact) => (
          <article className="liquid-glass rounded-2xl p-3" key={artifact.id}>
            <div className="flex items-center justify-between gap-2">
              <h3 className="text-sm font-semibold text-slate-800">{artifact.artifactType}</h3>
              <span className="rounded-full border border-emerald-100 bg-emerald-50 px-2.5 py-0.5 text-[11px] font-semibold text-emerald-700">
                已产出
              </span>
            </div>
            <pre className="liquid-glass mt-2 max-h-64 overflow-auto whitespace-pre-wrap break-words rounded-xl bg-slate-950 p-3 text-sm leading-6 text-slate-100">
              {formatJson(artifact.contentJson)}
            </pre>
          </article>
        ))}
      </div>
    </section>
  );
}
