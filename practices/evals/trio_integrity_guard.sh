#!/usr/bin/env bash
# practices/evals/trio_integrity_guard.sh — Frontend Spec Trio integrity guard.
#
# Validates that every domain listed in trio_integrity_allowlist.yaml satisfies
# its domain_mode requirements:
#
#   full_trio:     backend Spec Trio + frontend Spec Trio present; operation IDs resolve;
#                  100% coverage of frontend-required backend items.
#   backend_only:  backend spec exists; frontend check skipped.
#   frontend_only: frontend Spec Trio present; all routes and items have null
#                  backend_operation_id + non-empty static_source_ref resolving to
#                  existing files.
#
# Zero-scan guard: if no domain was scanned, FAIL with ZERO_SCAN.
#
# Usage:
#   bash practices/evals/trio_integrity_guard.sh [--root <repo_root>]
#   bash practices/evals/trio_integrity_guard.sh --root practices/evals/fixtures/trio_integrity/pass
#   bash practices/evals/trio_integrity_guard.sh --domain auth
#   bash practices/evals/trio_integrity_guard.sh --fixture frontend/tests/_fixtures/spec-trio-coverage-fail/
set -uo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
DOMAIN_FILTER=""

while [ $# -gt 0 ]; do
    case "$1" in
        --root) REPO_ROOT="$2"; shift 2 ;;
        --root=*) REPO_ROOT="${1#--root=}"; shift ;;
        --fixture) REPO_ROOT="$2"; shift 2 ;;
        --fixture=*) REPO_ROOT="${1#--fixture=}"; shift ;;
        --domain) DOMAIN_FILTER="$2"; shift 2 ;;
        --domain=*) DOMAIN_FILTER="${1#--domain=}"; shift ;;
        *) echo "trio_integrity_guard: unknown arg: $1" >&2; exit 2 ;;
    esac
done

# ── Fail closed: this guard verifies through PyYAML ──────────────────────────
# The trio scan parses the allowlist + every spec with PyYAML. Without the parser the
# python body dies on ImportError and the guard exits 1 — indistinguishable from a REAL
# trio violation (and from its own ZERO_SCAN block just below). Exit 2 = "cannot verify".
# Pinned by practices/evals/pyyaml_preflight_coverage_guard.sh [95].
if ! command -v python3 >/dev/null 2>&1 || ! python3 -c 'import yaml' >/dev/null 2>&1; then
    echo "trio_integrity_guard: BLOCK — cannot verify: python3 + PyYAML required (python3 -m pip install pyyaml)" >&2
    exit 2
fi

ALLOWLIST="$REPO_ROOT/trio_integrity_allowlist.yaml"
if [ ! -f "$ALLOWLIST" ]; then
    ALLOWLIST="$REPO_ROOT/practices/evals/trio_integrity_allowlist.yaml"
fi
if [ ! -f "$ALLOWLIST" ]; then
    echo "trio_integrity_guard: allowlist not found — ZERO_SCAN" >&2
    exit 1
fi

python3 - "$REPO_ROOT" "$ALLOWLIST" "$DOMAIN_FILTER" <<'PY'
import sys, pathlib, yaml

repo_root = pathlib.Path(sys.argv[1]).resolve()
allowlist_path = pathlib.Path(sys.argv[2]).resolve()
domain_filter = sys.argv[3] if len(sys.argv) > 3 else ""

data = yaml.safe_load(allowlist_path.read_text()) or {}
all_domains = data.get("domains", {})

# Apply --domain filter if provided
if domain_filter:
    if domain_filter not in all_domains:
        print(f"trio_integrity_guard: domain '{domain_filter}' not in allowlist", file=sys.stderr)
        sys.exit(2)
    domains = {domain_filter: all_domains[domain_filter]}
else:
    domains = all_domains

violations = []
files_scanned = 0

# ── Manifest-content census (P2-59) ──────────────────────────────────────────
# Before P2-59 the guard only asserted that blueprints/<domain>-ui-manifest.yaml
# EXISTED. Its CONTENT — the operation ids each surface claims, the spec items each
# policy block backlinks, the page/view files the render boundary names — was never
# parsed, so all of it could drift to nonsense while the guard stayed green.
#
# The three checks below are structure-conditional: a manifest that declares no
# routes/spec_item/render_boundary contributes nothing (the older manifests use a
# different, older shape). That makes vacuity the failure mode, so the live catalog
# run additionally asserts CENSUS FLOORS — shrink-only ratchets. Deleting the
# manifest content the checks read can no longer buy a green run.
MANIFEST_OP_FLOOR = 5          # webhook 2 + scheduled-task 2 + email-outbox 1
MANIFEST_SPEC_ITEM_FLOOR = 30  # observed 36 (audit-log 9, billing 4, email 8, sched 8, webhook 7)
MANIFEST_ROUTE_SOURCE_FLOOR = 6  # 3 manifests x (page, view)
manifest_op_refs = 0
manifest_spec_items = 0
manifest_route_sources = 0


