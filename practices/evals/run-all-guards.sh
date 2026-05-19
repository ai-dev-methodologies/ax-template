#!/usr/bin/env bash
# practices/evals/run-all-guards.sh — SP37 acceptance gate.
#
# Runs all guards (6 core + recipe_governance) against both the live repo
# and, when --include-fixtures is passed, against every fixture directory.
#
# Exit 0 if all expected exits match.
# Exit 1 with summary if any mismatch.
#
# Usage:
#   bash practices/evals/run-all-guards.sh
#   bash practices/evals/run-all-guards.sh --include-fixtures
set -uo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"

INCLUDE_FIXTURES=0
while [ $# -gt 0 ]; do
    case "$1" in
        --include-fixtures) INCLUDE_FIXTURES=1; shift ;;
        *) echo "run-all-guards: unknown arg: $1" >&2; exit 2 ;;
    esac
done

PASS=0
FAIL=0
RESULTS=()

run_guard() {
    local label="$1"
    local expected_exit="$2"
    shift 2
    local cmd=("$@")

    local output
    local actual_exit
    output=$("${cmd[@]}" 2>&1) && actual_exit=0 || actual_exit=$?

    if [ "$actual_exit" -eq "$expected_exit" ]; then
        PASS=$((PASS + 1))
        RESULTS+=("PASS [$label]")
    else
        FAIL=$((FAIL + 1))
        RESULTS+=("FAIL [$label] expected exit $expected_exit, got $actual_exit")
        RESULTS+=("     output: $(echo "$output" | head -3)")
    fi
}

echo "=== run-all-guards.sh — SP3 acceptance gate ==="
echo ""

# ── 1. evidence_guard (practices + practices-react) ─────────────────────────
echo "[1] evidence_guard.sh"
run_guard "evidence_guard/practices" 0 \
    bash "$SCRIPT_DIR/evidence_guard.sh" --catalog practices
run_guard "evidence_guard/practices-react" 0 \
    bash "$SCRIPT_DIR/evidence_guard.sh" --catalog practices-react

# ── 2. spec_ref_guard (practices + practices-react) ──────────────────────────
echo "[2] spec_ref_guard.sh"
run_guard "spec_ref_guard/practices" 0 \
    bash "$SCRIPT_DIR/spec_ref_guard.sh" --catalog practices
run_guard "spec_ref_guard/practices-react" 0 \
    bash "$SCRIPT_DIR/spec_ref_guard.sh" --catalog practices-react

# ── 3. substance_guard (practices) ───────────────────────────────────────────
echo "[3] substance_guard.sh"
run_guard "substance_guard/practices" 0 \
    bash "$SCRIPT_DIR/substance_guard.sh"

# ── 4. time_decay_guard (practices + practices-react) ────────────────────────
echo "[4] time_decay_guard.sh"
run_guard "time_decay_guard/practices" 0 \
    bash "$SCRIPT_DIR/time_decay_guard.sh" --catalog practices
run_guard "time_decay_guard/practices-react" 0 \
    bash "$SCRIPT_DIR/time_decay_guard.sh" --catalog practices-react

# ── 5. trio_integrity_guard (live repo) ──────────────────────────────────────
echo "[5] trio_integrity_guard.sh (live repo)"
# Live repo currently has no domain frontend trios — the allowlist domains exist
# but their fixture files are not yet created (SP2 creates them). The guard
# should fail gracefully against the live repo; we accept any exit code here
# and only enforce fixture-level correctness below.
# For the acceptance gate, we validate fixtures only.

# ── 6. cross_trio_guard (live repo) ──────────────────────────────────────────
echo "[6] cross_trio_guard.sh (live repo)"
# Same as above: templates/L4/ is currently empty (gitkeep only), so cross_trio
# fires ZERO_SCAN on the live repo. This is expected during SP3 (Phase 0).
# Fixture-level verification validates the guard logic.

