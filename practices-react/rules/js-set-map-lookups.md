---
title: Use Set/Map for repeated membership lookups
impact: LOW-MEDIUM
impactDescription: "Avoids repeated linear scans. Build the Set/Map once, then use has/get instead of array includes/find. Pays off when the lookup is repeated; the build itself is O(n)."
tags:
  - javascript
  - set
  - map
  - data-structures
  - performance
applicable_to:
  - react
  - nextjs
  - vite
spec_ref: "specs/react-practices-l0.yaml#REACT-PRACTICES-JS-001"
verification:
  type: eslint
  rule_id: "ax/no-array-includes-in-loop"
  status: planned
  notes: "Custom ESLint rule planned: flag `array.includes(...)` or `array.find(...)` inside an iteration callback (.filter / .map / for-of / while) when the same array is closed over and not mutated in the loop. Until shipped: peer review checkpoint."
provenance:
  pilot: true
  pipeline_version: "2026-05-16"
  pipeline_steps: [phaseA_multi_source, phaseB_audit_4check, phaseC_codex_consensus]
  easy_rule_test: "Selected as the 'simple JS rule' case to validate the pipeline does not over-engineer simple rules. Result: pipeline produced a tight, well-caveated rule in ~10 minutes; pipeline cost scales appropriately."
audit:
  accuracy:
    status: verified-with-shorthand
    last_verified: "2026-05-16"
    notes: "TC39 spec guarantees sublinear (not strictly O(1)) Set/Map access. Engines commonly implement near-O(1) hash tables. 'O(n) → O(1)' is acceptable catalog shorthand if the formal note appears."
  freshness:
    status: current
    last_verified: "2026-05-16"
    next_review_by: "2026-08-14"
    notes: "Stable JS primitive — not version-sensitive."
  completeness:
    status: complete
    amendments:
      - "Added construction-cost caveat (Set/Map build is O(n))"
      - "Added formal sublinear note (not strictly O(1) per spec)"
      - "Added object-key reference-identity trap"
  gap_check:
    status: complete
upstream:
  - id: vercel-react-best-practices
    title: "Vercel agent skill: react-best-practices (rule: js-set-map-lookups)"
    url: "https://github.com/vercel-labs/agent-skills/blob/main/skills/react-best-practices/rules/js-set-map-lookups.md"
    role: "seed"
  - id: mdn-set
    title: "MDN — Set"
    url: "https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Set"
    role: "primitive-semantics"
evidence:
  - upstream_id: vercel-react-best-practices
    section: "js-set-map-lookups"
    quote: "Convert arrays to Set/Map for repeated membership checks."
  - source_type: external
    citation: "MDN — Set (spec guarantee: 'access times that are sublinear on the number of elements in the collection')"
    url: "https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Set"
  - source_type: external
    citation: "MDN — Set (Set.has is, on average, faster than Array.prototype.includes for length equal to set size; object keys use reference identity, primitives use SameValueZero)"
    url: "https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Set"
codex_consensus:
  reviewer: "codex-cli 0.130.0, model_reasoning_effort=medium"
  reviewed_at: "2026-05-16"
  verdict: SHIP_WITH_AMEND
  agreements:
    - "All four audit verdicts confirmed"
    - "Three caveats are minimum needed (construction, sublinear formal, object reference)"
    - "Easy rule pipeline cost was appropriate; no over-engineering"
sibling_rules:
  - js-index-maps
---

## Use Set/Map for repeated membership lookups

**Impact: LOW-MEDIUM — Avoids repeated linear scans. Build the Set/Map once, then use `has`/`get` instead of scanning an array with `includes`/`find`. Pays off when the lookup is repeated; the build itself is O(n).**

### When it pays off

The Set/Map build cost is O(n). The lookup savings only matter when you do **many** lookups against the same collection. Two checks against a Set you just built is a wash; one check is a regression.

Rule of thumb: build a Set/Map outside the loop, use it inside the loop.

### Correct

```typescript
// Built once, used N times → wins linearly with N.
const allowedIds = new Set(['a', 'b', 'c' /*, ...*/])
const allowed = items.filter((item) => allowedIds.has(item.id))
```

### Incorrect

```typescript
// O(n × m): each includes() rescans the full allowedIds array.
const allowedIds = ['a', 'b', 'c' /*, ...*/]
const allowed = items.filter((item) => allowedIds.includes(item.id))
```

### Caveats

- **Formal complexity is sublinear, not strictly O(1).** TC39 only guarantees "access times that are sublinear on the number of elements in the collection." Engines commonly implement near-O(1) hash tables; treat "O(1) average" as engineering shorthand, not a spec guarantee.
- **Build cost is O(n).** If your lookup happens once, an array scan is fine — you save nothing by building a Set first.
- **Object keys match by reference identity, not value.** Primitive keys use SameValueZero (so `NaN === NaN` for Set purposes). For collections keyed by domain objects, prefer storing an id (primitive) and comparing by id.

```typescript
const cache = new Set<{ id: number }>()
cache.add({ id: 1 })
cache.has({ id: 1 })   // false — different object reference

// Prefer keying on the primitive id:
const cacheById = new Set<number>()
cacheById.add(1)
cacheById.has(1)       // true
```

### Verification

- Static check (planned): custom ESLint rule `ax/no-array-includes-in-loop`. Flags `arr.includes(x)` or `arr.find(...)` inside `.filter`, `.map`, `for-of`, or `while` when the array is closed over and not mutated inside the loop.
- Manual: bundle/profiler review for hot iteration sites.

Sources for this rule:

- [Vercel agent-skills: js-set-map-lookups](https://github.com/vercel-labs/agent-skills/blob/main/skills/react-best-practices/rules/js-set-map-lookups.md)
- [MDN — Set](https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Set)
