# Codex PR #3 Review (P1 Absorption Round 4)

## Verdict: BLOCK

Do not merge as-is. I found multiple implementation blockers that are independent of the sandbox's inability to bind Gradle/Playwright sockets.

## Verification gate

- `bash skills/ax-verify/scripts/run-all.sh`: **FAIL** at backend step. Guards pass first, then Gradle fails before tests run with `java.net.SocketException: Operation not permitted` from `TcpIncomingConnector.accept`. Retried with writable `GRADLE_USER_HOME=/private/tmp/ax-template-gradle-home` and copied Gradle cache; same daemon socket failure.
- `bash skills/ax-fork-receiver/scripts/run.sh --bundle-only`: **PASS**. Tarball `dist/ax-template-catalog-b6112d9.tar.gz`, size 2MB, sha256 `83114f24c90d2084b20dde0f6a9787d9a751c31faecf3a216bd817dc3b983fe0`.
- `bash practices/evals/run-all-guards.sh --include-fixtures`: **PASS**, 19 passed / 0 failed.
- `/ax-verify-domain` matrix: **FAIL**.
  - `auth`, `crud`, `payment`, `notification`, `audit-log`, `file-storage`, `search`, `feature-flags`: structural guards pass, then Gradle socket EPERM blocks the backend step.
  - `practices`: Gradle skipped as frontend-only, then Playwright fails because sandbox blocks `listen 0.0.0.0:3000`.
  - `billing`: **FAIL before Gradle**. `trio_integrity_guard`: `NULL_OPERATION_ID: route /billing/pricing has null backend_operation_id in full_trio mode`.
  - `identity-verification`: **FAIL before Gradle**. `DOMAIN_NOT_IN_ALLOWLIST: identity-verification`.

## PRD traceability (5 SPs)

- SP30 billing: backend exists at `templates/backend/billing/`; allowlist has `billing: full_trio` at `practices/evals/trio_integrity_allowlist.yaml:27`. **BLOCKER:** `templates/L4/billing/app/page.tsx:7` has `backend_operation_id: null` while full_trio requires non-null operation ids.
- SP31 identity-verification: backend exists at `templates/backend/identity-verification/`, but allowlist is missing `identity-verification: backend_only`. This directly violates the PRD and breaks `/ax-verify-domain identity-verification`.
- SP32 TipTap: `templates/L1/components/rich-text-editor.tsx` has `'use client'`; `frontend/src/components/rich-text-editor-client.tsx` wraps dynamic import with `ssr: false`. Looks RSC-compatible.
- SP33 tables advanced: artifacts exist, but I found no actual bundle delta measurement proving `<30 kB`; only rationale comments in `bulk-export.tsx`. Unconfirmed.
- SP34 impersonation banner: canonical `session.actingAs` matching is implemented in `impersonation-banner-required-when-acting-as-other-user`; fixture runner passed, including renamed helper `runAsUser()`.

## Critical contracts

- `no-billing-cross-import-from-payment`: Java and React rules exist with failing fixtures.
- Business-registration fixture: uses public DART/data.go.kr-style samples, not mocks.
- `no-rrn-collection-without-legal-basis`: Java and React rules exist with failing fixtures and PIPA Article 24 references.
- `no-impersonation-bypass-via-helper-rename`: **name not present**. Equivalent behavior exists under `impersonation-banner-required-when-acting-as-other-user`, and the renamed-helper fixture passes, but the exact requested rule id is absent.
- Tier-1 cap: no `skills/` changes in `main...HEAD`; no new top-level skills.
- Per-subclass `@SQLDelete`: present on `Subscription`, `Plan`, `Invoice`, `BillingEvent`, and `VerifiedIdentity`.
- Flyway migrations: **BLOCKER.** No billing or identity-verification table migrations exist under `templates/backend/data/migrations` or `backend/src/main/resources/db/migration`, despite new JPA entities/tables.

## Anti-pattern check

- New billing and identity-verification ITs use RestAssured with `@LocalServerPort`. Existing MockMvc tests remain from `main`; no new MockMvc-only test was added in this PR.
- No `--no-verify` / skip-hook diff found.
- No `.github` / CI workflow changes found. There is release/tag wording in `templates/DECISIONS.md`, but no deployment artifact.
- Composition-kit framing remains present in `README.md`, `CLAUDE.md`, and `templates/AGENTS.md`.

## Branch hygiene

- Current branch: `feat/p1-absorption-sp30-sp34`; `main` remains at `1ab8f54`; `HEAD` is `b6112d9` tagged `v1.2.0-p1-absorbed`.
- No secret-like file additions (`.env`, credentials, private keys) found in the PR diff.
- **BLOCKER:** middleware was copied, not moved. Both `frontend/middleware.ts` and `frontend/src/middleware.ts` exist, and `frontend/tsconfig.json:37` still includes root `middleware.ts`. This fails the Next.js 15 src-dir fix requirement.

## My independent attack

New domain tests are not wired into Gradle. `backend/src/test/java/.../BillingFlowIT.java` is tagged `BILLING` and documents `./gradlew testBilling`; `IdentityVerificationFlowIT.java` is tagged `IDENTITY_VERIFICATION`. But `backend/build.gradle.kts` registers tasks only through `testFeatureFlags` and has no `testBilling` or `testIdentityVerification`. Even after the allowlist/trio blockers are fixed, `/ax-verify-domain billing` and `/ax-verify-domain identity-verification` will map to missing Gradle tasks.

Additional smoke: running local template Vitest anchors through `./frontend/node_modules/.bin/vitest` fails because root template imports cannot resolve `react` from repo root (`currency-input.tsx`, `business-registration-input.tsx`, `tree-table.tsx`).

## Merge recommendation

**Block merge.** Minimum fixes before re-review:

1. Add `identity-verification: backend_only` to `practices/evals/trio_integrity_allowlist.yaml`.
2. Fix billing full_trio metadata so `/billing/pricing` resolves to a non-null backend operation.
3. Add Flyway migrations for billing and identity-verification tables.
4. Remove root `frontend/middleware.ts` and root tsconfig inclusion; keep `frontend/src/middleware.ts` only.
5. Register `testBilling` and `testIdentityVerification` Gradle tasks.
6. Either add the exact `no-impersonation-bypass-via-helper-rename` rule id or update the canonical contract to the implemented rule name.
7. Provide real SP33 bundle delta evidence or a reproducible local check.
