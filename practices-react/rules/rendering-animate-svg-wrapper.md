---
title: For whole-SVG transform/opacity animations, animate a wrapper div instead of the <svg> element
impact: LOW
impactDescription: "Many browser compositors handle div transforms better than SVG transforms. Applies only to whole-asset animations (rotate the whole icon). Internal path/shape animation still belongs on SVG elements."
tags: [rendering, svg, css, animation, performance]
applicable_to: [react, nextjs, vite]
spec_ref: "specs/react-practices-l0.yaml#REACT-PRACTICES-RENDERING-001"
verification:
  type: review
  status: manual
  notes: "Reviewer checks that the animation targets a wrapper `<div>` (transform/opacity) only for whole-asset animations (e.g. spin the entire icon); animation of an internal path/shape stays on the SVG element itself."
provenance:
  pilot: true
  pipeline_version: "2026-05-16"
  pipeline_steps: [phaseA_multi_source, phaseB_audit_4check, phaseC_codex_consensus]
audit:
  accuracy: { status: verified, last_verified: "2026-05-16" }
  freshness: { status: current, last_verified: "2026-05-16", next_review_by: "2026-08-14" }
  completeness:
    status: complete
    amendments:
      - "Scoped to whole-SVG transform/opacity"
      - "Excluded internal path/shape animation"
      - "Added will-change during active animation only"
  gap_check: { status: complete }
upstream:
  - id: vercel-react-best-practices
    title: "Vercel agent skill: rendering-animate-svg-wrapper"
    url: "https://github.com/vercel-labs/agent-skills/blob/main/skills/react-best-practices/rules/rendering-animate-svg-wrapper.md"
    role: seed
evidence:
  - upstream_id: vercel-react-best-practices
    section: "rendering-animate-svg-wrapper"
    quote: "Many browsers don't have hardware acceleration for CSS3 animations on SVG elements."
codex_consensus:
  reviewer: "codex-cli 0.130.0, model_reasoning_effort=medium"
  reviewed_at: "2026-05-16"
  verdict: SHIP_WITH_AMEND
sibling_rules: []
---

## For whole-SVG transform/opacity animations, animate a wrapper div instead of the <svg> element

**Impact: LOW — Many browser compositors handle `div` transforms better than `<svg>` transforms. Applies only to whole-asset animations (rotate the whole icon). Internal path/shape animation still belongs on SVG elements.**

### Scope

This rule applies when you're animating the **entire SVG as a unit** with CSS `transform` / `opacity` / `translate` / `rotate` / `scale`. It does NOT apply to:
- Animating individual `<path>`, `<circle>`, `<rect>` etc. within the SVG.
- SMIL animation (`<animate>`, `<animateTransform>`).
- CSS animations targeting inner SVG attributes.

### Correct — wrapper-level transform

```tsx
function Spinner() {
  return (
    <div className="animate-spin">
      <svg viewBox="0 0 24 24" width={24} height={24}>
        <circle cx="12" cy="12" r="10" stroke="currentColor" />
      </svg>
    </div>
  )
}
```

### Incorrect — transform on the <svg>

```tsx
function Spinner() {
  return (
    <svg className="animate-spin" viewBox="0 0 24 24" width={24} height={24}>
      <circle cx="12" cy="12" r="10" stroke="currentColor" />
    </svg>
  )
}
```

### `will-change` only during active animation

```css
.animate-spin {
  animation: spin 1s linear infinite;
  will-change: transform;   /* tell browser to promote layer */
}

/* Remove will-change when animation ends — see CSS spec recommendation */
.animate-spin.idle {
  will-change: auto;
}
```

`will-change` is not free — it allocates a compositor layer. Apply only while animating; remove after.

Sources:
- [Vercel: rendering-animate-svg-wrapper](https://github.com/vercel-labs/agent-skills/blob/main/skills/react-best-practices/rules/rendering-animate-svg-wrapper.md)
