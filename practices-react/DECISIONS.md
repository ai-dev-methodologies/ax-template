# DECISIONS — React catalog rule provenance + acceptance trail

> Sibling of `practices/DECISIONS.md` (the Java/Spring trail). Closes BACKLOG P2-7:
> the React catalog (99 rules + 14 ESLint rules) previously had **per-rule** `provenance:`
> blocks but **no catalog-level acceptance/decision trail**, an asymmetry vs the Java side.
> This file records HOW the React catalog was built, WHICH families it contains and why,
> the evidence basis, and the category-level ACCEPT/REJECT/DEFER decisions — so a
> fork-receiver (or auditor) can reconstruct the provenance of the React rules, not just
> trust them.

## How to read an entry

Each rule file under `practices-react/rules/*.md` already carries its own machine-readable
trail in frontmatter:

- `spec_ref:` — the owning spec item in `specs/react-practices-l0.yaml` (item-id anchored).
- `provenance:` — the build pipeline that produced the rule (see "Build pipeline" below).
- `audit:` — per-rule `accuracy` / `freshness` / `completeness` / `gap_check` status with
  `last_verified` + `next_review_by` dates. A `partially-stale` freshness entry names the
  upstream drift (e.g. a React-version change) and the re-positioning amendment.
- `upstream:` — the external source URLs; `evidence:` — verbatim quotes (same
  evidence-anchoring contract as the Java catalog; `evidence_guard` verifies STRUCTURE,
  the offline `evidence_quote_spotcheck_guard` cross-checks snapshot-backed quotes — see
  the residual gap tracked in `docs/BACKLOG.md` P2-1a/b, which applies to both catalogs).

This file is the catalog-LEVEL trail; the per-rule audit blocks are the row-level trail.

## Build pipeline (provenance)

Every React rule was produced by the 3-phase pipeline recorded in its `provenance:` block
(`pipeline_version: "2026-05-16"`):

1. **phaseA_multi_source** — gather the candidate practice from multiple authorities
   (primary: the Vercel *React Best Practices* skill; secondary: MDN Web Docs, web.dev Core
   Web Vitals, and library docs where a rule is library-specific). No rule rests on a single
   un-corroborated source.
2. **phaseB_audit_4check** — the 4-axis audit (accuracy / freshness / completeness /
   gap_check) is run and recorded per rule, with `last_verified` + `next_review_by` so a
   rule that drifts (e.g. a React API stabilizes) is flagged `partially-stale` rather than
   silently rotting.
3. **phaseC_codex_consensus** — an independent model pass (codex) cross-checks the rule's
   claim and code example before ACCEPT, to reduce single-model hallucination.

`provenance.pilot: true` on the early rules marks them as the pipeline's pilot batch (the
pipeline itself was validated on them before the catalog was scaled out).

## Rules — ACCEPTED (families)

The 99 rules cluster into evidence-anchored families. Counts are disk-true at this writing
(`ls practices-react/rules/*.md`); the canonical headline count is guarded by
`doc_headline_count_guard.sh`.

- **rerender (15)** — render-stability: stable references, memo boundaries, key discipline,
  context-split, derived-not-stored state. Source: Vercel RBP re-render section + React docs.
- **js (13)** — JS-level micro-perf and correctness inside render/handlers (allocation in
  render, array ops in loops, falsy-numeric render, etc.). Source: Vercel RBP + MDN.
- **rendering (11)** — render-strategy: Suspense/streaming boundaries, lazy/dynamic import,
  list virtualization triggers, render-blocking avoidance. Source: Vercel RBP + web.dev CWV.
- **server (8)** — Server/Client Component boundary discipline, server-data-fetching,
  no route-level client fetch. Source: Vercel RBP + Next.js App Router docs.
- **async (5)** — async composition: parallel vs waterfall fetch, Promise.all/allSettled,
  abort/cleanup. Source: Vercel RBP + MDN Promise docs.
