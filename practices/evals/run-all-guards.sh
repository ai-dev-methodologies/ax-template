#!/usr/bin/env bash
# practices/evals/run-all-guards.sh — SP37 acceptance gate.
#
# Runs all guards (6 core + recipe_governance) against both the live repo
# and, when --include-fixtures is passed, against every fixture directory.
#
# Exit 0 if all expected exits match.
# Exit 1 with summary if any mismatch.
#
# NOTE (R25): completion_checklist_recency_guard.sh (49th hard guard) is
# intentionally NOT invoked from this script. It audits the audit log that
# verify-completion.sh writes — including it here would create a self-
# referential cycle (verify-completion → this script → 49th guard → log
# that does not yet exist for the current run). The 49th guard runs from
# `.githooks/pre-push` and can be invoked standalone. See HOOKS-GUIDE.md.
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

# ── 25. realtime_connection_tenant_scope_guard (R7 — P2 GAP-NEW-1 closure, 40th guard) ─
echo ""
echo "[25] realtime_connection_tenant_scope_guard.sh (live repo + passing fixture)"
# 40th hard guard. Enforces blueprints/multi-tenant-manifest.yaml
# #realtime-connection-tenant-scope: SseEmitter / WebSocketSession-based
# long-lived push connections in multi-tenant fork-receivers MUST adopt
# the registry + per-message set/clear pattern with three load-bearing
# clauses — connection-registration reads tenantId from TenantContext
# .current() (not from @RequestParam / @RequestHeader), broadcast
# iterates with a tenantId equality filter (not bare emitters.forEach
# (send)), and per-message TenantContext.set / TenantContext.clear
# wraps each .send() call with count equality (mirrors the R6 39th
# clause-1 algorithm). Closes P2 dogfood R7 GAP-NEW-1: the manifest
# covered request-scoped (#context-resolution), AOP-scoped
# (#aop-guard), async-submission-scoped (#async-propagation),
# callback-scoped (#callback-tenant-resolution), and scheduler-scoped
# (#scheduled-task-tenant-scope) — but the long-lived push connection
# regime (SSE / STOMP @MessageMapping / raw WebSocketHandler) was
# undefined, leaving fork-receivers writing a tenant admin dashboard
# SseEmitter to reach for either bare emitters.forEach(e -> e.send(...))
# inside an @EventListener (silent cross-tenant push leak) or
# @RequestParam("tenant_id") tenantId at register time (attacker
# subscribes to any tenant's stream by passing the URL).
run_guard "realtime_connection_tenant_scope/live" 0 \
    bash "$SCRIPT_DIR/realtime_connection_tenant_scope_guard.sh"

if [ "$INCLUDE_FIXTURES" -eq 1 ]; then
    echo ""
    echo "[25f] realtime_connection_tenant_scope_guard.sh --fixtures"
    run_guard "realtime_connection_tenant_scope/fixtures" 0 \
        bash "$SCRIPT_DIR/realtime_connection_tenant_scope_guard.sh" --fixtures
fi

# ── 26. broker_fanout_tenant_scope_guard (R8 — P2 GAP-NEW-2 closure, 41st guard) ─
echo ""
echo "[26] broker_fanout_tenant_scope_guard.sh (live repo + passing fixture)"
# 41st hard guard. Enforces blueprints/multi-tenant-manifest.yaml
# #broker-fanout-tenant-scope: cross-node broker fan-out bridges
# (Redis Pub/Sub, Kafka) used to scale SSE / WebSocket realtime push
# horizontally beyond a single node MUST adopt the envelope-header +
# per-message set/clear pattern with four load-bearing clauses —
# publish-side wraps payload in TenantBrokerEnvelope (or Kafka
# X-Tenant-Id header) before convertAndSend (not bare
# convertAndSend(channel, payload)), subscribe-side listener body
# reads tenantId from the envelope BEFORE TenantContext.current()
# (broker thread has empty TenantContext by construction),
# per-message TenantContext.set / TenantContext.clear wraps each
# dispatch with count equality (mirrors the R7 40th clause-3
# algorithm), and local sendToTenant dispatch passes
# envelope.tenantId() EXPLICITLY (not TenantContext.current()).
# Closes P2 dogfood R8 GAP-NEW-2: R7 closed single-node long-lived
# connection scope but explicitly named broker fan-out as the
# unresolved >1-node SSE deployment case. fork-receivers writing
# a 2-node SSE deployment behind an LB would reach for either bare
# redisTemplate.convertAndSend(channel, payload) (consumer-side
# listener has no tenant signal — NPE / silent default-tenant
# fallback / stale-tenantId-from-previous-message leak) or
# consumer-side TenantContext.current() (returns Optional.empty on
# broker thread). 41st guard mechanically blocks all four
# anti-patterns. Single-node deployments live-repo SKIP — no
# .../multitenancy/TenantAware*Bridge.java present.
run_guard "broker_fanout_tenant_scope/live" 0 \
    bash "$SCRIPT_DIR/broker_fanout_tenant_scope_guard.sh"

