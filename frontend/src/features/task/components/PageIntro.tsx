interface PageIntroProps {
  title: string;
  description: string;
  eyebrow?: string;
}

export function PageIntro({ title, description, eyebrow }: PageIntroProps) {
  return (
    <section className="page-intro space-y-2">
      {eyebrow ? <p className="page-intro__eyebrow">{eyebrow}</p> : null}
      <h2 className="page-intro__title">{title}</h2>
      <p className="page-intro__description">{description}</p>
    </section>
  );
}
