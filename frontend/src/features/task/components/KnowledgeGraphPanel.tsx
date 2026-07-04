import { Network, RotateCcw, CircleDot } from "lucide-react";
import { PointerEvent, useEffect, useMemo, useRef, useState } from "react";
import { KnowledgeGraphResponse } from "../../../api/taskApi";

interface KnowledgeGraphPanelProps {
  graph: KnowledgeGraphResponse | null;
  isLoading: boolean;
  error: string | null;
  onRefresh: () => void;
}

type GraphNode = KnowledgeGraphResponse["nodes"][number];
type GraphEdge = KnowledgeGraphResponse["edges"][number];

interface PositionedNode extends GraphNode {
  x: number;
  y: number;
  radius: number;
  fill: string;
  stroke: string;
}

interface GraphLayout {
  nodes: PositionedNode[];
  edges: GraphEdge[];
}

interface CanvasSize {
  width: number;
  height: number;
}

export function KnowledgeGraphPanel({ graph, isLoading, error, onRefresh }: KnowledgeGraphPanelProps) {
  const shellRef = useRef<HTMLDivElement | null>(null);
  const canvasRef = useRef<HTMLCanvasElement | null>(null);
  const [canvasSize, setCanvasSize] = useState<CanvasSize>({ width: 0, height: 360 });
  const [selectedNodeId, setSelectedNodeId] = useState<string | null>(null);
  const nodes = graph?.nodes ?? [];
  const edges = graph?.edges ?? [];

  const layout = useMemo(() => buildLayout(nodes, edges, canvasSize), [canvasSize, edges, nodes]);
  const selectedNode = useMemo(
    () => nodes.find((node) => node.id === selectedNodeId) ?? null,
    [nodes, selectedNodeId]
  );
  const selectedStats = useMemo(() => {
    if (!selectedNode) {
      return null;
    }

    const relatedEdges = edges.filter(
      (edge) => edge.source === selectedNode.id || edge.target === selectedNode.id
    );
    return {
      relatedEdges: relatedEdges.length,
      outgoing: relatedEdges.filter((edge) => edge.source === selectedNode.id).length,
      incoming: relatedEdges.filter((edge) => edge.target === selectedNode.id).length
    };
  }, [edges, selectedNode]);

  useEffect(() => {
    const element = shellRef.current;
    if (!element) {
      return;
    }

    const measure = () => {
      const width = Math.max(1, Math.floor(element.clientWidth));
      setCanvasSize({ width, height: 360 });
    };

    measure();

    if (typeof ResizeObserver === "undefined") {
      return;
    }

    const observer = new ResizeObserver(measure);
    observer.observe(element);
    return () => observer.disconnect();
  }, []);

  useEffect(() => {
    if (nodes.length > 0 && !nodes.some((node) => node.id === selectedNodeId)) {
      setSelectedNodeId(nodes[0].id);
    }
  }, [nodes, selectedNodeId]);

  useEffect(() => {
    drawGraph(canvasRef.current, layout, canvasSize, isLoading);
  }, [canvasSize, isLoading, layout]);

  function handleCanvasClick(event: PointerEvent<HTMLCanvasElement>) {
    const canvas = canvasRef.current;
    if (!canvas || layout.nodes.length === 0) {
      return;
    }

    const rect = canvas.getBoundingClientRect();
    const x = event.clientX - rect.left;
    const y = event.clientY - rect.top;
    const hitNode = layout.nodes.find((node) => {
      const dx = node.x - x;
      const dy = node.y - y;
      return Math.sqrt(dx * dx + dy * dy) <= node.radius + 8;
    });

    if (hitNode) {
      setSelectedNodeId(hitNode.id);
    }
  }

  const documentNodes = nodes.filter((node) => node.type === "document");
  const conceptNodes = nodes.filter((node) => node.type === "concept");
  const otherNodes = nodes.filter((node) => node.type !== "document" && node.type !== "concept");

  return (
    <section className="glass-panel liquid-glass relative overflow-hidden p-4 sm:p-5">
      <div className="mb-4 flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <div className="flex items-center gap-2">
            <Network aria-hidden="true" size={18} className="text-yellow-600" />
            <h2 className="text-base font-semibold text-slate-950">知识图谱</h2>
          </div>
          <p className="mt-1 text-sm text-slate-500">Canvas 优先渲染资料、概念和关联，便于直接看见知识流动。</p>
        </div>
        <button
          className="inline-flex h-9 items-center justify-center gap-2 rounded-lg border border-slate-200 bg-white/80 px-3 text-sm font-semibold text-slate-700 transition hover:border-yellow-200 hover:text-yellow-700"
          type="button"
          onClick={onRefresh}
        >
          <RotateCcw aria-hidden="true" size={15} className={isLoading ? "animate-spin" : ""} />
          刷新图谱
        </button>
      </div>

      {error ? (
        <div className="mb-3 rounded-lg bg-red-50 px-3 py-2 text-sm font-medium text-red-700">{error}</div>
      ) : null}

      <div className="grid gap-4 lg:grid-cols-[minmax(0,1.55fr)_minmax(260px,0.75fr)]">
        <div ref={shellRef} className="space-y-3">
          <div className="flex flex-wrap gap-2">
            <span className="liquid-glass rounded-full px-3 py-1 text-xs font-semibold text-blue-700">
              {documentNodes.length} 个资料节点
            </span>
            <span className="liquid-glass rounded-full px-3 py-1 text-xs font-semibold text-yellow-700">
              {conceptNodes.length} 个概念节点
            </span>
            <span className="liquid-glass rounded-full px-3 py-1 text-xs font-semibold text-slate-600">
              {edges.length} 条关联
            </span>
            <span className="liquid-glass rounded-full px-3 py-1 text-xs font-semibold text-slate-500">
              {otherNodes.length} 个其他节点
            </span>
          </div>

          <div className="relative overflow-hidden rounded-3xl border border-white/70 bg-white/65 shadow-[0_20px_45px_rgba(15,23,42,0.08)]">
            <canvas
              ref={canvasRef}
              aria-label="知识图谱画布"
              className="block h-[360px] w-full cursor-pointer"
              height={360}
              width={Math.max(canvasSize.width, 1)}
              onPointerDown={handleCanvasClick}
            />
            {isLoading ? (
              <div className="absolute inset-0 flex items-center justify-center bg-white/35 text-sm font-medium text-slate-600 backdrop-blur-[1px]">
                正在刷新图谱
              </div>
            ) : null}
            {nodes.length === 0 ? (
              <div className="absolute inset-0 flex items-center justify-center px-6 text-center text-sm leading-6 text-slate-500">
                上传并索引资料后，Canvas 会在这里展示资料、概念与关系。
              </div>
            ) : null}
          </div>
        </div>

        <div className="space-y-3">
          <div className="liquid-glass rounded-2xl p-4">
            <p className="text-xs font-semibold uppercase tracking-[0.14em] text-slate-400">当前选中</p>
            {selectedNode ? (
              <div className="mt-3 space-y-2">
                <div className="flex items-center gap-2">
                  <CircleDot aria-hidden="true" size={16} className="text-blue-600" />
                  <p className="text-sm font-semibold text-slate-950">{selectedNode.label}</p>
                </div>
                <p className="text-sm leading-6 text-slate-600">
                  类型：{selectedNode.type} · 权重：{selectedNode.weight}
                </p>
                {selectedStats ? (
                  <p className="text-sm leading-6 text-slate-600">
                    关联：{selectedStats.relatedEdges} 条，其中 {selectedStats.outgoing} 条出边，{selectedStats.incoming} 条入边。
                  </p>
                ) : null}
              </div>
            ) : (
              <p className="mt-3 text-sm leading-6 text-slate-500">点击画布中的节点即可查看详情。</p>
            )}
          </div>

          <div className="liquid-glass rounded-2xl p-4">
            <p className="text-xs font-semibold uppercase tracking-[0.14em] text-slate-400">图谱说明</p>
            <p className="mt-2 text-sm leading-6 text-slate-600">
              资料节点靠左，概念节点靠右，关系线在中间串起知识结构。节点点击后会展示局部关联。
            </p>
          </div>
        </div>
      </div>
    </section>
  );
}