if [ "$INCLUDE_FIXTURES" -eq 1 ]; then
    echo ""
    echo "[26f] broker_fanout_tenant_scope_guard.sh --fixtures"
    run_guard "broker_fanout_tenant_scope/fixtures" 0 \
        bash "$SCRIPT_DIR/broker_fanout_tenant_scope_guard.sh" --fixtures
fi

# ── 27. kafka_consumer_tenant_scope_guard (R9 — kafka-consumer closure, 42nd guard) ─
echo ""
echo "[27] kafka_consumer_tenant_scope_guard.sh (live repo + passing fixture)"
# 42nd hard guard. Enforces blueprints/multi-tenant-manifest.yaml
# #kafka-consumer-tenant-scope: long-running Kafka business-event
# consumers (distinct surface from #broker-fanout-tenant-scope which
# covers fan-out-INTO-SSE bridges) MUST adopt the shared-topic +
# X-Tenant-Id header + per-record set/clear pattern with five
# load-bearing clauses — listener body reads the per-record
# X-Tenant-Id header BEFORE any TenantContext.current() call
# (consumer poll thread has empty TenantContext by construction);
# batch listeners (List<ConsumerRecord> / ConsumerRecords
# signature) MUST set TenantContext INSIDE the for-loop, not
# once at method entry (batch_set_once is the most subtle
# failure mode — passes single-tenant tests, leaks in
# interleaved production batches); count(set) == count(clear)
# in the listener body; ConsumerRebalanceListener callbacks
# (onPartitionsAssigned / onPartitionsRevoked) MUST be tenant-
# free (the poll thread between batches has no tenant signal);
# manual Acknowledgment MUST NOT run inside the per-record
# TenantContext span (canonical: batch ack after the for-loop).
# Closes the kafka-consumer open question carried in R7
# (#realtime-connection-tenant-scope.open_questions_remaining[2])
# and re-affirmed in R8 (#broker-fanout-tenant-scope.open_questions_remaining[1]).
# Kafka-free deployments live-repo SKIP — no
# .../multitenancy/TenantAwareKafkaConsumer.java present.
run_guard "kafka_consumer_tenant_scope/live" 0 \
    bash "$SCRIPT_DIR/kafka_consumer_tenant_scope_guard.sh"

if [ "$INCLUDE_FIXTURES" -eq 1 ]; then
    echo ""
    echo "[27f] kafka_consumer_tenant_scope_guard.sh --fixtures"
    run_guard "kafka_consumer_tenant_scope/fixtures" 0 \
        bash "$SCRIPT_DIR/kafka_consumer_tenant_scope_guard.sh" --fixtures
fi

