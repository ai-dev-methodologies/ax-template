#!/usr/bin/env bash
# FIXTURE stand-in for evidence_guard.sh.
# Mirrors the real gate's contract just enough to exercise the evidence proof toggle:
#   exit 1 (BLOCK) if a rule's evidence is a _template.md placeholder citation or an
#                  empty url (fabricated / un-anchored);
#   exit 0 (PASS)  if it carries a real citation + url.
# Used only by the agent_block_proof_guard pass fixture — never the real catalog.
set -uo pipefail
CAT="."
while [ $# -gt 0 ]; do
    case "$1" in
        /*) CAT="$1"; shift ;;
        --catalog) CAT="$2"; shift 2 ;;
        --catalog=*) CAT="${1#--catalog=}"; shift ;;
        *) shift ;;
    esac
done
RULES="$CAT/rules"
[ -d "$RULES" ] || { echo "fixture evidence guard: no rules to scan (zero-scan)"; exit 1; }
for r in "$RULES"/*.md; do
    [ -f "$r" ] || continue
    if grep -q 'replace with the standard' "$r" || grep -qE 'url:[[:space:]]*""' "$r"; then
        echo "fixture evidence guard: placeholder/empty evidence — BLOCKED"
        exit 1
    fi
done
echo "fixture evidence guard: evidence anchored — PASS"
exit 0
