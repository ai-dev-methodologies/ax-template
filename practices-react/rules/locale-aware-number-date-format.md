---
title: "Frontend number/currency/date display MUST use Intl.NumberFormat / Intl.DateTimeFormat, never raw toLocaleString() or manual date-string concatenation"
rule_id: locale-aware-number-date-format
impact: MEDIUM
impactDescription: "Bare .toLocaleString() silently follows the runtime's default locale (which differs between server-rendered and client-hydrated environments, and across replicas/containers) instead of the caller's actual locale — the same amount can render with a different grouping/symbol depending on where it happened to run. Manual date-part string concatenation (getMonth()+getDate()+getFullYear() joined with '+') hard-codes a single field order and can never switch order per locale (ko-KR uses yyyy.MM.dd; en-US uses MM/dd/yyyy). Both defects are invisible in a single-locale dev environment and only surface once a second locale or a server/client hydration mismatch appears — exactly the failure mode the S3.e-commerce checkout receipt page hit (CANARY-001)."
tags:
  - i18n
  - locale
  - formatting
  - date
  - currency
  - frontend
applicable_to:
  - react
  - nextjs
provenance_class: locked_constraint
spec_ref: "specs/i18n-policy-l0.yaml#I18N-FORMATTING-001"
failing_fixture_path: practices/evals/fixtures/locale-aware-format/fail_manual_format/
verification:
  type: guard
  status: automated
  notes: "practices/evals/locale_aware_format_guard.sh scans *.ts AND *.tsx (default root frontend/src, or --root DIR; comments stripped so a descriptive comment can't trigger a match) for four locale-blind shapes: bare .toLocaleString() with no argument; manual getMonth()/getDate()/getFullYear() '+'-concatenation (single-line OR multiline); .toFixed() on a money-named value; and string-concatenated currency symbols (\"$\" + amount). Exit 1 (signature LOCALE_FORMAT_VIOLATION) on a hit; exit 0 when the tree is clean; exit 0 (SKIP) when there are 0 *.ts/*.tsx files to scan. WIRED live into practices/evals/run-all-guards.sh as guard [91] (locale_aware_format/live), plus a pass fixture and four per-detector fail fixtures (each isolates one detector so deleting it greens exactly that fixture). Real frontend/src is 0 hits (exit 0)."
evidence:
  - source_type: external
    citation: "ECMA-402 (ECMAScript Internationalization API Specification) — Intl.NumberFormat and Intl.DateTimeFormat exist precisely so number/date display honors the BCP 47 locale tag, instead of a hard-coded format."
    url: "https://tc39.es/ecma402/#numberformat-objects"
    quote: "Objects that are instances of the NumberFormat constructor... which converts numbers to a language-sensitive digit sequence."
    quoted_at: "2026-07-19"
  - source_type: external
    citation: "MDN — Intl.DateTimeFormat: the standard mechanism for locale-sensitive date/time formatting, replacing hand-rolled string concatenation of date parts."
    url: "https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Intl/DateTimeFormat"
    quote: "The Intl.DateTimeFormat object enables language-sensitive date and time formatting."
    quoted_at: "2026-07-19"
  - source_type: external
    citation: "MDN — Number.prototype.toLocaleString(): without an explicit locales argument, the implementation-defined default locale is used, which is exactly the non-determinism this rule forbids relying on."
    url: "https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Number/toLocaleString"
    quote: "If locales and options are not provided, the returned string uses the default locale and options settings of the runtime."
    quoted_at: "2026-07-19"
decided_at: "2026-07-19"
---

## Frontend number/currency/date display MUST use Intl.NumberFormat / Intl.DateTimeFormat

**Impact: MEDIUM — locale-blind formatting produces silently wrong or inconsistent output across
environments (server vs. client default locale, replica-to-replica), and manual date concatenation
hard-codes a field order that breaks the moment a second locale is supported.**

### The violation — bare `toLocaleString()` and manual date concatenation

```tsx
// ❌ WRONG — toLocaleString() has no locale/options argument: it falls back
// to the runtime's implementation-defined default, which can differ between
// the server render and the client hydration, or between replicas.
const totalDisplay = order.total.toLocaleString()

// ❌ WRONG — manual date-part concatenation hard-codes US month/day/year
// order; ko-KR (yyyy.MM.dd) and other locales can never be supported without
// rewriting this line.
const dateDisplay =
  (paidAt.getMonth() + 1) + '/' + paidAt.getDate() + '/' + paidAt.getFullYear()
```

### Correct — explicit locale via Intl.NumberFormat / Intl.DateTimeFormat

```tsx
// ✅ CORRECT — locale is explicit (from the caller's session/Accept-Language,
// not implied), and the format follows CLDR rules for that locale.
const totalDisplay = new Intl.NumberFormat(locale, {
  style: 'currency',
  currency: order.currency,
}).format(order.total)

const dateDisplay = new Intl.DateTimeFormat(locale, {
  year: 'numeric',
  month: '2-digit',
  day: '2-digit',
}).format(paidAt)
```

### Enforcement

`practices/evals/locale_aware_format_guard.sh` (wired into `run-all-guards.sh` as guard `[91]`) scans
`*.ts`/`*.tsx` for four locale-blind shapes — bare `.toLocaleString()`, manual `getMonth()/getDate()/
getFullYear()` `+`-concatenation (single-line or multiline), `.toFixed()` on a money-named value, and
string-concatenated currency symbols. `practices/evals/fixtures/locale-aware-format/pass_intl_format/`
is the corrected Intl-based rewrite (exit 0); four per-detector fail fixtures each isolate one detector
(`fail_manual_format` — multiline date concat; `fail_bare_tolocale`; `fail_money_tofixed` — a `.ts`
formatter util; `fail_currency_concat` — a `.ts` formatter util) so deleting any single detector greens
exactly that fixture (proper per-detector falsification).

### Gap this rule closes

Confirmed absent prior to this rule via `practices/consumer-proof/engine/canary-gaps.yaml`
`CANARY-001` and independently via the S3.e-commerce consumer-proof scenario
(`practices/consumer-proof/scenarios/S3.e-commerce/scenario-guards/locale_format_guard.sh`, a
scenario-local hand-rolled stand-in that predates this catalog rule). This rule + guard + fixture
pair is the catalog-level closure; the scenario-local guard remains as the scenario's own proof
artifact and is unchanged.