# ── 28. kafka_streams_tenant_scope_guard (R10 — kafka-streams closure, 43rd guard) ─
echo ""
echo "[28] kafka_streams_tenant_scope_guard.sh (live repo + passing fixture)"
# 43rd hard guard. Enforces blueprints/multi-tenant-manifest.yaml
# #kafka-streams-tenant-scope: Kafka Streams (KStream / KTable)
# topologies (distinct surface from #kafka-consumer-tenant-scope:
# R9 covers stateless @KafkaListener consumers; this anchor covers
# stateful aggregation pipelines with RocksDB-backed state stores,
# wall-clock punctuators, and tenant-namespaced joins) MUST adopt
# the tenant-prefixed-key + punctuator key-decode + tenant-namespaced
# join pattern with five load-bearing clauses — topologies with
# groupBy/groupByKey/aggregate MUST have an upstream selectKey
# lambda that reads the X-Tenant-Id header and constructs a
# composite key with KEY_SEPARATOR (single RocksDB store partitions
# per-tenant by key prefix); punctuator bodies that call forward(...)
# MUST wrap each per-key forward in TenantContext.set/clear
# (StreamThread has empty TenantContext by construction);
# count(set) == count(clear) inside the punctuator lambda body;
# topologies with .join/.leftJoin/.outerJoin MUST also have a
# tenant-prefix selectKey on the upstream stream; Materialized.as
# state store names MUST NOT interpolate tenantId (static topology
# build cannot declare dynamic stores — rebalance/standby breaks).
# Closes the kafka-streams open question that was the first
# entry (R9-era index [0]) of #kafka-consumer-tenant-scope.open_questions_remaining
# in the R9-committed manifest — entry removed on R10 closure.
# Stream-processing-free deployments live-repo SKIP — no
# .../multitenancy/TenantAwareKafkaStreamsTopology.java present.
run_guard "kafka_streams_tenant_scope/live" 0 \
    bash "$SCRIPT_DIR/kafka_streams_tenant_scope_guard.sh"

if [ "$INCLUDE_FIXTURES" -eq 1 ]; then
    echo ""
    echo "[28f] kafka_streams_tenant_scope_guard.sh --fixtures"
    run_guard "kafka_streams_tenant_scope/fixtures" 0 \
        bash "$SCRIPT_DIR/kafka_streams_tenant_scope_guard.sh" --fixtures
fi

# ── 29. kafka_streams_interactive_queries_tenant_scope_guard
#      (R11 — IQ read-side OBVERSE of R10 closure, 44th guard) ────────────────
echo ""
echo "[29] kafka_streams_interactive_queries_tenant_scope_guard.sh (live repo + passing fixture)"
# 44th hard guard. Enforces blueprints/multi-tenant-manifest.yaml
# #kafka-streams-interactive-queries-tenant-scope: Kafka Streams
# Interactive Queries (HTTP-exposed state-store reads — distinct
# surface from #kafka-streams-tenant-scope: R10 is the WRITE side
# (selectKey tenant-prefix + punctuator set/clear); this anchor
# covers the READ side that mirrors the write-side prefix at
# request time) MUST adopt the (TenantContext.current() prefix +
# store.range scoped scan + path mismatch → 404 + fresh store
# reference per query) pattern with four load-bearing clauses —
# IQ files MUST call TenantContext.current() (no path/query/body
# tenantId as prefix); MUST NOT call store.all() (unscoped scan
# fragile under refactor); MUST NOT throw AccessDeniedException
# or map to HTTP 403 (existence leak — canonical is 404 via
# TenantBoundaryViolationException + MultiTenantProblemDetailAdvice);
# MUST NOT declare a ReadOnly*Store field (caching across requests
# breaks under Streams rebalance — partition reassignment makes
# the cached reference read from the OLD assignment).
# Closes the IQ open question that was the first entry (R10-era
# index [0]) of #kafka-streams-tenant-scope.open_questions_remaining
# in the R10-committed manifest — entry removed on R11 closure.
# IQ-free deployments live-repo SKIP — no
# .../multitenancy/TenantAwareInteractiveQueryService.java present.
run_guard "kafka_streams_interactive_queries_tenant_scope/live" 0 \
    bash "$SCRIPT_DIR/kafka_streams_interactive_queries_tenant_scope_guard.sh"

if [ "$INCLUDE_FIXTURES" -eq 1 ]; then
    echo ""
    echo "[29f] kafka_streams_interactive_queries_tenant_scope_guard.sh --fixtures"
    run_guard "kafka_streams_interactive_queries_tenant_scope/fixtures" 0 \
        bash "$SCRIPT_DIR/kafka_streams_interactive_queries_tenant_scope_guard.sh" --fixtures
