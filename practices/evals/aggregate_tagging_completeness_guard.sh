#!/usr/bin/env bash
# practices/evals/aggregate_tagging_completeness_guard.sh
# DDD decomposition back-tag wave forcing function.
# Spec: docs/superpowers/specs/2026-06-08-ddd-decomposition-rules-design.md §4 / §10 Phase 1.
#
# The marker-dependent TIER-1 guards (HG-AGG-REPO / HG-AGG-REF / HG-AGG-MEMBER-ENCAP)
# can only be sound once EVERY @Entity carries a root/member classification. Partial
# tagging produces day-1 false positives + silent gaps. This guard is the flip-forcing
# function the spec mandates: it BLOCKS the build unless every JPA @Entity in the
# backend main sources carries EXACTLY ONE of {@AggregateRoot, @AggregateMember}.
#
# Effect: tagging cannot regress, and a NEW domain that adds an @Entity without
# classifying it fails the build until it is tagged (and recorded in the
# NEW-DOMAIN-CHECKLIST flow).
#
# Detection note: matches the @Entity annotation by word boundary so @EntityGraph /
# @EntityListeners are NOT mistaken for an @Entity declaration.
#
# Exit: 0 PASS · 1 an @Entity is untagged or double-tagged · 2 usage/setup error.
#
# Usage:
#   bash practices/evals/aggregate_tagging_completeness_guard.sh
#   bash practices/evals/aggregate_tagging_completeness_guard.sh --src DIR

set -uo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
SRC_DIR="$REPO_ROOT/backend/src/main/java"

while [ $# -gt 0 ]; do
    case "$1" in
        --src) SRC_DIR="$2"; shift 2 ;;
        --src=*) SRC_DIR="${1#--src=}"; shift ;;
        --root) SRC_DIR="$2/backend/src/main/java"; shift 2 ;;
        --root=*) SRC_DIR="${1#--root=}/backend/src/main/java"; shift ;;
        *) echo "aggregate_tagging_completeness_guard: unknown arg: $1" >&2; exit 2 ;;
    esac
done

if [ ! -d "$SRC_DIR" ]; then
    echo "aggregate_tagging_completeness_guard: missing src dir $SRC_DIR" >&2
    exit 2
fi

# precise @Entity declaration: annotation token followed by space, '(' or EOL
ENTITY_RE='^[[:space:]]*@Entity([[:space:]]|\(|$)'

total=0
tagged_root=0
tagged_member=0
violations=0
fail() { echo "  VIOLATION: $1" >&2; violations=$((violations + 1)); }

# bash 3.2 (macOS) has no mapfile — stream the matches through a while-read loop.
while IFS= read -r f; do
    [ -z "$f" ] && continue
    total=$((total + 1))
    has_root=0; has_member=0
    grep -qE '@AggregateRoot\b' "$f" && has_root=1
    grep -qE '@AggregateMember\b' "$f" && has_member=1
    rel="${f#"$SRC_DIR"/}"
    if [ "$has_root" -eq 0 ] && [ "$has_member" -eq 0 ]; then
        fail "$rel: @Entity is NOT tagged with @AggregateRoot or @AggregateMember (back-tag wave incomplete)"
    elif [ "$has_root" -eq 1 ] && [ "$has_member" -eq 1 ]; then
        fail "$rel: @Entity carries BOTH @AggregateRoot and @AggregateMember (must be exactly one)"
    elif [ "$has_root" -eq 1 ]; then
        tagged_root=$((tagged_root + 1))
    else
        tagged_member=$((tagged_member + 1))
    fi
done < <(grep -rlE "$ENTITY_RE" --include='*.java' "$SRC_DIR" 2>/dev/null | sort)

if [ "$total" -eq 0 ]; then
    echo "aggregate_tagging_completeness_guard: no @Entity found under $SRC_DIR (vacuous PASS)"
    exit 0
fi

if [ "$violations" -gt 0 ]; then
    echo "" >&2
    echo "aggregate_tagging_completeness_guard: $violations of $total @Entity classes mis-tagged — BLOCKED" >&2
    echo "Every @Entity must carry exactly one of @AggregateRoot / @AggregateMember(root=...)." >&2
    exit 1
fi

echo "aggregate_tagging_completeness_guard: OK — $total/$total @Entity tagged (${tagged_root} roots, ${tagged_member} members)"
exit 0
