#!/usr/bin/env bash
# practices/evals/payment_callback_restassured_compliance_guard.sh
#
# 35th hard guard — dogfood-13 R13 GAP-C closure.
#
# Why this guard exists:
#   specs/payment-l0.yaml#PAYMENT-CALLBACK-001 declares
#     test_method: "RestAssured integration test"
#   verification_type: "negative-primary"
#
#   The spec's test_method scalar is currently only narrative — nothing
#   mechanically enforces that the matching integration test is actually
#   a RestAssured black-box test against /api/payments/callback/{provider},
#   as opposed to a MockMvc test (which is forbidden by CLAUDE.md anti-pattern
#   "MockMvc 전용 테스트") or a pure-unit verifier test (which would let the
#   controller dispatch + signature audit ledger interaction go unchecked).
#
#   This guard makes the spec's test_method scalar binding: it scans
#   backend/src/test/java/com/ax/template/authblueprint/payment/ for at
#   least one test that hits the spec's promise — RestAssured import,
#   @SpringBootTest with RANDOM_PORT (black-box HTTP, not slice), and at
#   least one literal POST to /api/payments/callback/{provider}.
#
#   Without this guard, a fork-receiver could mechanically replace
#   PaymentCallbackSignatureFailIT with a MockMvc unit test that asserts
#   401-on-mismatch in isolation, and the catalog would silently lose
#   the audit-row + counter + ledger-nullability cross-cuts the spec
#   requires.
#
# Algorithm:
#   1. Confirm specs/payment-l0.yaml declares PAYMENT-CALLBACK-001 with
#      test_method = "RestAssured integration test". (If the scalar drifts,
#      this guard's premise no longer holds — fail closed with an
#      explanatory message.)
#   2. Scan the payment test package for files that match all three:
#        (a) import io.restassured.RestAssured  (or io.restassured.*)
#        (b) @SpringBootTest with WebEnvironment.RANDOM_PORT
#        (c) a literal "/api/payments/callback/" substring
#      A single file satisfying all three suffices.
#   3. Reject MockMvc usage in the same file (forbidden cross-pattern):
#      if a candidate file imports org.springframework.test.web.servlet.*
#      AND the file's name matches *Callback*, fail with anti-pattern
#      explanation.
#
# Exit: 0 = PASS; 1 = FAIL (binary, like all hard guards).

set -euo pipefail
cd "$(dirname "$0")/../.."

SPEC="specs/payment-l0.yaml"
TEST_DIR="backend/src/test/java/com/ax/template/authblueprint/payment"
SPEC_MUST="specs/payment-l0.yaml#PAYMENT-CALLBACK-001"

# Step 1 — spec premise check.
if [ ! -f "$SPEC" ]; then
    echo "VIOLATION [payment-callback-restassured]: spec missing: $SPEC" >&2
    echo "  35th guard premise relies on $SPEC_MUST declaring" >&2
    echo "  test_method: \"RestAssured integration test\"." >&2
    exit 1
fi

# awk over the spec to find PAYMENT-CALLBACK-001's test_method.
TEST_METHOD=$(awk '
    /^  - id: "PAYMENT-CALLBACK-001"/ { in_block=1; next }
    in_block && /^  - id:/             { in_block=0 }
    in_block && /^    test_method:/    {
        line=$0
        sub(/^    test_method:[[:space:]]*"/, "", line)
        sub(/"[[:space:]]*$/, "", line)
        print line
        exit
    }
' "$SPEC")

if [ "$TEST_METHOD" != "RestAssured integration test" ]; then
    echo "VIOLATION [payment-callback-restassured]: spec drift detected." >&2
    echo "  Expected $SPEC_MUST test_method = \"RestAssured integration test\"" >&2
    echo "  Found:    \"${TEST_METHOD:-<missing>\"}" >&2
    echo "  Either restore the spec scalar or delete this guard if the spec" >&2
    echo "  promise has been deliberately downgraded." >&2
    exit 1
fi

# Step 2 — locate a matching integration test.
if [ ! -d "$TEST_DIR" ]; then
    echo "VIOLATION [payment-callback-restassured]: payment test dir missing: $TEST_DIR" >&2
    echo "  enforced by spec MUST: $SPEC_MUST" >&2
    exit 1
fi

# Find any file that matches all three required markers.
MATCH=""
while IFS= read -r f; do
    grep -q "io\.restassured\." "$f" || continue
    grep -q "WebEnvironment\.RANDOM_PORT" "$f" || continue
    grep -q "/api/payments/callback/" "$f" || continue
    MATCH="$f"
    break
done < <(find "$TEST_DIR" -type f -name "*.java")

if [ -z "$MATCH" ]; then
    echo "VIOLATION [payment-callback-restassured]: no RestAssured integration test found for callback." >&2
    echo "  scanned: $TEST_DIR" >&2
    echo "  required markers (all three in a single file):" >&2
    echo "    - import io.restassured.* (or io.restassured.RestAssured)" >&2
    echo "    - @SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)" >&2
    echo "    - a literal \"/api/payments/callback/\" substring" >&2
    echo "  enforced by spec MUST: $SPEC_MUST" >&2
    echo "  (CLAUDE.md anti-pattern: MockMvc 전용 테스트는 프로젝트 패키지 구조에 결합되어 이식 불가.)" >&2
    exit 1
fi

# Step 3 — reject MockMvc usage in any *Callback* file. This is the
# anti-pattern enforcement half: a future fork-receiver can't paper over
# this guard by adding a MockMvc-based callback test alongside the real one.
while IFS= read -r f; do
    case "$(basename "$f")" in
        *Callback*)
            if grep -q "org\.springframework\.test\.web\.servlet\." "$f"; then
                echo "VIOLATION [payment-callback-restassured]: MockMvc detected in callback test." >&2
                echo "  file: $f" >&2
                echo "  CLAUDE.md anti-pattern: callback tests MUST be RestAssured black-box," >&2
                echo "  not MockMvc — MockMvc binds to the controller bean and bypasses" >&2
                echo "  SecurityConfig (permitAll carve-out for /api/payments/callback/**)" >&2
                echo "  and the actual Servlet request pipeline." >&2
                exit 1
            fi
            ;;
    esac
done < <(find "$TEST_DIR" -type f -name "*.java")

echo "PASS [payment-callback-restassured]: $MATCH"
exit 0
