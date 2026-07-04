import { act, render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { listWorksheets } from "../../api/taskApi";
import { TaskConsolePage } from "./TaskConsolePage";

class FakeEventSource {
  static instances: FakeEventSource[] = [];
  onmessage: ((event: MessageEvent) => void) | null = null;
  constructor(public readonly url: string) {
    FakeEventSource.instances.push(this);
  }
  close = vi.fn();
  emit(data: unknown) {
    this.onmessage?.({ data: JSON.stringify(data) } as MessageEvent);
  }
}

const mockState = {
  documents: [] as Array<{
    id: string;
    userId: string;
    fileName: string;
    mimeType: string;
    extractedText: string;
    textPreview: string;
    parseStatus: string;
    createdAt: string;
  }>,
  knowledgeGraphs: [] as Array<{
    id: string;
    userId: string;
    title: string;
    documentIds: string[];
    graphJson: {
      nodes: Array<{ id: string; label: string; type: string; weight: number }>;
      edges: Array<{ source: string; target: string; label: string; weight: number }>;
    };
    status: string;
    taskId: string;
    createdAt: string;
    updatedAt: string;
  }>,
  wrongQuestions: [] as Array<{
    id: string;
    worksheetId: string;
    attemptId: string;
    questionId: string;
    questionStem: string;
    submittedAnswer: string;
    correctAnswer: string;
    explanation: string;
    weaknessTag: string;
    retryWorksheetId: string | null;
    resolved: boolean;
    createdAt: string;
    updatedAt: string;
  }>
};

const localStorageMock = {
  store: new Map<string, string>(),
  clear() {
    this.store.clear();
  },
  getItem(key: string) {
    return this.store.has(key) ? this.store.get(key)! : null;
  },
  setItem(key: string, value: string) {
    this.store.set(key, value);
  },
  removeItem(key: string) {
    this.store.delete(key);
  }
};

describe("TaskConsolePage", () => {
  beforeEach(() => {
    localStorageMock.clear();
    FakeEventSource.instances = [];
    mockState.documents = [
      {
        id: "existing-document",
        userId: "user-1",
        fileName: "existing.txt",
        mimeType: "text/plain",
        extractedText: "Existing parsed material.",
        textPreview: "Existing parsed material.",
        parseStatus: "PARSED",
        createdAt: "2026-07-03T00:00:00"
      }
    ];
    mockState.knowledgeGraphs = [
      {
        id: "graph-1",
        userId: "user-1",
        title: "当前图谱快照",
        documentIds: ["document-1"],
        graphJson: {
          nodes: [
            { id: "doc:document-1", label: "lesson.txt", type: "document", weight: 1 },
            { id: "term:分数加减法", label: "分数加减法", type: "concept", weight: 2 }
          ],
          edges: [{ source: "doc:document-1", target: "term:分数加减法", label: "contains", weight: 2 }]
        },
        status: "COMPLETED",
        taskId: "task-graph-1",
        createdAt: "2026-07-03T00:00:00",
        updatedAt: "2026-07-03T00:00:00"
      }
    ];
    mockState.wrongQuestions = [];
    vi.stubGlobal("EventSource", FakeEventSource);
    vi.stubGlobal("localStorage", localStorageMock);
    vi.stubGlobal("HTMLAnchorElement", class HTMLAnchorElementMock {
      click() {}
    });
    vi.stubGlobal("fetch", vi.fn(mockFetch));
    vi.stubGlobal("URL", {
      createObjectURL: vi.fn(() => "blob:worksheet-word"),
      revokeObjectURL: vi.fn()
    });
    vi.spyOn(HTMLAnchorElement.prototype, "click").mockImplementation(() => {});
  });

  afterEach(() => {
    vi.unstubAllGlobals();
    localStorageMock.clear();
  });

  it("shows login form when no session is stored", () => {
    render(<TaskConsolePage />);

    expect(screen.getByLabelText("用户名")).toBeInTheDocument();
    expect(screen.getByLabelText("密码")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "登录" })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "去注册" })).toBeInTheDocument();
  });

  it("lists worksheets with the default user-scoped limit", async () => {
    await listWorksheets({ userId: "user-1", username: "teacher", token: "token-1" });

    expect(fetch).toHaveBeenCalledWith(
      "http://localhost:8080/api/users/user-1/worksheets?limit=12",
      expect.objectContaining({
        headers: expect.objectContaining({
          Authorization: "Bearer token-1"
        })
      })
    );
  });

  it("logs in and creates task with scoped authenticated request", async () => {
    render(<TaskConsolePage />);

    await login();
    expect(screen.getByText("AI 工作台")).toBeInTheDocument();
    await userEvent.type(screen.getByLabelText("教育任务指令"), "生成一份数学练习");
    await userEvent.click(screen.getByRole("button", { name: "创建任务" }));

    await waitFor(() => {
      expect(fetch).toHaveBeenCalledWith(
        "http://localhost:8080/api/auth/login",
        expect.objectContaining({
          method: "POST",
          body: JSON.stringify({ username: "teacher", password: "secret123" })
        })
      );
      expect(fetch).toHaveBeenCalledWith(
        "http://localhost:8080/api/users/user-1/tasks",
        expect.objectContaining({
          method: "POST",
          headers: expect.objectContaining({
            Authorization: "Bearer token-1"
          }),
          body: JSON.stringify({ instruction: "生成一份数学练习" })
        })
      );
    });

    expect(FakeEventSource.instances[0].url).toBe(
      "http://localhost:8080/api/users/user-1/tasks/task-1/stream?token=token-1"
    );
  });

  it("uploads selected document before creating task with document ids", async () => {
    render(<TaskConsolePage />);

    await login();
    await userEvent.type(screen.getByLabelText("教育任务指令"), "基于材料生成练习");
    await userEvent.upload(
      screen.getByLabelText("参考资料"),
      new File(["课堂材料：比例应用题。"], "lesson.txt", { type: "text/plain" })
    );
    await userEvent.click(screen.getByRole("button", { name: "创建任务" }));

    await waitFor(() => {
      expect(fetch).toHaveBeenCalledWith(
        "http://localhost:8080/api/users/user-1/documents",
        expect.objectContaining({
          method: "POST",
          headers: expect.objectContaining({
            Authorization: "Bearer token-1"
          }),
          body: expect.any(FormData)
        })
      );
      expect(fetch).toHaveBeenCalledWith(
        "http://localhost:8080/api/users/user-1/tasks",
        expect.objectContaining({
          method: "POST",
          body: JSON.stringify({ instruction: "基于材料生成练习", documentIds: ["document-1"] })
        })
      );
    });

    expect(screen.getAllByText("lesson.txt").length).toBeGreaterThan(0);
    expect(screen.getByText("课堂材料：比例应用题。")).toBeInTheDocument();
  });

  it("loads and deletes documents in document center", async () => {
    render(<TaskConsolePage />);

    await login();
    expect(await screen.findByText("existing.txt")).toBeInTheDocument();
    expect(screen.getByText("Existing parsed material.")).toBeInTheDocument();

    const initialDocumentListCalls = countFetchCalls(
      "http://localhost:8080/api/users/user-1/documents"
    );
    await userEvent.click(screen.getByRole("button", { name: "删除 existing.txt" }));

    await waitFor(() => {
      expect(fetch).toHaveBeenCalledWith(
        "http://localhost:8080/api/users/user-1/documents/existing-document",
        expect.objectContaining({
          method: "DELETE",
          headers: expect.objectContaining({ Authorization: "Bearer token-1" })
        })
      );
      expect(countFetchCalls("http://localhost:8080/api/users/user-1/documents")).toBeGreaterThan(
        initialDocumentListCalls
      );
    });
  });

  it("generates worksheet from uploaded material and exports Word", async () => {
    render(<TaskConsolePage />);

    await login();
    await userEvent.upload(
      screen.getByLabelText("参考资料"),
      new File(["课堂材料：分数加减法。"], "fraction.txt", { type: "text/plain" })
    );
    await userEvent.clear(screen.getByLabelText("练习标题"));
    await userEvent.type(screen.getByLabelText("练习标题"), "分数练习");
    await userEvent.click(screen.getByRole("button", { name: "生成练习卷" }));

    await waitFor(() => {
      expect(fetch).toHaveBeenCalledWith(
        "http://localhost:8080/api/users/user-1/worksheets",
        expect.objectContaining({
          method: "POST",
          headers: expect.objectContaining({
            Authorization: "Bearer token-1"
          }),
          body: JSON.stringify({
            title: "分数练习",
            documentIds: ["document-1"],
            questionCount: 8,
            gradeLevel: "五年级",
            difficulty: "中等",
            questionTypes: ["选择题", "判断题"],
            includeExplanation: true
          })
        })
      );
      expect(fetch).toHaveBeenCalledWith(
        "http://localhost:8080/api/users/user-1/worksheets/worksheet-1",
        expect.objectContaining({
          headers: expect.objectContaining({ Authorization: "Bearer token-1" })
        })
      );
    });

    expect(await screen.findByText("题目预览")).toBeInTheDocument();
    expect(screen.getByText("AI 出题依据")).toBeInTheDocument();
    expect(screen.getByText("围绕分数加减法生成练习。")).toBeInTheDocument();
    expect(screen.getByText(/同分母分数相加/)).toBeInTheDocument();

    await userEvent.click(screen.getByLabelText("A. 分母不变"));
    await userEvent.click(screen.getByRole("button", { name: "提交批改" }));

    await waitFor(() => {
      expect(fetch).toHaveBeenCalledWith(
        "http://localhost:8080/api/users/user-1/worksheets/worksheet-1/attempts",
        expect.objectContaining({
          method: "POST",
          headers: expect.objectContaining({ Authorization: "Bearer token-1" }),
          body: JSON.stringify({ answers: [{ questionId: "question-1", answer: "A" }] })
        })
      );
    });
    expect(await screen.findByText(/你的答案：A/)).toBeInTheDocument();

    await userEvent.click(screen.getByRole("button", { name: "导出 Word" }));

    await waitFor(() => {
      expect(fetch).toHaveBeenCalledWith(
        "http://localhost:8080/api/users/user-1/worksheets/worksheet-1/export.docx",
        expect.objectContaining({
          headers: expect.objectContaining({ Authorization: "Bearer token-1" })
        })
      );
      expect(URL.createObjectURL).toHaveBeenCalled();
      expect(URL.revokeObjectURL).toHaveBeenCalledWith("blob:worksheet-word");
    });
  });

  it("collects wrong questions and starts retry practice", async () => {
    render(<TaskConsolePage />);

    await login();
    await userEvent.upload(
      screen.getByLabelText("参考资料"),
      new File(["课堂材料：分数加减法。"], "fraction.txt", { type: "text/plain" })
    );
    await userEvent.clear(screen.getByLabelText("练习标题"));
    await userEvent.type(screen.getByLabelText("练习标题"), "分数练习");
    await userEvent.click(screen.getByRole("button", { name: "生成练习卷" }));

    await screen.findByText("题目预览");
    await userEvent.click(screen.getByLabelText("A. 分母不变"));
    await userEvent.click(screen.getByRole("button", { name: "提交批改" }));

    await waitFor(() => {
      expect(fetch).toHaveBeenCalledWith(
        "http://localhost:8080/api/users/user-1/worksheets/worksheet-1/attempts",
        expect.objectContaining({
          method: "POST",
          headers: expect.objectContaining({ Authorization: "Bearer token-1" }),
          body: JSON.stringify({ answers: [{ questionId: "question-1", answer: "A" }] })
        })
      );
    });

    await userEvent.click(screen.getByRole("button", { name: "错题" }));
    expect(await screen.findByText("同分母分数相加时，分母应该如何处理？")).toBeInTheDocument();
    await userEvent.click(screen.getByLabelText("选择错题 question-1"));
    await userEvent.click(screen.getByRole("button", { name: "错题再练" }));

    await waitFor(() => {
      expect(fetch).toHaveBeenCalledWith(
        "http://localhost:8080/api/users/user-1/wrong-questions/retry",
        expect.objectContaining({
          method: "POST",
          headers: expect.objectContaining({ Authorization: "Bearer token-1" }),
          body: JSON.stringify({ wrongQuestionIds: ["wrong-1"], title: "分数练习 - 错题再练" })
        })
      );
    });
  });

  it("registers and persists the session", async () => {
    render(<TaskConsolePage />);

    await userEvent.click(screen.getByRole("button", { name: "去注册" }));
    await userEvent.type(screen.getByLabelText("用户名"), "teacher");
    await userEvent.type(screen.getByLabelText("密码"), "secret123");
    await userEvent.type(
      screen.getByLabelText("验证码输入"),
      screen.getByLabelText("验证码").textContent ?? ""
    );
    await userEvent.click(
      screen
        .getAllByRole("button")
        .find((button) => button.getAttribute("type") === "submit" && /注册/.test(button.textContent ?? ""))!
    );

    await screen.findByText("teacher");
    expect(JSON.parse(localStorage.getItem("eduspark.auth") ?? "{}")).toEqual({
      userId: "user-1",
      username: "teacher",
      token: "token-1"
    });
  });

  it("loads final results with authenticated scoped requests and logs out", async () => {
    render(<TaskConsolePage />);

    await login();
    await userEvent.type(screen.getByLabelText("教育任务指令"), "生成一份数学练习");
    await userEvent.click(screen.getByRole("button", { name: "创建任务" }));
    await waitFor(() => expect(FakeEventSource.instances).toHaveLength(1));

    act(() => {
      FakeEventSource.instances[0].emit({ status: "COMPLETED" });
    });

    await waitFor(() => {
      expect(fetch).toHaveBeenCalledWith(
        "http://localhost:8080/api/users/user-1/tasks/task-1",
        expect.objectContaining({
          headers: expect.objectContaining({ Authorization: "Bearer token-1" })
        })
      );
      expect(fetch).toHaveBeenCalledWith(
        "http://localhost:8080/api/users/user-1/tasks/task-1/artifacts",
        expect.objectContaining({
          headers: expect.objectContaining({ Authorization: "Bearer token-1" })
        })
      );
    });

    expect(await screen.findByText("最终结果")).toBeInTheDocument();
    await userEvent.click(screen.getByRole("button", { name: "退出登录" }));

    expect(localStorage.getItem("eduspark.auth")).toBeNull();
    expect(screen.getByRole("button", { name: "登录" })).toBeInTheDocument();
  });

  it("switches workspace pages without losing the active session", async () => {
    render(<TaskConsolePage />);

    await login();
    mockState.wrongQuestions = [
      {
        id: "wrong-1",
        worksheetId: "worksheet-1",
        attemptId: "attempt-1",
        questionId: "question-1",
        questionStem: "同分母分数相加时，分母应该如何处理？",
        submittedAnswer: "B",
        correctAnswer: "A",
        explanation: "同分母分数加法中分母保持不变。",
        weaknessTag: "Needs review: 同分母分数相加时，分母应该如何处理？",
        retryWorksheetId: null,
        resolved: false,
        createdAt: "2026-07-03T00:00:00",
        updatedAt: "2026-07-03T00:00:00"
      }
    ];

    expect(screen.getByRole("heading", { name: "工作台首页" })).toBeInTheDocument();
    expect(screen.queryByRole("heading", { name: "历史与回放" })).not.toBeInTheDocument();
    await userEvent.click(screen.getByRole("button", { name: "练习卷工作区" }));
    expect(screen.getByRole("heading", { name: "练习卷工作区" })).toBeInTheDocument();
    expect(screen.getByText("teacher")).toBeInTheDocument();

    await userEvent.click(screen.getByRole("button", { name: "知识图谱" }));
    expect(screen.getByRole("heading", { name: "知识图谱专栏" })).toBeInTheDocument();
    expect(screen.getByLabelText("知识图谱画布")).toBeInTheDocument();
    expect(screen.getByText("teacher")).toBeInTheDocument();

    await userEvent.click(screen.getByRole("button", { name: "历史与回放" }));
    expect(screen.getByRole("heading", { name: "历史与回放" })).toBeInTheDocument();
    expect(await screen.findByRole("heading", { name: "历史任务" })).toBeInTheDocument();
    expect(screen.getByText("teacher")).toBeInTheDocument();

    await userEvent.click(screen.getByRole("button", { name: "个人中心" }));
    expect(screen.getByRole("heading", { name: "个人中心" })).toBeInTheDocument();
    expect(screen.getByText("teacher")).toBeInTheDocument();

    await userEvent.click(screen.getByRole("button", { name: "错题本" }));
    expect(await screen.findByRole("heading", { name: "错题本" })).toBeInTheDocument();
    expect(screen.getByText("同分母分数相加时，分母应该如何处理？")).toBeInTheDocument();
    await waitFor(() => {
      expect(fetch).toHaveBeenCalledWith(
        "http://localhost:8080/api/users/user-1/wrong-questions",
        expect.objectContaining({
          headers: expect.objectContaining({ Authorization: "Bearer token-1" })
        })
      );
    });
  });

  it("shows a workspace overview strip on the home dashboard", async () => {
    render(<TaskConsolePage />);

    await login();

    expect(screen.getByRole("heading", { name: "工作台概览" })).toBeInTheDocument();
    expect(screen.getByLabelText("工作台流转")).toBeInTheDocument();
    expect(screen.getByText("当前资料")).toBeInTheDocument();
    expect(screen.getByText("图谱快照")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "查看图谱" })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "删除快照" })).toBeInTheDocument();

    const initialKnowledgeGraphListCalls = countFetchCalls(
      "http://localhost:8080/api/users/user-1/knowledge-graphs"
    );
    await userEvent.click(screen.getByRole("button", { name: "删除快照" }));

    await waitFor(() => {
      expect(fetch).toHaveBeenCalledWith(
        "http://localhost:8080/api/users/user-1/knowledge-graphs/graph-1",
        expect.objectContaining({
          method: "DELETE",
          headers: expect.objectContaining({ Authorization: "Bearer token-1" })
        })
      );
      expect(countFetchCalls("http://localhost:8080/api/users/user-1/knowledge-graphs")).toBeGreaterThan(
        initialKnowledgeGraphListCalls
      );
    });
  });

  it("refreshes the wrong-question bank after deleting a mastered question", async () => {
    render(<TaskConsolePage />);

    await login();
    await userEvent.upload(
      screen.getByLabelText("参考资料"),
      new File(["课堂材料：分数加减法。"], "fraction.txt", { type: "text/plain" })
    );
    await userEvent.clear(screen.getByLabelText("练习标题"));
    await userEvent.type(screen.getByLabelText("练习标题"), "分数练习");
    await userEvent.click(screen.getByRole("button", { name: "生成练习卷" }));

    await screen.findByText("题目预览");
    await userEvent.click(screen.getByLabelText("A. 分母不变"));
    await userEvent.click(screen.getByRole("button", { name: "提交批改" }));

    await waitFor(() =>
      expect(fetch).toHaveBeenCalledWith(
        "http://localhost:8080/api/users/user-1/worksheets/worksheet-1/attempts",
        expect.objectContaining({
          method: "POST",
          headers: expect.objectContaining({ Authorization: "Bearer token-1" })
        })
      )
    );

    await userEvent.click(screen.getByRole("button", { name: "错题" }));
    expect(await screen.findByText("同分母分数相加时，分母应该如何处理？")).toBeInTheDocument();

    const initialWrongQuestionBankCalls = countFetchCalls(
      "http://localhost:8080/api/users/user-1/wrong-questions"
    );
    await userEvent.click(screen.getByRole("button", { name: "标记已掌握" }));

    await waitFor(() => {
      expect(countFetchCalls("http://localhost:8080/api/users/user-1/wrong-questions")).toBeGreaterThan(
        initialWrongQuestionBankCalls
      );
    });
  });

  it("shows worksheet studio tabs and intro after switching views", async () => {
    render(<TaskConsolePage />);

    await login();
    await userEvent.upload(
      screen.getByLabelText("参考资料"),
      new File(["课堂材料：分数加减法。"], "fraction.txt", { type: "text/plain" })
    );
    await userEvent.clear(screen.getByLabelText("练习标题"));
    await userEvent.type(screen.getByLabelText("练习标题"), "分数练习");
    await userEvent.click(screen.getByRole("button", { name: "生成练习卷" }));
    await screen.findByText("题目预览");
    await userEvent.click(screen.getByRole("button", { name: "练习卷工作区" }));

    expect(screen.getByRole("heading", { name: "练习卷工作区" })).toBeInTheDocument();
    expect(screen.getByText("题目预览")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "做题" })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "解析" })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "错题" })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "导出 Word" })).toBeInTheDocument();
  });

  it("renders the knowledge graph as a canvas-first surface", async () => {
    render(<TaskConsolePage />);

    await login();
    await userEvent.click(screen.getByRole("button", { name: "知识图谱" }));

    expect(screen.getByRole("heading", { name: "知识图谱专栏" })).toBeInTheDocument();
    expect(screen.getByText("Canvas 优先")).toBeInTheDocument();
    expect(screen.getByLabelText("知识图谱画布")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "刷新图谱" })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "保存当前图谱" })).toBeInTheDocument();
    expect(screen.getByText("图谱记录")).toBeInTheDocument();
    expect(screen.getByText("当前图谱快照")).toBeInTheDocument();
  });

  it("persists theme changes in local storage", async () => {
    render(<TaskConsolePage />);

    await login();

    expect(document.documentElement.dataset.theme).toBe("aurora");
    await userEvent.click(screen.getByRole("button", { name: "切换到深沉黑主题" }));
    expect(document.documentElement.dataset.theme).toBe("sunrise");
    expect(localStorage.getItem("eduspark.theme")).toBe("sunrise");
    await userEvent.click(screen.getByRole("button", { name: "切换到透明白主题" }));
    expect(document.documentElement.dataset.theme).toBe("aurora");
    expect(localStorage.getItem("eduspark.theme")).toBe("aurora");
  });
});

