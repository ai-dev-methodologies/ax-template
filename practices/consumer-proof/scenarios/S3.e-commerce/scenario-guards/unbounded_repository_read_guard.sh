#!/usr/bin/env bash
# practices/consumer-proof/scenarios/S3.e-commerce/scenario-guards/unbounded_repository_read_guard.sh
#
# HAND-ROLLED — capability-gap signal.
# ax-template's catalog states the "no unbounded findAll" invariant as a RULE
# (practices/rules/api-pagination-pageable.md) and enforces it at the JVM layer
# via ArchUnit (requires `./gradlew test`, a full compile). There is NO
# standalone, --root-parameterized shell guard in practices/evals/ for this
# invariant (verified: `ls practices/evals/*.sh | grep -iE 'page|list|unbounded'`
# returns nothing that scans *Repository.java text). So this scenario cannot
# reuse a catalog asset for the "unbounded order findAll" violation named in
# the dogfood brief — it is hand-rolled here, isolated to this scenario dir,
# modeled on the style of the catalog's own text-scanning shell guards
# (money_boundary_seam_guard.sh / controller_repository_shell_guard.sh).
#
# WHAT IT ENFORCES
# A *Repository.java interface method whose declared return type is
# List<Entity> or Iterable<Entity> and whose name starts with `find` MUST take
# a org.springframework.data.domain.Pageable parameter (or return Page<Entity>
# instead). A finder with no Pageable and a bare List/Iterable return type
# loads the ENTIRE matching result set into memory on a single call — the
# unbounded-read anti-pattern.
#
# Exit codes: 0 — no unbounded finder · 1 — violation (signature:
# UNBOUNDED_REPOSITORY_READ) · 2 — usage/env error.
#
# Usage: bash unbounded_repository_read_guard.sh --root DIR

set -uo pipefail

ROOT_OVERRIDE=""
while [ $# -gt 0 ]; do
    case "$1" in
        --root) ROOT_OVERRIDE="$2"; shift 2 ;;
        --root=*) ROOT_OVERRIDE="${1#--root=}"; shift ;;
        *) echo "unbounded_repository_read_guard: unknown arg: $1" >&2; exit 2 ;;
    esac
done
if [ -z "$ROOT_OVERRIDE" ]; then
    echo "unbounded_repository_read_guard: --root DIR is required" >&2
    exit 2
fi

SRC="$ROOT_OVERRIDE/backend/src/main/java/com/ax/template/authblueprint"
if [ ! -d "$SRC" ]; then
    echo "unbounded_repository_read_guard: no backend source tree at $SRC — SKIP"
    exit 0
fi

repo_files="$(find "$SRC" -name '*Repository.java' 2>/dev/null || true)"
count=0
if [ -n "$repo_files" ]; then
    count="$(printf '%s\n' "$repo_files" | grep -c . || true)"
fi
if [ "$count" -eq 0 ]; then
    echo "unbounded_repository_read_guard: 0 *Repository.java files — nothing to check"
    exit 0
fi
echo "unbounded_repository_read_guard: scanned $count *Repository.java file(s)"

# Candidate finder-method declaration: a return type of List<X> or Iterable<X>,
# a method name starting with `find`, and a parenthesized parameter list —
# all on one line (fixtures are single-line declarations, consistent with the
# rest of the catalog's text-scanning guards).
PATTERN='^[[:space:]]*(List|Iterable)<[A-Za-z_][A-Za-z0-9_]*>[[:space:]]+find[A-Za-z0-9_]*\(.*\)[[:space:]]*;'

violations=""
while IFS= read -r f; do
    [ -z "$f" ] && continue
    hits="$(grep -nE "$PATTERN" "$f" 2>/dev/null || true)"
    [ -z "$hits" ] && continue
    while IFS= read -r line; do
        [ -z "$line" ] && continue
        lineno="${line%%:*}"
        rest="${line#*:}"
        if printf '%s' "$rest" | grep -q 'Pageable'; then
            continue  # bounded — takes a Pageable param, not a violation
        fi
        violations="$violations
  $f:$lineno: $rest"
    done <<EOF
$hits
EOF
done <<EOF
$repo_files
EOF

if [ -n "$violations" ]; then
    echo "VIOLATION: unbounded repository finder — List/Iterable<Entity> findXxx(...) with no Pageable" >&2
    echo "  this loads the ENTIRE matching result set into memory on one call:" >&2
    echo "$violations" | sed 's/^/  /' >&2
    echo "unbounded_repository_read_guard: UNBOUNDED_REPOSITORY_READ — BLOCKED" >&2
    exit 1
fi

echo "unbounded_repository_read_guard: no unbounded List/Iterable finder without Pageable"
exit 0
