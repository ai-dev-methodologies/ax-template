#!/usr/bin/env bash
# practices/evals/evidence_guard.sh — fourth binary hard gate.
#
# Forbids rules whose claims are not anchored to a recorded external source. The rule body
# itself is allowed to be Claude-authored; the *justification* must trace back to either
# (a) a snapshot in {catalog}/upstream/_MANIFEST.yaml, or (b) an explicit external citation
# (RFC, JEP, vendor docs, peer-reviewed paper).
#
# This gate is the answer to "why was this rule made?" — every rule must be auditable.
#
# SCOPE — STRUCTURE, not TRUTH (BACKLOG P2-1): this gate verifies the evidence SHAPE
# (upstream_id resolves in _MANIFEST.yaml, section/quote/citation/url non-empty). It does
# NOT verify the quote text actually appears in the snapshot or on the live page — a
# fabricated quote with valid structure passes. The offline half of that escape is covered
# by evidence_quote_spotcheck_guard.sh (deterministic quote-vs-snapshot sweep, advisory);
# the online half (quote-vs-live-page) only a live-fetch audit can verify.
#
# Usage:
#   bash practices/evals/evidence_guard.sh                       # default catalog=practices
#   bash practices/evals/evidence_guard.sh --catalog practices-react
set -uo pipefail

CATALOG="${CATALOG:-practices}"
CATALOG_DIR_OVERRIDE=""
while [ $# -gt 0 ]; do
    case "$1" in
        --catalog) CATALOG="$2"; shift 2 ;;
        --catalog=*) CATALOG="${1#--catalog=}"; shift ;;
        /*) CATALOG_DIR_OVERRIDE="$1"; shift ;;
        *) echo "evidence_guard: unknown arg: $1" >&2; exit 2 ;;
    esac
done

# ── Fail closed: this guard verifies through PyYAML ──────────────────────────
# Without the parser there is nothing to report, so exit 2 ("cannot verify") — NEVER 0.
# A skip that shares its exit code with a pass is a green gate that checked nothing,
# which is the failure class this catalog exists to prevent. Pinned mechanically by
# practices/evals/pyyaml_preflight_coverage_guard.sh [95].
if ! command -v python3 >/dev/null 2>&1 || ! python3 -c 'import yaml' >/dev/null 2>&1; then
    echo "evidence_guard: BLOCK — cannot verify: python3 + PyYAML required (python3 -m pip install pyyaml)" >&2
    exit 2
fi

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
CATALOG_DIR="${CATALOG_DIR_OVERRIDE:-$REPO_ROOT/$CATALOG}"

if [ ! -d "$CATALOG_DIR" ]; then
    echo "evidence_guard: catalog '$CATALOG' not found at $CATALOG_DIR — nothing to check"
    exit 0
fi

cd "$CATALOG_DIR"

violations=0
shopt -s nullglob

# Build the set of registered snapshot ids for upstream_id validation.
MANIFEST_IDS=""
if [[ -f upstream/_MANIFEST.yaml ]]; then
    MANIFEST_IDS=$(python3 - <<'PY'
import yaml, pathlib
d = yaml.safe_load(pathlib.Path("upstream/_MANIFEST.yaml").read_text()) or {}
print("\n".join(s.get("id","") for s in d.get("snapshots", [])))
PY
    )
    # Same distinction as the per-rule loop below: an unparseable manifest yields an
    # EMPTY id set, under which every upstream_id looks unregistered — a parse failure
    # masquerading as a catalog-wide evidence violation. Cannot parse ⇒ BLOCK.
    if [[ $? -ne 0 ]]; then
        echo "evidence_guard: BLOCK — cannot parse upstream/_MANIFEST.yaml (tooling/parse failure, NOT an evidence violation)" >&2
        exit 2
    fi
fi

for rule in rules/*.md; do
    [[ "$(basename "$rule")" == "_template.md" ]] && continue
    [[ "$(basename "$rule")" == ".gitkeep" ]] && continue

    python3 - "$rule" "$MANIFEST_IDS" <<'PY'
import pathlib, sys, yaml

path = pathlib.Path(sys.argv[1])
manifest_ids = set(filter(None, sys.argv[2].splitlines()))
text = path.read_text()

# Extract frontmatter (between leading --- fences).
if not text.startswith("---"):
    print(f"VIOLATION [{path}]: no YAML frontmatter")
    sys.exit(1)
parts = text.split("---", 2)
if len(parts) < 3:
    print(f"VIOLATION [{path}]: malformed frontmatter")
    sys.exit(1)

try:
    fm = yaml.safe_load(parts[1]) or {}
except yaml.YAMLError as e:
    print(f"VIOLATION [{path}]: frontmatter YAML parse error: {e}")
    sys.exit(1)

ev = fm.get("evidence")
if not isinstance(ev, list) or len(ev) == 0:
    print(f"VIOLATION [{path}]: `evidence` field missing or empty (need ≥1 entry)")
    sys.exit(1)

prov = fm.get("provenance_class")
errors = []
for i, item in enumerate(ev):
    if not isinstance(item, dict):
        errors.append(f"entry {i}: not a mapping")
        continue

    if "upstream_id" in item:
        uid = item["upstream_id"]
        if uid not in manifest_ids:
            errors.append(f"entry {i}: upstream_id={uid!r} not found in _MANIFEST.yaml (known: {sorted(manifest_ids)})")
        if not str(item.get("section", "")).strip():
            errors.append(f"entry {i}: missing `section`")
        if not str(item.get("quote", "")).strip():
            errors.append(f"entry {i}: missing `quote`")
    elif item.get("source_type") == "external":
        if not str(item.get("citation", "")).strip():
            errors.append(f"entry {i}: missing `citation`")
        if not str(item.get("url", "")).strip():
            errors.append(f"entry {i}: missing `url`")
        # P2-20: an internal_design rule's design decision is author-made, so any
        # external citation it carries can only anchor a GENERIC principle, never
        # mandate the specific rule. Force that honesty mechanically — the entry
        # must declare `anchors: generic_principle_only`. If a citation truly
        # mandated the rule, the rule would not be internal_design.
        if prov == "internal_design" and str(item.get("anchors", "")).strip() != "generic_principle_only":
            errors.append(f"entry {i}: provenance_class=internal_design + source_type=external requires `anchors: generic_principle_only`")
    else:
        errors.append(f"entry {i}: must have either `upstream_id` or `source_type: external`")

# Template placeholder rejection: any url containing the exact placeholder from _template.md
# must not survive into a real rule.
placeholder_marker = "(replace with the standard / docs you actually consulted)"
for i, item in enumerate(ev):
    if isinstance(item, dict) and placeholder_marker in str(item.get("citation", "")):
        errors.append(f"entry {i}: citation still contains the _template.md placeholder")

if errors:
    print(f"VIOLATION [{path}]:")
    for e in errors:
        print(f"  - {e}")
    sys.exit(1)
sys.exit(0)
PY
    # Distinguish "this rule violates the contract" from "the checker could not run".
    # exit 1 is the checker's own violation signal; ANY other non-zero (ImportError,
    # traceback, killed interpreter) means we did not actually verify this rule, and
    # counting it as a violation produces a false diagnosis — historically 233 phantom
    # "lack auditable evidence" findings when PyYAML was merely absent, sending a
    # contributor after a problem that did not exist. Cannot-verify ⇒ BLOCK (exit 2).
    rule_rc=$?
    if [[ $rule_rc -eq 1 ]]; then
        violations=$((violations + 1))
    elif [[ $rule_rc -ne 0 ]]; then
        echo "evidence_guard: BLOCK — cannot verify $rule: checker exited $rule_rc (tooling/parse failure, NOT an evidence violation)" >&2
        exit 2
    fi
done

if [[ $violations -gt 0 ]]; then
    echo "evidence_guard: $violations rule(s) lack auditable evidence — merge BLOCKED" >&2
    exit 1
fi

# ── templates/ walk extension (§4.10) ────────────────────────────────────────
# Walk templates/L{1,2,3,4}/**/*.{md,tsx,ts,yaml}, templates/backend/**/*.{java,yaml,md},
# and templates/DECISIONS.md.
# Zero-scan guard: if templates/ exists but produces zero matching files → FAIL ZERO_SCAN.

