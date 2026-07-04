import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";
import { AuthPage } from "./AuthPage";

describe("AuthPage", () => {
  it("renders the branded intro, theme control, and auth mode switch", async () => {
    const user = userEvent.setup();
    const onThemeChange = vi.fn();
    const onAuthModeChange = vi.fn();

    render(
      <AuthPage
        authMode="login"
        username="teacher"
        password="secret123"
        captchaCode="ABCD"
        captchaInput=""
        authError={null}
        isAuthenticating={false}
        theme="aurora"
        onAuthModeChange={onAuthModeChange}
        onUsernameChange={vi.fn()}
        onPasswordChange={vi.fn()}
        onCaptchaInputChange={vi.fn()}
        onThemeChange={onThemeChange}
        onSubmit={vi.fn()}
      />
    );

    expect(screen.getByRole("heading", { name: "EduSpark 工作区" })).toBeInTheDocument();
    expect(
      screen.getByText(
        "登录后进入统一的教育任务流，从导入资料到生成练习，再到回看结果与整理产物，所有步骤都在同一处完成。"
      )
    ).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "登录模式" })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "注册模式" })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "去注册" })).toBeInTheDocument();
    expect(screen.getByLabelText("验证码输入")).toBeInTheDocument();

    await user.click(screen.getByRole("button", { name: "切换到深沉黑主题" }));
    await user.click(screen.getByRole("button", { name: "注册模式" }));

    expect(onThemeChange).toHaveBeenCalledWith("sunrise");
    expect(onAuthModeChange).toHaveBeenCalledWith("register");
  });
});
