---
name: ax-design-composer
description: Recommends MULTIPLE validated component compositions for a service plan. Use when a fork-receiver describes a product/service (a 서비스 기획) and needs persona-driven page compositions assembled from the codified 21st.dev component catalog, each mechanically validated (components exist · codify-ready · needs covered · persona-fit · avoid-respected) before it is recommended.
---

# ax-design-composer — validated component-composition recommender

Turn a **service plan** (product + audience + pages) into **multiple validated component compositions**
per page, assembled from the codified component catalog and gated by a binary validation harness. A
composition is only recommended if it passes every hard check — the ax "single command, binary verdict"
philosophy applied to design.

## When to use
A fork-receiver says, in their words, what they are building ("a B2B analytics console", "a D2C
storefront", "a creator community feed"). This skill classifies that into a **persona** + **pages**,
composes several design strategies, validates them, and returns the ones that pass.

## Pipeline (all under `practices/scripts/design-decision/`)
```
service brief ─▶ classify ─▶ compose.py ─▶ validate_composition.py ─▶ recommend.py (verdict)
 (NL)            persona+pages  N variants/page   binary hard checks      only PASS variants surfaced
```

1. **Classify** the brief into one of the 6 personas (`personas.yaml`) by audience + design intent:
   | If the product is… | persona |
   |---|---|
   | internal B2B console / admin / operator tool | `enterprise-operator` |
   | consumer app / marketing site / conversion-led | `consumer-delight` |
   | brand / portfolio / publishing / luxury | `editorial-luxury` |
   | dev tool / API docs / CLI companion (dark, code) | `developer-tool` |
   | banking / payments / regulated finance | `fintech-trust` |
   | social / creator / community / reactions | `playful-creator` |
   Pick the pages from the product's surfaces (`admin-dashboard`, `marketing-landing`, `pricing`,
   `auth`, `checkout`, `docs`, `feed`, `portfolio` — see `compose.py:PAGE_BLUEPRINTS`).

2. **Add the plan** to `service_plans.yaml` (name · persona · pages · notes), or pass it ad-hoc.

3. **Run the agent** (one command — composes every variant, validates, prints the verdict):
   ```bash
   python3 practices/scripts/design-decision/recommend.py
   # or a single page:  python3 .../recommend.py fintech-trust checkout
   ```
   Output: per page, three strategy variants — `lean` (MVP), `conversion` (full funnel),
   `premium` (brand) — each marked **✓ RECOMMENDED** or **✗ rejected** with the failed checks, plus
   the persona theme, motion budget, per-component codify actions, and over-reuse warnings.
   `recommendations.json` is the machine artifact. **exit 0 = every plan validation-GREEN.**

## What "validated" means (the hard gate — `validate_composition.py`)
A variant is only RECOMMENDED if ALL pass:
- **exist** — every component is a real catalog component (`attributes.json`)
- **codify-ready** — every pick has a known ax normalization path (`codify_action`)
- **needs-covered** — every page need resolved to ≥ 1 component
- **persona-fit** — every pick scores ≥ 0.42 (a genuine fit, not a fallback)
- **avoid-respect** — no pick's category is in the persona's `avoid` set

Soft signals are surfaced, not blocking: **over-reuse** (a component chosen for ≥ 3 needs → low
category confidence) and **low-confidence** (category not slug-proven). These flag the known
heuristic-categorization limit of the crawled supply (fix path: registry tags / LLM categorization).

## Regenerating the supply
`attributes.json` (the scored catalog the recommender consumes) is derived from the gitignored 21st.dev
crawl by `build_attributes.py`. The shipped `attributes.sample.json` (40 components) lets `recommend.py`
run out-of-the-box; regenerate the full set where the crawl exists.

## Provenance
Built on the codified-component pipeline (`codify.py` → ax rule
`ux-block-uses-design-tokens-and-a11y` + `templates/L2/blocks/status-badge.tsx`). The composer
recommends from the catalog; the codifier turns any pick into an ax-conformant block.