TEMPLATES_DIR="$REPO_ROOT/templates"
if [[ -d "$TEMPLATES_DIR" ]]; then
    templates_files=()
    shopt -s nullglob
    for f in \
        "$TEMPLATES_DIR"/DECISIONS.md \
        "$TEMPLATES_DIR"/L1/*.md "$TEMPLATES_DIR"/L1/*.tsx "$TEMPLATES_DIR"/L1/*.ts "$TEMPLATES_DIR"/L1/*.yaml \
        "$TEMPLATES_DIR"/L2/*.md "$TEMPLATES_DIR"/L2/*.tsx "$TEMPLATES_DIR"/L2/*.ts "$TEMPLATES_DIR"/L2/*.yaml \
        "$TEMPLATES_DIR"/L3/*.md "$TEMPLATES_DIR"/L3/*.tsx "$TEMPLATES_DIR"/L3/*.ts "$TEMPLATES_DIR"/L3/*.yaml \
        "$TEMPLATES_DIR"/L4/*.md "$TEMPLATES_DIR"/L4/*.tsx "$TEMPLATES_DIR"/L4/*.ts "$TEMPLATES_DIR"/L4/*.yaml \
        "$TEMPLATES_DIR"/backend/*.java "$TEMPLATES_DIR"/backend/*.yaml "$TEMPLATES_DIR"/backend/*.md; do
        [[ -f "$f" ]] && templates_files+=("$f")
    done
    # Also recurse into subdirs
    while IFS= read -r f; do
        [[ -f "$f" ]] && templates_files+=("$f")
    done < <(find "$TEMPLATES_DIR/L1" "$TEMPLATES_DIR/L2" "$TEMPLATES_DIR/L3" "$TEMPLATES_DIR/L4" "$TEMPLATES_DIR/backend" \
        -name "*.md" -o -name "*.tsx" -o -name "*.ts" -o -name "*.yaml" -o -name "*.java" 2>/dev/null | sort)
    shopt -u nullglob

    # Deduplicate
    IFS=$'\n' templates_files=($(printf '%s\n' "${templates_files[@]}" | sort -u)); unset IFS

    if [[ ${#templates_files[@]} -eq 0 ]]; then
        echo "evidence_guard: ZERO_SCAN — templates/ exists but no scannable files found — merge BLOCKED" >&2
        exit 1
    fi
    echo "evidence_guard: templates/ walk found ${#templates_files[@]} file(s) — evidence check passed (catalog rules already verified above)"
fi

echo "evidence_guard: all rules have auditable evidence"
exit 0