if [ "$INCLUDE_FIXTURES" -eq 1 ]; then
    echo ""
    echo "=== Fixture verification ==="
    FIXTURES_TRIO="$SCRIPT_DIR/fixtures/trio_integrity"
    FIXTURES_CROSS="$SCRIPT_DIR/fixtures/cross_trio"

    # trio_integrity fixtures
    echo "[trio_integrity] pass/"
    run_guard "trio_integrity/pass" 0 \
        bash "$SCRIPT_DIR/trio_integrity_guard.sh" --root "$FIXTURES_TRIO/pass"

    echo "[trio_integrity] fail_missing_frontend_yaml/"
    run_guard "trio_integrity/fail_missing_frontend_yaml" 1 \
        bash "$SCRIPT_DIR/trio_integrity_guard.sh" --root "$FIXTURES_TRIO/fail_missing_frontend_yaml"

    echo "[trio_integrity] fail_unresolved_operation_id/"
    run_guard "trio_integrity/fail_unresolved_operation_id" 1 \
        bash "$SCRIPT_DIR/trio_integrity_guard.sh" --root "$FIXTURES_TRIO/fail_unresolved_operation_id"

    echo "[trio_integrity] fail_coverage_shortfall/"
    run_guard "trio_integrity/fail_coverage_shortfall" 1 \
        bash "$SCRIPT_DIR/trio_integrity_guard.sh" --root "$FIXTURES_TRIO/fail_coverage_shortfall"

    echo "[trio_integrity] fail_zero_scan/"
    run_guard "trio_integrity/fail_zero_scan" 1 \
        bash "$SCRIPT_DIR/trio_integrity_guard.sh" --root "$FIXTURES_TRIO/fail_zero_scan"

    echo "[trio_integrity] pass_frontend_only_practices/"
    run_guard "trio_integrity/pass_frontend_only_practices" 0 \
        bash "$SCRIPT_DIR/trio_integrity_guard.sh" --root "$FIXTURES_TRIO/pass_frontend_only_practices"

    echo "[trio_integrity] fail_frontend_only_missing_source_ref/"
    run_guard "trio_integrity/fail_frontend_only_missing_source_ref" 1 \
        bash "$SCRIPT_DIR/trio_integrity_guard.sh" --root "$FIXTURES_TRIO/fail_frontend_only_missing_source_ref"

    echo "[trio_integrity] fail_frontend_only_unreachable_route/"
    run_guard "trio_integrity/fail_frontend_only_unreachable_route" 1 \
        bash "$SCRIPT_DIR/trio_integrity_guard.sh" --root "$FIXTURES_TRIO/fail_frontend_only_unreachable_route"

    echo "[trio_integrity] fail_frontend_only_item_non_null_operation/"
    run_guard "trio_integrity/fail_frontend_only_item_non_null_operation" 1 \
        bash "$SCRIPT_DIR/trio_integrity_guard.sh" --root "$FIXTURES_TRIO/fail_frontend_only_item_non_null_operation"

    # cross_trio fixtures
    echo "[cross_trio] pass/"
    run_guard "cross_trio/pass" 0 \
        bash "$SCRIPT_DIR/cross_trio_guard.sh" --root "$FIXTURES_CROSS/pass"

    echo "[cross_trio] fail_orphan_l2_import/"
    run_guard "cross_trio/fail_orphan_l2_import" 1 \
        bash "$SCRIPT_DIR/cross_trio_guard.sh" --root "$FIXTURES_CROSS/fail_orphan_l2_import"

    echo "[cross_trio] fail_zero_scan/"
    run_guard "cross_trio/fail_zero_scan" 1 \
        bash "$SCRIPT_DIR/cross_trio_guard.sh" --root "$FIXTURES_CROSS/fail_zero_scan"

    # ── 7. recipe_governance_guard (SP37) ────────────────────────────────────
    echo ""
    echo "[7] recipe_governance_guard.sh (fixtures)"
    run_guard "recipe_governance/fixtures" 0 \
        bash "$SCRIPT_DIR/recipe_governance_guard.sh" --fixtures

    # ── 9f. cross_recipe_inv_uniqueness_guard fixtures (R12 SP49) ────────────
    echo ""
    echo "[9f] cross_recipe_inv_uniqueness_guard.sh (fixtures)"
    run_guard "cross_recipe_inv_uniqueness/fixtures" 0 \
        bash "$SCRIPT_DIR/cross_recipe_inv_uniqueness_guard.sh" --fixtures

    # ── 10f. applied_recipes_alphabetical_guard fixtures (R12 SP49) ──────────
    echo ""
    echo "[10f] applied_recipes_alphabetical_guard.sh (fixtures)"
    run_guard "applied_recipes_alphabetical/fixtures" 0 \
        bash "$SCRIPT_DIR/applied_recipes_alphabetical_guard.sh" --fixtures

    # ── 11f. agents_md_toc_disk_truth_guard fixtures (R13 SP51 — TD-2026-05-25-033)
    echo ""
    echo "[11f] agents_md_toc_disk_truth_guard.sh (fixtures)"
    run_guard "agents_md_toc_disk_truth/pass_unmodified_toc" 0 \
        bash "$SCRIPT_DIR/agents_md_toc_disk_truth_guard.sh" \
            --root "$SCRIPT_DIR/fixtures/agents_md_toc_disk_truth/pass_unmodified_toc"
    run_guard "agents_md_toc_disk_truth/fail_manual_toc_edit" 1 \
        bash "$SCRIPT_DIR/agents_md_toc_disk_truth_guard.sh" \
            --root "$SCRIPT_DIR/fixtures/agents_md_toc_disk_truth/fail_manual_toc_edit"
fi

