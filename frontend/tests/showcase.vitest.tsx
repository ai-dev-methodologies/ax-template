import { describe, it, expect, beforeAll } from "vitest";
import { render, screen } from "@testing-library/react";

// jsdom lacks the observer APIs framer-motion's viewport features need.
beforeAll(() => {
  class ObserverStub {
    observe() {}
    unobserve() {}
    disconnect() {}
    takeRecords() {
      return [];
    }
  }
  globalThis.IntersectionObserver ??= ObserverStub as unknown as typeof IntersectionObserver;
  globalThis.ResizeObserver ??= ObserverStub as unknown as typeof ResizeObserver;
});

// The codified 21st.dev blocks, mirrored into the UI/UX layer, must render in a real React tree.
import StatusBadge from "@/components/showcase/ax-blocks/status-badge";
import { Button04 } from "@/components/showcase/ax-blocks/animated-arrow-button";
import { AnimatedBadge } from "@/components/showcase/ax-blocks/animated-badge";

describe("codified ax UI blocks render in a real React tree", () => {
  it("StatusBadge exposes an accessible status with its label", () => {
    render(<StatusBadge status="success" />);
    const el = screen.getByRole("status");
    expect(el).toHaveTextContent("Success");
  });

  it("StatusBadge resolves color via a design token, never a raw hex", () => {
    render(<StatusBadge status="in_review" />);
    const style = screen.getByRole("status").getAttribute("style") ?? "";
    expect(style).toContain("var(--ax-status-");
    expect(style).not.toMatch(/#[0-9a-fA-F]{3,6}/);
  });

  it("Button04 renders its provided label", () => {
    render(<Button04 text="Continue" />);
    expect(screen.getByText("Continue")).toBeInTheDocument();
  });

  it("AnimatedBadge renders its text", () => {
    render(<AnimatedBadge text="New" />);
    expect(screen.getByText("New")).toBeInTheDocument();
  });
});
