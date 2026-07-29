/*
---
template_id: L1/components/aspect-ratio
layer: L1
provenance_class: external_canonical
evidence:
  - source_type: upstream_id
    upstream_id: shadcn-ui-2026-05
    section: aspect-ratio
    quote: "Displays content within a desired ratio."
  - source_type: external
    citation: "WCAG 2.2 SC 1.4.4 Resize Text (Level AA) — full normative text, W3C Recommendation 2023-10-05"
    url: "https://www.w3.org/TR/WCAG22/#resize-text"
    quote: "Except for captions and images of text, text can be resized without assistive technology up to 200 percent without loss of content or functionality."
    quoted_at: "2026-07-29"
a11y_criteria:
  - "WCAG 2.2 SC 1.4.4 Resize Text — ensure text inside reflows at 400%"
  - "Pure layout utility; no ARIA role needed; images inside need alt"
dependencies: ["@radix-ui/react-aspect-ratio"]
drift_snapshot_ref: "practices-react/upstream/shadcn-registry-2026-05.snapshot.md#aspect-ratio"
---
*/
import * as AspectRatioPrimitive from '@radix-ui/react-aspect-ratio'

const AspectRatio = AspectRatioPrimitive.Root

export { AspectRatio }
