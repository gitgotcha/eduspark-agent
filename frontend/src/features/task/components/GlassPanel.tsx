import { CSSProperties, HTMLAttributes, PropsWithChildren, useMemo } from "react";

type GlassPanelProps = PropsWithChildren<HTMLAttributes<HTMLElement>>;

export function GlassPanel({ children, className = "", ...props }: GlassPanelProps) {
  const motionStyle = useMemo(() => createMotionStyle(), []);
  const { style, ...restProps } = props;
  const mergedStyle = { ...motionStyle, ...(style as CSSProperties | undefined) };

  return (
    <section
      className={`glass-panel liquid-glass ${className}`.trim()}
      style={mergedStyle}
      {...restProps}
    >
      {children}
    </section>
  );
}

function createMotionStyle(): CSSProperties {
  const random = (min: number, max: number) => min + Math.random() * (max - min);
  const percent = (min: number, max: number) => `${random(min, max).toFixed(1)}%`;
  const seconds = (min: number, max: number) => `${random(min, max).toFixed(1)}s`;
  const degrees = (min: number, max: number) => `${random(min, max).toFixed(1)}deg`;

  return {
    ["--flow-x1" as never]: percent(8, 26),
    ["--flow-y1" as never]: percent(10, 28),
    ["--flow-x2" as never]: percent(68, 88),
    ["--flow-y2" as never]: percent(8, 24),
    ["--flow-x3" as never]: percent(22, 46),
    ["--flow-y3" as never]: percent(62, 88),
    ["--flow-x4" as never]: percent(78, 96),
    ["--flow-y4" as never]: percent(64, 90),
    ["--glass-speed-a" as never]: seconds(4.5, 7.5),
    ["--glass-speed-b" as never]: seconds(5.5, 8.5),
    ["--glass-speed-c" as never]: seconds(4.0, 6.5),
    ["--glass-speed-d" as never]: seconds(6.0, 10.0),
    ["--glass-delay-a" as never]: seconds(-4.0, 0),
    ["--glass-delay-b" as never]: seconds(-5.0, 0),
    ["--glass-delay-c" as never]: seconds(-4.0, 0),
    ["--glass-delay-d" as never]: seconds(-5.5, 0),
    ["--glass-hue" as never]: degrees(-18, 18)
  } as CSSProperties;
}
