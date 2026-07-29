#!/usr/bin/env bash
# templates/backend/_check-anchors.sh — DEV CONVENIENCE WRAPPER (BACKLOG P3-90).
#
# ┌─ OWNING GATE ───────────────────────────────────────────────────────────────┐
# │  practices/evals/evidence_guard.sh  (§4.10 templates/ walk)                 │
# │  It owns BOTH axes this script used to check independently:                 │
# │    · evidence  — shape A/B/C entries, upstream_id resolution, required      │
# │                  fields, the _template.md placeholder rejection             │
# │    · anchors   — anchors_rule resolves to an existing practices/rules/*.md  │
# │                  OR to a <document>#<item> spec/contract reference whose    │
# │                  document exists AND contains the item                      │
# │  evidence_guard runs inside run-all-guards.sh, and therefore inside R25.    │
# └─────────────────────────────────────────────────────────────────────────────┘
#
# WHY THIS IS NOW A WRAPPER. Until 2026-07-29 this script carried its OWN checker
# (_check-anchors.py). Two implementations of "is this template's metadata sound" drifted,
# and the drift was the defect (BACKLOG P3-90): the script reported 57 violations while
# being wired to no gate at all, so nobody ran it and the count could grow unnoticed — and
# where it was STRICTER than the real gate it produced noise, while where it was WEAKER it
# hid real defects. Two it hid:
#   · anchors_rule values naming a spec/contract item (`specs/audit-log-l0.yaml#AUDIT-...`)
#     were scanned only for `*.md` tokens; finding none, it checked NOTHING and passed —
#     an entire anchor form outside every gate, including one genuinely stale reference.
#   · `source_type: internal` evidence was rejected here but is valid to the owning gate.
# Folding the anchors axis into evidence_guard removed the second implementation's reason
# to exist. This wrapper stays so the familiar command keeps working; it delegates, so the
# two can no longer disagree.
#
# Exit 0  — all template evidence + anchors valid
# Exit 1  — one or more violations (details printed by evidence_guard)
# Exit 2  — cannot verify (missing python3/PyYAML, unparseable manifest) — never a pass
#
# Usage:
#   bash templates/backend/_check-anchors.sh
#   bash templates/backend/_check-anchors.sh --verbose   # accepted, no extra output
set -uo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"

while [ $# -gt 0 ]; do
    case "$1" in
        # Kept for compatibility with the documented invocation. The owning gate always
        # reports every violation it finds, so there is no quiet mode to toggle.
        --verbose|-v) shift ;;
        *) echo "_check-anchors: unknown arg: $1" >&2; exit 2 ;;
    esac
done

echo "_check-anchors: delegating to the owning gate — practices/evals/evidence_guard.sh"
exec bash "$REPO_ROOT/practices/evals/evidence_guard.sh" --templates-root "$REPO_ROOT"
