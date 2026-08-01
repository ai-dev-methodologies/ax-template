# tanstack-virtual-2026-05 — upstream snapshot (2026-08-01 refresh, append-only)

**Source URL(s):** https://tanstack.com/virtual/v3/docs/introduction (re-fetched 2026-08-01; every pre-existing section below the divider is preserved verbatim)
**HTTP status:** 200
**Fetched at:** 2026-08-01T01:47:00Z
**Extractor invocation:** `practices/scripts/snapshot-extract.sh https://tanstack.com/virtual/v3/docs/introduction`
**Fetch receipt:** `practices/upstream/_FETCH-RECEIPTS.yaml` id `r097`
**Body SHA-256 (below the `---` divider, header excluded):** c80347230e780da399d6c605ab50dfb766b14d7d882a8541dd2b1f61cdd1d214

---

# Upstream Snapshot — @tanstack/react-virtual v3

**id:** tanstack-virtual-2026-05  
**source:** https://tanstack.com/virtual/v3/docs/introduction  
**fetched_at:** 2026-05-18T00:00:00Z  
**via:** WebFetch  
**tier:** 2  
**version_observed:** @tanstack/react-virtual@3.x

---

## Overview

TanStack Virtual is a headless UI utility for virtualizing long lists of elements in React.
It does not render any markup or styles — it gives you the measurements and transforms needed
to implement virtual scrolling yourself.

> "Virtualize only what you see. @tanstack/react-virtual gives you the primitives to render
> only the visible rows of a large dataset, keeping DOM node count proportional to the
> viewport rather than the data size."

## Core API: useVirtualizer

```ts
import { useVirtualizer } from '@tanstack/react-virtual'

const virtualizer = useVirtualizer({
  count: rows.length,
  getScrollElement: () => parentRef.current,
  estimateSize: () => 35, // estimated row height in px
})
```

### Key properties

| Property | Type | Description |
|---|---|---|
| `count` | `number` | Total number of items to virtualize |
| `getScrollElement` | `() => Element \| null` | Returns the scrollable container |
| `estimateSize` | `(index: number) => number` | Estimated item size in px |
| `overscan` | `number` | Extra items to render beyond viewport (default: 3) |

### Rendering virtual items

```tsx
<div ref={parentRef} style={{ height: '400px', overflow: 'auto' }}>
  <div style={{ height: `${virtualizer.getTotalSize()}px`, position: 'relative' }}>
    {virtualizer.getVirtualItems().map((virtualItem) => (
      <div
        key={virtualItem.key}
        data-index={virtualItem.index}
        ref={virtualizer.measureElement}
        style={{
          position: 'absolute',
          top: 0,
          left: 0,
          width: '100%',
          transform: `translateY(${virtualItem.start}px)`,
        }}
      >
        {rows[virtualItem.index]}
      </div>
    ))}
  </div>
</div>
```

### Key benefit

Only `getVirtualItems().length` DOM nodes are rendered at any time (proportional to viewport
height / item height), regardless of total `count`. For 5000 rows at 35px each with a 400px
viewport, approximately 11–15 rows are rendered (plus overscan).

## Installation

```bash
npm install @tanstack/react-virtual@^3
```

## License

MIT — https://github.com/TanStack/virtual

---

## Upstream refresh 2026-08-01 (verbatim extractor output)

Source: https://tanstack.com/virtual/v3/docs/introduction
HTTP status: 200 · extracted bytes: 4850 · sha256: 1bef62607d4571cffd8987645662c6e0712ccc88bb77c4e1f627a93dfa1a8d0c
Extractor: `practices/scripts/snapshot-extract.sh` (curl -> deterministic HTML->text; no model in the loop)
Fetch receipt: `practices/upstream/_FETCH-RECEIPTS.yaml` id `r097`

Everything above this divider is the previous snapshot, preserved byte-for-byte (append-only:
history is recorded, never rewritten). The block below is the UNMODIFIED extractor output for
the 2026-08-01 re-fetch of the same URL — it is the current upstream text, and any citation that
claims to quote this source verbatim must match it.

