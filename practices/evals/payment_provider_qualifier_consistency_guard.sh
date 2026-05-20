#!/usr/bin/env bash
# payment_provider_qualifier_consistency_guard.sh — dogfood-14 hard guard (36th).
#
# Enforces the R14 GAP-A bean-naming contract for the PaymentProvider SPI:
#
#   (1) SlowProviderLatencyDecorator constructor MUST resolve its delegate via
#       the PaymentProvider INTERFACE + @Qualifier("rawPaymentProvider") — not
#       via the concrete MockProvider class.
#   (2) MockProvider MUST be registered under the bean name "rawPaymentProvider"
#       (i.e. @Component("rawPaymentProvider")).
#   (3) The bean-name constant RAW_PROVIDER_BEAN_NAME inside
#       SlowProviderLatencyDecorator MUST equal the literal string
#       "rawPaymentProvider".
#
# Why this guard exists (R14 GAP-A):
#   Before R14 the decorator constructor took a concrete `MockProvider delegate`
#   parameter. A fork-receiver adding a real PG provider (Stripe / Toss / KG
#   Inicis / NICE / KCP) and trying to keep both beans (mock + real, scoped by
#   @Profile) would hit a bean-resolution conflict the moment the decorator
#   wired its concrete-type dependency — silent runtime breakage discovered
#   only when the @Primary decorator returned to MockProvider in production.
#
#   Fixing the constructor to interface + qualifier is necessary but not
#   sufficient: nothing prevents a future edit from reverting either side.
#   This guard mechanically locks both sides + the constant.
#
# Usage: bash practices/evals/payment_provider_qualifier_consistency_guard.sh
# Exit:  0 = PASS; 1 = FAIL.

set -euo pipefail
cd "$(dirname "$0")/../.."

DECORATOR="backend/src/main/java/com/ax/template/authblueprint/payment/SlowProviderLatencyDecorator.java"
MOCK="backend/src/main/java/com/ax/template/authblueprint/payment/MockProvider.java"
SPEC_MUST="specs/payment-l0.yaml#PAYMENT-PROVIDER-007"
EXPECTED_BEAN="rawPaymentProvider"

fail() {
    echo "VIOLATION [provider-qualifier]: $1" >&2
    echo "  enforced by spec MUST: $SPEC_MUST" >&2
    echo "  remedy: see practices/evals/payment_provider_qualifier_consistency_guard.sh header" >&2
    exit 1
}

[ -f "$DECORATOR" ] || fail "decorator file missing: $DECORATOR"
[ -f "$MOCK" ]      || fail "mock provider file missing: $MOCK"

# ── Check (1): decorator constructor signature ───────────────────────────────
# Must contain "@Qualifier(...)" + "PaymentProvider delegate" (interface type)
# on the same constructor parameter, and MUST NOT take a `MockProvider delegate`.
if grep -qE 'public SlowProviderLatencyDecorator\(\s*MockProvider\s+delegate' "$DECORATOR"; then
    fail "decorator still takes concrete MockProvider — must use interface PaymentProvider + @Qualifier(\"$EXPECTED_BEAN\")"
fi

if ! grep -qE '@Qualifier\(\s*(RAW_PROVIDER_BEAN_NAME|"rawPaymentProvider")\s*\)\s*PaymentProvider\s+delegate' "$DECORATOR"; then
    fail "decorator constructor missing @Qualifier(\"$EXPECTED_BEAN\") PaymentProvider delegate parameter"
fi

# ── Check (2): MockProvider bean name annotation ─────────────────────────────
# Must be @Component("rawPaymentProvider"). Bare @Component is rejected — it
# defaults to "mockProvider" and silently breaks the decorator @Qualifier lookup.
if ! grep -qE '@Component\(\s*"rawPaymentProvider"\s*\)' "$MOCK"; then
    fail "MockProvider must declare @Component(\"$EXPECTED_BEAN\") so the decorator @Qualifier resolves"
fi

# Defensive: bare @Component is rejected unless followed by a non-empty value.
if grep -qE '^@Component\s*$' "$MOCK"; then
    fail "MockProvider has bare @Component — required: @Component(\"$EXPECTED_BEAN\")"
fi

# ── Check (3): bean-name constant matches expectation ────────────────────────
# The decorator declares `static final String RAW_PROVIDER_BEAN_NAME = "..."`.
# Tie the string literal to the contract so a constant rename doesn't desync.
ACTUAL_CONST=$(awk -F'"' '
    /static[[:space:]]+final[[:space:]]+String[[:space:]]+RAW_PROVIDER_BEAN_NAME[[:space:]]*=/ {
        print $2
        exit
    }
' "$DECORATOR")

if [ -z "$ACTUAL_CONST" ]; then
    fail "decorator missing constant declaration: static final String RAW_PROVIDER_BEAN_NAME = \"$EXPECTED_BEAN\""
fi

if [ "$ACTUAL_CONST" != "$EXPECTED_BEAN" ]; then
    fail "RAW_PROVIDER_BEAN_NAME = \"$ACTUAL_CONST\" but expected \"$EXPECTED_BEAN\""
fi

echo "payment_provider_qualifier_consistency_guard: PASS — decorator wires interface PaymentProvider + @Qualifier(\"$EXPECTED_BEAN\"); MockProvider registered as \"$EXPECTED_BEAN\""
exit 0
