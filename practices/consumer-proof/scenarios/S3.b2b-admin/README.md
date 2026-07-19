# S3.b2b-admin — consumer-proof scenario

DOGFOOD cell: **B2B ADMIN ledger + audit-export vertical slice** (admin lists
ledger entries, enqueues an audit export, polls status). Composed from the
catalog's CRUD/authz posture, audit-log L4's append-only ledger shape, and
report-export/api-key L4's export-job lifecycle shape, assembled into a thin
`LedgerAdminController` + `AuditExportService` slice under
`java/{violating-root,clean-root}` plus a matching FE page under
`react/{violating,clean}`.

## What this proves

Three realistic AI-generated rule violations, each with a BLOCKED violating
fixture and a scanned+PASS clean fixture:

| # | Violation | Guard | Asset |
|---|-----------|-------|-------|
| 1 | Admin `GET /api/admin/ledger` reachable with no `@PreAuthorize` (IDOR) | `scenario-guards/admin_preauthorize_guard.sh` | **hand-rolled** |
| 2 | `@ExceptionHandler` returns a bare `Map<String,String>` instead of `ProblemDetail` | `practices/evals/controller_problemdetail_guard.sh` | **catalog-reused** |
| 3 | A client-side analytics/telemetry call carries a raw PII field (admin email) | `scenario-guards/fe_pii_telemetry_denylist_guard.sh` | **hand-rolled** |

Violation #3 is the dogfood brief's ADDITIONAL REQUIREMENT: "an enforced FE
deny-list preventing PII fields from being placed into client-side
analytics/telemetry event payloads."

## Capability-gap signals (assets_handrolled)

Two of the three guards had to be hand-rolled — confirmed absent from the
catalog, not merely "not found by a quick grep":

1. **`admin_preauthorize_guard.sh`** — no standalone `--root`-parameterized
   shell guard enforces "every mapped method in a `*AdminController` is
   covered by `@PreAuthorize`/`@PostAuthorize` (class- or method-level)".
   `role_literal_guard.sh` validates that `@PreAuthorize` authority STRINGS
   are known-valid — a different invariant (it never checks whether the
   annotation exists at all). `controller_repository_shell_guard.sh` enforces
   a different invariant (controller→repository layering).
2. **`fe_pii_telemetry_denylist_guard.sh`** — no catalog asset (ESLint rule,
   L2 block, or shell guard) enforces a PII deny-list on outbound
   analytics/telemetry call payloads. `phi_in_logs_guard.sh` is the nearest
   catalog neighbor but checks SERVER-side log statements, a different
   surface. `templates/L2/blocks/event-stream.tsx` is an inbound SSE/polling
   feed component, not an outbound analytics wrapper.

Both are modeled on the catalog's own text-scanning shell-guard style
(`controller_problemdetail_guard.sh`'s annotation-then-declaration parsing;
`locale_format_guard.sh`'s deliberately-simple grep-based approach from the
sibling `S3.e-commerce` scenario) and isolated to this scenario dir.

## Isolation

Everything lives under this scenario dir (`java/`, `react/`,
`scenario-guards/`) plus read-only calls into the real
`practices/evals/*.sh` catalog guards. Nothing here edits `backend/src` or
`frontend/src`, and this scenario is NOT wired into `run-all-guards.sh` or
R25 — it is a standalone probe, run manually.

## Run it

```bash
bash practices/consumer-proof/scenarios/S3.b2b-admin/run-scenario-proof.sh
```

Exit 0 = proof holds (every violating fixture BLOCKED by its intended
signature, every clean fixture scanned + PASS, cardinality gate satisfied).
Exit 1 = proof falsified or a case could not run.
