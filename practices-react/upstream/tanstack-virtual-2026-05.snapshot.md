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