function buildLayout(nodes: GraphNode[], edges: GraphEdge[], canvasSize: CanvasSize): GraphLayout {
  const width = Math.max(canvasSize.width, 1);
  const height = Math.max(canvasSize.height, 1);
  const padding = 42;

  const documents = nodes.filter((node) => node.type === "document");
  const concepts = nodes.filter((node) => node.type === "concept");
  const others = nodes.filter((node) => node.type !== "document" && node.type !== "concept");

  const positioned: PositionedNode[] = [
    ...placeColumn(documents, width * 0.2, padding, height),
    ...placeColumn(concepts, width * 0.78, padding, height),
    ...placeColumn(others, width * 0.5, padding, height)
  ];

  return { nodes: positioned, edges };
}

function placeColumn(nodes: GraphNode[], x: number, padding: number, height: number): PositionedNode[] {
  if (nodes.length === 0) {
    return [];
  }

  if (nodes.length === 1) {
    return [positionNode(nodes[0], x, height / 2)];
  }

  const top = padding + 16;
  const bottom = Math.max(top + 16, height - padding - 16);
  const step = (bottom - top) / Math.max(nodes.length - 1, 1);

  return nodes.map((node, index) => positionNode(node, x, top + step * index));
}

function positionNode(node: GraphNode, x: number, y: number): PositionedNode {
  const radius = 18 + Math.min(10, Math.max(0, node.weight));
  const palette =
    node.type === "document"
      ? { fill: "#dbeafe", stroke: "#2563eb" }
      : node.type === "concept"
        ? { fill: "#fef3c7", stroke: "#d97706" }
        : { fill: "#e2e8f0", stroke: "#475569" };

  return { ...node, x, y, radius, ...palette };
}

