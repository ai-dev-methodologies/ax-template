# S2.AUDIT-PII.XB — consumer-proof scenario

DOGFOOD cell: **cross-boundary AUDIT-PII** — a BE audit/error record with a
PII field crossing to the FE. The cell was flagged as a GAP: no test proved
PII redaction survives BE->FE. Composed from the catalog's audit-log L4 PII
posture (`templates/backend/audit-log/AuditLogPiiRedactor.java`,
`common/AuditPiiHelper#piiHash`) and the `webhook-signing-l0` BE spec's
four-outcome verification contract
(`backend/src/main/java/.../webhooksigning/WebhookSigningException.java`),
assembled into a thin `AuditEventController/Service/Response` slice under
`java/{violating-root,clean-root}` plus a matching FE component under
`react/{violating,clean}`.

## What this proves

Two realistic AI-generated defects, each with a BLOCKED violating fixture
and a scanned+PASS clean fixture:

| # | Defect | Guard | Asset |
|---|--------|-------|-------|
| 1 | The GAP: `AuditEventResponse.from()` maps `entry.getActorEmail()` straight into the FE-facing DTO with no redaction | `scenario-guards/audit_pii_cross_boundary_guard.sh` | **hand-rolled** |
| 2 | The ADDITIONAL REQUIREMENT: a FE component consumes inbound webhook `signatureStatus` but always renders the same "Delivered" success pill — never surfaces a failed-verification state | `scenario-guards/webhook_signature_status_ux_guard.sh` | **hand-rolled** |

### Why #1 is a real gap, not a strawman

The catalog is **not** empty-handed here — it has real PII-handling assets:

- `templates/backend/audit-log/AuditLogPiiRedactor.java` masks `actorIp` at
  **write time** (called from `AuditLogService.record()` / the real
  `AuditLoggingAspect`), and
- `common/AuditPiiHelper#piiHash` exists precisely to hash a PII identifier
  before it reaches an audit line or a stored column (R61
  `audit-log-pii-hash-required`).

But nothing enforces that **every** PII field on an audit entity is actually
routed through one of them at the **exact seam** where a row becomes a
FE-facing DTO. Confirmed by reading the real domain: the live
`AuditLogDto.Detail.from()` maps `log.getActorIp()` and `log.getMetadataJson()`
straight across — `metadataJson` is a free-form `Map<String,Object>`-derived
column with **no field-level redaction at all**, so any PII an AI agent
stuffs into it (a very natural "log the whole request body for audit"
instinct) survives BE persistence and reaches the browser unredacted. This
scenario reproduces that exact shape with a first-class `actorEmail` field
(rather than reaching into `metadataJson`) so the guard's signal is precise
and falsifiable, but the underlying seam — DTO factory method, no enforced
redaction — is the same one the real `auditlog` domain has today.

## Capability-gap signals (assets_handrolled)

Both guards had to be hand-rolled — confirmed absent from the catalog, not
merely "not found by a quick grep":

1. **`audit_pii_cross_boundary_guard.sh`** — `grep -ril "Response\|Dto"
   practices/evals/*.sh` returns 0 hits that scan a DTO factory method body
   for an unredacted PII getter. `phi_in_logs_guard.sh` is the nearest
   catalog neighbor but checks SLF4J **log** statements, a different surface
   — a PII getter reaching a DTO record untouched is invisible to it. No
   ArchUnit rule in the real backend targets DTO-mapping call sites either.
2. **`webhook_signature_status_ux_guard.sh`** — `grep -ril "signatureStatus\|
   could not be verified" templates/L2/blocks practices-react/rules` returns
   0 hits. `templates/L2/blocks/status-badge.tsx` is the nearest neighbor (a
   generic status pill) but has no invariant that a verification-failure
   outcome must be surfaced — a consumer can wire any `StatusKind` to any UI
   state, including silently mapping every signature outcome to "success".
   `templates/L4/webhook/app/(admin)/webhooks/deliveries/page.tsx` lists
   deliveries but has no signature-verification-failed state either.

Both are modeled on the catalog's own text-scanning shell-guard style
(`controller_problemdetail_guard.sh`'s annotation-then-declaration parsing;
the sibling `S3.b2b-admin` scenario's `fe_pii_telemetry_denylist_guard.sh`
grep-window approach) and isolated to this scenario dir.

## Isolation

Everything lives under this scenario dir (`java/`, `react/`,
`scenario-guards/`). Nothing here edits `backend/src` or `frontend/src`, and
this scenario is NOT wired into `run-all-guards.sh` or R25 — it is a
standalone probe, run manually.

## Run it

```bash
bash practices/consumer-proof/scenarios/S2.AUDIT-PII.XB/run-scenario-proof.sh
```

Exit 0 = proof holds (every violating fixture BLOCKED by its intended
signature, every clean fixture scanned + PASS, cardinality gate satisfied).
Exit 1 = proof falsified or a case could not run.
