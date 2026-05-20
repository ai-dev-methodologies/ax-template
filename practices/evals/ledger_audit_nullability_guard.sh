#!/usr/bin/env bash
# ledger_audit_nullability_guard.sh — dogfood-11 R11 GAP-B mechanical guard (32nd).
#
# Enforces lockstep nullability between the JPA entity declaration and the
# Flyway migration SQL for the payment_events.payment_id column.
#
# Why this guard exists (R11 dogfooding closure of GAP-B):
#   The dogfood-10 stopgap routed redirect-style PG callback signature_fail
#   audit rows whose inboundOrderId did NOT resolve to any Payment row to a
#   sentinel UUID(0,0), because payment_events.payment_id had a NOT NULL
#   constraint. The sentinel polluted the PAYMENT-RECON-001 hash chain by
#   forming a fake "sentinel chain" of unrelated signature_fail entries.
#
#   The R11 closure (dogfood-11) relaxed the NOT NULL constraint via Flyway
#   V006 and switched PaymentEvent.paymentId to nullable=true. If a future
#   fork-receiver edits one side without the other (e.g. only the JPA entity,
#   only the SQL, or reverts one of them without reverting the other) the
#   reference workload would silently desync between Hibernate ddl-auto
#   (H2) and the production Postgres schema (Flyway). This guard makes
#   that desync a hard fail.
#
# Algorithm:
#   1. Parse PaymentEvent.java to determine whether the payment_id column
#      JPA mapping is currently nullable (presence of `nullable = true` in
#      the @Column annotation for paymentId).
#   2. Scan ALL Flyway migrations under backend/src/main/resources/db/migration
#      for statements that affect the nullability of payment_events.payment_id
#      (CREATE TABLE ... payment_id ... NOT NULL, ALTER TABLE ... DROP NOT NULL,
#      ALTER TABLE ... SET NOT NULL). Latest matching statement wins.
#   3. Fail if the two views disagree.
#
# Usage: bash practices/evals/ledger_audit_nullability_guard.sh
# Exit:  0 = PASS; 1 = FAIL.

set -euo pipefail
cd "$(dirname "$0")/../.."

ENTITY="backend/src/main/java/com/ax/template/authblueprint/payment/PaymentEvent.java"
MIGRATION_DIR="backend/src/main/resources/db/migration"
SPEC_MUST="specs/payment-l0.yaml#PAYMENT-CALLBACK-001"

if [ ! -f "$ENTITY" ]; then
    echo "VIOLATION [ledger-audit-nullability]: PaymentEvent entity missing: $ENTITY" >&2
    echo "  enforced by spec MUST: $SPEC_MUST" >&2
    exit 1
fi

if [ ! -d "$MIGRATION_DIR" ]; then
    echo "VIOLATION [ledger-audit-nullability]: migration dir missing: $MIGRATION_DIR" >&2
    echo "  enforced by spec MUST: $SPEC_MUST" >&2
    exit 1
fi

# Step 1: Determine JPA nullability for paymentId.
# Look for the @Column annotation that names payment_id, then check for
# `nullable = true` vs `nullable = false`. Default is nullable=true if the
# attribute is omitted entirely, but the entity here always declares it.
JPA_NULLABILITY=$(awk '
    BEGIN { found=0 }
    /@Column\(name = "payment_id"/ {
        line = $0
        # Concatenate continuation lines until we see the closing paren.
        while (line !~ /\)/) {
            if (getline next_line <= 0) break
            line = line " " next_line
        }
        if (line ~ /nullable[[:space:]]*=[[:space:]]*true/)  { print "nullable"; found=1; exit }
        if (line ~ /nullable[[:space:]]*=[[:space:]]*false/) { print "not_null"; found=1; exit }
        # Attribute omitted → JPA default is nullable.
        print "nullable"; found=1; exit
    }
    END {
        if (!found) print "missing"
    }
' "$ENTITY")

if [ "$JPA_NULLABILITY" = "missing" ]; then
    echo "VIOLATION [ledger-audit-nullability]: paymentId @Column annotation not found in $ENTITY" >&2
    echo "  enforced by spec MUST: $SPEC_MUST" >&2
    exit 1
fi

# Step 2: Scan migrations for the latest nullability-affecting statement on
# payment_events.payment_id. We process files in lexicographic order
# (V001, V002, ...) so the last match wins.
SQL_NULLABILITY="missing"
for sql in $(ls "$MIGRATION_DIR"/V*.sql 2>/dev/null | sort); do
    # CREATE TABLE block — capture payment_id line if it's inside the
    # payment_events table declaration. We use a state machine in awk to
    # avoid matching payment_id columns from refunds, etc.
    CREATE_LINE=$(awk '
        BEGIN { in_payment_events=0 }
        /CREATE TABLE[[:space:]]+(IF NOT EXISTS[[:space:]]+)?payment_events[[:space:]]*\(/ {
            in_payment_events=1; next
        }
        in_payment_events && /^[[:space:]]*\);/ { in_payment_events=0 }
        in_payment_events && /^[[:space:]]*payment_id[[:space:]]/ { print; exit }
    ' "$sql")

    if [ -n "$CREATE_LINE" ]; then
        if echo "$CREATE_LINE" | grep -qiE 'NOT[[:space:]]+NULL'; then
            SQL_NULLABILITY="not_null"
        else
            SQL_NULLABILITY="nullable"
        fi
    fi

    # ALTER TABLE statements — these override the CREATE TABLE result if
    # they appear in a later migration.
    if grep -qiE 'ALTER[[:space:]]+TABLE[[:space:]]+payment_events[[:space:]]+ALTER[[:space:]]+COLUMN[[:space:]]+payment_id[[:space:]]+DROP[[:space:]]+NOT[[:space:]]+NULL' "$sql"; then
        SQL_NULLABILITY="nullable"
    fi
    if grep -qiE 'ALTER[[:space:]]+TABLE[[:space:]]+payment_events[[:space:]]+ALTER[[:space:]]+COLUMN[[:space:]]+payment_id[[:space:]]+SET[[:space:]]+NOT[[:space:]]+NULL' "$sql"; then
        SQL_NULLABILITY="not_null"
    fi
done

if [ "$SQL_NULLABILITY" = "missing" ]; then
    echo "VIOLATION [ledger-audit-nullability]: no migration declares payment_events.payment_id" >&2
    echo "  expected in one of $MIGRATION_DIR/V*.sql" >&2
    echo "  enforced by spec MUST: $SPEC_MUST" >&2
    exit 1
fi

# Step 3: Compare.
if [ "$JPA_NULLABILITY" != "$SQL_NULLABILITY" ]; then
    echo "VIOLATION [ledger-audit-nullability]: JPA and SQL disagree on payment_events.payment_id nullability" >&2
    echo "  entity ($ENTITY): $JPA_NULLABILITY" >&2
    echo "  flyway ($MIGRATION_DIR/V*.sql): $SQL_NULLABILITY" >&2
    echo "  enforced by spec MUST: $SPEC_MUST" >&2
    echo "" >&2
    echo "  Either:" >&2
    echo "    (a) align the JPA annotation (PaymentEvent.java paymentId @Column nullable=...)," >&2
    echo "    (b) add a new V<N> migration with the matching ALTER COLUMN statement, or" >&2
    echo "    (c) revert both sides together (do NOT desync)." >&2
    exit 1
fi

echo "ledger_audit_nullability_guard: PASS — payment_events.payment_id nullability = $JPA_NULLABILITY (entity + flyway agree)"
exit 0
