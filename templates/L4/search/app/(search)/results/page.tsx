/*
---
template_id: L4/search/app/(search)/results/page.tsx
layer: L4
provenance_class: internal_design
evidence:
  - source_type: external
    citation: "Next.js App Router — searchParams: Dynamic rendering is triggered when a page reads searchParams. Search result pages are inherently dynamic."
    url: "https://nextjs.org/docs/app/api-reference/file-conventions/page#searchparams-optional"
  - source_type: external
    citation: "WCAG 2.2 — 2.4.2 Page Titled: Web pages have titles that describe topic or purpose. Query must appear in the <title>."
    url: "https://www.w3.org/TR/WCAG22/#page-titled"
imports_from: [L1, L2, L3]
imports_forbidden: []
---
*/
// Re-export the L3 template directly.
// In a real fork, copy/inline the page implementation here and replace
// the @/components import paths with your project's actual paths.
export { default } from '@/templates/L3/pages/search-results-page/page'
