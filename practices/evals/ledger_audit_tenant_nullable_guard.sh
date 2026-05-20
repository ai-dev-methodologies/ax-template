#!/usr/bin/env bash
# ledger_audit_tenant_nullable_guard.sh — dogfood R4 GAP-R3-3 closure (37th hard guard).
#
# Mechanically enforces blueprints/multi-tenant-manifest.yaml anchor
# `#ledger-audit-tenant-scope`. Audit / ledger / append-only event entities
# that may be inserted OUTSIDE a tenant-scoped request boundary
# (e.g. PG callback signature_fail at a permitAll endpoint) MUST:
#
#   (a) NOT implement TenantOwned   — TenantOwned mandates non-null
#                                     getTenantId(); audit rows are
#                                     legitimately tenant-less.
#   (b) declare tenant_id @Column nullable=true
#                                   — orphan audit rows persist with null
#                                     rather than triggering sentinel
#                                     UUIDs or insert failures.
#   (c) expose getTenantId() returning Optional<UUID>
#                                   — bare UUID return type silently
#                                     returns null at call sites; the
#                                     Optional contract forces the
#                                     empty case to be acknowledged.
#
# Algorithm:
#   1. For each candidate audit fixture (default: the multi-tenant-aop
#      passing fixture's AuditEvent.java), parse the file and verify:
#        - the class declaration does NOT list TenantOwned as an
#          implemented interface,
#        - any field annotated @Column(name = "tenant_id" ...) declares
#          nullable = true,
#        - the getTenantId() method's return type is Optional<UUID>.
#   2. With --fixtures, ALSO assert the failing-side variants (sentinel
#      UUID(0,0), bare UUID return, TenantOwned implementation) trip the
#      guard. Today the failing/ directory does not yet ship the audit
#      counterpart; this branch is reserved for the symmetric extension
#      and skips with a notice when the file is absent.
#
# Usage:
#   bash practices/evals/ledger_audit_tenant_nullable_guard.sh
#   bash practices/evals/ledger_audit_tenant_nullable_guard.sh --fixtures
#
# Exit codes:
#   0 — every audit entity satisfies the three policy clauses
#   1 — at least one clause violated OR (with --fixtures) the failing/
#       fixture unexpectedly passes

set -uo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
MANIFEST_ANCHOR="blueprints/multi-tenant-manifest.yaml#ledger-audit-tenant-scope"

PASS_AUDIT="$SCRIPT_DIR/fixtures/multi-tenant-aop-guard-skeleton/passing/com/acme/multitenancy/AuditEvent.java"
FAIL_AUDIT="$SCRIPT_DIR/fixtures/multi-tenant-aop-guard-skeleton/failing/com/acme/multitenancy/AuditEvent.java"

