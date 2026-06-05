---
title: "Search UI must render a Cmd+K SearchPalette posting to the search endpoint and a ResultHighlighter that wraps matches in <mark>"
rule_id: search-frontend-palette-highlight
impact: LOW
impactDescription: "A search surface with no keyboard palette forces mouse-only access; results with no highlighting make the user hunt for why a row matched; highlighting that injects server ts_headline output as raw HTML is an XSS sink. The search UI must be keyboard-reachable and highlight matches safely via the semantic <mark> element, not dangerouslySetInnerHTML."
tags:
  - search
  - frontend
  - command-palette
  - highlighting
  - xss-safety
applicable_to:
  - react
  - nextjs
spec_ref: "specs/search-frontend-l0.yaml#SEARCH-FE-001"
verification:
  type: review
  notes: |
    Reviewer confirms the search UI against specs/search-frontend-l0.yaml: the search page renders a
    SearchPalette opened by Cmd+K that sends POST /api/v1/search and displays results with highlighting
    (001); the ResultHighlighter wraps matched terms in semantic <mark> tags derived from the backend
    ts_headline output, WITHOUT injecting raw server HTML (no dangerouslySetInnerHTML of unsanitized
    markup) (003). (Korean IME suppression is SEARCH-FE-002, governed by
    combobox-respects-hangul-ime-composition; RecentSearches localStorage is SEARCH-FE-004, governed by
    client-localstorage-schema.)
evidence:
  - source_type: external
    citation: "React Docs — Reacting to input with state (declarative UI): the palette renders idle/results/empty states declaratively (SEARCH-FE-001)"
    url: "https://react.dev/learn/reacting-to-input-with-state"
    quote: "React provides a declarative way to manipulate the UI. Instead of manipulating individual pieces of the UI directly, you describe the different states that your component can be in, and switch between them in response to the user input."
    quoted_at: "2026-06-06"
  - source_type: external
    citation: "MDN — <mark>: the HTML Mark Text element represents text marked or highlighted for reference; the ResultHighlighter uses it for matched terms (SEARCH-FE-003)"
    url: "https://developer.mozilla.org/en-US/docs/Web/HTML/Reference/Elements/mark"
    quote: "The <mark> HTML element represents text which is marked or highlighted for reference or notation purposes due to the marked passage's relevance in the enclosing context."
    quoted_at: "2026-06-06"
decided_at: "2026-06-06"
---

## Search UI must render a Cmd+K palette posting to the search endpoint and a ResultHighlighter that wraps matches in <mark>

**Impact: LOW — A good search surface is keyboard-first and shows the user WHY each result matched. A palette opened by Cmd+K (`SearchPalette`) makes search reachable without the mouse and is the expected interaction in a modern app. Match highlighting via the semantic `<mark>` element — *the HTML element ... represents text which is marked or highlighted for reference or notation purposes due to the marked passage's relevance in the enclosing context* — shows the matched terms accessibly. The trap is highlighting by injecting the backend's `ts_headline` output as raw HTML: that is an XSS sink. The ResultHighlighter wraps matches in `<mark>` from the structured headline data, never `dangerouslySetInnerHTML` of unsanitized server markup.**

There are two load-bearing requirements here (SEARCH-FE-002 IME → `combobox-respects-hangul-ime-composition`; SEARCH-FE-004 localStorage → `client-localstorage-schema`).

**SearchPalette (SEARCH-FE-001).** The search page renders a SearchPalette opened by Cmd+K that sends `POST /api/v1/search` and displays results with highlighting (Korean IME-safe input, per SEARCH-FE-002).

**ResultHighlighter (SEARCH-FE-003).** Matched terms are wrapped in semantic `<mark>` tags derived from the backend `ts_headline` output — WITHOUT injecting raw server HTML (no `dangerouslySetInnerHTML` of unsanitized markup).

**Incorrect — no palette; highlight by injecting raw server HTML (XSS):**

```tsx
<input onChange={e => search(e.target.value)} />            {/* VIOLATION: no Cmd+K palette (SEARCH-FE-001) */}
<div dangerouslySetInnerHTML={{ __html: result.tsHeadline }} /> {/* VIOLATION: raw server HTML = XSS (SEARCH-FE-003) */}
```

**Correct — Cmd+K palette; safe <mark> highlighting from structured segments:**

```tsx
useHotkey('mod+k', () => setPaletteOpen(true));             // SEARCH-FE-001
const results = await api.post('/v1/search', { q });
// ResultHighlighter renders structured segments, marking matches with <mark> (no raw HTML)  SEARCH-FE-003
<>{segments.map(s => s.match ? <mark key={s.i}>{s.text}</mark> : <span key={s.i}>{s.text}</span>)}</>
```

Verification: review-tier. Search-UI correctness is an interaction + XSS-safety property with no compile signal. Verify by review against `specs/search-frontend-l0.yaml`: a Cmd+K SearchPalette posts to the search endpoint; the ResultHighlighter uses semantic `<mark>` from structured headline data with no raw-HTML injection. When a fork-receiver wires real tests (Cmd+K opens the palette; highlighted HTML is escaped), this rule's verification may be upgraded from review to a test-tag binding.

Reference: [MDN — <mark> element](https://developer.mozilla.org/en-US/docs/Web/HTML/Reference/Elements/mark)

Reference: [React — Reacting to input with state](https://react.dev/learn/reacting-to-input-with-state)
