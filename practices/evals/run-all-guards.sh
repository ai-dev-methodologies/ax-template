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
# specs/multi-tenant-l0.yaml#MULTI-TENANT-ISOLATION-DEFAULT-001 clause (a)
# ("RECIPE.md frontmatter MUST declare tenant_model: single | multi").
# iter-2 closed the 10-recipe coverage gap; this guard locks the
# regression so the next recipe author cannot silently omit declaration.
run_guard "recipe_tenant_model_declaration/live" 0 \
    bash "$SCRIPT_DIR/recipe_tenant_model_declaration_guard.sh"

# ── 11.6. l4_readme_tenant_model_declaration_guard (iter-8 — NC11 symmetry) ─
echo "[11.6] l4_readme_tenant_model_declaration_guard.sh (live repo)"
# Mechanical regression prevention for spec MUST
# specs/multi-tenant-l0.yaml#MULTI-TENANT-ISOLATION-DEFAULT-001 clause (b)
# ("every templates/L4/<domain>/README.md MUST declare its tenant model
# via a `**Tenant model**:` line that cites this spec anchor"). iter-7
# extended the spec MUST to cover both entry surfaces and added the
# declaration line to all 12 L4 READMEs; this guard locks the L4 side
# symmetrically (the RECIPE.md side is already locked above).
run_guard "l4_readme_tenant_model_declaration/live" 0 \
    bash "$SCRIPT_DIR/l4_readme_tenant_model_declaration_guard.sh"

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

# ── 13. payment_provider_type_enum_guard (iter-10 — NEW-NC6 closure) ─────────
echo "[13] payment_provider_type_enum_guard.sh (live repo)"
# 29th hard guard. Enforces spec MUST
# specs/payment-l0.yaml#PAYMENT-PROVIDER-ENUM-001:
# blueprints/payment-manifest.yaml#provider.type MUST be a member of
# #provider.type_allowed (strict string equality, snake_case ASCII).
# Closes P2 Round 10 NEW-NC6 — silent-acceptance of free-string typos
# in the PaymentProvider SPI selection key.
run_guard "payment_provider_type_enum/live" 0 \
    bash "$SCRIPT_DIR/payment_provider_type_enum_guard.sh"

# ── 14. multi_tenant_aop_guard_skeleton (dogfood-5 — P2 R3 GAP-NEW-2 closure) ─
echo "[14] multi_tenant_aop_guard_skeleton_guard.sh (live repo + passing fixture)"
# 30th hard guard. Enforces practices/rules/multi-tenant-aop-guard-skeleton.md
# 11-file canonical adoption at every .../multitenancy/ package. Closes
# P2 Round 3 GAP-NEW-2: manifest aop-guard named AuthorizedTenantInterceptor
# + @AuthorizedTenant + @TenantId but shipped no body, leaving the most
# security-critical 60 lines of multi-tenant adoption to fork-receiver
# invention (risking 403-vs-404 existence leakage and tenant_id detail
# leakage). dogfood-5 ships the bodies + this guard.
run_guard "multi_tenant_aop_guard_skeleton/live" 0 \
    bash "$SCRIPT_DIR/multi_tenant_aop_guard_skeleton_guard.sh"

# ── 15. recipe_sibling_sync_guard (dogfood-7 — gap 5 closure) ────────────────
echo "[15] recipe_sibling_sync_guard.sh (live repo)"
# 31st hard guard. Compares recipes/<pattern>/RECIPE.md frontmatter against
# specs/recipes/<pattern>-recipe-l0.yaml across enabled_l4_domains,
# l2_blocks_used, l3_pages_used. Discovered by P2 R7 dry-run: booking
# RECIPE.md had three L1 primitives (calendar / date-range-picker /
# relative-time) in l2_blocks_used that the spec yaml deliberately
# excluded — cross_trio_guard never compared the two recipe siblings.
run_guard "recipe_sibling_sync/live" 0 \
    bash "$SCRIPT_DIR/recipe_sibling_sync_guard.sh"

# ── 16. manifest_yaml_strict_parse_guard (dogfood-9 — NEW-3 closure, 32nd guard) ─
echo "[16] manifest_yaml_strict_parse_guard.sh (live repo)"
# Strict YAML parse + duplicate-key detection for every blueprints/*-manifest.yaml.
# Discovered by P2 R8 dry-run: ui-tokens-manifest.yaml line 27 had
# `placeholder:{ ... }` (no space between key colon and inline flow mapping)
# — lenient parsers accept, strict reject. Same pattern as the dogfood-6
# manifest typo fix; this guard locks in the property across all 29 manifests.
run_guard "manifest_yaml_strict_parse/live" 0 \
    bash "$SCRIPT_DIR/manifest_yaml_strict_parse_guard.sh"

