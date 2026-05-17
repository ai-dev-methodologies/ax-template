#!/usr/bin/env bash
# practices/evals/cross_trio_guard.sh — L4→L1/L2/L3 import evidence guard.
#
# Static-parses .tsx imports under templates/L4/<domain>/ for each domain.
# For each import resolving to templates/L{1,2,3}/, verifies the imported file
# carries an `evidence:` block in its frontmatter (YAML or inline comment).
#
# Zero-scan guard: if no L4 domain directory was walked, FAIL with ZERO_SCAN.
#
# Usage:
#   bash practices/evals/cross_trio_guard.sh [--root <repo_root>]
#   bash practices/evals/cross_trio_guard.sh --root practices/evals/fixtures/cross_trio/pass
set -uo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"

while [ $# -gt 0 ]; do
    case "$1" in
        --root) REPO_ROOT="$2"; shift 2 ;;
        --root=*) REPO_ROOT="${1#--root=}"; shift ;;
        *) echo "cross_trio_guard: unknown arg: $1" >&2; exit 2 ;;
    esac
done

python3 - "$REPO_ROOT" <<'PY'
import sys, pathlib, re

repo_root = pathlib.Path(sys.argv[1]).resolve()
l4_root = repo_root / "templates" / "L4"

violations = []
l4_dirs_walked = 0
tsx_files_walked = 0

# Pattern to match TypeScript/TSX import statements
IMPORT_RE = re.compile(r'''from\s+['"]([^'"]+)['"]''')

def file_has_evidence(filepath):
    """Check if a TSX/TS file carries an evidence: block.
    Accepts either YAML frontmatter (---...---) or inline comment (// evidence:).
    """
    try:
        text = filepath.read_text(encoding="utf-8")
    except Exception:
        return False
    # Check for YAML frontmatter with evidence:
    if text.startswith("---"):
        end = text.find("---", 3)
        if end != -1:
            frontmatter = text[3:end]
            if "evidence:" in frontmatter:
                return True
    # Check for inline evidence comment
    if "evidence:" in text[:500]:  # first 500 chars
        return True
    return False


def resolve_import(importing_file, import_path, templates_root):
    """Resolve a relative import path to an absolute file path under templates/.
    Returns the absolute path if it resolves to L1/L2/L3, else None.
    """
    # Only care about relative imports or explicit templates/ imports
    if not import_path.startswith(".") and "templates" not in import_path:
        return None

    base = importing_file.parent
    if import_path.startswith("."):
        resolved = (base / import_path).resolve()
    else:
        # Try treating as relative to repo_root
        resolved = (repo_root / import_path).resolve()

    # Check if it lands in L1, L2, or L3
    templates_abs = templates_root.resolve()
    for layer in ("L1", "L2", "L3"):
        layer_path = (templates_abs / layer).resolve()
        try:
            resolved.relative_to(layer_path)
            # It's in L1/L2/L3 — find the actual file
            candidates = []
            for ext in ("", ".tsx", ".ts", "/index.tsx", "/index.ts"):
                candidate = pathlib.Path(str(resolved) + ext)
                if candidate.exists():
                    candidates.append(candidate)
            if candidates:
                return candidates[0]
            # Try without resolve (the path may not exist yet)
            return resolved
        except ValueError:
            continue
    return None


if not l4_root.exists():
    print("ZERO_SCAN: templates/L4/ directory not found", file=sys.stderr)
    print("cross_trio_guard: ZERO_SCAN — merge BLOCKED", file=sys.stderr)
    sys.exit(1)

templates_root = repo_root / "templates"
for domain_dir in sorted(l4_root.iterdir()):
    if not domain_dir.is_dir():
        continue
    # Skip .gitkeep-only dirs
    tsx_in_domain = list(domain_dir.rglob("*.tsx"))
    if not tsx_in_domain:
        continue

    l4_dirs_walked += 1
    for tsx_file in sorted(tsx_in_domain):
        tsx_files_walked += 1
        try:
            source = tsx_file.read_text(encoding="utf-8")
        except Exception:
            continue
        imports = IMPORT_RE.findall(source)
        for imp in imports:
            resolved = resolve_import(tsx_file, imp, templates_root)
            if resolved is None:
                continue
            # Check evidence
            if not file_has_evidence(resolved):
                rel = str(resolved.relative_to(repo_root)) if resolved.is_relative_to(repo_root) else str(resolved)
                violations.append(f"ORPHAN_EVIDENCE: {rel} (imported from {tsx_file.relative_to(repo_root)})")

if l4_dirs_walked == 0:
    print("ZERO_SCAN: no L4 domain directories with .tsx files were found", file=sys.stderr)
    print("cross_trio_guard: ZERO_SCAN — merge BLOCKED", file=sys.stderr)
    sys.exit(1)

if violations:
    for v in violations:
        print(f"VIOLATION: {v}", file=sys.stderr)
    print(f"cross_trio_guard: {len(violations)} violation(s) — merge BLOCKED", file=sys.stderr)
    sys.exit(1)

print(f"cross_trio_guard: all imports evidence-anchored ({l4_dirs_walked} domains, {tsx_files_walked} files)")
sys.exit(0)
PY
