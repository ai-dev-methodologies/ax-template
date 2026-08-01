---
title: "User-facing strings in L4 templates must use t() — no hardcoded Korean or natural-language literals"
rule_id: no-hardcoded-user-facing-string-in-l4
impact: HIGH
impactDescription: "Hardcoded natural-language strings in L4 templates break i18n: the app cannot switch between ko-KR and en-US, and all Korean text appears in English-locale builds."
tags:
  - i18n
  - locale
  - korean
  - l4-template
  - l2-block
applicable_to:
  - react
  - nextjs
provenance_class: internal_design
applies_to: paths_created_after_2026-05-18
excludes:
  - templates/L4/auth/**
  - templates/L4/crud/**
  - templates/L4/payment/**
  - templates/L4/practices/**
  - templates/L4/notification/**
  - templates/L4/audit-log/**
  - templates/L4/file-storage/**
  - templates/L4/search/**
protects_template_id: templates/L2/blocks/translation-boundary.tsx
failing_fixture_path: practices-react/evals/fixtures/no_hardcoded_i18n/fail_korean_literal/
spec_ref: "specs/react-practices-l0.yaml#REACT-PRACTICES-I18N-001"
verification:
  type: regex_scan
  pattern: "Korean Unicode range \\u3131-\\u3163 \\uAC00-\\uD7A3 in JSX outside t() wrapper"
  status: fixture_driven
  notes: |
    Fixture _run.sh implements the check via a Python regex scan.
    Pass fixture: uses t('key') — exits 0.
    Fail fixture: contains <button>결제하기</button> — exits 1.
    Existing-l4-must-skip: rule excludes pre-2026-05-18 L4 paths — exits 0.
evidence:
  # Re-anchored 2026-08-01 (BACKLOG P2-73): the previous quote was an ADAPTED code example
  # (the page's example reads `useTranslations('HomePage')`; 'Payment' was substituted for
  # this rule), so it was never verbatim page text. Quote below is copied verbatim from the
  # 2026-08-01 extractor output appended to the snapshot.
  - source_type: upstream_id
    upstream_id: next-intl-2026-05
    section: "useTranslations"
    quote: "Messages represent the translations that are available per language and can be provided either locally or loaded from a remote data source."
  - source_type: external
    anchors: generic_principle_only
    citation: "next-intl docs — using t() for all user-visible text to enable locale switching"
    url: "https://next-intl.dev/docs/usage/messages"
    quoted_at: "2026-05-18"
  - source_type: external
    anchors: generic_principle_only
    citation: "Unicode — Hangul syllable block U+AC00 to U+D7A3; Hangul jamo U+3131 to U+3163"
    url: "https://unicode.org/charts/PDF/UAC00.pdf"
    quoted_at: "2026-05-18"
decided_at: "2026-05-18"
next_review_by: "2026-11-18"
---

## User-facing strings in L4 templates must use `t()` — no hardcoded Korean or natural-language literals

**Impact: HIGH — Hardcoded Korean (한글) string literals in JSX break the i18n contract: the application cannot switch to English locale, and templates become non-reusable for non-Korean enterprise deployments.**

**Scope (Option β):** This rule applies **only to files created on or after 2026-05-18**. Existing L4 domains (auth, crud, payment, practices, notification, audit-log, file-storage, search) are explicitly excluded — their string migration is deferred to a future P1 sprint.

### The violation — hardcoded Korean literal in JSX

```tsx
// ❌ WRONG — hardcoded 한글 literal; breaks ko-KR ↔ en-US switching
export default function PaymentPage() {
  return (
    <div>
      <h1>결제</h1>
      <button>결제하기</button>     {/* ← hardcoded Korean, not t() */}
      <p>금액을 입력해 주세요.</p>  {/* ← hardcoded Korean */}
    </div>
  )
}
```

### Correct — all user-facing text via `t()`

```tsx
// ✅ CORRECT — locale-aware strings via next-intl t()
'use client'
import { useTranslations } from 'next-intl'

export default function PaymentPage() {
  const t = useTranslations('Payment')
  return (
    <div>
      <h1>{t('title')}</h1>
      <button>{t('submit')}</button>
      <p>{t('amountPrompt')}</p>
    </div>
  )
}
```

Corresponding message file (`messages/ko.json`):
```json
{
  "Payment": {
    "title": "결제",
    "submit": "결제하기",
    "amountPrompt": "금액을 입력해 주세요."
  }
}
```

English translation (`messages/en.json`):
```json
{
  "Payment": {
    "title": "Payment",
    "submit": "Pay Now",
    "amountPrompt": "Please enter the amount."
  }
}
```

### Detect the violation

Pattern: Korean Unicode characters (`ㄱ–ㅣ` jamo, `가–힣` syllables) appearing in JSX string literals **outside** a `t()` function call.

The `_run.sh` fixture script implements this as a Python regex scan:
- Regex: `[ㄱ-ㅣ가-힣]` in `.tsx`/`.jsx` files
- Exclusion: if the Korean text appears as an argument to `t(` (i.e., inside `t('...')` or `t("...")`) it is permitted
- Exclusion: pre-2026-05-18 L4 domains are skipped entirely

### Why this rule exists

Korean enterprise forks of ax-template must support at minimum two locales: `ko-KR` (default) and `en-US`. Hardcoded Korean strings in new L4 domains:
1. Break the locale switch — switching to English still renders Korean text
2. Create template coupling — templates become Korea-only instead of fork-adaptable
3. Fail the composition-kit promise — a US fork of the template cannot replace strings without modifying component code

The `TranslationBoundary` L2 block (see `templates/L2/blocks/translation-boundary.tsx`) wraps subtrees that depend on translations and provides a graceful fallback when messages fail to load.

See also: `blueprints/i18n-policy-manifest.yaml` for the full locale policy including KRW formatting rules.
