#!/usr/bin/env bash
# practices/consumer-proof/scenarios/S2.AUTHZ.FE/scenario-guards/ssrf_missing_allowlist_check_guard.sh
#
# HAND-ROLLED — capability-gap signal.
# ADDITIONAL REQUIREMENT named for this dogfood cell: a rule/guard requiring
# outbound SSRF URL-allowlist validation before server-side fetch of a
# user-supplied URL. Confirmed absent from the catalog:
# practices/consumer-proof/engine/canary-gaps.yaml CANARY-005 —
# `grep -rliE "ssrf|url.?allowlist|allowlist.?url" practices/rules/*.md
# practices/evals/*.sh` returns 0 matches (verified 2026-07-19). CANARY-005's
# framing is REGISTRATION-time (storing a webhook subscription target URL);
# this fixture/guard targets a DIFFERENT, equally real call site the canary
# does not cover: TEST-DELIVERY time — an on-demand server-side fetch of an
# ALREADY-STORED target URL, which needs its own re-validation because the
# URL can be repointed (DNS rebinding, a since-edited endpoint) between
# registration and this later fetch. Real defect class: SSRF
# (OWASP API Security Top 10 2023, API7:2023 Server Side Request Forgery).
#
# WHAT IT ENFORCES
# For every *Service.java file under --root: find every outbound HTTP fetch
# call of the form `restTemplate.(getForObject|getForEntity|exchange|
# postForObject|postForEntity)(` whose first argument is a local variable
# (not a string literal — a literal URL is not attacker-controlled). That
# call's line number MUST be preceded, in the SAME file, by a call to
# `<something>AllowlistValidator.assertAllowed(` (or `.validate(` on an
# Allowlist-named collaborator) — the catalog-absent seam this scenario hand-
# rolls (see UrlAllowlistValidator.java, clean-root only). No such call
# before the fetch means the fetched URL is unvalidated.
#
# ZERO_SCAN safety: root must contain at least one *Service.java performing a
# restTemplate fetch call, or this is an environment/usage problem (exit 2).
#
# Exit codes:
#   0 — every variable-sourced outbound fetch is preceded by an allowlist check
#   1 — at least one fetch call has no preceding allowlist check
#       (signature: SSRF_MISSING_URL_ALLOWLIST_CHECK)
#   2 — usage / environment error (root missing, zero-scan)
#
# Usage: bash ssrf_missing_allowlist_check_guard.sh --root DIR

set -uo pipefail

ROOT_OVERRIDE=""
while [ $# -gt 0 ]; do
    case "$1" in
        --root) ROOT_OVERRIDE="$2"; shift 2 ;;
        --root=*) ROOT_OVERRIDE="${1#--root=}"; shift ;;
        *) echo "ssrf_missing_allowlist_check_guard: unknown arg: $1" >&2; exit 2 ;;
    esac
done
if [ -z "$ROOT_OVERRIDE" ]; then
    echo "ssrf_missing_allowlist_check_guard: --root DIR is required" >&2
    exit 2
fi
if [ ! -d "$ROOT_OVERRIDE" ]; then
    echo "ssrf_missing_allowlist_check_guard: no such dir $ROOT_OVERRIDE" >&2
    exit 2
fi

files="$(find "$ROOT_OVERRIDE" -name '*Service.java' 2>/dev/null || true)"
services_scanned=0
fetch_calls=0
violations=""

# First arg must be a bare identifier (variable), NOT a string literal —
# a call like restTemplate.getForObject("https://literal.example/x", ...)
# is not attacker-controlled and is intentionally out of scope.
FETCH_RE='restTemplate\.(getForObject|getForEntity|exchange|postForObject|postForEntity)\([[:space:]]*[A-Za-z_][A-Za-z0-9_]*[[:space:]]*[,)]'
ALLOWLIST_RE='AllowlistValidator\.(assertAllowed|validate)\('

while IFS= read -r f; do
    [ -z "$f" ] && continue
    hits="$(grep -nE "$FETCH_RE" "$f" 2>/dev/null || true)"
    [ -z "$hits" ] && continue
    services_scanned=$((services_scanned + 1))

    allowlist_line="$(grep -nE "$ALLOWLIST_RE" "$f" 2>/dev/null | head -1 | cut -d: -f1)"

    while IFS= read -r hitline; do
        [ -z "$hitline" ] && continue
        fetch_calls=$((fetch_calls + 1))
        lineno="${hitline%%:*}"
        checked_before=0
        if [ -n "$allowlist_line" ] && [ "$allowlist_line" -lt "$lineno" ]; then
            checked_before=1
        fi
        if [ "$checked_before" -eq 0 ]; then
            violations="$violations
  $f:$lineno: outbound fetch of variable-sourced URL with no preceding allowlist check — $(printf '%s' "$hitline" | sed 's/^[0-9]*://' | sed 's/^ *//')"
        fi
    done <<EOF
$hits
EOF
done <<EOF
$files
EOF

if [ "$services_scanned" -eq 0 ]; then
    echo "ssrf_missing_allowlist_check_guard: ZERO_SCAN — no *Service.java with a variable-sourced restTemplate fetch found under $ROOT_OVERRIDE" >&2
    exit 2
fi

echo "ssrf_missing_allowlist_check_guard: scanned $services_scanned service(s) with outbound fetch, $fetch_calls variable-sourced fetch call(s)"

if [ -n "$violations" ]; then
    echo "VIOLATION: outbound server-side fetch of a user-supplied URL with no SSRF allowlist check:" >&2
    echo "$violations" | sed 's/^/  /' >&2
    echo "" >&2
    echo "Every server-side fetch of a variable (non-literal) URL MUST be" >&2
    echo "preceded, in the same file, by an AllowlistValidator.assertAllowed(...)" >&2
    echo "/ .validate(...) call (OWASP API7:2023 SSRF)." >&2
    echo "ssrf_missing_allowlist_check_guard: SSRF_MISSING_URL_ALLOWLIST_CHECK — BLOCKED" >&2
    exit 1
fi

echo "ssrf_missing_allowlist_check_guard: every variable-sourced outbound fetch is preceded by an allowlist check"
exit 0
