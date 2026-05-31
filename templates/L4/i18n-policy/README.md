# L4 / i18n-policy — Fork & Copy Guide

**Tenant model**: `single` — per [`specs/multi-tenant-l0.yaml#MULTI-TENANT-ISOLATION-DEFAULT-001`](../../../specs/multi-tenant-l0.yaml). i18n-policy is tenancy-agnostic: locale negotiation and message resolution happen per request regardless of tenant. A multi-tenant fork layers tenant context on top of this runtime without changing it.

**Status**: backend-only (promoted future_add → selectable, recipe_orphan). The Spring i18n CONFIG + policy runtime ships in this commit (`backend/src/main/java/com/ax/template/authblueprint/i18n/`); verified by `./gradlew testI18n` (5 testable items GREEN). This is a cross-cutting policy domain — no entity, no state machine, no user-facing UI — so it follows the `multi-tenant` backend-only L4 convention, not the entity-domain (CRUD) convention.

## Domain summary

Locale + message-source + time/number/currency formatting policy for fork-receivers that ship in more than one language. The spec ([`specs/i18n-policy-l0.yaml`](../../../specs/i18n-policy-l0.yaml)) defines the contract; the runtime here defines the canonical Spring wiring (`LocaleResolver` + `MessageSource` + a UTC-`Instant` time policy + locale-aware formatting).

Six items / five families:

- `I18N-LOCALE-NEG-001` — `Accept-Language` negotiation (RFC 7231 §5.3.5 q-values) with an explicit non-null fallback Locale (never `System.getDefault`).
- `I18N-MESSAGE-SOURCE-001` — user-facing strings come from a `MessageSource` bundle, not inline literals; bundles keep key-set parity across every `supported_locales`.
- `I18N-MESSAGE-SOURCE-002` — plural/gender via ICU MessageFormat (Korean 1 form / English 2 forms), never `String.format`.
- `I18N-TIMEZONE-001` — store UTC `java.time.Instant`, display at the API boundary in the caller's `ZoneId`; reject naive (offset-less) inbound date-times 400 `INVALID_DATETIME` (RFC 3339 §5.6); serialize the ISO 8601 Z-form.
- `I18N-FORMATTING-001` — `NumberFormat.getCurrencyInstance(locale)` / locale-aware date formatting, never hard-coded format strings.
- `I18N-DEFAULT-DECL-001` (verification_type: review) — declare the RECIPE.md `default_locale` / `supported_locales` convention (below). A mechanical guard is explicitly deferred by the spec (a future `recipe_locale_policy_guard.sh`).

## Backend reference

- Java package: `backend/src/main/java/com/ax/template/authblueprint/i18n/` — cross-cutting, NOT per-domain
  - `I18nConfig` — additive `LocaleResolver` (`AcceptHeaderLocaleResolver`, default `Locale.ENGLISH`, supported `ko-KR`/`en-US`) + `MessageSource` (`useCodeAsDefaultMessage=false`)
  - `I18nTimePolicy` — UTC `Instant` round-trip + strict-offset inbound parse
  - `I18nProblemAdvice` — package-scoped 400 `INVALID_DATETIME` mapping (additive; never claims another domain's exceptions)
  - `I18nProbeController` — black-box probe surface (`/api/i18n/greeting|plural|format|time`)
  - `backend/src/main/resources/i18n/messages{,_ko,_en}.properties` — parallel bundles (greeting + ICU plural key) with parity across locales
- Spec: [`specs/i18n-policy-l0.yaml`](../../../specs/i18n-policy-l0.yaml) — `domain_mode: backend_only`
- Blueprint: [`blueprints/i18n-policy-manifest.yaml`](../../../blueprints/i18n-policy-manifest.yaml) — locale/currency/middleware policy anchors
- Tests: `./gradlew testI18n` — 5 testable items (LOCALE-NEG / MESSAGE-SOURCE ×2 / TIMEZONE / FORMATTING); `I18N-DEFAULT-DECL-001` is review-only

## Frontend

i18n-policy has **no first-class UI**. It is request-scoped Spring CONFIG + policy consumed by every other domain's responses. The blueprint sketches optional React primitives (`locale-switcher`, `currency-formatter`, `relative-time`) a fork MAY add, but they are not part of this backend-only L4. Registered as `backend_only` in `practices/evals/trio_integrity_allowlist.yaml`.

## Composition contract

When a fork-receiver's recipe targets more than one language, adopt the spec items IN ORDER (each is foundational for the next):

1. `I18N-LOCALE-NEG-001` — fix non-deterministic locale resolution first.
2. `I18N-MESSAGE-SOURCE-001` — pull user-facing strings out of code into bundles.
3. `I18N-MESSAGE-SOURCE-002` — survive plural/gender forms via ICU MessageFormat.
4. `I18N-TIMEZONE-001` — fix silent offset loss before scale.
5. `I18N-FORMATTING-001` — locale-aware numbers / currency / dates.
6. `I18N-DEFAULT-DECL-001` — declare the contract in RECIPE.md (below).

### RECIPE.md `default_locale` / `supported_locales` convention (I18N-DEFAULT-DECL-001)

Every recipe whose audience is multilingual declares two BCP 47 fields in its RECIPE.md frontmatter:

```yaml
---
recipe: saas-subscription
tenant_model: multi
default_locale: ko-KR
supported_locales: [ko-KR, en-US]
---
```

- `default_locale` — the fallback Locale when negotiation finds no match.
- `supported_locales` — the exhaustive list; every `messages_*.properties` MUST cover this set.

Single-audience recipes MAY omit both; absence is treated as `supported_locales: [<default_locale or en>]`. Fork-receivers targeting the Korean enterprise default (KO+EN) may copy the block above verbatim and add `ja-JP` / `zh-CN` over time. A mechanical `recipe_locale_policy_guard.sh` is deferred by the spec until adoption pressure is real.

## Next steps

- Promote `recipe_orphan: true` → wired into a multilingual recipe when a fork-receiver flips `supported_locales` to more than one entry.
- Add a dogfood ledger entry (`docs/dogfood-ledger/i18n-policy-iter1.yaml`) once a recipe composes it — the 2-persona protocol applies to cross-cutting primitives the same as to domain verticals.