def glob_expand(pattern_str, root):
    """Expand a shell-style glob pattern rooted at root. Returns list of matching paths."""
    import glob as glob_module
    # Try as literal path first
    literal = root / pattern_str
    if literal.exists():
        return [str(literal)]
    # Try as glob relative to root
    matches = glob_module.glob(str(root / pattern_str), recursive=True)
    return matches


# Keys the manifest walker recognises. Everything else in a manifest is prose/policy
# and is deliberately NOT interpreted — this guard checks resolvable references, it is
# not a schema validator.
_OP_SCALAR_KEYS = ("backend_operation_id",)
_OP_LIST_KEYS = ("backend_operation_ids", "secondary_operation_ids")
_SPEC_ITEM_SCALAR_KEYS = ("spec_item",)
_SPEC_ITEM_LIST_KEYS = ("spec_items",)
_ROUTE_SOURCE_SCALAR_KEYS = ("page", "view")
_ROUTE_SOURCE_LIST_KEYS = ("views",)


def _walk_manifest(node, path, ops, spec_items, route_sources, in_render_boundary=False):
    """Collect (value, yaml-path) pairs for the reference kinds the guard resolves."""
    if isinstance(node, dict):
        for key, val in node.items():
            here = f"{path}.{key}" if path else key
            if key in _OP_SCALAR_KEYS and not isinstance(val, (dict, list)):
                ops.append((val, here))
                continue
            if key in _OP_LIST_KEYS and isinstance(val, list):
                for i, v in enumerate(val):
                    ops.append((v, f"{here}[{i}]"))
                continue
            if key in _SPEC_ITEM_SCALAR_KEYS and not isinstance(val, (dict, list)):
                spec_items.append((val, here))
                continue
            if key in _SPEC_ITEM_LIST_KEYS or (key in _SPEC_ITEM_SCALAR_KEYS and isinstance(val, list)):
                if isinstance(val, list):
                    for i, v in enumerate(val):
                        spec_items.append((v, f"{here}[{i}]"))
                    continue
            # page/view are only file refs inside the render_boundary block; the same
            # word elsewhere in a policy manifest is prose.
            if in_render_boundary and key in _ROUTE_SOURCE_SCALAR_KEYS and isinstance(val, str):
                route_sources.append((val, here))
                continue
            if in_render_boundary and key in _ROUTE_SOURCE_LIST_KEYS and isinstance(val, list):
                for i, v in enumerate(val):
                    route_sources.append((v, f"{here}[{i}]"))
                continue
            _walk_manifest(
                val, here, ops, spec_items, route_sources,
                in_render_boundary or key == "render_boundary",
            )
    elif isinstance(node, list):
        for i, v in enumerate(node):
            _walk_manifest(v, f"{path}[{i}]", ops, spec_items, route_sources, in_render_boundary)