# verify_audit_entity FILE LABEL
# Returns 0 if all three policy clauses hold, 1 otherwise.
verify_audit_entity() {
    local file="$1"
    local label="$2"
    local violations=0

    if [ ! -f "$file" ]; then
        echo "VIOLATION [$label]: audit entity file missing: $file" >&2
        echo "  enforced by manifest anchor: $MANIFEST_ANCHOR" >&2
        return 1
    fi

    # Clause (a): does NOT implement TenantOwned.
    # Match the class declaration line (line beginning with `public class`,
    # `class`, etc., and an `implements` clause). The TenantOwned token may
    # appear in javadoc comments — restrict the match to non-comment lines
    # that contain `implements`.
    if awk '
        BEGIN { in_block_comment = 0 }
        /^[[:space:]]*\/\*/ { in_block_comment = 1 }
        in_block_comment {
            if (/\*\//) in_block_comment = 0
            next
        }
        /^[[:space:]]*\/\// { next }   # line comment
        /implements/ {
            # Strip everything up to and including the keyword `implements`
            # to avoid false positives from class names containing the
            # substring (e.g. WithImplementsAdvice).
            line = $0
            sub(/.*implements[[:space:]]+/, "", line)
            # Trim trailing `{`.
            sub(/\{.*/, "", line)
            n = split(line, parts, /[[:space:]]*,[[:space:]]*/)
            for (i = 1; i <= n; i++) {
                # Strip trailing whitespace.
                gsub(/^[[:space:]]+|[[:space:]]+$/, "", parts[i])
                if (parts[i] == "TenantOwned" || parts[i] ~ /\.TenantOwned$/) {
                    exit 1
                }
            }
        }
    ' "$file"; then
        : # PASS clause (a)
    else
        echo "VIOLATION [$label] clause(a): audit entity implements TenantOwned in $file" >&2
        echo "  TenantOwned mandates non-null getTenantId() — incompatible with orphan audit rows." >&2
        echo "  enforced by manifest anchor: $MANIFEST_ANCHOR.policy.entity_must_implement_tenant_owned (false)" >&2
        violations=$((violations + 1))
    fi

    # Clause (b): @Column(name = "tenant_id" ...) declares nullable = true.
    # The annotation may span multiple lines; concatenate continuation lines
    # until the closing `)` is seen, then test for `nullable = true`.
    local col_nullability
    col_nullability=$(awk '
        BEGIN { found = 0 }
        /@Column\(name[[:space:]]*=[[:space:]]*"tenant_id"/ {
            line = $0
            while (line !~ /\)/) {
                if (getline next_line <= 0) break
                line = line " " next_line
            }
            if (line ~ /nullable[[:space:]]*=[[:space:]]*true/) {
                print "nullable"; found = 1; exit
            }
            if (line ~ /nullable[[:space:]]*=[[:space:]]*false/) {
                print "not_null"; found = 1; exit
            }
            # @Column with no nullable attribute defaults to nullable=true.
            print "nullable"; found = 1; exit
        }
        END {
            if (!found) print "missing"
        }
    ' "$file")

    case "$col_nullability" in
        nullable)
            : # PASS clause (b)
            ;;
        not_null)
            echo "VIOLATION [$label] clause(b): tenant_id @Column declares nullable = false in $file" >&2
            echo "  orphan audit rows (signature_fail callback at permitAll endpoint) require nullable tenant_id." >&2
            echo "  enforced by manifest anchor: $MANIFEST_ANCHOR.policy.tenant_id_column_nullability (nullable)" >&2
            violations=$((violations + 1))
            ;;
        missing)
            echo "VIOLATION [$label] clause(b): no @Column(name = \"tenant_id\" ...) annotation found in $file" >&2
            echo "  audit entity in a multi-tenant recipe MUST declare a tenant_id column." >&2
            echo "  enforced by manifest anchor: $MANIFEST_ANCHOR.policy" >&2
            violations=$((violations + 1))
            ;;
    esac

    # Clause (c): getTenantId() returns Optional<UUID>.
    # Match a method whose name is exactly getTenantId. Allow any modifier
    # prefix; capture the return type token immediately preceding the
    # method name.
    if grep -qE 'Optional<UUID>[[:space:]]+getTenantId\(\)' "$file"; then
        : # PASS clause (c)
    elif grep -qE '\bgetTenantId\(\)' "$file"; then
        # Method exists but does not have Optional<UUID> return.
        echo "VIOLATION [$label] clause(c): getTenantId() does not return Optional<UUID> in $file" >&2
        echo "  Bare UUID return type lets call sites silently observe null." >&2
        echo "  enforced by manifest anchor: $MANIFEST_ANCHOR.policy.tenant_id_field_visibility" >&2
        violations=$((violations + 1))
    else
        echo "VIOLATION [$label] clause(c): no getTenantId() accessor found in $file" >&2
        echo "  audit entity MUST expose tenant attribution as Optional<UUID> getTenantId()." >&2
        echo "  enforced by manifest anchor: $MANIFEST_ANCHOR.policy.tenant_id_field_visibility" >&2
        violations=$((violations + 1))
    fi

    if [ "$violations" -gt 0 ]; then
        echo "ledger_audit_tenant_nullable_guard: FAIL [$label] — $violations clause(s) violated" >&2
        return 1
    fi

    echo "ledger_audit_tenant_nullable_guard: PASS [$label] — all 3 clauses hold (no TenantOwned + nullable tenant_id + Optional<UUID> getter)"
    return 0
}

MODE="default"
while [ $# -gt 0 ]; do
    case "$1" in
        --fixtures) MODE="fixtures"; shift ;;
        *) echo "ledger_audit_tenant_nullable_guard: unknown arg: $1" >&2; exit 2 ;;
    esac
done

OVERALL=0

# ── Default scan ────────────────────────────────────────────────────────────
# Passing fixture MUST satisfy all three clauses.
verify_audit_entity "$PASS_AUDIT" "passing-fixture" || OVERALL=1

# Live-repo scan: every backend `.../multitenancy/` package that ships an
# AuditEvent-style file MUST satisfy the policy. Single-tenant repos have
# no such files and SKIP.
LIVE_FOUND=0
while IFS= read -r dir; do
    if [ -f "$dir/AuditEvent.java" ]; then
        LIVE_FOUND=$((LIVE_FOUND + 1))
        verify_audit_entity "$dir/AuditEvent.java" "live:$dir" || OVERALL=1
    fi
done < <(find "$REPO_ROOT/backend" -type d -name multitenancy 2>/dev/null)
if [ "$LIVE_FOUND" -eq 0 ]; then
    echo "ledger_audit_tenant_nullable_guard: live-repo SKIP — no .../multitenancy/AuditEvent.java (single-tenant default)"
fi

# ── --fixtures mode ─────────────────────────────────────────────────────────
if [ "$MODE" = "fixtures" ]; then
    if [ -f "$FAIL_AUDIT" ]; then
        if verify_audit_entity "$FAIL_AUDIT" "failing-fixture" 2>/dev/null; then
            echo "ledger_audit_tenant_nullable_guard: FAIL — failing/ fixture unexpectedly passes" >&2
            OVERALL=1
        else
            echo "ledger_audit_tenant_nullable_guard: PASS [failing-fixture-detected] — failing/ correctly trips guard"
        fi
    else
        echo "ledger_audit_tenant_nullable_guard: failing-fixture SKIP — $FAIL_AUDIT absent (symmetric extension reserved for next round)"
    fi
fi

exit "$OVERALL"
