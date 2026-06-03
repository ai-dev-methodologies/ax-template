import type { ReactNode } from "react";
import StatusBadge from "@/components/showcase/ax-blocks/status-badge";
import { Button04 } from "@/components/showcase/ax-blocks/animated-arrow-button";
import { Component as CategoryBarChart } from "@/components/showcase/ax-blocks/category-bar-chart";
import { PrimeButton } from "@/components/showcase/ax-blocks/prime-button";
import { AnimatedBadge } from "@/components/showcase/ax-blocks/animated-badge";
import { ImageSwiper } from "@/components/showcase/ax-blocks/image-swiper";
import { AnimatedFeatureCard } from "@/components/showcase/ax-blocks/animated-feature-card";

export const metadata = {
  title: "ax-template — UI/UX showcase",
  description: "Codified 21st.dev component blocks, composed and verified in a real Next.js build.",
};

const swiperImages = [
  "https://picsum.photos/seed/ax1/640/420",
  "https://picsum.photos/seed/ax2/640/420",
  "https://picsum.photos/seed/ax3/640/420",
];

function Cell({ title, source, children }: { title: string; source: string; children: ReactNode }) {
  return (
    <section className="flex flex-col gap-3 rounded-xl border bg-card p-5 text-card-foreground">
      <header className="flex items-center justify-between gap-2">
        <h2 className="text-sm font-semibold">{title}</h2>
        <code className="truncate text-xs text-muted-foreground">{source}</code>
      </header>
      <div className="flex min-h-24 flex-wrap items-center justify-center gap-3">{children}</div>
    </section>
  );
}

export default function ShowcasePage() {
  return (
    <main className="mx-auto max-w-6xl px-6 py-12">
      <h1 className="text-3xl font-semibold tracking-tight">ax-template — UI/UX</h1>
      <p className="mt-2 max-w-2xl text-sm text-muted-foreground">
        Codified 21st.dev components, normalized to ax conventions (semantic design tokens, a11y, no
        <code className="px-1">@/lib</code> imports) and verified by the catalog&rsquo;s own block-lint.
        Composed here into a real Next.js build — the UI/UX layer of the ax-template.
      </p>

      <div className="mt-10 grid grid-cols-1 gap-5 md:grid-cols-2">
        <Cell title="Status Badge" source="status-badge">
          <StatusBadge status="success" />
          <StatusBadge status="pending" />
          <StatusBadge status="failed" />
          <StatusBadge status="in_review" />
        </Cell>

        <Cell title="Animated Badge" source="animated-badge">
          <AnimatedBadge text="New" />
          <AnimatedBadge text="Beta" />
        </Cell>

        <Cell title="Buttons" source="prime-button · animated-arrow-button">
          <PrimeButton>Get started</PrimeButton>
          <Button04 text="Continue" />
        </Cell>

        <Cell title="Feature Card" source="animated-feature-card">
          <AnimatedFeatureCard
            title="Composition kit"
            description="Recommend, codify, and verify UI blocks."
            imageSrc="https://picsum.photos/seed/axf/480/320"
            featureNumber="01"
            handle="@ax-template"
          />
        </Cell>

        <Cell title="Image Swiper" source="image-swiper">
          <div className="w-64">
            <ImageSwiper images={swiperImages} />
          </div>
        </Cell>

        <Cell title="Category Bar Chart" source="category-bar-chart (shadcn → L1 mapped)">
          <div className="w-full">
            <CategoryBarChart />
          </div>
        </Cell>
      </div>
    </main>
  );
}
