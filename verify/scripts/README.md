# verify/scripts skeleton

Purpose: hold manifest-driven verify entrypoints.

Planned wiring (not implemented in this task):
1. Load `blueprints/auth-manifest.yaml`.
2. Validate required manifest shape against `verify/manifest.schema.json`.
3. Route checks to security/contract/RBAC verify modules.
4. Exit non-zero on any violation or unresolved check path (fail-closed, no fail-open path).

## Local runnable baseline (T4-3)

Run the placeholder triplet harness locally:

```bash
python3 verify/scripts/run_verify_triplet.py
```

This runner is intentionally minimal and manifest-driven in spirit:
- reads required keys from `verify/manifest.schema.json`
- validates top-level key presence in `blueprints/auth-manifest.yaml`
- executes fixture categories from `verify/fixtures/{golden,violation,false-positive}`
- enforces expected placeholder outcomes (`PASS`, `REJECT`, `GUARD PASS`)