fi

# ── 30. kafka_streams_standby_rpc_tenant_scope_guard
#      (R12 — cluster fan-out OBVERSE of R11 closure, 45th guard) ──────────────
echo ""
echo "[30] kafka_streams_standby_rpc_tenant_scope_guard.sh (live repo + passing fixture)"
# 45th hard guard. Enforces blueprints/multi-tenant-manifest.yaml
# #kafka-streams-standby-rpc-tenant-scope: Kafka Streams Interactive
# Queries cluster fan-out (the cross-node RPC layer that activates
# when the local node does NOT host the partition for a tenant's
# prefix; distinct surface from
# #kafka-streams-interactive-queries-tenant-scope: R11 is the
# SINGLE-NODE store-range read, this anchor covers the MULTI-NODE
# router that decides local-vs-remote and HTTP-forwards remote
# calls) MUST adopt the (TenantContext.current() at the router +
# X-Tenant-Id header on every forward + no tenantId in forward URL
# + fresh metadata lookup per query) pattern with four load-bearing
# clauses — standby forwarder files MUST set X-Tenant-Id on every
# forward (without it the receiving node's TenantContext is empty
# and R11 clause(1) trips on the remote IQ service); MUST call
# TenantContext.current() (no path/query/body tenantId as the
# metadata-lookup key); MUST NOT embed tenantId as a URL path
# segment (header is the sole tenant carrier across the wire);
# MUST NOT cache KeyQueryMetadata / Collection<StreamsMetadata>
# fields or HostInfo fields with initialisers (Streams rebalance
# invalidates prior metadata — constructor-injected self-host
# HostInfo identity fields without `=` initialisers are permitted).
# Closes the standby replica RPC open question that was the first
# entry (R11-era index [0]) of
# #kafka-streams-interactive-queries-tenant-scope.open_questions_remaining
# in the R11-committed manifest — entry removed on R12 closure.
# Single-node clusters and IQ-free deployments live-repo SKIP —
# no .../multitenancy/TenantAwareStandbyForwardingService.java present.
run_guard "kafka_streams_standby_rpc_tenant_scope/live" 0 \
    bash "$SCRIPT_DIR/kafka_streams_standby_rpc_tenant_scope_guard.sh"

if [ "$INCLUDE_FIXTURES" -eq 1 ]; then
    echo ""
    echo "[30f] kafka_streams_standby_rpc_tenant_scope_guard.sh --fixtures"
    run_guard "kafka_streams_standby_rpc_tenant_scope/fixtures" 0 \
        bash "$SCRIPT_DIR/kafka_streams_standby_rpc_tenant_scope_guard.sh" --fixtures
fi

# ── 31. webclient_async_tenant_scope_guard
#      (R13 — reactive async-router OBVERSE of R12 closure, 46th guard) ─────────
echo ""
echo "[31] webclient_async_tenant_scope_guard.sh (live repo + passing fixture)"
# 46th hard guard. Enforces blueprints/multi-tenant-manifest.yaml
# #webclient-async-tenant-scope: outbound Spring WebFlux WebClient
# adoption (the reactive HTTP client surface that fork-receivers
# reach for when calling third-party SaaS APIs — Slack, Stripe,
# SendGrid, NICE페이먼츠 채널 조회, etc.) MUST adopt the
# (X-Tenant-Id header set in the filter + Mono.deferContextual as
# the SOLE tenant extraction point + Reactor Context written at the
# controller from TenantContext.current() + zero ThreadLocal access
# inside the reactive chain) pattern with four load-bearing
# clauses — every ExchangeFilterFunction file MUST stamp the
# X-Tenant-Id header (without it the receiving service sees no
# tenant signal); MUST reference Mono.deferContextual / ContextView
# (Reactor Context is the only chain-safe tenant carrier across
# scheduler hops); MUST NOT call TenantContext.current() outside a
# `public static` servlet-thread helper (the filter body runs on a
# Reactor scheduler thread where the ThreadLocal is empty or holds
# a stale prior subscription's tenant); MUST NOT call
# TenantContext.set / TenantContext.clear at all (these have zero
# legitimate use inside a WebClient filter and are the canonical
# scheduler-worker reuse leak vector). Closes the reactive /
# WebClient async fan-out open question that was the third entry
# (R12-era index [2]) of
# #kafka-streams-standby-rpc-tenant-scope.open_questions_remaining
# in the R12-committed manifest — entry removed on R13 closure.
# WebClient-free deployments live-repo SKIP —
# no .../multitenancy/TenantAwareWebClientFilter.java present.
run_guard "webclient_async_tenant_scope/live" 0 \
    bash "$SCRIPT_DIR/webclient_async_tenant_scope_guard.sh"