Introduction | TanStack Virtual Docs Libraries Framework Start Router Data & State Query DB Store AI UI & UX Table Charts Form Hotkeys Markdown Highlight Performance Virtual Pacer Tooling Devtools Config CLI Intent Browse all libraries Blog Blog & Release Notes About YouTube The official TanStack channel. Workshops Live sessions from the maintainers. Release Notes The latest releases and changelog. Community Channels Discord Real-time community support. GitHub Source, issues, discussions, and releases. People & Work Maintainers Meet the people maintaining the stack. Contributors Core, library, and community contributors. Showcase Teams building with TanStack. Tools Tools Builder Alpha Generate TanStack app starters. Stats NPM and ecosystem usage data. Merch View all Support Support Support Overview Find the right support path. Partners Companies supporting TanStack. OSS Sponsors Sponsors keeping TanStack open source. Enterprise Support Private consulting and expert support. Contact Get in touch with the TanStack team. About Ethos How we approach open source. Tenets The values that shape TanStack libraries. Design System Logos, tokens, and UI components. Partners Sponsorships, placements, and partner pages. Work with Partnership Inquiry Search AI Ask AI Log In Log In Libraries Blog Blog & Release Notes About YouTube The official TanStack channel. Workshops Live sessions from the maintainers. Release Notes The latest releases and changelog. Community Channels Discord Real-time community support. GitHub Source, issues, discussions, and releases. People & Work Maintainers Meet the people maintaining the stack. Contributors Core, library, and community contributors. Showcase Teams building with TanStack. Tools Tools Builder Alpha Generate TanStack app starters. Stats NPM and ecosystem usage data. Merch View all Support Support Support Overview Find the right support path. Partners Companies supporting TanStack. OSS Sponsors Sponsors keeping TanStack open source. Enterprise Support Private consulting and expert support. Contact Get in touch with the TanStack team. About Ethos How we approach open source. Tenets The values that shape TanStack libraries. Design System Logos, tokens, and UI components. Partnership Inquiry Virtual Docs React v3 Search... K Home Get Started Guides API Examples Getting Started Introduction Installation Text Measurement with Pretext React Virtual Virtual Menu Menu Home Get Started Guides API Examples React v3 Getting Started Introduction Installation Text Measurement with Pretext React Virtual AI/LLM: This documentation page is available in plain markdown format at /virtual/v3/docs/introduction .md Getting Started Introduction Copy page TanStack Virtual is a headless UI utility for virtualizing long lists of elements in JS/TS, React, Vue, Svelte, Solid, Lit, and Angular. It is not a component therefore does not ship with or render any markup or styles for you. While this requires a bit of markup and styles from you, you will retain 100% control over your styles, design and implementation. The Virtualizer # At the heart of TanStack Virtual is the Virtualizer . Virtualizers can be oriented on either the vertical (default) or horizontal axes which makes it possible to achieve vertical, horizontal and even grid-like virtualization by combining the two axis configurations together. For chat, AI streams, logs, and other reverse feeds, see the Chat guide . Here is just a quick example of what it looks like to virtualize a long list within a div using TanStack Virtual in React: tsx import { useVirtualizer } from '@tanstack/react-virtual' ; function App () { // The scrollable element for your list const parentRef = React . useRef ( null ) // The virtualizer const rowVirtualizer = useVirtualizer ({ count: 10000 , getScrollElement: () => parentRef. current , estimateSize: () => 35 , }) return ( <> { /* The scrollable element for your list */ } <div ref={parentRef} style={{ height: `400px` , overflow: 'auto' , // Make it scroll! }} > { /* The large inner element to hold all of the items */ } <div style={{ height: ` ${ rowVirtualizer. getTotalSize () } px` , width: '100%' , position: 'relative' , }} > { /* Only the visible items in the virtualizer, manually positioned to be in view */ } {rowVirtualizer. getVirtualItems (). map ((virtualItem) => ( <div key={virtualItem. key } style={{ position: 'absolute' , top: 0 , left: 0 , width: '100%' , height: ` ${ virtualItem. size } px` , transform: `translateY( ${ virtualItem. start } px)` , }} > Row {virtualItem. index } </ div > ))} </ div > </ div > </> ) } Let's dig into some more examples! Edit on GitHub Next Installation Blog @Tan_Stack on X.com @TannerLinsley on X.com GitHub YouTube Ethos Tenets Privacy Policy Terms of Service © 2026 TanStack LLC Partners Become a Partner Gold Silver Bronze Latest Posts
