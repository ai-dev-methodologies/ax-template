# L4 / ratelimit — Fork & Copy Guide

**Tenant model**: `single` — per [`specs/multi-tenant-l0.yaml#MULTI-TENANT-ISOLATION-DEFAULT-001`](../../../specs/multi-tenant-l0.yaml). Rate-limit counters are keyed per caller within a tenant; a multi-tenant fork scopes the counter key by tenant id via the tenancy runtime.

**Status**: backend-only, **backend already realized** (promoted future_add → selectable, recipe_orphan). Unlike a pure namespace reservation, the ratelimit reference workload ships in `backend/` today — this README promotes the existing domain to the discoverable L4 surface. Registered `backend_only` in `practices/evals/trio_integrity_allowlist.yaml` (protective cross-cutting concern, no first-class UI).

## Domain summary

Protective cross-cutting concern: per-caller request rate limiting with RFC 6585 §4 `429 Too Many Requests` + `Retry-After`. The Spec Trio defines 4 items — rejection (429 on over-limit), headers (`Retry-After` on the 429), isolation (one caller's burst does not starve another), and window reset (the counter clears after the window). A Caffeine-backed filter enforces the policy values (window / max-per-window / key strategy) declared in the manifest.

## Backend reference (realized — not a stub)

- Java package: `backend/src/main/java/com/ax/template/authblueprint/ratelimit/` — `RateLimitConfig`, `RateLimitFilter`, `RateLimitPingController`, `RateLimitProperties`
- Spec: [`specs/ratelimit-l0.yaml`](../../../specs/ratelimit-l0.yaml) — RFC 6585 §4 + IETF RateLimit-header draft → 4 items
- Contract: [`contracts/ratelimit-openapi.yaml`](../../../contracts/ratelimit-openapi.yaml) — `/api/ratelimit/ping` + 429 with `Retry-After`
- Blueprint: [`blueprints/ratelimit-manifest.yaml`](../../../blueprints/ratelimit-manifest.yaml) — `window_millis`, `max_per_window`, `key_strategy`, `on_missing_key`
- Tests: `./gradlew testRateLimit` — GREEN (4/4 RATELIMIT items; VIOLATION proof: tampering `max_per_window: 5 → 9999` turns the suite RED)
- Methodology worked example: [`METHODOLOGY.md`](../../../METHODOLOGY.md) Appendix A.2 (rate-limit as the cross-domain generalization case)

## Frontend

Rate-limiting has **no first-class UI**. The client-facing surface is the `429` + `Retry-After` response; the catalog ships the presentational half as an L2 block — [`templates/L2/blocks/rate-limit-banner.tsx`](../../L2/blocks/rate-limit-banner.tsx) (R56 — RFC 6585 §4 / WCAG 2.2 SC 4.1.3 live-region countdown). Frontend trio deliberately skipped — registered `backend_only`.

## Composition contract

When a fork-receiver adopts rate limiting:

1. Wire `RateLimitFilter` ahead of the protected routes; set `window_millis` / `max_per_window` / `key_strategy` from the manifest (do NOT hardcode).
2. Surface the `429` to the client through `templates/L2/blocks/rate-limit-banner.tsx` (reads `Retry-After`, announces a polite countdown).
3. For a multi-tenant fork, scope `key_strategy` by tenant id via the `multi-tenant` runtime so one tenant cannot exhaust another's budget.
4. Re-run `bash practices/scripts/verify-completion.sh` — `testRateLimit` plus the per-domain suites must stay GREEN.

## Next steps

- A distributed counter store (Redis token-bucket) for multi-node deployments is a fork-receiver concern; the reference ships the single-node Caffeine filter. Promote `recipe_orphan: true` → an active recipe when a recipe adopts rate limiting as a first-class feature (e.g. `api-gateway-relay`).
