#!/usr/bin/env bash
# practices/evals/private_boundary_guard.sh — R26 public/private 경계 기계강제 guard [86]
#
# PURPOSE
#   ax-template은 public fork-base catalog이다. fork-receiver(회사·팀)의 특화·민감 정보가
#   이 public 트리에 유입되면 안 된다. 이 guard는 두 층으로 그 경계를 기계적으로 강제한다.
#
# LAYER 1 — opt-in marker (fork-receiver 활성화)
#   .ax-private-markers 파일이 repo root에 존재하고 활성 패턴(#·빈 줄 제외)이 있으면,
#   각 줄을 ERE 패턴으로 스캔 대상 트리에서 검색한다. 매칭 → violation.
#   ax-template public base는 이 파일을 주석만으로 유지 → 층1 매칭 0.
#   fork-receiver가 자기 식별자(회사명·브랜드·코드네임)를 등록하면 즉시 활성화됨.
#
# LAYER 2 — generic 시크릿 휴리스틱 (항상 실행)
#   알려진 고위험 시크릿 패턴을 스캔한다:
#     a) PEM private key header: -----BEGIN [A-Z ]*PRIVATE KEY-----
#     b) AWS access key:         AKIA[0-9A-Z]{16}
#     c) API key assignment:     api[_-]?key\s*[:=]\s*['""][A-Za-z0-9_\-]{20,}
#     d) JWT token:              eyJ[A-Za-z0-9_-]{10,}\.[A-Za-z0-9_-]{10,}\.
#
# FALSE POSITIVE ALLOWLIST (같은 라인 또는 파일 경로에 있으면 무시):
#   라인 내 키워드: EXAMPLE, example, placeholder, your-, xxxx, REDACTED
#   경로 패턴: src/test/ — 테스트 코드는 보안 테스트용 crafted token을 합법적으로 포함함.
#              Layer 1(marker)은 test 경로도 포함(회사 식별자는 테스트에도 금지).
#              Layer 2(secret)만 test 경로 제외(crafted JWT·더미 키 오탐 방지).
#   → 테스트 데이터·문서 예시가 오탐으로 차단되는 것을 방지한다.
#
# SCAN TARGETS
#   backend/src, frontend/src, specs, contracts, blueprints, practices/rules, docs
#   (guard 자신과 practices/evals는 self-match 방지를 위해 제외)
#
# LIMITS / DESIGN RATIONALE
#   이 guard는 트리에 존재하는 파일을 정적으로 스캔한다. 깃 히스토리·커밋 메시지·
#   바이너리·인코딩된 시크릿은 스캔하지 않는다. ax-template HEAD 자체는 markers가
#   비어 있어 층1 매칭이 0이고, 트리에 실제 시크릿이 없어 층2도 매칭 0이어야 한다.
#   fork-receiver의 실제 회사 차단은 fork가 .ax-private-markers에 식별자를 등록함으로써
#   opt-in 활성화된다 — vision의 fork 자율성 원칙을 보존한다.
#
# USAGE
#   bash practices/evals/private_boundary_guard.sh
#   bash practices/evals/private_boundary_guard.sh --repo-root DIR   # fixture 테스트용
#
# EXIT CODES
#   0 = clean (층1·층2 위반 없음)
#   1 = violation 발견 (BLOCK)
#   2 = usage 오류

set -uo pipefail

REPO_ROOT_OVERRIDE=""
while [ $# -gt 0 ]; do
    case "$1" in
        --repo-root)
            REPO_ROOT_OVERRIDE="$2"; shift 2 ;;
        --repo-root=*)
            REPO_ROOT_OVERRIDE="${1#--repo-root=}"; shift ;;
        *)
            echo "private_boundary_guard: unknown arg: $1" >&2; exit 2 ;;
    esac
done

# Resolve repo root
if [ -n "$REPO_ROOT_OVERRIDE" ]; then
    [ -d "$REPO_ROOT_OVERRIDE" ] || { echo "private_boundary_guard: repo-root not found: $REPO_ROOT_OVERRIDE" >&2; exit 2; }
    REPO_ROOT="$(cd "$REPO_ROOT_OVERRIDE" && pwd)"
else
    SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
    REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
fi

cd "$REPO_ROOT" || { echo "private_boundary_guard: cannot cd $REPO_ROOT" >&2; exit 2; }

