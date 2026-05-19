# Implementation Status — 12 L4 Domains (R15+)

> **Fork-receiver expectation alignment.** This doc closes the gap between catalog promises and runnable code. Persona simulation (R15 옵션A) revealed that fork-receivers consistently confuse `templates/L4/<domain>/` (catalog reference template + Next.js stub) with `backend/src/main/java/com/ax/template/authblueprint/<domain>/` (actual Java reference workload). The two layers are different by design — this table makes the boundary explicit.

## Status taxonomy

- **impl** — Backend Java reference workload exists at `backend/src/main/java/com/ax/template/authblueprint/<domain>/`. Fork-receiver can run `./gradlew test{Domain}` on day 1. Spec Trio + Next.js stub also present.
- **spec-only** — Spec Trio (`specs/`, `contracts/`, `blueprints/`) + Next.js stub (`templates/L4/<domain>/app/` or `components/`) exist. **Backend Java code does NOT exist.** Fork-receiver implements the backend themselves following METHODOLOGY.md 5-step (≈5-10 engineering days per domain).
- **skeleton** — Backend `.skeleton` file present (entity stub only). Fork-receiver fleshes out controller/service/repository. No Next.js stub.

## 12 L4 status (disk-verified 2026-05-20)

| L4 domain | Backend Java | Frontend Next.js stub | Spec Trio | Status |
|---|---|---|---|---|
| auth | ✅ reference workload | ✅ stub | 2/3 | **impl** |
| crud | ✅ reference workload | ✅ stub | 2/3 | **impl** |
| payment | ✅ reference workload | ✅ stub | 3/3 | **impl** |
| practices | ✅ reference workload | ✅ stub | 0/3 | **impl** (rules-as-code) |
| audit-log | ❌ none | ✅ stub | 3/3 | **spec-only** |
| billing | ❌ none | ✅ stub | 3/3 | **spec-only** |
| feature-flags | ❌ none | ✅ stub | 3/3 | **spec-only** |
| file-storage | ❌ none | ✅ stub | 3/3 | **spec-only** |
| notification | ❌ none | ✅ stub | 3/3 | **spec-only** |
| search | ❌ none | ✅ stub | 3/3 | **spec-only** |
| scheduled-task | ⚠️ skeleton only (`.skeleton` file) | ❌ none | 3/3 | **skeleton** |
| webhook | ⚠️ skeleton only (`.skeleton` file) | ❌ none | 3/3 | **skeleton** |

**Totals:** 4 impl · 6 spec-only · 2 skeleton.

## What this means for fork-receivers

1. **Recipe activates a composition of L4 domains.** A recipe like `b2b-admin` enables 5 L4 (`audit-log, auth, crud, feature-flags, search`). Of those, **only `auth` + `crud` ship with backend Java**; `audit-log` + `feature-flags` + `search` are spec-only — you implement them following the Spec Trio.

2. **Sealed verdict PASS is NOT a backend-code working guarantee.** Sealed verdicts validate that the catalog is **self-discoverable by a context-0 AI agent**. They do not assert that the recipe's backend code runs in production. Refer to this status table for the actual code completeness.

3. **METHODOLOGY.md 5-step is the implementation playbook.** For every spec-only L4 in your chosen recipe, follow the Spec Trio + TDD + `./gradlew test{Domain}` cycle to flesh out the backend.

4. **Estimated effort per spec-only L4** (per Appendix C):
   - Backend reference workload: ~5-10 engineering days (controller + service + repository + JPA entities + RestAssured tests + observability)
   - Frontend implementation beyond stub: ~3-5 engineering days per domain
   - **Bias toward composing existing impl L4 (auth/crud/payment) first**, then adding spec-only as recipe needs.

## Recipe-by-recipe completeness

See each `recipes/<pattern>/RECIPE.md` for an inline "Backend Implementation Status" table showing exactly which L4 in that recipe are `impl` vs `spec-only` vs `skeleton`.

## Roadmap

R16+ candidate: convert top spec-only L4 (audit-log, notification) to reference workload as they appear in 5+ recipes. Bias toward generating from spec rather than hand-implementing.
