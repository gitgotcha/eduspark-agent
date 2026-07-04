import { ReactNode } from "react";
import { GlassPanel } from "../components/GlassPanel";
import { PageIntro } from "../components/PageIntro";

interface HistoryPageProps {
  children: ReactNode;
}

export function HistoryPage({ children }: HistoryPageProps) {
  return (
    <section className="space-y-5">
      <PageIntro
        eyebrow="历史"
        title="历史与回放"
        description="把任务、日志和作答记录串成一条可回头查看的学习轨迹。"
      />
      <GlassPanel className="flex flex-wrap gap-2 p-3">
        <span className="liquid-glass rounded-full px-3 py-1 text-xs font-semibold text-slate-600">
          任务历史
        </span>
        <span className="liquid-glass rounded-full px-3 py-1 text-xs font-semibold text-slate-600">
          日志回放
        </span>
        <span className="liquid-glass rounded-full px-3 py-1 text-xs font-semibold text-slate-600">
          结果回看
        </span>
        <span className="liquid-glass rounded-full px-3 py-1 text-xs font-semibold text-blue-700">
          折叠展开可快速定位阶段
        </span>
        <p className="w-full text-sm leading-6 text-slate-500">
          这里只保留历史链路和回放入口，任务创建与资料操作已经回收到工作台首页。
        </p>
      </GlassPanel>
      {children}
    </section>
  );
}
