---
title: "RichTextEditor and MarkdownRenderer must be imported via next/dynamic in Server Components"
rule_id: rich-content-must-use-dynamic-import
impact: HIGH
impactDescription: |
  TipTap's ProseMirror and react-markdown both use browser-only APIs. Importing
  them statically in Next.js Server Components causes SSR failures (window/document
  not defined), which crashes the page at build or runtime. In Next.js 15+, using
  next/dynamic with ssr:false is ONLY valid inside a Client Component — a wrapper
  Client Component is required.
tags:
  - rich-content
  - wysiwyg
  - markdown
  - nextjs
  - rsc
  - ssr
  - l1-component
  - l4-template
applicable_to:
  - nextjs
provenance_class: internal_design
applies_to: paths_created_after_2026-05-18
protects_template_id: templates/L1/components/rich-text-editor.tsx
failing_fixture_path: practices-react/evals/fixtures/rich-content-must-use-dynamic-import/fail_static_server_import/
spec_ref: "specs/react-practices-l0.yaml#REACT-PRACTICES-RICH-CONTENT-001"
verification:
  type: regex_scan
  pattern: "import.*RichTextEditor|import.*MarkdownRenderer"
  status: fixture_driven
  notes: |
    Fixture _run.sh checks for static imports of RichTextEditor or MarkdownRenderer
    in files that are Server Components (no 'use client' directive).
    Pass fixture: uses next/dynamic inside a Client Component wrapper.
    Fail fixture: statically imports RichTextEditor in a Server Component.
evidence:
  - source_type: external
    citation: "TipTap v2 — Getting started with React: Add 'use client' to the component; the Editor is a browser-only construct."
    url: "https://tiptap.dev/docs/editor/getting-started/install/react"
    quoted_at: "2026-05-18"
  - source_type: upstream_id
    upstream_id: tiptap-2026-05
    section: "RSC Compatibility"
    quote: "Add `'use client'` to the component; the Editor is a browser-only construct."
  - source_type: external
    citation: "Next.js 15 Docs — Lazy Loading: ssr:false is not allowed with next/dynamic in Server Components. Move it into a Client Component."
    url: "https://nextjs.org/docs/app/guides/lazy-loading"
    quoted_at: "2026-05-18"
  - source_type: upstream_id
    upstream_id: nextjs-lazy-loading
    section: "ssr:false restriction"
    quote: "in Server Components. Please move it into a Client Component."
decided_at: "2026-05-18"
next_review_by: "2026-11-18"
---

## Rule

When using `RichTextEditor` or `MarkdownRenderer` from `templates/L1/`, do NOT
import them statically in Server Components. Always consume them through a Client
Component wrapper that uses `next/dynamic` with `ssr: false`.

## Why

Both components depend on browser-only APIs:

- **RichTextEditor** (TipTap / ProseMirror): Creates DOM nodes, uses `window`,
  `document.createElement`, `MutationObserver`. SSR execution throws.
- **MarkdownRenderer**: Loads `react-markdown` and `remark-gfm` dynamically via
  `useEffect`. The component itself is safe in SSR, but TipTap integration patterns
  often import both together — enforce the dynamic rule uniformly.

## Correct pattern (Next.js 15+)

In Next.js 15+, `next/dynamic` with `ssr:false` is **only valid in Client Components**.
Create a thin Client Component wrapper:

```tsx
// src/components/rich-text-editor-client.tsx
'use client'

import dynamic from 'next/dynamic'
import type { ComponentType } from 'react'

const _RichTextEditor = dynamic(
  () => import('@templates/L1/components/rich-text-editor').then(
    m => ({ default: m.RichTextEditor })
  ),
  {
    ssr: false,
    loading: () => <textarea className="w-full h-32 rounded border p-2" readOnly />,
  }
) as unknown as ComponentType<RichTextEditorProps>

export function RichTextEditorClient(props: RichTextEditorProps) {
  return <_RichTextEditor {...props} />
}
```

Then from any Server Component:

```tsx
// page.tsx (Server Component)
import { RichTextEditorClient } from '@/components/rich-text-editor-client'

export default function Page() {
  return <RichTextEditorClient placeholder="입력하세요…" minHeight="12rem" />
}
```

## Wrong patterns

```tsx
// ❌ WRONG — static import in Server Component
import { RichTextEditor } from '@/components/rich-text-editor'

export default function Page() {
  return <RichTextEditor />  // SSR crash: window is not defined
}

// ❌ WRONG — next/dynamic with ssr:false directly in Server Component (Next.js 15+)
import dynamic from 'next/dynamic'
const Editor = dynamic(() => import('./rich-text-editor'), { ssr: false })
// Error: ssr:false is not allowed with next/dynamic in Server Components
```
