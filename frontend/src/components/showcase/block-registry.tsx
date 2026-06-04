"use client";
import type { ReactNode } from "react";
import StatusBadge from "@/components/showcase/ax-blocks/status-badge";
import { Button04 } from "@/components/showcase/ax-blocks/animated-arrow-button";
import { Component as CategoryBarChart } from "@/components/showcase/ax-blocks/category-bar-chart";
import { PrimeButton } from "@/components/showcase/ax-blocks/prime-button";
import { AnimatedBadge } from "@/components/showcase/ax-blocks/animated-badge";
import { ImageSwiper } from "@/components/showcase/ax-blocks/image-swiper";
import { AnimatedFeatureCard } from "@/components/showcase/ax-blocks/animated-feature-card";
import SocialButton from "@/components/showcase/ax-blocks/social-button";
import AutoLayoutCard from "@/components/showcase/ax-blocks/auto-layout-card";
import { Component as SplitText } from "@/components/showcase/ax-blocks/split-text-effect";
import { ImageCarouselHero } from "@/components/showcase/ax-blocks/ai-image-generator-hero";
import CyberneticGridShader from "@/components/showcase/ax-blocks/cybernetic-grid-shader";
import { AuroraHero } from "@/components/showcase/ax-blocks/futurastic-hero-section";
import {
  Card,
  CardHeader,
  CardTitle,
  CardDescription,
  CardContent,
} from "@/components/showcase/ax-blocks/interfaces-card";

const swiperImages = [
  "https://picsum.photos/seed/ax1/640/420",
  "https://picsum.photos/seed/ax2/640/420",
  "https://picsum.photos/seed/ax3/640/420",
];
const heroImages = [
  { id: "1", src: "https://picsum.photos/seed/axg1/300/400", alt: "Generated 1", rotation: -8 },
  { id: "2", src: "https://picsum.photos/seed/axg2/300/400", alt: "Generated 2", rotation: 0 },
  { id: "3", src: "https://picsum.photos/seed/axg3/300/400", alt: "Generated 3", rotation: 8 },
];

export interface BlockEntry {
  label: string;
  source: string;
  /** visual-heavy blocks span the full row and get a fixed height */
  full?: boolean;
  render: () => ReactNode;
}

// slug -> how to render the codified block (props extracted from the verified flat showcase)
export const BLOCKS: Record<string, BlockEntry> = {
  "status-badge": {
    label: "Status Badge", source: "status-badge",
    render: () => (
      <>
        <StatusBadge status="success" />
        <StatusBadge status="pending" />
        <StatusBadge status="failed" />
        <StatusBadge status="in_review" />
      </>
    ),
  },
  "animated-badge": {
    label: "Animated Badge", source: "animated-badge",
    render: () => (<><AnimatedBadge text="New" /><AnimatedBadge text="Beta" /></>),
  },
  "prime-button": {
    label: "Prime Button", source: "prime-button",
    render: () => <PrimeButton>Get started</PrimeButton>,
  },
  "animated-arrow-button": {
    label: "Animated Arrow Button", source: "animated-arrow-button",
    render: () => <Button04 text="Continue" />,
  },
  "social-button": {
    label: "Social Button", source: "social-button",
    render: () => <SocialButton />,
  },
  "interfaces-card": {
    label: "Interfaces Card", source: "interfaces-card",
    render: () => (
      <Card className="w-full max-w-xs">
        <CardHeader>
          <CardTitle>Codified card</CardTitle>
          <CardDescription>Composed from the interfaces-card primitive set.</CardDescription>
        </CardHeader>
        <CardContent>
          <p className="text-sm text-muted-foreground">Card · Header · Title · Content</p>
        </CardContent>
      </Card>
    ),
  },
  "animated-feature-card": {
    label: "Feature Card", source: "animated-feature-card",
    render: () => (
      <AnimatedFeatureCard
        title="Composition kit"
        description="Recommend, codify, and verify UI blocks."
        imageSrc="https://picsum.photos/seed/axf/480/320"
        featureNumber="01"
        handle="@ax-template"
      />
    ),
  },
  "auto-layout-card": {
    label: "Auto Layout Card", source: "auto-layout-card",
    render: () => <AutoLayoutCard />,
  },
  "category-bar-chart": {
    label: "Category Bar Chart", source: "category-bar-chart",
    render: () => (<div className="w-full"><CategoryBarChart /></div>),
  },
  "split-text-effect": {
    label: "Split Text Effect", source: "split-text-effect",
    render: () => <SplitText text="ax" />,
  },
  "image-swiper": {
    label: "Image Swiper", source: "image-swiper",
    render: () => (<div className="w-64"><ImageSwiper images={swiperImages} /></div>),
  },
  "ai-image-generator-hero": {
    label: "AI Image Generator Hero", source: "ai-image-generator-hero", full: true,
    render: () => (
      <ImageCarouselHero
        title="Generate"
        subtitle="AI imagery"
        description="Turn ideas into images in seconds."
        ctaText="Try it"
        images={heroImages}
      />
    ),
  },
  "cybernetic-grid-shader": {
    label: "Cybernetic Grid Shader", source: "cybernetic-grid-shader (three.js)", full: true,
    render: () => (<div className="relative h-64 overflow-hidden"><CyberneticGridShader /></div>),
  },
  "futurastic-hero-section": {
    label: "Aurora Hero", source: "futurastic-hero-section (@react-three/fiber)", full: true,
    render: () => (<div className="relative h-64 overflow-hidden"><AuroraHero /></div>),
  },
};
