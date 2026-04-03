# Verify Skeleton (Task 6 - Minimal)

This directory is the **manifest-driven** verification boundary for auth blueprint scaffolding.

## Intent
- Verify must consume blueprint rules from `blueprints/auth-manifest.yaml`.
- Verify must not hardcode policy logic that duplicates manifest policy.
- Verify is fail-closed in intent: if manifest load/validation/check routing is missing or ambiguous, verification must fail.

## Required verify triplet (future implementation)
- security verify
- contract verify
- RBAC verify

## Fixture strategy (placeholder only)
- `fixtures/golden/`: compliant examples expected to pass
- `fixtures/violation/`: explicit policy violations expected to fail
- `fixtures/false-positive/`: compliant-but-risky examples used to prevent accidental over-rejection

## Structure
- `manifest.schema.json`: placeholder schema for manifest shape checks (preserved)
- `scripts/`: entry points for manifest load + triplet routing (skeleton only)
- `fixtures/`: golden/violation/false-positive placeholder cases

## Local triplet command (T4-3 minimal harness)
```bash
python3 verify/scripts/run_verify_triplet.py
```

Expected behavior with current placeholders:
- `golden` -> `PASS`
- `violation` -> `REJECT`
- `false-positive` -> `GUARD PASS`

## Scope note
This task creates structure and execution intent only. It does not implement full verify runtime logic yet.
