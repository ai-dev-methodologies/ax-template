import { describe, it, expect, beforeAll } from "vitest";
import { render } from "@testing-library/react";
import { axe } from "vitest-axe";
import * as axeMatchers from "vitest-axe/matchers";
import { BLOCKS } from "@/components/showcase/block-registry";

expect.extend(axeMatchers);

// framer-motion viewport features need the observer APIs jsdom lacks.
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

// jsdom has no WebGL context; the two three.js blocks are decorative canvases (containers marked
// aria-hidden / role=presentation, verified separately), not exercised through axe here.
const SKIP_AXE = new Set(["cybernetic-grid-shader", "futurastic-hero-section"]);

// Component-level a11y: disable the PAGE-structure rules (heading-order / landmark / region) that only
// have meaning for a full document, since each block renders in isolation. Keep the component rules
// (image-alt, button-name, link-name, label, aria-*, color-contrast).
const AXE_OPTS = {
  rules: {
    "heading-order": { enabled: false },
    region: { enabled: false },
    "landmark-unique": { enabled: false },
  },
};

describe("codified blocks satisfy axe-core a11y (component-level)", () => {
  for (const [slug, b] of Object.entries(BLOCKS)) {
    if (SKIP_AXE.has(slug)) continue;
    it(`${slug} has no axe violations`, async () => {
      const { container } = render(<div>{b.render()}</div>);
      const results = await axe(container, AXE_OPTS);
      expect(results).toHaveNoViolations();
    });
  }
});

// The two WebGL blocks render nothing in jsdom; assert structurally that their decorative canvas is
// hidden from assistive tech (the a11y invariant for a purely-decorative animated background).
describe("decorative WebGL blocks hide their canvas from assistive tech", () => {
  for (const slug of SKIP_AXE) {
    it(`${slug} marks its container aria-hidden`, async () => {
      const fs = await import("fs");
      const path = await import("path");
      const src = fs.readFileSync(
        path.join(process.cwd(), "src/components/showcase/ax-blocks", `${slug}.tsx`),
        "utf8",
      );
      expect(src).toMatch(/aria-hidden/);
    });
  }
});
