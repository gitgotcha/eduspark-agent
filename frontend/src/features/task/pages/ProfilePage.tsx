import { ReactNode } from "react";
import { GlassPanel } from "../components/GlassPanel";
import { PageIntro } from "../components/PageIntro";

interface ProfilePageProps {
  children: ReactNode;
}

export function ProfilePage({ children }: ProfilePageProps) {
  return (
    <section className="space-y-5">
      <PageIntro
        eyebrow="设置"
        title="个人中心"
        description="整理账号、主题和本地偏好，让使用习惯更贴合你。"
      />
      <GlassPanel className="grid gap-3 p-4 sm:grid-cols-3">
        <div className="liquid-glass rounded-2xl p-4">
          <p className="text-xs font-semibold uppercase tracking-[0.16em] text-slate-400">主题</p>
          <p className="mt-2 text-sm leading-6 text-slate-600">顶部可在透明白与深沉黑之间切换。</p>
        </div>
        <div className="liquid-glass rounded-2xl p-4">
          <p className="text-xs font-semibold uppercase tracking-[0.16em] text-slate-400">会话</p>
          <p className="mt-2 text-sm leading-6 text-slate-600">当前账号与任务状态会保存在本地，退出后清空。</p>
        </div>
        <div className="liquid-glass rounded-2xl p-4">
          <p className="text-xs font-semibold uppercase tracking-[0.16em] text-slate-400">偏好</p>
          <p className="mt-2 text-sm leading-6 text-slate-600">后续可扩展字体、密度和工作区布局选项。</p>
        </div>
      </GlassPanel>
      {children}
    </section>
  );
}
