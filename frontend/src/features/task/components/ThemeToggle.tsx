import { MoonStar, SunMedium } from "lucide-react";
import { ThemeMode } from "../TaskUiTypes";

interface ThemeToggleProps {
  theme: ThemeMode;
  onThemeChange: (theme: ThemeMode) => void;
}

export function ThemeToggle({ theme, onThemeChange }: ThemeToggleProps) {
  const nextTheme: ThemeMode = theme === "aurora" ? "sunrise" : "aurora";
  const label = theme === "aurora" ? "切换到深沉黑主题" : "切换到透明白主题";
  const currentLabel = theme === "aurora" ? "透明白" : "深沉黑";

  return (
    <button
      className="liquid-glass inline-flex h-10 items-center gap-2 rounded-full px-3 text-sm font-semibold text-slate-700 shadow-sm transition hover:-translate-y-0.5 hover:text-slate-950"
      type="button"
      aria-label={label}
      onClick={() => onThemeChange(nextTheme)}
    >
      {theme === "aurora" ? <SunMedium aria-hidden="true" size={16} /> : <MoonStar aria-hidden="true" size={16} />}
      <span>{currentLabel}</span>
    </button>
  );
}
