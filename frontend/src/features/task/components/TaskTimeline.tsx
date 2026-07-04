import { ChevronDown, ChevronRight, CheckCircle2, CircleDashed, Loader2, XCircle } from "lucide-react";
import { useMemo, useState } from "react";
import { ConsoleLine } from "../TaskUiTypes";

interface TaskTimelineProps {
  lines: ConsoleLine[];
}

export function TaskTimeline({ lines }: TaskTimelineProps) {
  const [collapsedGroups, setCollapsedGroups] = useState<Record<string, boolean>>({});
  const groupedLines = useMemo(() => {
    return lines.reduce(
      (groups, line) => {
        const key = line.group ?? "日志";
        groups[key] ??= [];
        groups[key].push(line);
        return groups;
      },
      {} as Record<string, ConsoleLine[]>
    );
  }, [lines]);
  const groupEntries = Object.entries(groupedLines);

  return (
    <section className="glass-panel min-h-72 p-4 sm:p-5">
      <div className="mb-4 flex items-center justify-between gap-3">
        <div>
          <h2 className="text-base font-semibold text-slate-950">回放时间线</h2>
          <p className="mt-1 text-sm text-slate-500">任务、阶段和日志按组回放，便于快速定位当前进度。</p>
        </div>
        <span className="rounded-full border border-sky-100 bg-sky-50 px-3 py-1 text-xs font-semibold text-sky-700">
          {lines.length} 条日志
        </span>
      </div>

      {lines.length === 0 ? (
        <div className="liquid-glass flex min-h-44 items-center justify-center rounded-xl text-sm text-slate-500">
          等待创建任务
        </div>
      ) : (
        <div className="space-y-3">
          {groupEntries.map(([group, groupLines]) => {
            const isCollapsed = collapsedGroups[group] ?? false;
            const latestIndex = groupLines.length - 1;
            const latestLine = groupLines[latestIndex];
            return (
              <section className="liquid-glass rounded-xl ring-1 ring-white/70" key={group}>
                <button
                  className="flex w-full items-center justify-between gap-3 px-3 py-2 text-left"
                  type="button"
                  onClick={() =>
                    setCollapsedGroups((current) => ({ ...current, [group]: !current[group] }))
                  }
                >
                  <div className="min-w-0">
                    <div className="text-sm font-semibold text-slate-900">{group}</div>
                    <div className="truncate text-xs text-slate-500">{latestLine.text}</div>
                  </div>
                  <span className="rounded-full border border-white/70 bg-white/80 px-2 py-0.5 text-[11px] font-semibold text-slate-500">
                    回放
                  </span>
                  {isCollapsed ? (
                    <ChevronRight size={16} className="text-slate-500" />
                  ) : (
                    <ChevronDown size={16} className="text-slate-500" />
                  )}
                </button>
                {!isCollapsed ? (
                  <ol className="space-y-3 border-t border-white/70 px-3 py-3">
                    {groupLines.map((line, index) => (
                      <li className="flex gap-3" key={line.id}>
                        <span className="mt-0.5 text-blue-600">
                          {line.tone === "success" ? (
                            <CheckCircle2 aria-hidden="true" size={18} />
                          ) : line.tone === "error" ? (
                            <XCircle aria-hidden="true" size={18} className="text-red-600" />
                          ) : index === latestIndex ? (
                            <Loader2 aria-hidden="true" size={18} className="animate-spin" />
                          ) : (
                            <CircleDashed aria-hidden="true" size={18} />
                          )}
                        </span>
                        <p
                          className={
                            line.tone === "success"
                              ? "break-words text-sm font-medium text-green-700"
                              : line.tone === "error"
                                ? "break-words text-sm font-medium text-red-700"
                                : "break-words text-sm text-slate-700"
                          }
                        >
                          {line.text}
                        </p>
                      </li>
                    ))}
                  </ol>
                ) : null}
              </section>
            );
          })}
        </div>
      )}
    </section>
  );
}
