import { ReactNode } from "react";
import { PageIntro } from "../components/PageIntro";

interface WorksheetStudioPageProps {
  children: ReactNode;
}

export function WorksheetStudioPage({ children }: WorksheetStudioPageProps) {
  return (
    <section className="space-y-5">
      <PageIntro
        eyebrow="练习"
        title="练习卷工作区"
        description="从题目生成到解析、错题与再练，形成完整的练习闭环。"
      />
      {children}
    </section>
  );
}
