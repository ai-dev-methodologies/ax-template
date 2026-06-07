#!/usr/bin/env bash
# skills/ax-plan/scripts/check-plan-complete.sh — ax-plan (ultragoal G005) Steps 1 & 11.
#
# Binary check that a domain's PLAN is complete and fully traced — NOT that it is
# green. It NEVER re-implements guard logic; it delegates to the two promoted hard
# guards and adds a skeleton/fill check.
#
#   exit 0  PLAN_COMPLETE      — Spec Trio present for the declared domain_mode AND
#                               every applicable item resolves a verification binding
#                               (RED @Tag stubs count as bound). Hand off to dev.
#   exit 1  PLAN_INCOMPLETE    — files present but unfilled, OR an item for THIS domain
#                               is unbound, OR the Trio is incomplete for its mode.
#   exit 3  SKELETON_MISSING   — no spec file for the domain. Run /ax-scaffold first.
#   exit 2  usage error
#
# Usage: bash skills/ax-plan/scripts/check-plan-complete.sh <domain-kebab>
set -uo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../../.." && pwd)"
EVALS="$REPO_ROOT/practices/evals"

DOMAIN="${1:-}"
if [ -z "$DOMAIN" ]; then
    echo "check-plan-complete: usage: check-plan-complete.sh <domain-kebab>" >&2
    exit 2
fi

SPEC="$REPO_ROOT/specs/${DOMAIN}-l0.yaml"
SPEC_FE="$REPO_ROOT/specs/${DOMAIN}-frontend-l0.yaml"
ACTIVE_SPEC=""
[ -f "$SPEC" ] && ACTIVE_SPEC="$SPEC"
[ -z "$ACTIVE_SPEC" ] && [ -f "$SPEC_FE" ] && ACTIVE_SPEC="$SPEC_FE"

# ── 1. Skeleton present? ──────────────────────────────────────────────────────
if [ -z "$ACTIVE_SPEC" ]; then
    echo "SKELETON_MISSING: no specs/${DOMAIN}-l0.yaml or ${DOMAIN}-frontend-l0.yaml."
    echo "  Run /ax-scaffold ${DOMAIN} to emit the empty Spec Trio skeleton first."
    exit 3
fi

# ── 2. Fill check: real items present, no leftover scaffold placeholders ───────
item_count="$(grep -cE '^\s*-\s*id:' "$ACTIVE_SPEC" 2>/dev/null || echo 0)"
if [ "$item_count" -eq 0 ]; then
    echo "PLAN_INCOMPLETE: ${ACTIVE_SPEC#$REPO_ROOT/} has zero items — fill the Spec Trio (ax-plan Step 4)."
    exit 1
fi
# Match ONLY the marker /ax-scaffold (new-domain.sh) actually emits — `# TODO: Add ...`.
# Do NOT match bare TBD/FIXME: those legitimately appear in `introduced_at:` provenance
# prose of complete specs (caching-l0, data-subject-rights-l0, ...) and would false-fail.
if grep -qE '^[[:space:]]*#?[[:space:]]*TODO:[[:space:]]*Add' "$ACTIVE_SPEC" 2>/dev/null; then
    echo "PLAN_INCOMPLETE: ${ACTIVE_SPEC#$REPO_ROOT/} still contains the unfilled scaffold marker ('# TODO: Add ...')."
    exit 1
fi
if ! grep -qE '^\s*domain_mode:\s*\S' "$ACTIVE_SPEC" 2>/dev/null; then
    echo "PLAN_INCOMPLETE: ${ACTIVE_SPEC#$REPO_ROOT/} does not declare domain_mode (domain_spec_trio_guard requires it)."
    exit 1
fi

# ── 3. Trio completeness (delegate to domain_spec_trio_guard) ─────────────────
trio_out="$(bash "$EVALS/domain_spec_trio_guard.sh" 2>&1)"; trio_rc=$?
if [ "$trio_rc" -ne 0 ] && printf '%s' "$trio_out" | grep -qiE "(^|[^a-z])${DOMAIN}([^a-z]|$)"; then
    echo "PLAN_INCOMPLETE: domain_spec_trio_guard flags '${DOMAIN}':"
    printf '%s\n' "$trio_out" | grep -iE "${DOMAIN}" | sed 's/^/    /' | head -8
    exit 1
fi

# ── 4. Item binding (delegate to spec_item_verification_binding_guard) ────────
bind_out="$(bash "$EVALS/spec_item_verification_binding_guard.sh" 2>&1)"; bind_rc=$?
if [ "$bind_rc" -ne 0 ] && printf '%s' "$bind_out" | grep -qiE "(^|[^a-z])${DOMAIN}([^a-z]|$)"; then
    echo "PLAN_INCOMPLETE: spec_item_verification_binding_guard flags unbound item(s) in '${DOMAIN}':"
    printf '%s\n' "$bind_out" | grep -iE "${DOMAIN}" | sed 's/^/    /' | head -8
    echo "    Fix: ensure each item id matches ^[A-Z0-9]+-(?:[A-Z0-9]+-)?[0-9] and has a RED @Tag stub"
    echo "    (run emit-red-stubs.sh ${DOMAIN}) or an explicit verification:{mechanism,ref} block."
    exit 1
fi

# ── PASS ──────────────────────────────────────────────────────────────────────
echo "PLAN_COMPLETE: ${DOMAIN} — Spec Trio filled for its domain_mode, ${item_count} item(s),"
echo "  every applicable item resolves a verification binding (RED @Tag stubs count as bound)."
if [ "$trio_rc" -ne 0 ] || [ "$bind_rc" -ne 0 ]; then
    echo "  (note: a repo-wide guard is RED for OTHER domain(s); '${DOMAIN}' itself is clean.)"
fi
echo "  Green-ness of ./gradlew test* is NOT asserted here — that is the dev-handoff gate."
exit 0
