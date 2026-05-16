---
title: Run SVGs through SVGO with measured precision; require visual diff for logos/charts/thin strokes
impact: LOW
impactDescription: "Reduces SVG file size. Blanket --precision=1 visibly degrades icons, maps, and charts. Use measured config and visual-regression gate."
tags: [rendering, svg, optimization, svgo, assets]
applicable_to: [react, nextjs, vite]
spec_ref: "specs/react-practices-l0.yaml#REACT-PRACTICES-RENDERING-004"
verification:
  type: review
  status: manual
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
      - "Removed blanket --precision=1 recommendation"
      - "Required visual-regression check for logos/charts/maps/thin-stroke art"
      - "Noted higher precision preserved for animated SVGs and viewBox-critical art"
  gap_check: { status: complete }
upstream:
  - id: vercel-react-best-practices
    title: "Vercel agent skill: rendering-svg-precision"
    url: "https://github.com/vercel-labs/agent-skills/blob/main/skills/react-best-practices/rules/rendering-svg-precision.md"
    role: seed
evidence:
  - upstream_id: vercel-react-best-practices
    section: "rendering-svg-precision"
    quote: "Reduce SVG coordinate precision to decrease file size."
codex_consensus:
  reviewer: "codex-cli 0.130.0, model_reasoning_effort=medium"
  reviewed_at: "2026-05-16"
  verdict: SHIP_WITH_AMEND
sibling_rules: []
---

## Run SVGs through SVGO with measured precision; require visual diff for logos/charts/thin strokes

**Impact: LOW — Reduces SVG file size. Blanket `--precision=1` visibly degrades icons, maps, and charts.**

### Correct — measured SVGO config

```bash
# Default precision 3 is a safe starting point. Drop only after diffing.
npx svgo --precision=3 --multipass icon.svg

# For interface icons that are simple shapes, precision=1 is often OK.
npx svgo --precision=1 --multipass simple-icon.svg
```

### Visual regression gate

Logos, maps, charts, thin-stroke line art, and any artwork at small viewBox values MUST be diffed visually before precision reduction is committed. Acceptable workflow:

1. Optimize with the proposed precision.
2. Render before/after at the actual target sizes used in the UI.
3. Reject if any artifact is visible.

### Preserve precision for

- Animated SVGs — precision loss compounds across keyframes
- Logos at multiple sizes — visible at small renders
- Charts and visualizations — data fidelity matters
- Maps and complex paths — corners get rounded off
- Tiny viewBoxes where 0.1 unit difference is visible

### Don't ship precision changes by `--precision=1` default

The Vercel rule's example uses `--precision=1` as if it's universally safe. It's not. Default to `--precision=3`, justify any reduction with a visual diff.

### Automation

Add SVGO as a pre-commit hook on `*.svg` files with a measured `svgo.config.js` and a visual-diff CI step (Playwright snapshot or Percy/Chromatic) for the asset directory.

Sources:
- [Vercel: rendering-svg-precision](https://github.com/vercel-labs/agent-skills/blob/main/skills/react-best-practices/rules/rendering-svg-precision.md)
- [SVGO](https://github.com/svg/svgo)
