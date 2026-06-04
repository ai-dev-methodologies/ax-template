"use client";
import type { CSSProperties } from "react";
import { BLOCKS } from "./block-registry";

export interface PersonaData {
  name: string;
  blocks: string[];
  theme: { radius?: string; accent_saturation?: string; elevation?: string; mode?: string };
  motion_budget: string;
  motion_level: number;
  typography: string;
}

// Renders ONLY the blocks the recommender selected for this persona, re-skinned with the persona
// theme (radius override + dark mode). This is the agent driving the rendered showcase.
export function PersonaShowcase({ slug, data }: { slug: string; data: PersonaData }) {
  const isDark = data.theme.mode === "dark";
  const present = data.blocks.filter((s) => BLOCKS[s]);
  const grid = present.filter((s) => !BLOCKS[s].full);
  const full = present.filter((s) => BLOCKS[s].full);

  return (
    <div
      data-persona={slug}
      className={isDark ? "dark bg-background text-foreground" : ""}
      style={{ "--radius": data.theme.radius } as CSSProperties}
    >
      <main className="mx-auto max-w-6xl px-6 py-12">
        <a href="/showcase" className="text-sm text-muted-foreground hover:underline">
          ← all personas
        </a>
        <h1 className="mt-3 text-3xl font-semibold tracking-tight">{data.name}</h1>
        <p className="mt-2 text-sm text-muted-foreground">
          {present.length} blocks selected by the recommender (affinity/avoid) · radius{" "}
          {data.theme.radius} · {data.theme.accent_saturation} saturation · motion {data.motion_budget}
        </p>
        <p className="mt-1 text-xs text-muted-foreground">{data.typography}</p>

        <div className="mt-8 grid grid-cols-1 gap-5 md:grid-cols-2">
          {grid.map((s) => {
            const b = BLOCKS[s];
            return (
              <section
                key={s}
                className="flex flex-col gap-3 rounded-xl border bg-card p-5 text-card-foreground"
              >
                <header className="flex items-center justify-between gap-2">
                  <h2 className="text-sm font-semibold">{b.label}</h2>
                  <code className="truncate text-xs text-muted-foreground">{b.source}</code>
                </header>
                <div className="flex min-h-24 flex-wrap items-center justify-center gap-3">{b.render()}</div>
              </section>
            );
          })}
        </div>

        {full.length > 0 && (
          <div className="mt-6 flex flex-col gap-6">
            {full.map((s) => {
              const b = BLOCKS[s];
              return (
                <section key={s} className="overflow-hidden rounded-xl border">
                  <header className="flex items-center justify-between gap-2 border-b bg-card p-4 text-card-foreground">
                    <h2 className="text-sm font-semibold">{b.label}</h2>
                    <code className="text-xs text-muted-foreground">{b.source}</code>
                  </header>
                  {b.render()}
                </section>
              );
            })}
          </div>
        )}
      </main>
    </div>
  );
}
