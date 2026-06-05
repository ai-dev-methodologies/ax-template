---
title: A multilingual recipe MUST declare its default and supported locales as BCP 47 tags, with every message bundle covering the supported set
impact: MEDIUM
impactDescription: "A multilingual app with no declared default locale falls back unpredictably (the JVM default of whatever server it runs on) when negotiation finds no match; one with no declared supported set ships message bundles that silently miss a locale, so a user in that locale sees raw message keys or English fallbacks. Declaring default_locale + supported_locales as BCP 47 tags makes the fallback deterministic and the bundle-coverage checkable."
tags:
  - i18n
  - localization
  - bcp-47
  - locale
  - configuration
spec_ref: "specs/i18n-policy-l0.yaml#I18N-DEFAULT-DECL-001"
verification:
  type: review
  source: "specs/i18n-policy-l0.yaml#I18N-DEFAULT-DECL-001"
  pattern: "Every recipe whose target audience is multilingual MUST declare two RECIPE.md frontmatter fields: `default_locale: <BCP-47-tag>` (the deterministic fallback Locale when content negotiation finds no acceptable match) and `supported_locales: [<tag>, ...]` (the exhaustive set). Every `messages_*.properties` bundle MUST cover the full supported_locales set — a supported locale with a missing bundle is a gap. Locale tags MUST be valid BCP 47 (RFC 5646) language tags. A single-audience recipe MAY omit both (absence ⇒ single default). Reject a multilingual recipe with no declared default_locale (nondeterministic JVM-default fallback), a supported locale with no message bundle, and a non-BCP-47 locale tag."
upstream:
  - "https://www.rfc-editor.org/rfc/rfc5646"
evidence:
  - source_type: external
    citation: "RFC 5646 — Tags for Identifying Languages (BCP 47) (Abstract)"
    url: "https://www.rfc-editor.org/rfc/rfc5646"
    quote: "This document describes the structure, content, construction, and semantics of language tags for use in cases where it is desirable to indicate the language used in an information object."
    quoted_at: "2026-06-06"
decided_at: "2026-06-06"
---

## A multilingual recipe MUST declare its default and supported locales as BCP 47 tags

**Impact: MEDIUM — Internationalization fails quietly at the edges. When content negotiation cannot match the client's `Accept-Language` to anything the app offers, it falls back — and if no default is declared, it falls back to whatever the JVM default Locale happens to be on that server, which is nondeterministic across environments. And if the supported set is not declared, a `messages_ko.properties` can be forgotten and a Korean user silently sees raw keys or English. BCP 47 (RFC 5646) is the standard for the tags themselves — it *describes the structure, content, construction, and semantics of language tags for use in cases where it is desirable to indicate the language used in an information object*. Declaring `default_locale` and `supported_locales` makes the fallback deterministic and bundle coverage mechanically checkable.**

There is one load-bearing requirement for `I18N-DEFAULT-DECL-001`.

**1. Declared default + supported locales (BCP 47).** A multilingual recipe declares in RECIPE.md frontmatter:
- `default_locale: <BCP-47-tag>` — the single deterministic fallback when negotiation finds no acceptable match (composes the content-negotiation `Accept-Language` selection).
- `supported_locales: [<tag>, ...]` — the exhaustive set the app serves.

Both values are valid BCP 47 language tags (`ko`, `en-US`, `zh-Hant`). Every `messages_*.properties` bundle covers the full `supported_locales` set — a declared supported locale with no bundle is a coverage gap. A single-audience recipe may omit both; absence is treated as a single default locale.

**Incorrect — no declared default; a supported locale with no bundle:**

```yaml
# RECIPE.md frontmatter
recipe: orders
# VIOLATION: multilingual app, but no default_locale → falls back to the JVM default (nondeterministic) (I18N-DEFAULT-DECL-001)
# VIOLATION: no supported_locales declared → messages_ko.properties silently missing, Korean users see raw keys
```

**Correct — explicit default + supported set as BCP 47 tags, bundles cover the set:**

```yaml
# RECIPE.md frontmatter
recipe: orders
default_locale: en-US                    # deterministic fallback (BCP 47)  (I18N-DEFAULT-DECL-001)
supported_locales: [en-US, ko, ja]       # exhaustive; every tag is valid BCP 47
# messages_en_US.properties, messages_ko.properties, messages_ja.properties all present (full coverage)
```

Verification: review-tier. Locale declaration is a configuration-completeness property — a missing default or bundle compiles and runs, surfacing only as a wrong-language fallback for some users in some environments. Verify by review against `specs/i18n-policy-l0.yaml#I18N-DEFAULT-DECL-001`: a multilingual recipe declares `default_locale` and `supported_locales` as valid BCP 47 tags, and every supported locale has a message bundle. When a fork-receiver wires a guard that parses RECIPE.md frontmatter and cross-checks the `messages_*` bundles against `supported_locales`, this rule's verification may be upgraded from review to gradle_task+tag.

Reference: [RFC 5646 — Tags for Identifying Languages (BCP 47)](https://www.rfc-editor.org/rfc/rfc5646)