if [ "$INCLUDE_FIXTURES" -eq 1 ]; then
    echo ""
    echo "[31f] webclient_async_tenant_scope_guard.sh --fixtures"
    run_guard "webclient_async_tenant_scope/fixtures" 0 \
        bash "$SCRIPT_DIR/webclient_async_tenant_scope_guard.sh" --fixtures
fi

# ── 32. recipe_invariant_spec_normalization_guard
#       (R24 — enforcement-loop-closure, 47th guard) ─────────────────────────
echo ""
echo "[32] recipe_invariant_spec_normalization_guard.sh (live repo + passing fixture)"
# R24 35th-new hard guard (catalog total: 47).
# Closes the catalog enforcement-loop gap surfaced in R24 root-cause-fix mode:
# every business invariant ID prefixed with a recipe's own UPPERCASE prefix
# (e.g. ECOM-INV-001 in recipes/e-commerce/RECIPE.md, B2B-ADMIN-INV-003 in
# recipes/b2b-admin/RECIPE.md) MUST appear in the matching recipe spec
# (specs/recipes/<slug>-recipe-l0.yaml#business_invariants[].id). RECIPE.md is
# narrative; the spec yaml is the surface that downstream guards (substance,
# cross_recipe_inv_uniqueness, recipe_spec_referential_integrity,
# spec_policy_ref) actually read. Before this guard a maintainer could add an
# INV-XYZ paragraph to RECIPE.md without normalizing it to the spec and every
# mechanical check would silently skip it — catalog vision "규칙 밖 AI output
# BLOCKED" leaked at the recipe-narrative-to-spec boundary.
run_guard "recipe_invariant_spec_normalization/live" 0 \
    bash "$SCRIPT_DIR/recipe_invariant_spec_normalization_guard.sh"

if [ "$INCLUDE_FIXTURES" -eq 1 ]; then
    echo ""
    echo "[32f] recipe_invariant_spec_normalization_guard.sh --fixtures"
    run_guard "recipe_invariant_spec_normalization/fixtures" 0 \
        bash "$SCRIPT_DIR/recipe_invariant_spec_normalization_guard.sh" --fixtures
fi

# ── 33. test_tag_naming_convention_guard
#       (R24 — enforcement-loop-closure, 48th guard) ─────────────────────────
echo ""
echo "[33] test_tag_naming_convention_guard.sh (live repo + passing fixture)"
# R24 36th-new hard guard (catalog total: 48).
# Closes the catalog enforcement-loop gap surfaced in R24 root-cause-fix mode:
# every backend JUnit @Tag("...") value MUST follow the catalog UPPERCASE
# convention — pattern ^[A-Z][A-Z0-9_.-]*$ . The per-domain test tasks in
# backend/build.gradle.kts (testAsvs/testPayment/testCrud/testSearch/...)
# pivot on these tag values via includeTags("UPPERCASE"); tag drift silently
# excludes tests from `./gradlew test{Domain}` runs and breaks the catalog
# promise of "single command binary pass/fail". R24 surfaced 8 lowercase
# @Tag("search") drifts in backend/src/test/.../search/*.java that this
# guard catches mechanically + retroactively (and at PR time going forward).
run_guard "test_tag_naming_convention/live" 0 \
    bash "$SCRIPT_DIR/test_tag_naming_convention_guard.sh"

