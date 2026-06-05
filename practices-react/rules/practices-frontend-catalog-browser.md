---
title: "The practices catalog browser UI must list both catalogs with counts, filter by category, render rule detail with metadata, and 404 unknown rules"
rule_id: practices-frontend-catalog-browser
impact: LOW
impactDescription: "A catalog browser that lists only one catalog hides half the rules; one with a stale hardcoded count misleads; a category filter that does not span both Java and React catalogs gives incomplete results; a rule-detail page that does not 404 an unknown id renders a broken/blank page; missing breadcrumbs strand the user. The browser is the human window into the catalog — it must faithfully reflect what is on disk."
tags:
  - practices
  - frontend
  - catalog
  - routing
  - navigation
applicable_to:
  - react
  - nextjs
spec_ref: "specs/practices-frontend-l0.yaml#PRACTICES-FE-001"
verification:
  type: review
  notes: |
    Reviewer confirms the catalog browser against specs/practices-frontend-l0.yaml: the index lists ALL
    Java rules from practices/rules/**/*.md (001) AND all React rules from practices-react/rules/**/*.md
    (002) with title/impact/tags, shows a combined count (003), and groups by catalog with headings (009);
    the app-shell sidebar links to /practices (010). The category page filters by prefix and spans BOTH
    catalogs (004, 005), with an EmptyState when no rules match (011). The rule-detail page renders the
    full markdown body for a rule id (006), shows title/impact/tags/spec_ref metadata (007), returns a
    404 not-found state for an unknown id (008), and has a breadcrumb back to category + index (012). The
    counts are derived from the files on disk, not hardcoded.
evidence:
  - source_type: external
    citation: "React Docs — Reacting to input with state (declarative UI): the browser renders list/filtered/empty/not-found states declaratively from the catalog data (PRACTICES-FE-004/008/011)"
    url: "https://react.dev/learn/reacting-to-input-with-state"
    quote: "React provides a declarative way to manipulate the UI. Instead of manipulating individual pieces of the UI directly, you describe the different states that your component can be in, and switch between them in response to the user input."
    quoted_at: "2026-06-06"
decided_at: "2026-06-06"
---

## The practices catalog browser UI must list both catalogs with counts, filter by category, render rule detail, and 404 unknown rules

**Impact: LOW — The practices browser is the human-facing window into the catalog, and its only job is to faithfully reflect what is on disk. The failure modes are quiet: it lists only the Java rules and silently omits the React catalog; it shows a hardcoded count that drifts from reality; a category filter spans one catalog and misses matching rules in the other; a rule-detail page for a mistyped id renders blank instead of a 404; breadcrumbs are missing and the user is stranded three levels deep. None crash — they just misrepresent the catalog. React renders the browser's states declaratively — *you describe the different states that your component can be in, and switch between them in response to the user input*.**

There are twelve load-bearing requirements — the items of `specs/practices-frontend-l0.yaml`, all governed by this rule.

**Index (PRACTICES-FE-001..003, 009, 010).** Lists ALL Java rules from `practices/rules/**/*.md` (001) and all React rules from `practices-react/rules/**/*.md` (002) with title/impact/tags; shows a combined count derived from the files (003); groups by catalog with clear headings (009); the app-shell sidebar links to `/practices` (010).

**Category (PRACTICES-FE-004, 005, 011).** Filters rules by prefix (e.g. `/practices/category/async`) (004), spanning BOTH catalogs (005), with an EmptyState when none match (011).

**Rule detail (PRACTICES-FE-006..008, 012).** Renders the full markdown body for a rule id (006); shows title/impact/tags/spec_ref metadata (007); returns a 404 not-found state for an unknown id (008); has a breadcrumb back to the category and index (012).

**Incorrect — only one catalog, hardcoded count, no 404 on unknown rule:**

```tsx
const rules = await loadJavaRules();                 // VIOLATION: omits React catalog (PRACTICES-FE-002)
return <h1>147 rules</h1>;                            // VIOLATION: hardcoded count drifts (PRACTICES-FE-003)
const rule = rules.find(r => r.id === id);           // VIOLATION: no 404 when undefined (PRACTICES-FE-008)
return <Markdown>{rule.body}</Markdown>;              // crashes / blank on unknown id
```

**Correct — both catalogs, on-disk count, 404 + breadcrumb:**

```tsx
const java = await loadRules('practices/rules');     // PRACTICES-FE-001
const react = await loadRules('practices-react/rules'); // PRACTICES-FE-002
<RuleCount total={java.length + react.length} />;    // derived from disk (PRACTICES-FE-003)
// rule detail
const rule = allRules.find(r => r.id === id);
if (!rule) return <NotFound />;                       // 404 state (PRACTICES-FE-008)
return (<><Breadcrumb /><RuleMeta rule={rule} /><Markdown>{rule.body}</Markdown></>); // 007/012/006
```

Verification: review-tier. Catalog fidelity is a data-reflection property with no compile signal. Verify by review against `specs/practices-frontend-l0.yaml`: the index lists both catalogs with an on-disk count and headings; category filtering spans both catalogs with an EmptyState; rule detail renders the body + metadata, 404s an unknown id, and has a breadcrumb. When a fork-receiver wires real tests (count equals file count; unknown id → 404), this rule's verification may be upgraded from review to a test-tag binding.

Reference: [React — Reacting to input with state](https://react.dev/learn/reacting-to-input-with-state)
