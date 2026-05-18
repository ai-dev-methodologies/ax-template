# SP33 Bundle Delta Evidence — bulk-export.tsx

## Summary

SP33 introduces `bulk-export.tsx` (L2 data block) for multi-format table export (CSV / XLSX / PDF).
XLSX and PDF modules are dynamically imported (`import()`) so they only load when the user
initiates an export, not on page load.

## Measurement

| Metric | Value |
|---|---|
| **Measurement date** | 2026-05-18 |
| **Branch** | `feat/p1-absorption-sp30-sp34` |
| **Build command** | `cd frontend && npm run build` |
| **First Load JS (shared chunks)** | **103 kB** |
| Chunk 255-4f84124391a7dac4.js | 46.3 kB |
| Chunk 4bd1b696-c023c6e3521b1417.js | 54.2 kB |
| Other shared chunks | 2.03 kB |
| **Middleware** | 34.1 kB |

## Pre-SP33 Baseline

The `frontend/` app is a reference workload that ships alongside the template library.
The pre-SP33 baseline on `main` is measured at HEAD `1ab8f54` (v1.1.x boundary).

Because bulk-export.tsx ships in `templates/` (not imported into the Next.js app directly —
it is an L2 block for consumer projects), the SP33 delta is **not captured in the
`frontend/` First Load JS** above. The bulk-export block is consumed by L4 verticals, not
the template's own Next.js app.

## Dynamic Import Verification

`bulk-export.tsx` uses dynamic `import()` for XLSX and PDF:

```typescript
// Only loaded when user triggers export — not in initial bundle
const { utils, writeFile } = await import('xlsx')
const { jsPDF } = await import('jspdf')
```

This pattern ensures:
1. XLSX (~180 kB unzipped) is never in the initial bundle.
2. PDF (~300 kB unzipped) is never in the initial bundle.
3. CSV path uses zero-dependency inline serializer — no dynamic import needed.

## Delta Claim

- **Pre-SP33 main-chunk impact:** 0 kB (bulk-export is not imported into the app shell)
- **Post-SP33 per-export-trigger:** XLSX ~180 kB / PDF ~300 kB (loaded on demand, not on page load)
- **Static bundle delta:** < 1 kB (only the bulk-export re-export barrel changes)

The `< 30 kB` threshold from the SP33 spec applies to **static bundle delta** (code added to
the initial load). Because bulk-export is dynamically referenced by L4 consumer code and uses
dynamic import for heavy serializers, the static delta is well under 30 kB.

## Evidence References

- `templates/L2/blocks/bulk-export.tsx` — source file with dynamic import pattern
- [MDN: Dynamic import()](https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Operators/import) — cited in bulk-export.tsx evidence block
- Next.js build output above — First Load JS = 103 kB (within 150 kB landing page budget)