if [ "$INCLUDE_FIXTURES" -eq 1 ]; then
    echo ""
    echo "[33f] test_tag_naming_convention_guard.sh --fixtures"
    run_guard "test_tag_naming_convention/fixtures" 0 \
        bash "$SCRIPT_DIR/test_tag_naming_convention_guard.sh" --fixtures
fi

# ── R37 ralplan retrofit guards ───────────────────────────────────────────────
# Added at R37 per integrated review (architect + critic + codex consensus).
# Closes: methodology theater, dogfood ledger drift, multi-tenant traceability,
# L4 reachability, composition completeness.

echo ""
echo "[34] dogfood_ledger_guard.sh (live repo + passing fixture)"
run_guard "dogfood_ledger/live" 0 \
    bash "$SCRIPT_DIR/dogfood_ledger_guard.sh"

if [ "$INCLUDE_FIXTURES" -eq 1 ]; then
    echo ""
    echo "[34f] dogfood_ledger_guard.sh --fixtures"
    run_guard "dogfood_ledger/fixtures" 0 \
        bash "$SCRIPT_DIR/dogfood_ledger_guard.sh" --fixtures
fi

echo ""
echo "[35] multi_tenant_deferral_citation_guard.sh (live repo + passing fixture)"
run_guard "multi_tenant_deferral_citation/live" 0 \
    bash "$SCRIPT_DIR/multi_tenant_deferral_citation_guard.sh"

if [ "$INCLUDE_FIXTURES" -eq 1 ]; then
    echo ""
    echo "[35f] multi_tenant_deferral_citation_guard.sh --fixtures"
    run_guard "multi_tenant_deferral_citation/fixtures" 0 \
        bash "$SCRIPT_DIR/multi_tenant_deferral_citation_guard.sh" --fixtures
fi

echo ""
echo "[36] l4_domain_reachability_guard.sh (live repo)"
run_guard "l4_domain_reachability/live" 0 \
    bash "$SCRIPT_DIR/l4_domain_reachability_guard.sh"

echo ""
echo "[37] composition_completeness_guard.sh (live repo, WARN-level v1)"
run_guard "composition_completeness/live" 0 \
    bash "$SCRIPT_DIR/composition_completeness_guard.sh"

echo ""
echo "[38] l4_frontend_domain_mode_guard.sh (live repo — R59, 41st guard)"
run_guard "l4_frontend_domain_mode/live" 0 \
    bash "$SCRIPT_DIR/l4_frontend_domain_mode_guard.sh"

echo ""
echo "[39] l4_role_default_failclosed_guard.sh (live repo — R83, 42nd guard)"
run_guard "l4_role_default_failclosed/live" 0 \
    bash "$SCRIPT_DIR/l4_role_default_failclosed_guard.sh"

echo ""
echo "[40] stored_error_column_sanitize_guard.sh (live repo — R81, 43rd guard)"
run_guard "stored_error_column_sanitize/live" 0 \
    bash "$SCRIPT_DIR/stored_error_column_sanitize_guard.sh"

echo ""
echo "[41] background_poll_refresh_state_guard.sh (live repo — R82b, 44th guard)"
run_guard "background_poll_refresh_state/live" 0 \
    bash "$SCRIPT_DIR/background_poll_refresh_state_guard.sh"

echo ""
echo "[42] dogfood_finding_expiry_trigger_guard.sh (live repo — R85b, 45th guard)"
run_guard "dogfood_finding_expiry_trigger/live" 0 \
    bash "$SCRIPT_DIR/dogfood_finding_expiry_trigger_guard.sh"

echo ""
echo "[43] dogfood_finding_real_bug_closure_commit_guard.sh (live repo — R86b, 46th guard)"
run_guard "dogfood_finding_real_bug_closure_commit/live" 0 \
    bash "$SCRIPT_DIR/dogfood_finding_real_bug_closure_commit_guard.sh"

echo ""
echo "[44] dogfood_finding_real_bug_test_coverage_guard.sh (live repo — R87b, 47th guard)"
run_guard "dogfood_finding_real_bug_test_coverage/live" 0 \
    bash "$SCRIPT_DIR/dogfood_finding_real_bug_test_coverage_guard.sh"