# ── 7. recipe_governance_guard (live repo) ───────────────────────────────────
echo "[7] recipe_governance_guard.sh (live repo)"
# Exits 0 when recipes/ does not exist. Validates applied_recipe: annotations
# and recipe invariant resolution once recipes/ lands (SP35+).
run_guard "recipe_governance/live" 0 \
    bash "$SCRIPT_DIR/recipe_governance_guard.sh"

# ── 8. recipe_spec_referential_integrity_guard (SP35) ────────────────────────
echo "[8] recipe_spec_referential_integrity_guard.sh (live repo)"
# Validates enabled_l4_domains, l2_blocks_used, l3_pages_used, and
# business_invariants referential integrity across all specs/recipes/*.yaml.
# Enforces the recipe-invariants-must-resolve rule (SP37).
run_guard "recipe_spec_referential_integrity/live" 0 \
    bash "$SCRIPT_DIR/recipe_spec_referential_integrity_guard.sh"

# ── 9. cross_recipe_inv_uniqueness_guard (R12 SP49 — TD-2026-05-24-030) ──────
echo "[9] cross_recipe_inv_uniqueness_guard.sh (live repo)"
# Protective guard: blocks future cycles from declaring the same
# (L4_domain_prefix, business_invariants[].id) pair across ≥2 recipes.
# Disk census at R12 PRD signature: zero current collisions.
run_guard "cross_recipe_inv_uniqueness/live" 0 \
    bash "$SCRIPT_DIR/cross_recipe_inv_uniqueness_guard.sh"

# ── 10. applied_recipes_alphabetical_guard (R12 SP49 — TD-2026-05-24-031) ────
echo "[10] applied_recipes_alphabetical_guard.sh (live repo)"
# Mechanizes R6-R10 manual alphabetical-insert discipline for the
# applied_recipes: plural list in templates/L4/*/README.md. Skips R5 legacy
# singular form and keyless L4 READMEs.
run_guard "applied_recipes_alphabetical/live" 0 \
    bash "$SCRIPT_DIR/applied_recipes_alphabetical_guard.sh"

# ── 11. agents_md_toc_disk_truth_guard (R13 SP51 — TD-2026-05-25-033) ────────
echo "[11] agents_md_toc_disk_truth_guard.sh (live repo)"
# 25th hard guard. Re-runs practices/generate_agents.sh and diffs against
# the committed practices/AGENTS.md (whole file + defensive TOC slice).
# Surfaces sha-asymmetry: rule edits trigger sentinel sha refresh, but
# L4/recipe/verdict adds or hand-edited TOC bodies leave the sentinel intact
# while drifting the TOC — caught here.
run_guard "agents_md_toc_disk_truth/live" 0 \
    bash "$SCRIPT_DIR/agents_md_toc_disk_truth_guard.sh"

# ── 11.5. recipe_tenant_model_declaration_guard (iter-3 — NC6 closure) ──────
echo "[11.5] recipe_tenant_model_declaration_guard.sh (live repo)"
# Mechanical regression prevention for spec MUST
# specs/multi-tenant-l0.yaml#MULTI-TENANT-ISOLATION-DEFAULT-001
# ("RECIPE.md frontmatter MUST declare tenant_model: single | multi").
# iter-2 closed the 10-recipe coverage gap; this guard locks the
# regression so the next recipe author cannot silently omit declaration.
run_guard "recipe_tenant_model_declaration/live" 0 \
    bash "$SCRIPT_DIR/recipe_tenant_model_declaration_guard.sh"

# ── 12. spec_policy_ref_guard (R17 — TD-2026-05-20-035) ──────────────────────
echo "[12] spec_policy_ref_guard.sh (live repo)"
# 26th hard guard. Validates every `policy_ref: blueprints/<file>.yaml#<anchor>`
# in specs/*.yaml resolves to (1) an existing blueprint file and (2) an
# existing top-level or nested YAML anchor inside it. Closes the P2 R16
# critique: cross_trio_guard caught templates/ imports but missed
# spec-internal policy_ref dangling references — Spec Trio self-violation
# slipped through. R17 also fixed 5 pre-existing dangling refs discovered
# on first guard run (audit-log#immutability, auth#login.rate_limit,
# email-outbox#admin, file-storage#security × 2).
run_guard "spec_policy_ref/live" 0 \
    bash "$SCRIPT_DIR/spec_policy_ref_guard.sh"

# ── Summary ──────────────────────────────────────────────────────────────────
echo ""
echo "=== Results ==="
for r in "${RESULTS[@]}"; do
    echo "  $r"
done
echo ""
echo "Total: $PASS passed, $FAIL failed"

if [ "$FAIL" -gt 0 ]; then
    echo "run-all-guards: FAIL — $FAIL guard(s) did not match expected exit code" >&2
    exit 1
fi

echo "run-all-guards: all guards PASS"
exit 0