# ── Build list of directories to scan ─────────────────────────────────────────
SCAN_DIRS=""
for d in backend/src frontend/src specs contracts blueprints practices/rules docs; do
    [ -d "$d" ] && SCAN_DIRS="$SCAN_DIRS $d"
    [ -f "$d" ] && SCAN_DIRS="$SCAN_DIRS $d"
done
SCAN_DIRS="$(echo "$SCAN_DIRS" | xargs)"

# False positive allowlist pattern — lines/paths containing these are suppressed
ALLOWLIST_PATTERN='EXAMPLE|example|placeholder|your-|xxxx|REDACTED'

violations=0

report_violation() {
    local layer="$1"
    local pattern="$2"
    local matched_line="$3"
    echo "private_boundary_guard: VIOLATION [layer ${layer}] pattern='${pattern}'" >&2
    echo "  ${matched_line}" >&2
    violations=$((violations + 1))
}

# ── LAYER 1: opt-in marker scan ───────────────────────────────────────────────
MARKERS_FILE=".ax-private-markers"
if [ -f "$MARKERS_FILE" ]; then
    while IFS= read -r pattern; do
        # Skip comments and blank lines
        [ -z "$pattern" ] && continue
        case "$pattern" in
            '#'*) continue ;;
        esac

        if [ -n "$SCAN_DIRS" ]; then
            # Find matching lines, then filter allowlist
            # shellcheck disable=SC2086
            while IFS= read -r hit; do
                [ -z "$hit" ] && continue
                report_violation "1:marker" "$pattern" "$hit"
            done < <(grep -rnE -e "$pattern" $SCAN_DIRS 2>/dev/null \
                     | grep -vE "$ALLOWLIST_PATTERN" || true)
        fi
    done < "$MARKERS_FILE"
fi

# ── LAYER 2: generic secret heuristics ────────────────────────────────────────
if [ -n "$SCAN_DIRS" ]; then
    # Pattern a: PEM private key header
    SECRET_A='-----BEGIN [A-Z ]*PRIVATE KEY-----'
    # Pattern b: AWS access key
    SECRET_B='AKIA[0-9A-Z]{16}'
    # Pattern c: API key assignment (case-insensitive via lowercase pattern + -i flag separate pass)
    SECRET_C='[Aa][Pp][Ii][_-]?[Kk][Ee][Yy]['"'"'"]?[[:space:]]*[:=][[:space:]]*['"'"'"][A-Za-z0-9_-]{20,}'
    # Pattern d: JWT token (three base64url-encoded segments)
    SECRET_D='eyJ[A-Za-z0-9_-]{10,}\.[A-Za-z0-9_-]{10,}\.'

    for secret_pattern in "$SECRET_A" "$SECRET_B" "$SECRET_C" "$SECRET_D"; do
        # shellcheck disable=SC2086
        # Use -e so patterns starting with '-' (e.g. PEM header) are not mistaken for flags.
        # Exclude src/test/ paths: test code legitimately contains crafted tokens for security tests.
        # Layer 1 (marker) still scans test paths — company identifiers are forbidden even in tests.
        while IFS= read -r hit; do
            [ -z "$hit" ] && continue
            report_violation "2:secret" "$secret_pattern" "$hit"
        done < <(grep -rnE -e "$secret_pattern" $SCAN_DIRS 2>/dev/null \
                 | grep -vE "$ALLOWLIST_PATTERN" \
                 | grep -v '/src/test/' \
                 || true)
    done
fi

# ── Result ────────────────────────────────────────────────────────────────────
if [ "$violations" -gt 0 ]; then
    echo "" >&2
    echo "private_boundary_guard: FAIL — ${violations} private/secret boundary violation(s) detected." >&2
    echo "  Layer 1: add identifier to .ax-private-markers only in a PRIVATE fork (not here)." >&2
    echo "  Layer 2: remove or redact real secrets; use EXAMPLE/placeholder/REDACTED in public code." >&2
    exit 1
fi

if [ -n "$SCAN_DIRS" ]; then
    SCANNED="$SCAN_DIRS"
else
    SCANNED="(no scan dirs found)"
fi
echo "private_boundary_guard: PASS — no private boundary violations (scanned: $SCANNED)"
exit 0
