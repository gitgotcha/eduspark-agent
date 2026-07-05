import { LogIn, MoonStar, RefreshCw, SunMedium, UserPlus } from "lucide-react";
import { FormEvent } from "react";
import { GlassPanel } from "./GlassPanel";

export interface AuthPanelProps {
  authMode: "login" | "register";
  username: string;
  password: string;
  captchaCode: string;
  captchaInput: string;
  authError: string | null;
  isAuthenticating: boolean;
  theme: "aurora" | "sunrise";
  onAuthModeChange: (mode: "login" | "register") => void;
  onUsernameChange: (value: string) => void;
  onPasswordChange: (value: string) => void;
  onCaptchaInputChange: (value: string) => void;
  onThemeChange: (theme: "aurora" | "sunrise") => void;
  onSubmit: (event: FormEvent<HTMLFormElement>) => void;
}

export function AuthPanel({
  authMode,
  username,
  password,
  captchaCode,
  captchaInput,
  authError,
  isAuthenticating,
  theme,
  onAuthModeChange,
  onUsernameChange,
  onPasswordChange,
  onCaptchaInputChange,
  onSubmit
}: AuthPanelProps) {
  const themeLabel = theme === "aurora" ? "透明白主题" : "深沉黑主题";
  const formTitle = authMode === "login" ? "登录工作台" : "创建账号";
  const formDescription =
    authMode === "login"
      ? "继续进入你的资料、任务和练习空间。"
      : "新账号会沿用同一套主题与会话体验。";

  return (
    <GlassPanel className="relative z-10 overflow-hidden p-5 sm:p-6 lg:p-7">
      <form className="grid gap-5" onSubmit={onSubmit}>
        <div className="flex items-start justify-between gap-4">
          <div className="space-y-2">
            <p className="text-xs font-semibold uppercase tracking-[0.28em] text-slate-500">
              账户入口
            </p>
            <div className="space-y-1">
              <h2 className="text-2xl font-semibold tracking-tight text-slate-950">
                {formTitle}
              </h2>
              <p className="text-sm leading-6 text-slate-600">{formDescription}</p>
            </div>
          </div>
          <div
            className="liquid-glass inline-flex items-center gap-2 rounded-full px-3 py-1.5 text-xs font-semibold text-slate-600"
            aria-label="当前主题"
          >
            {theme === "aurora" ? <SunMedium aria-hidden="true" size={14} /> : <MoonStar aria-hidden="true" size={14} />}
            {themeLabel}
          </div>
        </div>

        <div className="grid grid-cols-2 gap-2">
          <button
            className={`inline-flex h-11 items-center justify-center gap-2 rounded-full text-sm font-semibold transition ${
              authMode === "login"
                ? "bg-slate-950 text-white shadow-lg shadow-slate-950/10"
                : "liquid-glass text-slate-600 hover:text-blue-700"
            }`}
            type="button"
            onClick={() => onAuthModeChange("login")}
            aria-pressed={authMode === "login"}
            aria-label="登录模式"
          >
            <LogIn aria-hidden="true" size={16} />
            登录
          </button>
          <button
            className={`inline-flex h-11 items-center justify-center gap-2 rounded-full text-sm font-semibold transition ${
              authMode === "register"
                ? "bg-slate-950 text-white shadow-lg shadow-slate-950/10"
                : "liquid-glass text-slate-600 hover:text-blue-700"
            }`}
            type="button"
            onClick={() => onAuthModeChange("register")}
            aria-pressed={authMode === "register"}
            aria-label="注册模式"
          >
            <UserPlus aria-hidden="true" size={16} />
            注册
          </button>
        </div>

        <div className="grid gap-2">
          <label className="text-sm font-medium text-slate-700" htmlFor="auth-username">
            用户名
          </label>
          <input
            id="auth-username"
            className="liquid-glass h-12 rounded-xl px-3.5 text-base outline-none transition placeholder:text-slate-400 focus:border-blue-400 focus:ring-4 focus:ring-blue-100/80"
            value={username}
            onChange={(event) => onUsernameChange(event.target.value)}
            placeholder="输入用户名"
            autoComplete="username"
          />
        </div>

        <div className="grid gap-2">
          <label className="text-sm font-medium text-slate-700" htmlFor="auth-password">
            密码
          </label>
          <input
            id="auth-password"
            className="liquid-glass h-12 rounded-xl px-3.5 text-base outline-none transition placeholder:text-slate-400 focus:border-blue-400 focus:ring-4 focus:ring-blue-100/80"
            type="password"
            value={password}
            onChange={(event) => onPasswordChange(event.target.value)}
            placeholder="至少 6 位密码"
            minLength={6}
            autoComplete={authMode === "login" ? "current-password" : "new-password"}
          />
        </div>

        <div className="grid gap-2">
          <div className="flex items-center justify-between text-sm font-medium text-slate-700">
            <span>图形验证码</span>
            <button
              className="inline-flex items-center gap-1.5 rounded-full px-2.5 py-1 text-xs font-semibold text-blue-700 transition hover:bg-blue-50/80"
              type="button"
              onClick={() => onCaptchaInputChange("")}
            >
              <RefreshCw size={12} />
              清空
            </button>
          </div>
          <div className="grid gap-3 sm:grid-cols-[auto_minmax(0,1fr)]">
            <div
              className="liquid-glass inline-flex h-12 min-w-28 items-center justify-center rounded-xl px-4 text-lg font-semibold tracking-[0.35em] text-slate-900 shadow-inner"
              aria-label="验证码"
            >
              {captchaCode}
            </div>
            <input
              aria-label="验证码输入"
              className="liquid-glass h-12 rounded-xl px-3.5 text-base outline-none transition placeholder:text-slate-400 focus:border-blue-400 focus:ring-4 focus:ring-blue-100/80"
              value={captchaInput}
              onChange={(event) => onCaptchaInputChange(event.target.value)}
              placeholder="请输入验证码"
              maxLength={6}
              autoComplete="off"
            />
          </div>
        </div>

        {authError ? (
          <p className="rounded-xl border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-700" role="alert">
            {authError}
          </p>
        ) : null}

        <button
          className="inline-flex h-12 items-center justify-center gap-2 rounded-xl bg-slate-950 px-4 text-sm font-semibold text-white shadow-lg shadow-slate-950/10 transition hover:-translate-y-0.5 hover:bg-slate-800 disabled:translate-y-0 disabled:cursor-not-allowed disabled:bg-slate-400"
          disabled={isAuthenticating || !username.trim() || !password || !captchaCode || !captchaInput.trim()}
          type="submit"
        >
          {authMode === "login" ? <LogIn aria-hidden="true" size={16} /> : <UserPlus aria-hidden="true" size={16} />}
          {authMode === "login" ? "登录" : "注册"}
        </button>

        <button
          className="h-10 rounded-lg text-sm font-semibold text-blue-700 transition hover:bg-blue-50/80"
          type="button"
          onClick={() => onAuthModeChange(authMode === "login" ? "register" : "login")}
        >
          {authMode === "login" ? "去注册" : "去登录"}
        </button>
      </form>
    </GlassPanel>
  );
}
