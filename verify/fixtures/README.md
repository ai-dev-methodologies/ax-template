# verify/fixtures placeholder map

- `golden/`: should pass all manifest-driven checks
- `violation/`: should fail at least one manifest-driven gate
- `false-positive/`: should pass and guard against over-strict rejection

Per-bucket placeholders are split across the verify triplet baseline:
- security
- contract
- rbac

Real fixture payloads are intentionally deferred.
