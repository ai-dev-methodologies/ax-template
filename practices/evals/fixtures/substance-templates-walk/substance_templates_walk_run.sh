#!/usr/bin/env bash
# practices/evals/fixtures/substance-templates-walk/substance_templates_walk_run.sh
#
# BACKLOG P3-73 — non-vacuity harness for substance_guard.sh's templates/ walk
# (the ZERO_SCAN subgate at the tail of the guard).
#
# WHY A HARNESS AND NOT A PLAIN FIXTURE DIRECTORY
#   Every other fixture in this tree is a directory handed to the guard through a CLI
#   flag (`--root`, `--repo-root`, an absolute catalog path). substance_guard's templates
#   root is deliberately NOT CLI-parameterizable: it is derived from the guard's own
#   location so the live catalog can never be swapped out from the command line. That is
#   the property P3-73 restored, so it must not be traded away to make testing easier.
#   Instead each scenario builds a THROWAWAY repo skeleton under mktemp, copies the LIVE
#   guard into it (no committed copy ⇒ no drift), and runs it there.
#
# SCENARIOS (each exits with the code run-all-guards.sh expects)
#   fail_zero_scan        — skeleton with an EMPTY templates/ ⇒ guard MUST exit 1
#                           (ZERO_SCAN). Invoked RELATIVELY on purpose: this is the exact
#                           shape .githooks/pre-commit uses, and it is what the P3-73 bug
#                           broke — before the fix this scenario exited 0 (walk skipped,
#                           "all rules pass"), so the fixture is non-vacuous with respect
#                           to the fix itself.
#   pass_templates_present — same skeleton, one scannable file in templates/ ⇒ exit 0.
#                           Control: proves fail_zero_scan fails for the intended reason
#                           (empty templates/) and not because the skeleton is malformed.
#   pass_relative_invocation — runs the guard against the LIVE repo both relatively and
#                           absolutely and asserts BOTH report the same non-zero walk
#                           count. Coverage must not depend on how the caller spelled the
#                           path. Exits 1 if either invocation walks nothing.
#
# WHY NO fixture_kill_manifest.yaml ([87]) ENTRY
#   [87] proves a fail fixture non-vacuous by writing the anchor→neuter mutation to a
#   `mktemp` file and running THAT copy against the fixture. Relocating substance_guard.sh
#   to /tmp changes the very thing under test — its templates root is derived from its own
#   location, so the relocated copy resolves a root with no practices/ or templates/ at all
#   and exits before the walk, no matter what the neuter says. The kill-proof is therefore
#   done here instead, and it is a real one: reverting the P3-73 fix (re-deriving the root
#   from ${BASH_SOURCE[0]} after the `cd`) flips fail_zero_scan 1 → 0 and
#   pass_relative_invocation 0 → 1. Verified by mutation on a throwaway copy, 2026-07-29.
#
# Usage:
#   bash practices/evals/fixtures/substance-templates-walk/substance_templates_walk_run.sh <scenario>
set -uo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../../../.." && pwd)"
GUARD="$REPO_ROOT/practices/evals/substance_guard.sh"

SCENARIO="${1:-}"
if [[ -z "$SCENARIO" ]]; then
    echo "substance_templates_walk: usage: $0 <fail_zero_scan|pass_templates_present|pass_relative_invocation>" >&2
    exit 2
fi
if [[ ! -f "$GUARD" ]]; then
    echo "substance_templates_walk: BLOCK — guard not found at $GUARD" >&2
    exit 2
fi

# Build a throwaway repo skeleton: <tmp>/practices/{rules,evals}/ + <tmp>/templates/.
# rules/ is present but empty so the rule loop passes trivially and the run reaches the
# templates walk — the subgate under test.
make_skeleton() {
    local tmp
    tmp="$(mktemp -d)" || return 1
    mkdir -p "$tmp/practices/rules" "$tmp/practices/evals" "$tmp/templates"
    cp "$GUARD" "$tmp/practices/evals/substance_guard.sh"
    echo "$tmp"
}

case "$SCENARIO" in
    fail_zero_scan)
        tmp="$(make_skeleton)" || exit 2
        # templates/ exists and is EMPTY → ZERO_SCAN must BLOCK.
        out="$(cd "$tmp" && bash practices/evals/substance_guard.sh 2>&1)"
        rc=$?
        echo "$out"
        rm -rf "$tmp"
        if [[ $rc -eq 0 ]]; then
            echo "substance_templates_walk: fail_zero_scan did NOT block (exit 0) — the templates walk was skipped, not run" >&2
        fi
        exit $rc
        ;;
    pass_templates_present)
        tmp="$(make_skeleton)" || exit 2
        printf '# scannable template\n' > "$tmp/templates/example.md"
        out="$(cd "$tmp" && bash practices/evals/substance_guard.sh 2>&1)"
        rc=$?
        echo "$out"
        rm -rf "$tmp"
        if [[ $rc -eq 0 ]] && ! grep -q "templates/ walk found 1 file(s)" <<< "$out"; then
            echo "substance_templates_walk: pass_templates_present exited 0 without walking the 1 template file" >&2
            exit 1
        fi
        exit $rc
        ;;
    pass_relative_invocation)
        rel_out="$(cd "$REPO_ROOT" && bash practices/evals/substance_guard.sh 2>&1)"
        rel_rc=$?
        abs_out="$(cd / && bash "$GUARD" 2>&1)"
        abs_rc=$?
        rel_n="$(sed -n 's/.*templates\/ walk found \([0-9]*\) file(s).*/\1/p' <<< "$rel_out" | head -1)"
        abs_n="$(sed -n 's/.*templates\/ walk found \([0-9]*\) file(s).*/\1/p' <<< "$abs_out" | head -1)"
        echo "substance_templates_walk: relative invocation exit=$rel_rc walked='${rel_n:-<none>}'"
        echo "substance_templates_walk: absolute invocation exit=$abs_rc walked='${abs_n:-<none>}'"
        if [[ $rel_rc -ne 0 || $abs_rc -ne 0 ]]; then
            echo "substance_templates_walk: live guard did not pass under both invocation styles" >&2
            exit 1
        fi
        if [[ -z "$rel_n" || "$rel_n" -eq 0 ]]; then
            echo "substance_templates_walk: RELATIVE invocation reported success while walking ZERO templates (P3-73 regression)" >&2
            exit 1
        fi
        if [[ "$rel_n" != "$abs_n" ]]; then
            echo "substance_templates_walk: coverage depends on invocation style — relative walked '$rel_n', absolute walked '$abs_n' (P3-73 regression)" >&2
            exit 1
        fi
        echo "substance_templates_walk: PASS — both invocation styles walk $rel_n file(s)"
        exit 0
        ;;
    *)
        echo "substance_templates_walk: unknown scenario '$SCENARIO'" >&2
        exit 2
        ;;
esac
