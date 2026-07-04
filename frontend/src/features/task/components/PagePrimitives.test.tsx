import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";
import { EnergyProgress } from "./EnergyProgress";
import { GlassPanel } from "./GlassPanel";
import { PageIntro } from "./PageIntro";
import { ThemeToggle } from "./ThemeToggle";

describe("page primitives", () => {
  it("renders the shared intro, glass panel, theme toggle, and progress bar", async () => {
    const user = userEvent.setup();
    const onThemeChange = vi.fn();

    render(
      <div>
        <PageIntro eyebrow="工作台" title="工作台首页" description="把资料、出题、批改和错题再练收束到一个安静而有力量的工作台。" />
        <GlassPanel>
          <p>玻璃内容</p>
        </GlassPanel>
        <ThemeToggle theme="aurora" onThemeChange={onThemeChange} />
        <EnergyProgress value={64} label="正在加载" />
      </div>
    );

    expect(screen.getByText("工作台首页")).toBeInTheDocument();
    expect(screen.getByText("把资料、出题、批改和错题再练收束到一个安静而有力量的工作台。")).toBeInTheDocument();
    expect(screen.getByText("玻璃内容")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "切换到深沉黑主题" })).toHaveTextContent("透明白");
    expect(screen.getByRole("button", { name: "切换到深沉黑主题" })).toBeInTheDocument();
    expect(screen.getByLabelText("正在加载")).toBeInTheDocument();

    await user.click(screen.getByRole("button", { name: "切换到深沉黑主题" }));

    expect(onThemeChange).toHaveBeenCalledWith("sunrise");
  });
});
