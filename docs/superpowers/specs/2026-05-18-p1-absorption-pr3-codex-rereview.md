# Codex PR #3 RE-review

## Verdict: REQUEST CHANGES

The seven prior blockers are mechanically addressed, and the requested guard regressions pass. One new blocking defect remains in the fix-cycle: the new billing Flyway migration does not match the `BillingEvent` entity schema.

## Blocker closure (7)

1. **identity-verification allowlist: CLOSED.** `practices/evals/trio_integrity_allowlist.yaml` contains `identity-verification: backend_only`.
2. **billing /pricing backend operation id: CLOSED.** `contracts/billing-openapi.yaml` defines `GET /billing/plans` as `operationId: listAvailablePlans`; `contracts/billing-ui.yaml` maps `/billing/pricing` to `backend_operation_id: listAvailablePlans`; `bash practices/evals/trio_integrity_guard.sh --domain billing` exits 0.
3. **Flyway migrations: CLOSED WITH NEW DEFECT BELOW.** Both requested migration files exist. Billing defines the four primary billing tables plus an element-collection table; identity-verification defines `verified_identity` with `ci` and `di` columns and no RRN column.
4. **middleware copy: CLOSED.** `frontend/middleware.ts` is absent; `frontend/src/middleware.ts` exists; `frontend/tsconfig.json` no longer includes `middleware.ts`.
5. **Gradle tasks: CLOSED.** `backend/build.gradle.kts` registers `testBilling` with `includeTags("BILLING")` and `testIdentityVerification` with `includeTags("IDENTITY_VERIFICATION")`, following the `testFeatureFlags` pattern.
6. **rule id: CLOSED.** `practices-react/rules/no-impersonation-bypass-via-helper-rename.md` exists. The old impersonation rule also exists, but it is compatible: both state that detection is based on `session.actingAs` / `{ actingAs: ... }`, not helper names.
7. **bundle delta evidence: CLOSED ENOUGH.** `templates/L2/blocks/bulk-export-bundle-delta.md` exists with `npm run build` output and a static-delta argument. The evidence is partly inferential because the L2 block is not imported into the app shell, but it does not create a blocking regression.

## Regression check

- `bash practices/evals/run-all-guards.sh --include-fixtures`: PASS, 19 passed / 0 failed.
- `trio_integrity_guard.sh --domain` for `auth`, `crud`, `payment`, `notification`, `audit-log`, `file-storage`, `search`, `feature-flags`, `practices`, `billing`, `identity-verification`: PASS.
- `bash skills/ax-fork-receiver/scripts/run.sh --bundle-only`: PASS. Tarball `dist/ax-template-catalog-8ed28c9.tar.gz`, size 2MB, sha256 `333417877fa9e6595217b8a839ee984a0c2147facefdfcc49be1c403147d4dc0`.
- `bash skills/_tests/tier1-topology.test.sh`: PASS, `count=4`.

## New attack

**BLOCKING: `V202605181203__create_billing_tables.sql` omits the `billing_events.occurred_at` column required by `BillingEvent`.**

Evidence:
- `templates/backend/billing/BillingEvent.java:60` indexes `occurred_at`.
- `templates/backend/billing/BillingEvent.java:93` declares `@Column(name = "occurred_at", nullable = false)`.
- `templates/backend/data/migrations/V202605181203__create_billing_tables.sql:110-126` creates `billing_events` without `occurred_at`.
- `templates/backend/data/migrations/V202605181203__create_billing_tables.sql:135-136` names the index `idx_billing_events_occurred` but builds it on `created_at`, not `occurred_at`.

Impact: a fork receiver applying the migration will not get the schema promised by the entity. Hibernate validation or any insert of `BillingEvent.occurredAt` can fail, and the event-time index silently indexes the wrong timestamp. This is a migration/entity contract bug, not documentation polish.

Secondary informational note: `billing_plan_features` is created in the migration, while `Plan.features` has `@ElementCollection` but no explicit `@CollectionTable`. That should be checked in the same fix so the generated collection table name and join column are deterministic.

## Merge recommendation

Do not merge yet. Fix the billing migration/entity mismatch by adding `occurred_at TIMESTAMP NOT NULL` to `billing_events` and changing `idx_billing_events_occurred` to index `occurred_at`. Also either add `@CollectionTable(name = "billing_plan_features", joinColumns = @JoinColumn(name = "plan_id"))` to `Plan.features` or align the migration with Hibernate's intended collection-table naming.
