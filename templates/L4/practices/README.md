# L4/practices — Practices Catalog Viewer

**Tenant model**: `single` — per [`specs/multi-tenant-l0.yaml#MULTI-TENANT-ISOLATION-DEFAULT-001`](../../../specs/multi-tenant-l0.yaml). (`practices` is `frontend_only` — reads static markdown; no per-tenant runtime state. `single` is the spec-compliant default in absence of an explicit strategy.) Recipes composing this domain into a multi-tenant SaaS (e.g. `b2b-admin` with `tenant_model: multi`) MUST adopt one of `MULTI-TENANT-ISOLATION-001` / `-002` / `-003` plus `MULTI-TENANT-PROPAGATION-001` + `-002` before production. fork-receivers MUST NOT assume cross-tenant data isolation in this L4 as-shipped.

**Domain mode:** `frontend_only` — reads static markdown files; no backend API.
**Status** (per [`docs/IMPLEMENTATION-STATUS.md`](../../../docs/IMPLEMENTATION-STATUS.md)): **impl** — frontend reference workload (catalog viewer); backend is intentionally absent (rules-as-code, no runtime service). Tenant model: N/A (read-only static markdown).

This is a reference workload for the `practices` domain in ax-template.
It demonstrates the `frontend_only` Spec Trio schema introduced in iter4.

## What this template provides

- **`/practices`** — index listing all Java + React rules grouped by catalog
- **`/practices/category/[prefix]`** — rules filtered by prefix (e.g. `async`, `cache`)
- **`/practices/rule/[id]`** — single rule detail with markdown body + metadata

All data is read at request time from the local `practices/rules/` and
`practices-react/rules/` directories using Next.js Server Components + `fs.readFile`.
No API calls. No database. No backend required.

## How to fork

1. **Copy** this directory into your Next.js project root:
   ```bash
   cp -r templates/L4/practices/app ./app
   cp -r templates/L4/practices/lib ./lib
   cp templates/L4/practices/next.config.ts ./next.config.ts
   ```

2. **Adjust `lib/load-rules.ts`** to point to your own markdown rule directories:
   ```ts
   function rulesGlob(catalogDir: string): string[] {
     const rulesDir = path.join(REPO_ROOT, catalogDir, 'rules')
     // ...
   }
   ```
   Update `'practices'` and `'practices-react'` to match your catalog paths.

3. **Install dependencies** — this template requires only `react` and `next`.
   No additional packages needed for the basic `<pre>` renderer.

4. **Optional: add a markdown renderer** for syntax highlighting:
   ```bash
   npm install react-markdown rehype-highlight
   ```
   Then replace the `MarkdownBody` component in `app/(practices)/rule/[id]/page.tsx`.

5. **Run the dev server:**
   ```bash
   npm run dev
   # Visit http://localhost:3000/practices
   ```

## Directory structure

```
templates/L4/practices/
├── app/
│   ├── (practices)/
│   │   ├── layout.tsx              # AppShell with sidebar + nav
│   │   ├── page.tsx                # INDEX — all rules, grouped by catalog
│   │   ├── category/
│   │   │   └── [prefix]/
│   │   │       └── page.tsx        # CATEGORY — filter by prefix
│   │   └── rule/
│   │       └── [id]/
│   │           └── page.tsx        # DETAIL — single rule, markdown + metadata
│   ├── layout.tsx                  # Root html/body layout
│   ├── page.tsx                    # Redirect → /practices
│   └── providers.tsx               # Minimal client provider tree
├── lib/
│   ├── load-rules.ts               # SERVER-ONLY: fs.readFile + React cache()
│   └── rule-parser.ts              # Inline YAML frontmatter parser
├── next.config.ts                  # Minimal Next.js config
└── README.md                       # This file
```

## Spec Trio (frontend_only)

| File | Purpose |
|------|---------|
| `specs/practices-frontend-l0.yaml` | 12 page compliance items, all `backend_operation_id: null` |
| `contracts/practices-ui.yaml` | 3 routes, all `backend_operation_id: null` + `static_source_ref` |
| `blueprints/practices-ui-manifest.yaml` | Design tokens, a11y, CWV targets |

Run `trio_integrity_guard --domain practices` to validate.

## Verification

```bash
# Playwright composition tests
npx playwright test tests/L4/practices/

# trio_integrity_guard
bash practices/evals/trio_integrity_guard.sh --domain practices

# Full domain verification
bash skills/ax-verify-domain/scripts/run.sh practices
```