# ── 17. override_schema_guard (dogfood-9 — gap 6 sentinel, 33rd guard) ───────
echo "[17] override_schema_guard.sh (live repo)"
# Sentinel: validates `override_allowed:` blocks in recipes/<slug>/RECIPE.md
# frontmatter + specs/recipes/<slug>-recipe-l0.yaml against the contract at
# specs/recipes/_override-schema.yaml. Today (dogfood-9 land) every override
# is commented-out — guard fires only when fork-receivers activate one and
# violate the schema (missing rationale, unknown L4 domain, placeholder
# citation, etc.). Closes P2 R3 gap 6.
run_guard "override_schema/live" 0 \
    bash "$SCRIPT_DIR/override_schema_guard.sh"

# ── 18. ledger_audit_nullability_guard (dogfood-11 — R11 GAP-B closure, 34th guard) ─
echo "[18] ledger_audit_nullability_guard.sh (live repo)"
# Locks the JPA entity column nullability (PaymentEvent.paymentId @Column
# nullable=...) and the Flyway migration SQL nullability (CREATE TABLE
# payment_events.payment_id + any ALTER COLUMN DROP/SET NOT NULL statements
# across V*.sql) in lockstep. Closes the dogfood-10 stopgap that routed
# redirect-style PG callback signature_fail audit rows with unresolved
# inboundOrderId to a sentinel UUID(0,0) inside payment_events — which
# polluted the PAYMENT-RECON-001 hash chain. dogfood-11 relaxed the NOT NULL
# (V006 + entity update) so orphan audit rows persist with paymentId=null;
# this guard makes future desync between the two sides a hard fail. Spec
# anchor: specs/payment-l0.yaml#PAYMENT-CALLBACK-001.
run_guard "ledger_audit_nullability/live" 0 \
    bash "$SCRIPT_DIR/ledger_audit_nullability_guard.sh"

# ── 19. l4_domain_enum_sync_guard (dogfood-12 — R12 closure, 35th guard) ─────
echo "[19] l4_domain_enum_sync_guard.sh (live repo)"
# Enforces 3-source coherence of L4 domain enumeration across
# (1) templates/L4/<domain>/ disk dirs,
# (2) specs/recipes/_override-schema.yaml $defs.l4_domain enum,
# (3) specs/recipes/*-recipe-l0.yaml enabled_l4_domains lists,
# against the canonical classification specs/l4-domain-classification.yaml.
# Validates 6 invariants (I1..I6): tier uniqueness + classified-disk-coverage
# + classified-schema-coverage + per-tier disk/schema/recipe presence rules.
# Closes the R12 framing gap (3-source disagreement was undocumented +
# unverified before dogfood-12).
run_guard "l4_domain_enum_sync/live" 0 \
    bash "$SCRIPT_DIR/l4_domain_enum_sync_guard.sh"

echo ""
echo "[20] payment_callback_restassured_compliance_guard.sh (live repo)"
# 35th hard guard — dogfood-13 R13 GAP-C closure.
# Makes specs/payment-l0.yaml#PAYMENT-CALLBACK-001's test_method scalar
# ("RestAssured integration test") mechanically binding: at least one
# integration test in backend/src/test/.../payment/ MUST hit all three
# markers (RestAssured import + @SpringBootTest RANDOM_PORT + literal
# "/api/payments/callback/" POST). MockMvc usage in any *Callback* file
# is rejected as CLAUDE.md anti-pattern.
run_guard "payment_callback_restassured_compliance/live" 0 \
    bash "$SCRIPT_DIR/payment_callback_restassured_compliance_guard.sh"

# ── 21. payment_provider_qualifier_consistency_guard (dogfood-14 — R14 GAP-A closure, 36th guard) ─
echo ""
echo "[21] payment_provider_qualifier_consistency_guard.sh (live repo)"
# 36th hard guard. Locks the SlowProviderLatencyDecorator ↔ MockProvider bean
# resolution contract: decorator constructor MUST take interface PaymentProvider
# + @Qualifier("rawPaymentProvider"), MockProvider MUST register under bean name
# "rawPaymentProvider". Without this guard, a fork-receiver adding a real PG
# adapter (Stripe / Toss / KG Inicis / NICE / KCP) would hit a silent bean
# resolution conflict the moment they tried to keep both beans scoped by
# @Profile — discovered only at runtime when @Primary fell back to the mock.
run_guard "payment_provider_qualifier_consistency/live" 0 \
    bash "$SCRIPT_DIR/payment_provider_qualifier_consistency_guard.sh"