echo ""
echo "[45] wave_kickoff_ledger_guard.sh (live repo — R97, 48th guard)"
run_guard "wave_kickoff_ledger/live" 0 \
    bash "$SCRIPT_DIR/wave_kickoff_ledger_guard.sh"

echo ""
echo "[46] registry_backfill_completeness_guard.sh (live repo — R98, 49th guard)"
run_guard "registry_backfill_completeness/live" 0 \
    bash "$SCRIPT_DIR/registry_backfill_completeness_guard.sh"

echo ""
echo "[47] entity_migration_guard.sh (IMW1-C — IDW1 entity↔migration coverage; ddl-auto hides drift)"
run_guard "entity_migration/live" 0 \
    bash "$SCRIPT_DIR/entity_migration_guard.sh"

if [ "$INCLUDE_FIXTURES" -eq 1 ]; then
    # IMW3 / IDW3 G4 regression: the INLINE @Entity @Table pair on ONE line.
    # The old anchor ^\s*@Entity\s*(\(|$) missed this form (own-line=EXIT1,
    # same-line=EXIT0), so an un-migrated entity shipped GREEN. The widened
    # (?m)^\s*@Entity\b now DETECTS it. pass/ proves inline @Table resolution +
    # a backing CREATE TABLE → 0; fail_inline_entity/ proves the false negative
    # is now caught (inline @Entity, no migration, empty allowlist → 1).
    echo ""
    echo "[47f] entity_migration_guard.sh --fixtures (inline @Entity @Table detection)"
    run_guard "entity_migration/fixture_pass" 0 \
        bash "$SCRIPT_DIR/entity_migration_guard.sh" \
            --root "$SCRIPT_DIR/fixtures/entity_migration/pass"
    run_guard "entity_migration/fixture_fail_inline_entity" 1 \
        bash "$SCRIPT_DIR/entity_migration_guard.sh" \
            --root "$SCRIPT_DIR/fixtures/entity_migration/fail_inline_entity"

    # name_collision_guard inline-stereotype detection (IMW3-followup / IDW3 G4 audit):
    # the guard's stereotype anchor had the SAME own-line false-negative as entity_migration —
    # an inline `@Service @Transactional` escaped detection. The \b token-boundary fix catches it.
    echo ""
    echo "[50f] name_collision_guard.sh --fixtures (inline @Service collision detection)"
    run_guard "name_collision/fixture_pass" 0 \
        bash "$SCRIPT_DIR/name_collision_guard.sh" \
            --root "$SCRIPT_DIR/fixtures/name_collision/pass"
    run_guard "name_collision/fixture_fail_inline" 1 \
        bash "$SCRIPT_DIR/name_collision_guard.sh" \
            --root "$SCRIPT_DIR/fixtures/name_collision/fail_inline"
fi

echo ""
echo "[48] spec_ref_code_guard.sh (IMW1-C — IDW1 backend specs/*.yaml reference resolution)"
run_guard "spec_ref_code/live" 0 \
    bash "$SCRIPT_DIR/spec_ref_code_guard.sh"

echo ""
echo "[49] controller_problemdetail_guard.sh (IMW1-D — IDW2: every domain @ExceptionHandler returns RFC9457 ProblemDetail)"
run_guard "controller_problemdetail/live" 0 \
    bash "$SCRIPT_DIR/controller_problemdetail_guard.sh"

echo ""
echo "[50] name_collision_guard.sh (IMW2-C — IDW2: cross-package @Entity/@Service/@Repository/@Controller simple-name collision)"
run_guard "name_collision/live" 0 \
    bash "$SCRIPT_DIR/name_collision_guard.sh"

echo ""
echo "[51] role_literal_guard.sh (IMW2-C — IDW2: every @PreAuthorize authority literal maps to a UserRole or API scope)"
run_guard "role_literal/live" 0 \
    bash "$SCRIPT_DIR/role_literal_guard.sh"

