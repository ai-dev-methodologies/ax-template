---
title: "Combobox / autocomplete must suppress onChange filtering during IME composition (한글 IME guard)"
rule_id: combobox-respects-hangul-ime-composition
impact: HIGH
impactDescription: "Korean IME fires multiple onChange events per keystroke during syllable composition; filtering on partial input produces wrong matches and degrades UX for Korean users"
tags:
  - combobox
  - ime
  - hangul
  - accessibility
  - korean
  - l1-component
applicable_to:
  - react
  - nextjs
provenance_class: internal_design
protects_template_id: templates/L1/components/combobox.tsx
failing_fixture_path: practices/evals/fixtures/combobox-respects-hangul-ime-composition/fail_fires_during_composition/
spec_ref: "specs/react-practices-l0.yaml#REACT-PRACTICES-CLIENT-001"
verification:
  type: review
  status: manual
  notes: "Combobox onChange handler must check composingRef.current or nativeEvent.isComposing before invoking the filter/search. onCompositionStart must set the guard; onCompositionEnd must clear it and fire the deferred filter."
evidence:
  - upstream_id: mdn-addeventlistener-passive
    section: "CompositionEvent — isComposing property"
    quote: "isComposing"
  - source_type: external
    citation: "MDN Web Docs — CompositionEvent: compositionstart / compositionend lifecycle for CJK input method editors"
    url: "https://developer.mozilla.org/en-US/docs/Web/API/CompositionEvent"
    quoted_at: "2026-05-18"
  - source_type: external
    citation: "W3C UI Events specification §CompositionEvent — IME composition lifecycle (compositionstart, compositionupdate, compositionend)"
    url: "https://www.w3.org/TR/uievents/#events-composition-types"
    quoted_at: "2026-05-18"
decided_at: "2026-05-18"
---

## Combobox / autocomplete must suppress `onChange` filtering during IME composition (한글 IME guard)

**Impact: HIGH — Korean (한글) input via IME fires 2-4 `onChange` events per character while the user is mid-syllable. Filtering on these partial values produces wrong matches ('ㅎ', '하', then '한' for a single keystroke) and causes visible flickering in the dropdown.**

Korean syllables are composed from up to three jamo components (초성/중성/종성). The IME emits:
1. `compositionstart` — user begins composing
2. Multiple `compositionupdate` events — each jamo stroke triggers an `input`/`change` event
3. `compositionend` — syllable committed, final character available

Filtering the option list on `compositionupdate` values produces meaningless partial tokens. The filter must only run after `compositionend`.

### The violation — onChange fires during IME composition

```typescript
// ❌ WRONG — no isComposing guard; filters fire on 'ㅎ', '하', '한' for one keystroke
"use client";
export default function Combobox({ options, onSelect }) {
  function handleChange(e: React.ChangeEvent<HTMLInputElement>) {
    const val = e.target.value;
    setQuery(val);
    // BUG: fires on compositionupdate — incomplete syllable triggers filter
    setFiltered(options.filter(o => o.includes(val)));
  }
  return <input type="text" onChange={handleChange} />;
}
```

### Correct — composition guard suppresses filter during IME

```typescript
// ✅ CORRECT — filter fires only after composition is committed
"use client";
import { useState, useRef } from "react";

export default function Combobox({ options, onSelect }: ComboboxProps) {
  const [query, setQuery] = useState("");
  const [filtered, setFiltered] = useState<string[]>([]);
  const composingRef = useRef(false); // true while IME is mid-composition

  function handleCompositionStart() { composingRef.current = true; }

  function handleCompositionEnd(e: React.CompositionEvent<HTMLInputElement>) {
    composingRef.current = false;
    // Fire filter once syllable is committed (compositionend value is final)
    const val = (e.target as HTMLInputElement).value;
    setFiltered(options.filter(o => o.includes(val)));
  }

  function handleChange(e: React.ChangeEvent<HTMLInputElement>) {
    const val = e.target.value;
    setQuery(val);
    if (composingRef.current) return; // skip filter during CJK composition
    setFiltered(options.filter(o => o.includes(val)));
  }

  return (
    <input
      type="text"
      value={query}
      onChange={handleChange}
      onCompositionStart={handleCompositionStart}
      onCompositionEnd={handleCompositionEnd}
    />
  );
}
```

### Why this rule exists

First captured in SP14 combobox implementation. Korean users type approximately 30% of all characters via IME. Without the guard, a combobox filtering on `compositionupdate` fires a network search for every jamo stroke, causing:
1. Unnecessary search requests (3-4x traffic for Korean input)
2. Incorrect intermediate results visible in the dropdown
3. Perceived UX jank as the dropdown flickers between partial matches

The `onCompositionEnd` pattern is also required for Chinese (Pinyin/Zhuyin) and Japanese (Hiragana/Katakana) IME input — the same guard handles all CJK scripts.

Reference: [MDN Web Docs — CompositionEvent](https://developer.mozilla.org/en-US/docs/Web/API/CompositionEvent)

Reference: [W3C UI Events §CompositionEvent](https://www.w3.org/TR/uievents/#events-composition-types)
