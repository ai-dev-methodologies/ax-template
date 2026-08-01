# trio_integrity/fail_manifest_drifted_operation_id — expected exit 1

Identical to pass_manifest_content except the surface claims `loginUserV2`, which
contracts/auth-openapi.yaml does not publish — the exact drift the old
existence-only check could not see.

Expected: exit 1, `MANIFEST_UNRESOLVED_OPERATION_ID`.
