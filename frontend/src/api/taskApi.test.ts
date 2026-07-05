import { beforeEach, describe, expect, it, vi } from "vitest";
import {
  createKnowledgeGraph,
  changePassword,
  deleteKnowledgeGraph,
  getKnowledgeGraphRecord,
  listKnowledgeGraphs,
  resolveApiBaseUrl,
  type AuthSession
} from "./taskApi";

const session: AuthSession = {
  userId: "user-1",
  username: "teacher",
  token: "token-1"
};

describe("taskApi knowledge graph contracts", () => {
  beforeEach(() => {
    vi.stubGlobal("fetch", vi.fn(mockFetch));
  });

  it("normalizes a production /api base without creating double api paths", async () => {
    expect(resolveApiBaseUrl("/api")).toBe("");
    expect(resolveApiBaseUrl("/api/")).toBe("");
    expect(resolveApiBaseUrl("http://localhost:8080/")).toBe("http://localhost:8080");
  });

  it("uses single /api auth paths when the build base is /api", async () => {
    vi.resetModules();
    vi.stubEnv("VITE_API_BASE_URL", "/api");
    vi.stubGlobal("fetch", vi.fn(mockFetch));
    const api = await import("./taskApi");

    await api.login("teacher", "secret1");

    expect(fetch).toHaveBeenCalledWith(
      "/api/auth/login",
      expect.objectContaining({
        method: "POST",
        body: JSON.stringify({ username: "teacher", password: "secret1" })
      })
    );
    vi.unstubAllEnvs();
  });

  it("creates knowledge graph with scoped payload", async () => {
    await createKnowledgeGraph(session, { title: "图谱", documentIds: ["doc-1"] });

    expect(fetch).toHaveBeenCalledWith(
      "http://localhost:8080/api/users/user-1/knowledge-graphs",
      expect.objectContaining({
        method: "POST",
        headers: expect.objectContaining({
          Authorization: "Bearer token-1",
          "Content-Type": "application/json"
        }),
        body: JSON.stringify({ title: "图谱", documentIds: ["doc-1"] })
      })
    );
  });

  it("lists and fetches knowledge graph records by user scope", async () => {
    await listKnowledgeGraphs(session);
    await getKnowledgeGraphRecord(session, "graph-1");

    expect(fetch).toHaveBeenCalledWith(
      "http://localhost:8080/api/users/user-1/knowledge-graphs",
      expect.objectContaining({
        headers: expect.objectContaining({
          Authorization: "Bearer token-1"
        })
      })
    );
    expect(fetch).toHaveBeenCalledWith(
      "http://localhost:8080/api/users/user-1/knowledge-graphs/graph-1",
      expect.objectContaining({
        headers: expect.objectContaining({
          Authorization: "Bearer token-1"
        })
      })
    );
  });

  it("deletes knowledge graph records by user scope", async () => {
    await deleteKnowledgeGraph(session, "graph-1");

    expect(fetch).toHaveBeenCalledWith(
      "http://localhost:8080/api/users/user-1/knowledge-graphs/graph-1",
      expect.objectContaining({
        method: "DELETE",
        headers: expect.objectContaining({
          Authorization: "Bearer token-1"
        })
      })
    );
  });

  it("changes password with scoped authenticated request", async () => {
    await changePassword(session, "secret123", "secret456");

    expect(fetch).toHaveBeenCalledWith(
      "http://localhost:8080/api/users/user-1/auth/change-password",
      expect.objectContaining({
        method: "POST",
        headers: expect.objectContaining({
          Authorization: "Bearer token-1",
          "Content-Type": "application/json"
        }),
        body: JSON.stringify({ oldPassword: "secret123", newPassword: "secret456" })
      })
    );
  });
});

async function mockFetch() {
  return {
    ok: true,
    status: 200,
    json: async () => ({
      graphId: "graph-1",
      taskId: "task-1",
      status: "COMPLETED",
      id: "graph-1",
      userId: "user-1",
      title: "图谱",
      documentIds: ["doc-1"],
      graphJson: { nodes: [], edges: [] },
      createdAt: "2026-07-04T00:00:00",
      updatedAt: "2026-07-04T00:00:00"
    })
  } as Response;
}