echo ""
echo "[52] audit_on_read_guard.sh (IMW4 — IDW4: a @Phi-returning read method must reference AuditLogService.record)"
# Forward-enforcing: the live main tree has NO @Phi usage yet, so the PHI type
# set is empty and the guard exits 0. It fires only once a fork-receiver tags
# real PHI with common/Phi.java. Closes the IDW4 hole where an adversarial probe
# shipped an un-audited PHI read with a fully GREEN build (HIPAA §164.312(b)).
run_guard "audit_on_read/live" 0 \
    bash "$SCRIPT_DIR/audit_on_read_guard.sh"

if [ "$INCLUDE_FIXTURES" -eq 1 ]; then
    echo ""
    echo "[52f] audit_on_read_guard.sh --fixtures (pass→0, fail→1)"
    run_guard "audit_on_read/fixtures" 0 \
        bash "$SCRIPT_DIR/audit_on_read_guard.sh" --fixtures
fi

echo ""
echo "[53] phi_in_logs_guard.sh (IMW4 — IDW4: no log.{info,debug,warn,error,trace}(...) may interpolate a @Phi getter)"
# Forward-enforcing companion to [52]: the live main tree has NO @Phi usage yet,
# so the forbidden-getter set is empty and the guard exits 0. It fires only once
# a fork-receiver tags real PHI. Closes the IDW4 hole where an adversarial probe
# shipped a raw-PHI log statement with a fully GREEN build (HIPAA §164.312(b)).
run_guard "phi_in_logs/live" 0 \
    bash "$SCRIPT_DIR/phi_in_logs_guard.sh"

if [ "$INCLUDE_FIXTURES" -eq 1 ]; then
    echo ""
    echo "[53f] phi_in_logs_guard.sh --fixtures (pass→0, fail→1)"
    run_guard "phi_in_logs/fixtures" 0 \
        bash "$SCRIPT_DIR/phi_in_logs_guard.sh" --fixtures
fi

echo ""
echo "[54] consent_gate_guard.sh (IMW5 — IDW4: a data-sharing method in a ConsentRecord-adopting tree must reference ConsentGate)"
# Forward-enforcing: the live main tree ships common/ConsentRecord as the consent
# ledger @Entity, but has NO domain method matching a data-sharing signal, so the
# candidate set is empty → vacuous pass. The guard fires only once a fork-receiver
# BOTH adopts the consent ledger AND writes a sharing path (third-party share /
# marketing send / export) — exactly when consent-management-l0#CONSENT-PURPOSE-001
# attaches. Closes the IDW4 hole where a SPEC-ONLY consent subsystem let an
# adversarial probe ship an un-gated third-party share with a fully GREEN build.
run_guard "consent_gate/live" 0 \
    bash "$SCRIPT_DIR/consent_gate_guard.sh"

if [ "$INCLUDE_FIXTURES" -eq 1 ]; then
    echo ""
    echo "[54f] consent_gate_guard.sh --fixtures (pass→0, fail→1, no_entity→0)"
    run_guard "consent_gate/fixtures" 0 \
        bash "$SCRIPT_DIR/consent_gate_guard.sh" --fixtures
fi

echo ""
echo "[55] controller_repository_shell_guard.sh (IMW5 — IDW4: shell-level Controller→Repository ban; closes the run-all-guards vs ArchUnit coverage asymmetry)"
# Green-on-current: IMW1-A routed every controller through a service, so no
# *Controller injects/calls a *Repository. Mirrors ArchitectureLayerBoundaryTest
# at the shell level so the boundary is caught in run-all-guards, not only under a
# full gradle test run.
run_guard "controller_repository_shell/live" 0 \
    bash "$SCRIPT_DIR/controller_repository_shell_guard.sh"

if [ "$INCLUDE_FIXTURES" -eq 1 ]; then
    echo ""
    echo "[55f] controller_repository_shell_guard.sh --fixtures (pass→0, fail_repo_injection→1)"
    run_guard "controller_repository_shell/fixtures" 0 \
        bash "$SCRIPT_DIR/controller_repository_shell_guard.sh" --fixtures
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