async function login() {
  await userEvent.type(screen.getByLabelText("用户名"), "teacher");
  await userEvent.type(screen.getByLabelText("密码"), "secret123");
  const captcha = screen.getByLabelText("验证码").textContent ?? "";
  await userEvent.type(screen.getByLabelText("验证码输入"), captcha);
  await userEvent.click(
    screen
      .getAllByRole("button")
      .find((button) => button.getAttribute("type") === "submit" && /登录/.test(button.textContent ?? ""))!
  );
  await screen.findByText("teacher");
}

async function mockFetch(input: RequestInfo | URL, init?: RequestInit) {
  const url = String(input);
  if (url.endsWith("/api/auth/login") || url.endsWith("/api/auth/register")) {
    return jsonResponse({ userId: "user-1", username: "teacher", token: "token-1" });
  }
  if (url.endsWith("/api/users/user-1/documents") && init?.method !== "POST") {
    return jsonResponse(mockState.documents);
  }
  if (url.endsWith("/api/users/user-1/knowledge-graph?limit=12")) {
    return jsonResponse({
      nodes: [
        { id: "doc:document-1", label: "lesson.txt", type: "document", weight: 1 },
        { id: "term:分数加减法", label: "分数加减法", type: "concept", weight: 2 }
      ],
      edges: [{ source: "doc:document-1", target: "term:分数加减法", label: "contains", weight: 2 }]
    });
  }
  if (url.endsWith("/api/users/user-1/knowledge-graphs") && init?.method !== "POST") {
    return jsonResponse(mockState.knowledgeGraphs);
  }
  if (url.endsWith("/api/users/user-1/knowledge-graphs") && init?.method === "POST") {
    return jsonResponse({ graphId: "graph-2", taskId: "task-graph-2", status: "PENDING" });
  }
  if (url.endsWith("/api/users/user-1/knowledge-graphs/graph-1") && init?.method === "DELETE") {
    mockState.knowledgeGraphs = [];
    return { ok: true, status: 204 } as Response;
  }
  if (url.endsWith("/api/users/user-1/documents/existing-document") && init?.method === "DELETE") {
    mockState.documents = [];
    return { ok: true, status: 204 } as Response;
  }
  if (url.endsWith("/api/users/user-1/documents") && init?.method === "POST") {
    return jsonResponse({
      id: "document-1",
      userId: "user-1",
      fileName: "lesson.txt",
      mimeType: "text/plain",
      extractedText: "课堂材料：比例应用题。",
      textPreview: "课堂材料：比例应用题。",
      parseStatus: "PARSED",
      createdAt: "2026-07-03T00:00:00"
    });
  }
  if (url.endsWith("/api/users/user-1/tasks/task-1/artifacts")) {
    return jsonResponse([
      {
        id: "artifact-1",
        taskId: "task-1",
        artifactType: "textSummaryTool",
        contentJson: "{\"summary\":\"本节课学习...\"}",
        createdAt: "2026-07-03T00:00:00"
      }
    ]);
  }
  if (url.endsWith("/api/users/user-1/tasks/task-1")) {
    return jsonResponse({
      id: "task-1",
      userId: "user-1",
      userInstruction: "生成一份数学练习",
      status: "COMPLETED",
      finalAnswer: "[{\"toolName\":\"textSummaryTool\",\"output\":{\"summary\":\"本节课学习...\"}}]"
    });
  }
  if (url.endsWith("/api/users/user-1/tasks?limit=20")) {
    return jsonResponse([
      {
        id: "task-1",
        userId: "user-1",
        userInstruction: "生成一份数学练习",
        status: "COMPLETED",
        finalAnswer: "[{\"toolName\":\"textSummaryTool\",\"output\":{\"summary\":\"本节课学习...\"}}]"
      }
    ]);
  }
  if (url.endsWith("/api/users/user-1/tasks")) {
    return jsonResponse({ taskId: "task-1", status: "PENDING" });
  }
  if (url.endsWith("/api/users/user-1/worksheets/worksheet-1/export.docx")) {
    return blobResponse(new Blob(["docx"]));
  }
  if (url.endsWith("/api/users/user-1/worksheets/worksheet-1/attempts") && init?.method === "POST") {
    mockState.wrongQuestions = [
      {
        id: "wrong-1",
        worksheetId: "worksheet-1",
        attemptId: "attempt-1",
        questionId: "question-1",
        questionStem: "同分母分数相加时，分母应该如何处理？",
        submittedAnswer: "B",
        correctAnswer: "A",
        explanation: "同分母分数加法中分母保持不变。",
        weaknessTag: "Needs review: 同分母分数相加时，分母应该如何处理？",
        retryWorksheetId: null,
        resolved: false,
        createdAt: "2026-07-03T00:00:00",
        updatedAt: "2026-07-03T00:00:00"
      }
    ];
    return jsonResponse({
      attemptId: "attempt-1",
      worksheetId: "worksheet-1",
      score: 100,
      items: [
        {
          questionId: "question-1",
          stem: "同分母分数相加时，分母应该如何处理？",
          submittedAnswer: "A",
          correctAnswer: "A",
          correct: true,
          explanation: "同分母分数加法中分母保持不变。"
        }
      ],
      weaknessSummary: "All answers are correct.",
      remediationSuggestion: "You can move on to a harder practice set.",
      createdAt: "2026-07-03T00:00:00"
    });
  }
  if (url.endsWith("/api/users/user-1/worksheets/worksheet-1/attempts")) {
    return jsonResponse([]);
  }
  if (url.endsWith("/api/users/user-1/wrong-questions") && init?.method !== "DELETE") {
    return jsonResponse(mockState.wrongQuestions.filter((item) => !item.resolved));
  }
  if (url.endsWith("/api/users/user-1/worksheets/worksheet-1/wrong-questions")) {
    return jsonResponse(mockState.wrongQuestions.filter((item) => item.worksheetId === "worksheet-1" && !item.resolved));
  }
  if (url.endsWith("/api/users/user-1/wrong-questions/wrong-1") && init?.method === "DELETE") {
    mockState.wrongQuestions = [];
    return { ok: true, status: 204 } as Response;
  }
  if (url.endsWith("/api/users/user-1/wrong-questions/retry") && init?.method === "POST") {
    mockState.wrongQuestions = mockState.wrongQuestions.map((item) => ({
      ...item,
      retryWorksheetId: "worksheet-retry-1"
    }));
    return jsonResponse({
      worksheetId: "worksheet-retry-1",
      taskId: "task-retry-1",
      status: "PENDING"
    });
  }
  if (url.endsWith("/api/users/user-1/worksheets/worksheet-1")) {
    return jsonResponse({
      id: "worksheet-1",
      userId: "user-1",
      taskId: "task-worksheet-1",
      title: "分数练习",
      documentIds: ["document-1"],
      config: {
        title: "分数练习",
        documentIds: ["document-1"],
        questionCount: 8,
        gradeLevel: "五年级",
        difficulty: "中等",
        questionTypes: ["选择题", "判断题"],
        includeExplanation: true
      },
      generationRationale: {
        summary: "围绕分数加减法生成练习。",
        keyPoints: ["同分母分数", "加减法"],
        difficultyPlan: "以中等难度为主。",
        typePlan: "选择题和判断题搭配。",
        deviationFromPreference: null
      },
      questions: [
        {
          id: "question-1",
          type: "选择题",
          stem: "同分母分数相加时，分母应该如何处理？",
          options: ["A. 分母不变", "B. 分母相加"],
          answer: "A",
          explanation: "同分母分数加法中分母保持不变。",
          difficulty: "中等",
          sourceChunkIds: ["chunk-1"]
        }
      ],
      status: "COMPLETED",
      createdAt: "2026-07-03T00:00:00",
      updatedAt: "2026-07-03T00:00:00"
    });
  }
  if (url.endsWith("/api/users/user-1/worksheets?limit=12")) {
    return jsonResponse([
      {
        id: "worksheet-1",
        taskId: "task-worksheet-1",
        title: "分数练习",
        status: "COMPLETED",
        createdAt: "2026-07-03T00:00:00",
        updatedAt: "2026-07-03T00:00:00"
      }
    ]);
  }
  if (url.endsWith("/api/users/user-1/worksheets")) {
    return jsonResponse({ worksheetId: "worksheet-1", taskId: "task-worksheet-1", status: "PENDING" });
  }
  return jsonResponse({}, false, 404);
}

function jsonResponse(body: unknown, ok = true, status = 200) {
  return {
    ok,
    status,
    json: async () => body
  } as Response;
}

function blobResponse(body: Blob) {
  return {
    ok: true,
    status: 200,
    blob: async () => body
  } as Response;
}

function countFetchCalls(expectedUrl: string) {
  return vi
    .mocked(fetch)
    .mock.calls.filter(([requestInput, init]) => String(requestInput) === expectedUrl && !init?.method)
    .length;
}
