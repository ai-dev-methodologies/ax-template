#!/usr/bin/env bash
# practices/evals/money_boundary_seam_guard.sh
# #39 money-l0 reconcile (2026-05-31) — mechanical enforcement of the canonical
# long-minor ↔ BigDecimal-major conversion seam (common/Money).
#
# The catalog deliberately uses two monetary representations at different layers:
# integer minor-units (long) at the storage/domain layer (billing/ecommerce —
# currency-amount-precision-explicit, ArchUnit-enforced) and BigDecimal MAJOR-units
# at the payment/PG edge (lang-bigdecimal-for-money + payment-iso-4217-currency).
# The ONLY correct minor→major conversion places the decimal point at the currency's
# minor-unit scale (common/Money.toMajorUnits → BigDecimal.valueOf(minor, fractionDigits)).
#
# The seam BUG this guard blocks: a raw single-arg `BigDecimal.valueOf(<minor-getter>)`
# leaves the value in minor units while payment interprets a BigDecimal as MAJOR units —
# a silent 100x over-charge for every 2-decimal currency (USD/EUR/…); zero-decimal
# currencies (KRW/JPY) only coincidentally agree, so tests using them hide the bug.
# ecommerce OrderService shipped exactly this (BigDecimal.valueOf(order.getTotalAmount()))
# until #39 routed it through common/Money.toMajorUnits.
#
# Forbidden:  BigDecimal.valueOf( <expr>.get{Amount|Price|Fee|Cost|TotalAmount|Subtotal|Total}() )
# Allowed:    common/Money.toMajorUnits(minor, currency)  [the two-arg BigDecimal.valueOf(minor, scale)
#             form inside Money is NOT matched — its argument is a parameter, not a money getter].
#
# Exit codes: 0 — no raw money-getter valueOf at the boundary · 1 — violation · 2 — usage.
#
# Usage:
#   bash practices/evals/money_boundary_seam_guard.sh
#   bash practices/evals/money_boundary_seam_guard.sh --root DIR

set -uo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"

ROOT_OVERRIDE=""
while [ $# -gt 0 ]; do
    case "$1" in
        --root) ROOT_OVERRIDE="$2"; shift 2 ;;
        --root=*) ROOT_OVERRIDE="${1#--root=}"; shift ;;
        *) echo "money_boundary_seam_guard: unknown arg: $1" >&2; exit 2 ;;
    esac
done
[ -n "$ROOT_OVERRIDE" ] && REPO_ROOT="$ROOT_OVERRIDE"

SRC="$REPO_ROOT/backend/src/main/java/com/ax/template/authblueprint"
if [ ! -d "$SRC" ]; then
    # Backend-less checkout (e.g. catalog-only bundle) — nothing to scan.
    echo "money_boundary_seam_guard: no backend source tree — SKIP"
    exit 0
fi

# Single-arg BigDecimal.valueOf wrapping a long-minor money getter. The money-getter
# anchor (.getAmount()/.getPrice()/.getFee()/.getCost()/.getTotalAmount()/.getSubtotal()/
# .getTotal()) is what distinguishes the dangerous minor→major case from a legitimate
# BigDecimal.valueOf(someCount). The canonical Money.toMajorUnits uses the two-arg
# valueOf(unscaled, scale) form whose argument is a parameter, so it is not matched.
PATTERN='BigDecimal\.valueOf\([^,()]*\.get(Amount|Price|Fee|Cost|TotalAmount|Subtotal|Total)\(\)\s*\)'

hits="$(grep -rnE "$PATTERN" "$SRC" --include='*.java' 2>/dev/null || true)"

if [ -n "$hits" ]; then
    echo "VIOLATION: raw BigDecimal.valueOf(<minor-units getter>) at the money boundary —" >&2
    echo "  this leaves the value in MINOR units while payment reads a BigDecimal as MAJOR units" >&2
    echo "  (a 100x over-charge for 2-decimal currencies). Use common/Money.toMajorUnits(minor, currency):" >&2
    echo "$hits" | sed 's/^/  /' >&2
    echo "money_boundary_seam_guard: money-seam violation — BLOCKED" >&2
    exit 1
fi

echo "money_boundary_seam_guard: no raw money-getter BigDecimal.valueOf at the long-minor → BigDecimal-major boundary"
exit 0
