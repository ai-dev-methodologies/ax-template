# Design-Decision Algorithm — "어떤 대상에 어떤 디자인을 결정할지"

Given a **target** (what you're building) and a **persona** (who it's for), deterministically
pick — from the 3696 crawled 21st.dev components — *which* components, *which* theme, *which*
typography, and *how much* motion. The supply is real (the crawl); the policy is the personas;
this is the matcher between them.

```
 TARGET ────────────┐
 { page_type,       │      ┌─────────────────────────┐
   persona,         ├────▶ │   design_decide.py      │ ───▶  DESIGN DECISION SHEET
   content_needs? } │      │  score(component,need,  │       · theme (tokens + persona overrides)
 PERSONA (policy) ──┤      │        persona)         │       · typography + motion budget
 attributes.json ───┘      └─────────────────────────┘       · per need: ranked components + codify_action
 (3696 supply)
```

## Inputs

1. **Target** — `{ page_type, persona, content_needs? }`. If `content_needs` is omitted it is
   expanded from `PAGE_BLUEPRINTS[page_type]` (e.g. `marketing-landing →
   [headline, value-props, social-proof, primary-cta]`).
2. **Persona** — `personas.yaml` (6 archetypes). The decision-relevant projection lives in
   `design_decide.py:PERSONAS`: `motion`, `density`, `a11y`, `category_affinity`, `avoid`,
   `theme` overrides, `typo`, `motion_budget`.
3. **Supply** — `attributes.json`, derived once from the crawled TSX: per component
   `{category[], lines, has_motion, icon_lib, uses_cva, hardcoded_hex, has_aria, is_client}`.

## The scoring function (per candidate component, per need, per persona)

```
score = 0.40·affinity  +  0.25·quality  +  0.20·motion_fit  +  0.10·density_fit ,  then × avoid_knockdown
```

| Term | Meaning | Source |
|---|---|---|
| **affinity** | max persona `category_affinity` weight (0–3→0–1) over the component's categories | persona × attributes.category |
| **quality** | `has_aria` (0.5) + codify-readiness `1 − min(hex,10)/10` (0.4) + `uses_cva` (0.1) | attributes |
| **motion_fit** | `1 − |component_motion − persona.motion| / 3` — punishes a cinematic block in an operator console and a dead button in a playful app | attributes.has_motion vs persona.motion |
| **density_fit** | `1 − |lines − target_lines| / target_lines`; target = compact 90 / comfortable 180 / airy 320 | attributes.lines vs persona.density |
| **avoid_knockdown** | `(1 − 0.05)^hits` for each category in the persona's `avoid` set | persona.avoid |

A candidate that doesn't actually belong to the need's category scores `None` (excluded). The need
→ category map is `NEED_CATEGORIES` (e.g. `data-grid → [table]`, `metric → [stat, card, chart]`).

## Output — the decision sheet

For each need, the top-K components **with a `codify_action`** — exactly what the codifier must
normalize to make that import ax-conformant (`tokenize N hex`, `add role/aria`, `type variants`, or
`conformant`). So the algorithm doesn't just pick UI — it picks UI *and emits its codify work order*.

### Worked output (real run over the 3696-component catalog)

| persona × page | a representative pick | why it differs by persona |
|---|---|---|
| enterprise-operator × admin-dashboard | `jollyshopland/table`, `8bit-chart-area-step` | table/chart affinity high, motion≈1, compact density |
| consumer-delight × marketing-landing | `animated-gradient-button`, `ruixen.ui/hero-page` | high-saturation + motion=2 reward the gradient/animated picks |
| fintech-trust × checkout | `sean0205/button`, tabular metric blocks | low-saturation, conservative motion, `avoid` knocks out shader/glass |
| editorial-luxury × portfolio | `vertical-image-stack`, `hero-section-with-gradient` | image/hero/text affinity, airy density, radius 0 |
| developer-tool × docs | `circular-command-menu` | command/code affinity, dark mode, motion=1 |

The same `consumer-delight` hero candidate is **demoted for enterprise-operator** (hero ∈ its
`avoid`), and the gradient button that wins for consumer loses to a flat `button` for fintech — the
persona, not the component's popularity, drives the choice. That is the whole point.

## Theme / typography / motion decisions

Not just component lists — the sheet also emits:
- **theme** = `specs/ux/ux-design-tokens-l0.yaml` **+** persona `token_overrides` (radius, accent
  saturation, elevation, dark-default, tabular-nums). One base token contract, six skins.
- **typography** + **motion_budget** strings, so downstream codegen knows the type pairing and how
  much motion is in-budget before it reaches for framer-motion.

## Honest limitations (deferred, not hidden)

- **Category derivation is heuristic** — 62 category-page memberships + slug-keyword inference.
  ~1117/3696 remain `uncategorized`, and slug matching has false positives (`qr-code-generator`
  matched `code-surface`; a `copy-button` matched `headline`). The durable fix is the per-component
  `registry.{version}.json` tags or an LLM categorization pass — out of scope for the resource-safe
  deterministic build; logged for G004+.
- **Motion is a 0/1 proxy** (`has_motion`) widened to a 0–3 scale; it can't yet tell a spring entrance
  from a parallax. Good enough to separate "moves" from "doesn't"; not enough for fine motion taste.
- The scoring weights (`W`) are an opinionated default; they're a single dict, tunable per project.

## Run it

```bash
python3 .crawl-21st/codified/design-system/design_decide.py                 # built-in sample matrix
python3 .crawl-21st/codified/design-system/design_decide.py fintech-trust checkout
# → writes recommendations.sample.json
```
