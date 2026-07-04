import { AuthPanel, AuthPanelProps } from "../components/AuthPanel";
import { PageIntro } from "../components/PageIntro";
import { ThemeToggle } from "../components/ThemeToggle";

export function AuthPage(props: AuthPanelProps) {
  return (
    <main className="energy-shell min-h-screen overflow-hidden text-slate-950">
      <section className="relative mx-auto grid min-h-screen w-full max-w-6xl items-center gap-8 px-5 py-8 md:grid-cols-[1.08fr_0.92fr] md:px-8 lg:px-10">
        <div className="space-y-6 lg:pr-6">
          <div className="flex items-center justify-between gap-3">
            <div className="liquid-glass inline-flex items-center gap-2 rounded-full px-3 py-1.5 text-sm font-medium text-slate-700 shadow-sm backdrop-blur-xl">
              <span className="inline-flex h-2.5 w-2.5 rounded-full bg-slate-950" />
              EduSpark Workspace
            </div>
            <ThemeToggle theme={props.theme} onThemeChange={props.onThemeChange} />
          </div>

          <div className="max-w-2xl space-y-4">
            <PageIntro
              eyebrow="欢迎"
              title="EduSpark 工作区"
              description="把资料、任务、练习和回放放在一张更安静的桌面上，透明白与深沉黑之间切换，保持专注。"
            />
            <p className="max-w-xl text-base leading-7 text-slate-600 sm:text-lg">
              登录后进入统一的教育任务流，从导入资料到生成练习，再到回看结果与整理产物，所有步骤都在同一处完成。
            </p>
          </div>

          <div className="grid gap-3 sm:grid-cols-3">
            {[
              ["01", "进入任务流"],
              ["02", "切换工作模式"],
              ["03", "整理结果"]
            ].map(([step, label]) => (
              <div className="liquid-glass rounded-xl px-4 py-3" key={step}>
                <div className="text-xs font-semibold text-slate-500">{step}</div>
                <div className="mt-1 text-sm font-medium text-slate-800">{label}</div>
              </div>
            ))}
          </div>

          <div className="grid gap-3 sm:grid-cols-2">
            <div className="liquid-glass rounded-xl px-4 py-4">
              <p className="text-xs font-semibold uppercase tracking-[0.16em] text-slate-500">
                当前主题
              </p>
              <p className="mt-2 text-sm leading-6 text-slate-600">
                {props.theme === "aurora" ? "透明白界面更明亮，适合白天阅读。" : "深沉黑界面更克制，适合长时间专注。"}
              </p>
            </div>
            <div className="liquid-glass rounded-xl px-4 py-4">
              <p className="text-xs font-semibold uppercase tracking-[0.16em] text-slate-500">
                登录方式
              </p>
              <p className="mt-2 text-sm leading-6 text-slate-600">
                在登录和注册之间切换，验证码会随输入状态保持同步。
              </p>
            </div>
          </div>
        </div>

        <AuthPanel {...props} />
      </section>
    </main>
  );
}
