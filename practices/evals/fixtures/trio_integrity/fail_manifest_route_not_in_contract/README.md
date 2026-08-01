# trio_integrity/fail_manifest_route_not_in_contract — expected exit 1

Identical to pass_manifest_content except the surface path `/(admin)/widgets` is not a
route in contracts/auth-ui.yaml. The manifest cannot police a surface the contract
does not publish; the two would drift apart silently.

Expected: exit 1, `MANIFEST_ROUTE_NOT_IN_CONTRACT`.
