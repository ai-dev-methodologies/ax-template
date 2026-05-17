---
name: ax-guard-trio-integrity
description: >
  Tier-3 trio_integrity guard wrapper. Thin skill wrapper around practices/evals/trio_integrity_guard.sh.
  Verifies Spec Trio completeness per domain mode (full_trio / backend_only / frontend_only).
  New guard implemented in SP3. Invoked by Tier-2 only — NOT pathPattern-triggered.
metadata:
  priority: 4
  tier: 3
  axis: concern
  docs:
    - "https://spec.openapis.org/oas/v3.1.0"
  pathPatterns: []
  bashPatterns:
    - 'bash practices/evals/trio_integrity_guard.sh'
  importPatterns: []
retrieval:
  aliases:
    - ax-guard-trio-integrity
    - trio integrity guard
    - spec trio completeness
    - check trio integrity
    - frontend spec completeness
  intents:
    - verify Spec Trio completeness per domain
    - check frontend spec coverage against backend spec
    - validate trio_integrity_allowlist
    - guard against missing frontend specs
  entities:
    - trio_integrity_guard.sh
    - trio_integrity_allowlist.yaml
    - full_trio
    - backend_only
    - frontend_only
    - MISSING_FRONTEND_SPEC
    - COVERAGE_SHORTFALL
    - ZERO_SCAN
---

# ax-guard-trio-integrity

Tier-3 trio_integrity guard wrapper. Wraps `practices/evals/trio_integrity_guard.sh`.
New guard implemented in SP3 (not a placeholder). Verifies that each domain's
Spec Trio is complete per its declared mode in `practices/evals/trio_integrity_allowlist.yaml`.

Three domain modes:
- `full_trio`: backend Spec Trio + frontend Spec Trio both required; every UI item
  has a non-null `backend_operation_id` matching an OpenAPI operation.
- `backend_only`: no frontend Spec Trio required; `frontend_required: false` marker
  in backend spec.
- `frontend_only`: frontend Spec Trio present; every UI item has `backend_operation_id: null`
  AND a non-empty `static_source_ref` pointing to existing files.

NOT pathPattern-triggered. Invoked exclusively by Tier-2 skills.

## Workflow checklist (copyable per Anthropic best-practices)

- [ ] Step 1: Confirm `practices/evals/trio_integrity_allowlist.yaml` contains all domains
- [ ] Step 2: Run `bash practices/evals/trio_integrity_guard.sh [--domain <domain>]`
- [ ] Step 3: Read error codes — see table below for fix actions
- [ ] Step 4: Fix the artifact named in the error message
- [ ] Step 5: Re-run guard for that domain — confirm exit 0

## Steps detail

### Step 2: trio_integrity_guard.sh
Script: `practices/evals/trio_integrity_guard.sh`.
Optional `--domain <domain>` to scope to one domain.
Zero-scan guard: if allowlist points to a domain directory that doesn't exist,
fails with `ZERO_SCAN`.

### Step 3: Error code fix table

| Error code | Root cause | Fix |
|---|---|---|
| `ZERO_SCAN` | Allowlist has a domain not in specs/ | Add the missing spec file or fix the allowlist entry |
| `MISSING_FRONTEND_SPEC` | `full_trio` domain missing `specs/<domain>-frontend-l0.yaml` | Create the frontend spec YAML |
| `COVERAGE_SHORTFALL: N/M` | Backend spec has M items requiring frontend; frontend spec has only N | Add the missing items to the frontend spec |
| `MISSING_OPERATION_ID` | UI route's `backend_operation_id` references an OpenAPI op not in `contracts/<domain>-openapi.yaml` | Add the operation to the OpenAPI contract or fix the ID |
| `frontend_only route missing static_source_ref` | `frontend_only` item has null operation ID but empty/missing `static_source_ref` | Add non-empty `static_source_ref` pointing to existing files |
| `static_source_ref resolves to zero files` | Glob in `static_source_ref` matches no existing files | Fix the glob or create the referenced files |
| `frontend_only item has non-null backend_operation_id` | `frontend_only` item sets `backend_operation_id` to a non-null value | Set `backend_operation_id: null` and add `static_source_ref` |

## Bundled scripts
- `skills/ax-guard-trio-integrity/scripts/run.sh` — thin wrapper; passes args to `trio_integrity_guard.sh`; exits with guard's exit code

## Feedback loop
Each error code names a specific file and field. Fix that artifact directly.
Do NOT add fake operations to OpenAPI to satisfy the guard — operations must
represent real backend endpoints.
Halt threshold: 3 consecutive `COVERAGE_SHORTFALL` failures on the same domain
→ the backend spec grew without a corresponding frontend spec update; treat as
an architecture gap and file an ADR.

## Invocation graph
- Calls: `practices/evals/trio_integrity_guard.sh` (implemented in SP3)
- Called by (Tier-2): `ax-verify-shared`, `ax-verify-L4`, `ax-verify-domain`

## Acceptance (binary)
```bash
bash practices/evals/trio_integrity_guard.sh
# Expected: exit 0 on green repo (with pass/ fixtures passing)
```