def check_manifest(domain, root, manifest_path, mode, known_ops, fe_item_ids, contract_route_paths):
    """P2-59 — parse blueprints/<domain>-ui-manifest.yaml and resolve what it CLAIMS.

    Before this the guard asserted only that the file existed. Checks added:
      1. MANIFEST_UNPARSEABLE          — the file must be a YAML mapping.
      2. MANIFEST_UNRESOLVED_OPERATION_ID / MANIFEST_OPERATION_ID_IN_FRONTEND_ONLY
         — every operation id the manifest names resolves in the backend OpenAPI
           (full_trio); a frontend_only manifest may name none.
      3. MANIFEST_DANGLING_SPEC_ITEM   — every spec_item backlink resolves to a real
           item id in specs/<domain>-frontend-l0.yaml.
      4. MANIFEST_MISSING_ROUTE_SOURCE — every render_boundary page/view file exists.
      5. MANIFEST_ROUTE_NOT_IN_CONTRACT — every routes.surfaces[].path is also a route
           in contracts/<domain>-ui.yaml (the manifest cannot police a surface the
           contract does not publish).
    """
    global manifest_op_refs, manifest_spec_items, manifest_route_sources
    errs = []
    try:
        manifest = yaml.safe_load(manifest_path.read_text())
    except Exception as exc:  # noqa: BLE001 — any parse failure is the same verdict
        return [f"MANIFEST_UNPARSEABLE: blueprints/{domain}-ui-manifest.yaml ({exc.__class__.__name__}: {exc})"]
    if manifest is None:
        return [f"MANIFEST_UNPARSEABLE: blueprints/{domain}-ui-manifest.yaml is empty"]
    if not isinstance(manifest, dict):
        return [f"MANIFEST_UNPARSEABLE: blueprints/{domain}-ui-manifest.yaml is not a mapping ({type(manifest).__name__})"]

    ops, spec_items, route_sources = [], [], []
    _walk_manifest(manifest, "", ops, spec_items, route_sources)

    for value, where in ops:
        if value is None:
            if mode == "full_trio":
                errs.append(f"MANIFEST_NULL_OPERATION_ID: {where} is null in full_trio mode")
            continue
        manifest_op_refs += 1
        if mode == "frontend_only":
            errs.append(f"MANIFEST_OPERATION_ID_IN_FRONTEND_ONLY: {where} = {value!r}")
            continue
        if known_ops is not None and value not in known_ops:
            errs.append(
                f"MANIFEST_UNRESOLVED_OPERATION_ID: {where} = {value!r} not published by the backend contract"
            )

    for value, where in spec_items:
        if value is None:
            continue
        manifest_spec_items += 1
        if fe_item_ids is not None and value not in fe_item_ids:
            errs.append(
                f"MANIFEST_DANGLING_SPEC_ITEM: {where} = {value!r} is not an item id in specs/{domain}-frontend-l0.yaml"
            )

    for value, where in route_sources:
        if not isinstance(value, str) or not value.strip():
            errs.append(f"MANIFEST_MISSING_ROUTE_SOURCE: {where} is empty")
            continue
        manifest_route_sources += 1
        # Literal resolution only: these are concrete files, and Next.js route
        # segments ("[id]", "(admin)") are glob metacharacters that would make a
        # glob-based check silently match the wrong thing.
        if not (root / value).exists():
            errs.append(f"MANIFEST_MISSING_ROUTE_SOURCE: {where} = {value!r} does not exist")

    routes_block = manifest.get("routes")
    if isinstance(routes_block, dict) and contract_route_paths is not None:
        for i, surface in enumerate(routes_block.get("surfaces") or []):
            if not isinstance(surface, dict):
                continue
            path_val = surface.get("path")
            if path_val is None:
                errs.append(f"MANIFEST_ROUTE_NOT_IN_CONTRACT: routes.surfaces[{i}] has no path")
                continue
            if path_val not in contract_route_paths:
                errs.append(
                    f"MANIFEST_ROUTE_NOT_IN_CONTRACT: routes.surfaces[{i}].path = {path_val!r} "
                    f"is not a route in contracts/{domain}-ui.yaml"
                )

    return errs


