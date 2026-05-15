#!/usr/bin/env bash
# practices/evals/evidence_guard.sh — fourth binary hard gate.
#
# Forbids rules whose claims are not anchored to a recorded external source. The rule body
# itself is allowed to be Claude-authored; the *justification* must trace back to either
# (a) a snapshot in practices/upstream/_MANIFEST.yaml, or (b) an explicit external citation
# (RFC, JEP, vendor docs, peer-reviewed paper).
#
# This gate is the answer to "why was this rule made?" — every rule must be auditable.
set -uo pipefail

cd "$(dirname "$0")/.."

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
    [[ $? -ne 0 ]] && violations=$((violations + 1))
done

if [[ $violations -gt 0 ]]; then
    echo "evidence_guard: $violations rule(s) lack auditable evidence — merge BLOCKED" >&2
    exit 1
fi

echo "evidence_guard: all rules have auditable evidence"
exit 0
