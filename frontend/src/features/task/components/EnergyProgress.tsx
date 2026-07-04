interface EnergyProgressProps {
  value: number;
  label: string;
  loading?: boolean;
}

export function EnergyProgress({ value, label, loading = false }: EnergyProgressProps) {
  const boundedValue = Math.min(Math.max(value, 0), 100);

  return (
    <div className="grid gap-2">
      <div className="flex items-center justify-between gap-3 text-xs font-semibold text-slate-500">
        <span className="uppercase tracking-[0.16em]">{label}</span>
        <span className="inline-flex items-center gap-1.5 rounded-full bg-white/60 px-2 py-1 text-[11px] text-slate-600">
          {loading ? "进行中" : "稳定"}
          <span>{boundedValue}%</span>
        </span>
      </div>
      <div
        className="energy-progress"
        role="progressbar"
        aria-label={label}
        aria-valuemin={0}
        aria-valuemax={100}
        aria-valuenow={boundedValue}
        aria-valuetext={`${boundedValue}%`}
        aria-busy={loading}
      >
        <span
          className={`energy-progress__fill ${loading ? "energy-progress__fill--loading" : ""}`}
          style={{ width: `${boundedValue}%` }}
        />
      </div>
    </div>
  );
}