def check_full_trio(domain, root):
    global files_scanned
    errs = []

    # Find backend spec — any specs/<domain>-*.yaml
    specs_dir = root / "specs"
    backend_specs = list(specs_dir.glob(f"{domain}-*.yaml")) if specs_dir.exists() else []
    # Exclude frontend spec from backend detection
    backend_specs = [s for s in backend_specs if "frontend" not in s.name]
    if not backend_specs:
        errs.append(f"MISSING_BACKEND_SPEC: no specs/{domain}-*.yaml found")
        return errs
    backend_spec_path = backend_specs[0]
    files_scanned += 1
    backend_spec = yaml.safe_load(backend_spec_path.read_text()) or {}

    # Check frontend_required field
    if backend_spec.get("frontend_required") is False:
        errs.append(f"MODE_MISMATCH: {backend_spec_path.name} has frontend_required: false but domain mode is full_trio")
        return errs

    # Require frontend Spec Trio
    fe_spec_candidates = list(specs_dir.glob(f"{domain}-frontend-l0.yaml")) if specs_dir.exists() else []
    if not fe_spec_candidates:
        errs.append(f"MISSING_FRONTEND_SPEC: specs/{domain}-frontend-l0.yaml not found")
    contracts_dir = root / "contracts"
    ui_contract_path = contracts_dir / f"{domain}-ui.yaml" if contracts_dir.exists() else None
    if not ui_contract_path or not ui_contract_path.exists():
        errs.append(f"MISSING_FRONTEND_SPEC: contracts/{domain}-ui.yaml not found")
    blueprints_dir = root / "blueprints"
    ui_manifest_path = blueprints_dir / f"{domain}-ui-manifest.yaml" if blueprints_dir.exists() else None
    if not ui_manifest_path or not ui_manifest_path.exists():
        errs.append(f"MISSING_FRONTEND_SPEC: blueprints/{domain}-ui-manifest.yaml not found")
    if errs:
        return errs

    files_scanned += 3

    # Load frontend specs
    fe_spec = yaml.safe_load(fe_spec_candidates[0].read_text()) or {}
    ui_contract = yaml.safe_load(ui_contract_path.read_text()) or {}

    # Resolve backend OpenAPI
    backend_contract_ref = ui_contract.get("backend_contract_ref")
    if not backend_contract_ref:
        errs.append(f"MISSING_BACKEND_CONTRACT_REF: contracts/{domain}-ui.yaml has no backend_contract_ref")
        return errs

    openapi_path = root / backend_contract_ref
    if not openapi_path.exists():
        errs.append(f"MISSING_OPENAPI: {backend_contract_ref} not found")
        return errs
    files_scanned += 1
    openapi = yaml.safe_load(openapi_path.read_text()) or {}
    # Collect all operationIds from OpenAPI
    known_ops = set()
    for path_item in (openapi.get("paths") or {}).values():
        for method_data in path_item.values():
            if isinstance(method_data, dict) and "operationId" in method_data:
                known_ops.add(method_data["operationId"])

    # Validate routes
    for route in ui_contract.get("routes", []):
        # Redirect-only routes legitimately have no backend_operation_id
        if route.get("redirect_to"):
            continue
        op_id = route.get("backend_operation_id")
        if op_id is None:
            errs.append(f"NULL_OPERATION_ID: route {route.get('path')} has null backend_operation_id in full_trio mode")
            continue
        if op_id not in known_ops:
            errs.append(f"UNRESOLVED_OPERATION_ID: {op_id} not found in {backend_contract_ref} (known: {sorted(known_ops)})")

    # Coverage check: count backend items with frontend_required: true
    be_items_requiring_fe = [
        item["id"]
        for item in backend_spec.get("items", [])
        if item.get("frontend_required", False)
    ]
    # Count frontend items that reference a backend item
    covered_be_refs = set()
    for fe_item in fe_spec.get("items", []):
        ref = fe_item.get("backend_spec_ref")
        if ref:
            covered_be_refs.add(ref)

    required_count = len(be_items_requiring_fe)
    covered_count = sum(1 for be_id in be_items_requiring_fe if be_id in covered_be_refs)
    if required_count > 0 and covered_count < required_count:
        errs.append(f"COVERAGE_SHORTFALL: {covered_count}/{required_count}")

    # P2-59 — the manifest's own content, not just its existence.
    errs.extend(check_manifest(
        domain, root, ui_manifest_path, "full_trio",
        known_ops,
        {i.get("id") for i in (fe_spec.get("items") or []) if isinstance(i, dict)},
        {r.get("path") for r in (ui_contract.get("routes") or []) if isinstance(r, dict)},
    ))

    return errs


def check_backend_only(domain, root):
    global files_scanned
    errs = []
    specs_dir = root / "specs"
    backend_specs = list(specs_dir.glob(f"{domain}-*.yaml")) if specs_dir.exists() else []
    backend_specs = [s for s in backend_specs if "frontend" not in s.name]
    if not backend_specs:
        errs.append(f"MISSING_BACKEND_SPEC: no specs/{domain}-*.yaml found")
    else:
        files_scanned += 1
    return errs