- **bundle (5)** — bundle budget: barrel-import breadth, code-split points, dependency weight.
  Source: Vercel RBP + bundle-analysis practice.
- **client (4)** — client-state boundaries (no server-state in local state, URL-as-state).
  Source: Vercel RBP + the project's web patterns.
- **nextjs (3)** — Next.js-specific (cache directives, App Router conventions). Source: Next.js docs.
- **advanced (3)** — hooks edge-cases (ref-backed handler fallback, useEffectEvent positioning).
  Source: Vercel RBP + React 19.2 release notes (the freshness amendments live here).
- **prefer / l2 / virtualized / ux / traceid / no-\* (remainder)** — targeted single rules
  (preferred-API selections, L2 block conventions, virtualization, a UX rule, trace-id
  propagation) + the namespace rules that mirror the ESLint plugin (see below).

## ESLint plugin — `eslint-plugin-ax` (15 rules)

The 15 mechanical rules under `practices-react/eslint-plugin-ax/rules/*.js` are the
machine-enforced subset (a rule that can be a lint check IS one, rather than review-tier):
`no-app-local-ui-primitives`, `no-array-includes-in-loop`, `no-array-mutate-on-state`,
`no-broad-barrel-imports`, `no-cross-feature-deep-import`, `no-falsy-numeric-render`,
`no-feature-internal-import`, `no-god-route`, `no-inline-component-definition`,
`no-route-client-data-fetching`, `no-caller-identity-from-props` (15th rule, shipped
2026-07-20 consumer-proof wave-2 Cell 4 — see `practices/DECISIONS.md` for its full
provenance entry), and the remainder. Promotion of two of these from `warn`
to `error` is tracked as **BACKLOG P2-2** (measurement-gated) — until promoted they are
`warn`, which is honest about their current enforcement strength.

## Category-level decisions (ACCEPT / REJECT / DEFER)

- **ACCEPT — performance-family breadth (rerender + rendering + js + bundle + async).** The
  React catalog is intentionally performance-weighted because that is where Vercel RBP (the
  dominant upstream) is strongest and where AI-generated React most reliably regresses.
- **ACCEPT — server/client boundary as a first-class family.** The App Router server/client
  split is the highest-leverage correctness boundary in modern React; 8 rules + 2 ESLint
  rules (`no-route-client-data-fetching`, `no-god-route`) enforce it.
- **DEFER — visual/design-system rules to the L2/L4 block templates.** Design-quality
  guidance (layout, motion, color) lives in the L2 blocks + the global web rules, NOT as
  per-rule lint in this catalog; encoding subjective design as a rule would over-reach.
- **DEFER — React-version-coupled rules carry an explicit freshness `next_review_by`.** Rather
  than reject a rule that a future React version may obsolete (e.g. the ref-backed-handler
  fallback once `useEffectEvent` stabilized in 19.2), the rule is ACCEPTED with a recorded
  re-positioning amendment in its `audit.completeness.amendments`. The catalog ages
  gracefully instead of silently.

## Known limitations

- **Freshness drift is tracked, not eliminated.** Several rules are `freshness:
  partially-stale` with a named `next_review_by` (e.g. React 19.2 `useEffectEvent`
  stabilization re-positions the advanced ref-handler rule from "latest React" to
  "≥19.2 prefer useEffectEvent; ref is the fallback"). These are honest TODOs, not defects.
- **External-URL evidence entries** (MDN / web.dev / Next.js docs) share the same
  truth-verification gap as the Java catalog (`evidence_guard` checks structure, not that
  the quote is live on the page) — tracked centrally in `docs/BACKLOG.md` P2-1b.

## Audit

- 2026-05-16 — React catalog built via the 3-phase pipeline (phaseA multi-source → phaseB
  4-check audit → phaseC codex consensus); pilot batch validated the pipeline.
- 2026-06-16 — this DECISIONS.md created (BACKLOG P2-7) to close the catalog-level
  provenance-trail asymmetry vs `practices/DECISIONS.md`.
