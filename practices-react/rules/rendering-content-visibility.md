---
title: Use content-visibility for long static sections, paired with realistic contain-intrinsic-size
impact: HIGH
impactDescription: "Skips layout/paint for off-screen sections. NOT a list virtualization replacement — DOM nodes still exist and consume memory/event budget."
tags: [rendering, css, content-visibility, long-lists]
applicable_to: [react, nextjs, vite]
spec_ref: "specs/react-practices-l0.yaml#REACT-PRACTICES-RENDERING-002"
verification:
  type: review
  status: manual
  notes: "Reviewer checks that `content-visibility` is applied to off-screen sections to skip layout/paint, and confirms it is not used as a substitute for list virtualization — the DOM nodes still exist and still cost memory/event budget."
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
      - "Distinguished from list virtualization (windowing libraries)"
      - "Required realistic contain-intrinsic-size"
      - "Noted DOM/memory/event costs unchanged"
  gap_check: { status: complete }
upstream:
  - id: vercel-react-best-practices
    title: "Vercel agent skill: rendering-content-visibility"
    url: "https://github.com/vercel-labs/agent-skills/blob/main/skills/react-best-practices/rules/rendering-content-visibility.md"
    role: seed
evidence:
  - upstream_id: vercel-react-best-practices
    section: "rendering-content-visibility"
    quote: "Apply `content-visibility: auto` to defer off-screen rendering."
codex_consensus:
  reviewer: "codex-cli 0.130.0, model_reasoning_effort=medium"
  reviewed_at: "2026-05-16"
  verdict: SHIP_WITH_AMEND
sibling_rules: []
---

## Use content-visibility for long static sections, paired with realistic contain-intrinsic-size

**Impact: HIGH — Skips layout/paint for off-screen sections. NOT a list virtualization replacement.**

### Correct

```css
.message-item {
  content-visibility: auto;
  contain-intrinsic-size: 0 80px;   /* approximate height when not yet rendered */
}
```

```tsx
function MessageList({ messages }: { messages: Message[] }) {
  return (
    <div className="overflow-y-auto h-screen">
      {messages.map((m) => (
        <div key={m.id} className="message-item">
          <Avatar user={m.author} />
          <div>{m.content}</div>
        </div>
      ))}
    </div>
  )
}
```

Browser skips paint/layout for off-screen `.message-item` elements. Visible ones render normally.

### Realistic `contain-intrinsic-size` is required

Without `contain-intrinsic-size`, the browser collapses off-screen elements to zero, causing the scrollbar to misrepresent total content height. Set an approximate height matching the rendered item. Wrong sizes cause scroll-jumps when items resolve.

### NOT a virtualization replacement

`content-visibility` keeps all DOM nodes in the tree:
- Memory cost: full DOM tree retained.
- Event budget: scroll/resize handlers see all elements.
- Selectors/queries: `document.querySelectorAll` returns all.

For truly huge lists (tens of thousands of items), use a virtualization library (TanStack Virtual, react-window) that mounts only visible items.

`content-visibility` shines for medium lists (hundreds) where virtualization adds complexity disproportionate to gain.

### Browser support

Chrome 85+ (2020), Firefox 125+ (2024), Safari 18+ (2024). For older Safari, treat as progressive enhancement.

Sources:
- [Vercel: rendering-content-visibility](https://github.com/vercel-labs/agent-skills/blob/main/skills/react-best-practices/rules/rendering-content-visibility.md)