def check_frontend_only(domain, root):
    global files_scanned
    errs = []

    # Require frontend Spec Trio
    specs_dir = root / "specs"
    fe_spec_candidates = list(specs_dir.glob(f"{domain}-frontend-l0.yaml")) if specs_dir.exists() else []
    if not fe_spec_candidates:
        errs.append(f"MISSING_FRONTEND_SPEC: specs/{domain}-frontend-l0.yaml not found")
    contracts_dir = root / "contracts"
    ui_contract_path = contracts_dir / f"{domain}-ui.yaml" if contracts_dir.exists() else None
    if not ui_contract_path or not ui_contract_path.exists():
        errs.append(f"MISSING_FRONTEND_SPEC: contracts/{domain}-ui.yaml not found")
    blueprints_dir = root / "blueprints"
    ui_manifest_path = blueprints_dir / f"{domain}-ui-manifest.yaml" if blueprints_dir.exists() else None
    if not ui_manifest_path or not ui_manifest_path.exists():
        errs.append(f"MISSING_FRONTEND_SPEC: blueprints/{domain}-ui-manifest.yaml not found")
    if errs:
        return errs

    files_scanned += 3

    fe_spec = yaml.safe_load(fe_spec_candidates[0].read_text()) or {}
    ui_contract = yaml.safe_load(ui_contract_path.read_text()) or {}

    # Check routes
    for route in ui_contract.get("routes", []):
        path = route.get("path", "?")
        op_id = route.get("backend_operation_id")
        if op_id is not None:
            errs.append(f"frontend_only route has non-null backend_operation_id: {op_id} (path: {path})")
            continue
        src_refs = route.get("static_source_ref") or []
        if not src_refs:
            errs.append(f"frontend_only route missing static_source_ref (path: {path})")
            continue
        for ref_entry in src_refs:
            matches = glob_expand(ref_entry, root)
            if not matches:
                errs.append(f"static_source_ref resolves to zero files: {ref_entry} (route: {path})")

    # Check items (mirror route check exactly — §4.8.4 iter4)
    for item in fe_spec.get("items", []):
        item_id = item.get("id", "?")
        op_id = item.get("backend_operation_id")
        if op_id is not None:
            errs.append(f"frontend_only item has non-null backend_operation_id: {op_id} (item: {item_id})")
            continue
        src_refs = item.get("static_source_ref") or []
        if not src_refs:
            errs.append(f"frontend_only item missing static_source_ref (item: {item_id})")
            continue
        for ref_entry in src_refs:
            matches = glob_expand(ref_entry, root)
            if not matches:
                errs.append(f"static_source_ref resolves to zero files: {ref_entry} (item: {item_id})")

    # P2-59 — the manifest's own content, not just its existence.
    errs.extend(check_manifest(
        domain, root, ui_manifest_path, "frontend_only",
        None,
        {i.get("id") for i in (fe_spec.get("items") or []) if isinstance(i, dict)},
        {r.get("path") for r in (ui_contract.get("routes") or []) if isinstance(r, dict)},
    ))

    return errs


# Process each domain
for domain, mode in domains.items():
    if mode == "full_trio":
        errs = check_full_trio(domain, repo_root)
    elif mode == "backend_only":
        errs = check_backend_only(domain, repo_root)
    elif mode == "frontend_only":
        errs = check_frontend_only(domain, repo_root)
    else:
        errs = [f"unknown domain_mode: {mode!r}"]
    for e in errs:
        violations.append(f"[{domain}] {e}")

# ── P2-59 census floors (live catalog run only) ──────────────────────────────
# The manifest checks are structure-conditional, so their failure mode is vacuity:
# strip routes/spec_item/render_boundary out of every manifest and the checks would
# assert nothing while still printing PASS. On the catalog tree (full sweep, no
# --domain filter) the guard therefore asserts shrink-only floors on how much it
# actually resolved. Fixture roots are exempt — they are minimal by construction.
catalog_allowlist = (repo_root / "practices" / "evals" / "trio_integrity_allowlist.yaml").resolve()
is_catalog_sweep = (allowlist_path == catalog_allowlist) and not domain_filter
if is_catalog_sweep:
    for label, observed, floor in (
        ("operation ids", manifest_op_refs, MANIFEST_OP_FLOOR),
        ("spec_item backlinks", manifest_spec_items, MANIFEST_SPEC_ITEM_FLOOR),
        ("route sources", manifest_route_sources, MANIFEST_ROUTE_SOURCE_FLOOR),
    ):
        if observed < floor:
            violations.append(
                f"[census] MANIFEST_CENSUS_SHRANK: resolved {observed} manifest {label}, floor is {floor} "
                f"— manifest content was deleted rather than fixed (raise the floor only when it grows)"
            )

# Zero-scan guard
if files_scanned == 0:
    print("ZERO_SCAN: no files were scanned — allowlist may point to non-existent domains", file=sys.stderr)
    print("trio_integrity_guard: ZERO_SCAN — merge BLOCKED", file=sys.stderr)
    sys.exit(1)

if violations:
    for v in violations:
        print(f"VIOLATION: {v}", file=sys.stderr)
    print(f"trio_integrity_guard: {len(violations)} violation(s) — merge BLOCKED", file=sys.stderr)
    sys.exit(1)

print(
    f"trio_integrity_guard: all domains pass ({files_scanned} files scanned; "
    f"manifest refs resolved — {manifest_op_refs} operation ids, "
    f"{manifest_spec_items} spec_item backlinks, {manifest_route_sources} route sources)"
)
sys.exit(0)
PY
