# trio_integrity/fail_manifest_missing_route_source — expected exit 1

Identical to pass_manifest_content except render_boundary.view names a file that does
not exist — the render boundary the manifest claims to enforce is unanchored.

Expected: exit 1, `MANIFEST_MISSING_ROUTE_SOURCE`.
