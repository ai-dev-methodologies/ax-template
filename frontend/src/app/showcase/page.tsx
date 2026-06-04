import Link from "next/link";
import personas from "@/components/showcase/personas-showcase.json";
import type { PersonaData } from "@/components/showcase/persona-showcase";

export const metadata = {
  title: "ax-template — UI/UX showcase",
  description: "Persona-driven showcase: the recommender selects codified blocks per persona.",
};

const DATA = personas as Record<string, PersonaData>;

export default function ShowcaseHub() {
  const entries = Object.entries(DATA);
  return (
    <main className="mx-auto max-w-6xl px-6 py-12">
      <h1 className="text-3xl font-semibold tracking-tight">ax-template — UI/UX</h1>
      <p className="mt-2 max-w-2xl text-sm text-muted-foreground">
        Persona-driven showcase. The recommender selects which of the codified 21st.dev blocks fit each
        persona (by <code className="px-1">affinity</code>/<code className="px-1">avoid</code>), and each
        route re-skins them with that persona&rsquo;s theme — the design-decision agent driving the
        rendered UI, not a flat gallery.
      </p>

      <div className="mt-10 grid grid-cols-1 gap-5 sm:grid-cols-2 lg:grid-cols-3">
        {entries.map(([slug, p]) => (
          <Link
            key={slug}
            href={`/showcase/${slug}`}
            className="flex flex-col gap-2 rounded-xl border bg-card p-5 text-card-foreground transition-colors hover:border-foreground/30"
          >
            <h2 className="text-base font-semibold">{p.name}</h2>
            <p className="text-xs text-muted-foreground">
              {p.blocks.length} blocks · radius {p.theme.radius} · {p.theme.accent_saturation} saturation
            </p>
            <p className="text-xs text-muted-foreground">motion: {p.motion_budget}</p>
          </Link>
        ))}
      </div>
    </main>
  );
}
