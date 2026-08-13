#!/usr/bin/env bash
# practices/evals/react_eslint_rule_doc_coverage_guard.sh — BACKLOG P2-87.
#
# THE INVARIANT: every practices-react/eslint-plugin-ax/rules/*.js ESLint rule MUST have a
# corresponding catalog doc under practices-react/rules/*.md, so the ax-practices knowledge
# layer can actually surface it to an AI agent. A rule that ships in the plugin but never
# gets a catalog doc is invisible to anyone reading rules/ or INDEX.md — exactly the gap
# P2-87 found for no-god-route / no-route-client-data-fetching / no-server-state-in-local-state
# (implemented + tested since 2026-06-08, undocumented until this arc).
#
# A rule counts as covered by EITHER of two matches (both are real precedent on disk):
#   (1) filename match       — practices-react/rules/<rule-id>.md exists.
#   (2) verification.rule_id — SOME doc's frontmatter declares `rule_id: "ax/<rule-id>"`
#                               under its `verification:` block (e.g. bundle-barrel-imports.md
#                               covers ax/no-broad-barrel-imports; rerender-no-inline-components.md
#                               covers ax/no-inline-component-definition — the doc's filename
#                               is a human-readable slug, not the ESLint rule id).
#
# Deliberately NOT a full YAML parse: `verification.rule_id` is disambiguated from the
# UNRELATED top-level `rule_id:` field some non-ESLint rule docs carry for their own naming
# (e.g. `rule_id: audit-log-frontend-viewer-rbac-virtualized`, no quotes, no "ax/" prefix) by
# requiring the quoted "ax/..." value shape, which only the real verification.rule_id field
# uses on disk. This keeps the guard free of a PyYAML dependency (see
# pyyaml_preflight_coverage_guard.sh [95] — a new PyYAML-dependent guard must be reachable
# from a checklist step or that guard's own coverage sweep fails; a grep-based check has no
# such obligation).
#
# Exit: 0 PASS (every rule covered) · 1 an eslint-plugin-ax rule has no catalog doc · 2 usage/
# setup error.
#
# Usage:
#   bash practices/evals/react_eslint_rule_doc_coverage_guard.sh
#   bash practices/evals/react_eslint_rule_doc_coverage_guard.sh --root DIR   # fixture tree
#     (DIR must contain practices-react/eslint-plugin-ax/rules/*.js and practices-react/rules/*.md)
#
# NOT YET registered in practices/evals/run-all-guards.sh — see P2-87 task report for the
# registration this guard still needs (owned by the orchestrating session, not this one).

set -uo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"

ROOT="$REPO_ROOT"
while [ $# -gt 0 ]; do
    case "$1" in
        --root) ROOT="$2"; shift 2 ;;
        --root=*) ROOT="${1#--root=}"; shift ;;
        *) echo "react_eslint_rule_doc_coverage_guard: unknown arg: $1" >&2; exit 2 ;;
    esac
done

ESLINT_RULES_DIR="$ROOT/practices-react/eslint-plugin-ax/rules"
DOCS_DIR="$ROOT/practices-react/rules"

if [ ! -d "$ESLINT_RULES_DIR" ]; then
    echo "react_eslint_rule_doc_coverage_guard: $ESLINT_RULES_DIR not found — nothing to check"
    exit 0
fi
if [ ! -d "$DOCS_DIR" ]; then
    echo "react_eslint_rule_doc_coverage_guard: $DOCS_DIR not found" >&2
    exit 2
fi

# All verification.rule_id values of shape `"ax/<id>"` declared anywhere under DOCS_DIR/*.md.
# The quoted "ax/" prefix is the disambiguator (see header comment) — a plain grep for
# `rule_id:` alone would also match the unrelated top-level field some docs carry.
declared_rule_ids="$(grep -rhoE 'rule_id:[[:space:]]*"ax/[a-zA-Z0-9_-]+"' "$DOCS_DIR" 2>/dev/null \
    | sed -E 's/^rule_id:[[:space:]]*"ax\/([a-zA-Z0-9_-]+)"$/\1/' | sort -u)"

violations=0
checked=0
shopt -s nullglob
for f in "$ESLINT_RULES_DIR"/*.js; do
    id="$(basename "$f" .js)"
    checked=$((checked + 1))

    # Method (1): filename match.
    if [ -f "$DOCS_DIR/$id.md" ]; then
        continue
    fi
    # Method (2): some doc's frontmatter verification.rule_id == "ax/<id>".
    if printf '%s\n' "$declared_rule_ids" | grep -qxF "$id"; then
        continue
    fi

    echo "VIOLATION: practices-react/eslint-plugin-ax/rules/$id.js has no catalog doc — expected practices-react/rules/$id.md, or some rule's frontmatter verification.rule_id: \"ax/$id\"" >&2
    violations=$((violations + 1))
done
shopt -u nullglob

if [ "$checked" -eq 0 ]; then
    echo "react_eslint_rule_doc_coverage_guard: no *.js rules found under $ESLINT_RULES_DIR — nothing to check"
    exit 0
fi

if [ "$violations" -gt 0 ]; then
    echo "" >&2
    echo "react_eslint_rule_doc_coverage_guard: $violations of $checked eslint-plugin-ax rule(s) with no catalog doc — BLOCKED" >&2
    exit 1
fi

echo "react_eslint_rule_doc_coverage_guard: PASS — all $checked eslint-plugin-ax/rules/*.js have a catalog doc (filename or verification.rule_id match)"
exit 0
