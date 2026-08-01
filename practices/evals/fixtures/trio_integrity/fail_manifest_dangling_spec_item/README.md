# trio_integrity/fail_manifest_dangling_spec_item — expected exit 1

Identical to pass_manifest_content except the surface backlinks `AUTH-FE-404`, which
is not an item id in specs/auth-frontend-l0.yaml. A policy block that backlinks a
deleted or renamed spec item governs nothing.

Expected: exit 1, `MANIFEST_DANGLING_SPEC_ITEM`.