# ── 22. ledger_audit_tenant_nullable_guard (R4 — P2 GAP-R3-3 closure, 37th guard) ─
echo ""
echo "[22] ledger_audit_tenant_nullable_guard.sh (live repo + passing fixture)"
# 37th hard guard. Enforces blueprints/multi-tenant-manifest.yaml
# #ledger-audit-tenant-scope: audit / ledger / append-only event entities
# that may be appended OUTSIDE a tenant-scoped request boundary
# (e.g. PG callback signature_fail at permitAll endpoint) MUST NOT
# implement TenantOwned, MUST declare tenant_id @Column nullable=true,
# and MUST expose getTenantId() returning Optional<UUID>. Closes
# P2 dogfood R3 GAP-R3-3: PaymentEventLedger.append() invoked from
# PaymentCallbackController signature_fail path threw
# TenantContextMissingException → 500 → external PG retried indefinitely
# (NICE / Toss V1 retry up to 24h). The catalog previously had no
# policy for the asymmetry between request-scoped resources and audit
# entities; this guard locks the policy mechanically.
run_guard "ledger_audit_tenant_nullable/live" 0 \
    bash "$SCRIPT_DIR/ledger_audit_tenant_nullable_guard.sh"

if [ "$INCLUDE_FIXTURES" -eq 1 ]; then
    echo ""
    echo "[22f] ledger_audit_tenant_nullable_guard.sh --fixtures"
    run_guard "ledger_audit_tenant_nullable/fixtures" 0 \
        bash "$SCRIPT_DIR/ledger_audit_tenant_nullable_guard.sh" --fixtures
fi

# ── 23. callback_tenant_resolution_guard (R5 — P2 GAP-R3-4 closure, 38th guard) ─
echo ""
echo "[23] callback_tenant_resolution_guard.sh (live repo + passing fixture)"
# 38th hard guard. Enforces blueprints/multi-tenant-manifest.yaml
# #callback-tenant-resolution.verifier_contract: external PG callback
# verifiers (NICE / Toss / KakaoPay) in multi-tenant fork-receivers MUST
# atomically pair signature verification with tenant resolution in one
# call returning UUID, MUST consume raw bytes (not String) for the
# request body, and MUST distinguish no-match from multiple-match via
# two distinct exception types (CallbackSignatureMismatchException vs
# AmbiguousTenantResolutionException). Closes P2 dogfood R3 GAP-R3-4:
# the manifest's #context-resolution forbade orderId / path / query as
# tenant signals but never said what IS allowed for permitAll callback
# endpoints, leaving the most security-critical resolution to invented
# (and forgeable) heuristics. R5 ships the canonical per-tenant secret
# policy + this guard.
run_guard "callback_tenant_resolution/live" 0 \
    bash "$SCRIPT_DIR/callback_tenant_resolution_guard.sh"

# ── 24. scheduled_task_tenant_scope_guard (R6 — P2 GAP-R3-5 closure, 39th guard) ─
echo ""
echo "[24] scheduled_task_tenant_scope_guard.sh (live repo + passing fixture)"
# 39th hard guard. Enforces blueprints/multi-tenant-manifest.yaml
# #scheduled-task-tenant-scope: @Scheduled / Quartz / Shedlock-style
# periodic jobs (reconciliation sweeps, retention purges, daily summaries)
# in multi-tenant fork-receivers MUST adopt the per-tenant iteration
# pattern with three load-bearing properties — balanced TenantContext
# set/clear via try/finally (count equality), tenantCatalog.listActive()
# as the SINGLE source of tenant enumeration (no hardcoded UUID lists),
# and a @SchedulerLock name template containing the #tenantId substring
# (Shedlock SpEL) so two cluster nodes can process two tenants in
# parallel instead of serializing the whole fleet behind one global
# lock. Closes P2 dogfood R3 GAP-R3-5: the manifest covered
# request-scoped (#context-resolution), AOP-scoped (#aop-guard),
# async-submission-scoped (#async-propagation), and callback-scoped
# (#callback-tenant-resolution) — but the scheduler/cron path was
# undefined, leaving fork-receivers writing a nightly
# PaymentReconciliationJob to reach for either bare repository.findAll()
# (silent cross-tenant read) or TenantContext.set(SYSTEM_TENANT_UUID)
# (the GAP-R3-3 sentinel anti-pattern in a new disguise).
run_guard "scheduled_task_tenant_scope/live" 0 \
    bash "$SCRIPT_DIR/scheduled_task_tenant_scope_guard.sh"

if [ "$INCLUDE_FIXTURES" -eq 1 ]; then
    echo ""
    echo "[24f] scheduled_task_tenant_scope_guard.sh --fixtures"
    run_guard "scheduled_task_tenant_scope/fixtures" 0 \
        bash "$SCRIPT_DIR/scheduled_task_tenant_scope_guard.sh" --fixtures
fi

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