function drawGraph(
  canvas: HTMLCanvasElement | null,
  layout: GraphLayout,
  canvasSize: CanvasSize,
  isLoading: boolean
) {
  if (!canvas || canvasSize.width <= 0 || canvasSize.height <= 0) {
    return;
  }

  const context = canvas.getContext("2d");
  if (!context) {
    return;
  }

  const dpr = window.devicePixelRatio || 1;
  canvas.width = Math.floor(canvasSize.width * dpr);
  canvas.height = Math.floor(canvasSize.height * dpr);
  canvas.style.width = `${canvasSize.width}px`;
  canvas.style.height = `${canvasSize.height}px`;
  context.setTransform(dpr, 0, 0, dpr, 0, 0);
  context.clearRect(0, 0, canvasSize.width, canvasSize.height);

  const background = context.createLinearGradient(0, 0, canvasSize.width, canvasSize.height);
  background.addColorStop(0, "rgba(255,255,255,0.82)");
  background.addColorStop(1, "rgba(248,250,252,0.92)");
  context.fillStyle = background;
  context.fillRect(0, 0, canvasSize.width, canvasSize.height);

  context.save();
  context.strokeStyle = "rgba(148,163,184,0.45)";
  context.lineWidth = 1.5;
  layout.edges.forEach((edge) => {
    const source = layout.nodes.find((node) => node.id === edge.source);
    const target = layout.nodes.find((node) => node.id === edge.target);
    if (!source || !target) {
      return;
    }

    context.beginPath();
    context.moveTo(source.x, source.y);
    const midX = (source.x + target.x) / 2;
    const curveOffset = Math.min(68, Math.abs(target.x - source.x) * 0.18 + 20);
    context.quadraticCurveTo(midX, Math.min(source.y, target.y) - curveOffset, target.x, target.y);
    context.stroke();

    const labelX = (source.x + target.x) / 2;
    const labelY = (source.y + target.y) / 2 - 10;
    context.fillStyle = "rgba(71,85,105,0.75)";
    context.font = "11px Inter, system-ui, sans-serif";
    context.textAlign = "center";
    context.fillText(edge.label, labelX, labelY);
  });
  context.restore();

  layout.nodes.forEach((node) => {
    context.save();
    context.beginPath();
    context.fillStyle = node.fill;
    context.strokeStyle = node.stroke;
    context.lineWidth = 2;
    context.arc(node.x, node.y, node.radius, 0, Math.PI * 2);
    context.fill();
    context.stroke();

    context.fillStyle = "#0f172a";
    context.font = "600 12px Inter, system-ui, sans-serif";
    context.textAlign = "center";
    context.textBaseline = "top";
    context.fillText(node.label, node.x, node.y + node.radius + 6);
    context.restore();
  });

  if (isLoading) {
    context.save();
    context.fillStyle = "rgba(15,23,42,0.08)";
    context.fillRect(0, 0, canvasSize.width, canvasSize.height);
    context.restore();
  }
}
