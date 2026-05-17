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

print(f"trio_integrity_guard: all domains pass ({files_scanned} files scanned)")
sys.exit(0)
PY
