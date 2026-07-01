#!/usr/bin/env bash
# practices/evals/private_boundary_guard.sh — R26 public/private 경계 기계강제 guard [86]
#
# PURPOSE
#   ax-template은 public fork-base catalog이다. fork-receiver(회사·팀)의 특화·민감 정보가
#   이 public 트리에 유입되면 안 된다. 이 guard는 두 층으로 그 경계를 기계적으로 강제한다.
#
# LAYER 1 — opt-in marker (fork-receiver 활성화)
#   .ax-private-markers 파일이 repo root에 존재하고 활성 패턴(#·빈 줄 제외)이 있으면,
#   각 줄을 ERE 패턴으로 스캔 대상 트리에서 case-insensitive 검색한다.
#   매칭 → violation (회사 식별자는 test 경로 포함 어디서도 금지).
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
# FALSE POSITIVE SUPPRESSION
#   두 단계로 suppress 여부를 판정한다 — 경로나 주석의 부수어 등장으로 real secret이
#   묻히는 C1 오버-suppression 버그를 방지한다:
#
#   1) pragma 에스케이프 (라인 전체 판정):
#      같은 라인에 "pragma: allow-secret" (또는 pragma allow-secret)이 주석으로
#      붙어 있으면 suppress. docs/practices/rules의 설명용 fenced-block 예시에 사용.
#      예: `-----BEGIN RSA PRIVATE KEY-----  # pragma: allow-secret`
#
#   2) 매칭된 시크릿 토큰 값 자체가 placeholder일 때만 suppress (값-only 판정):
#      ALLOWLIST_PATTERN이 파일 경로·줄번호·라인의 다른 텍스트(주석 등)가 아닌
#      실제 매칭된 토큰 값에 포함될 때만 suppress한다.
#      예: api_key = "EXAMPLE_KEY"          → suppress (값이 EXAMPLE)
#          AKIA0123456789ABCDEF  // your-env → NOT suppressed (토큰 값은 real)
#          backend/src/com/example/Real.java → 경로 example로 suppress 안 함
#
#   3) src/test/ 경로 제외 (층2만):
#      보안 테스트 코드는 crafted JWT·더미 키를 합법적으로 포함. 층1(marker)은
#      test 포함 — 회사 식별자는 테스트에도 금지.
#
# SCAN TARGETS
#   backend/src, frontend/src, specs, contracts, blueprints, practices/rules, docs
#   README.md, CLAUDE.md, .github
#   (guard 자신과 practices/evals는 self-match 방지를 위해 제외)
#
# LIMITS (honest scope)
#   - 정적 트리 스캔 전용. git 히스토리·커밋 메시지·바이너리·인코딩 시크릿은 스캔 안 함.
#   - docs/practices/rules의 markdown fenced-block 내부는 자동 제외되지 않음 →
#     문서화용 PEM/JWT 예시는 `# pragma: allow-secret`으로 라인 단위 이스케이프 필요.
#   - 스캔은 위 SCAN TARGETS 경로로 한정. 루트 레벨 .env·.envrc·기타 dotfile은
#     현재 커버리지 밖 (silent gap이 아닌 honest gap).
#   - ax-template HEAD 자체: markers 비어 있어 층1 0-match, 실 시크릿 없어 층2 0-match.
#   - fork-receiver 차단은 .ax-private-markers opt-in 활성화로 작동.
#
# PRAGMA ESCAPE
#   라인에 `pragma: allow-secret` (또는 `pragma allow-secret`) 주석을 추가하면
#   해당 라인의 시크릿 탐지를 suppress한다. 문서화 목적 예시 코드에 사용.
#   예: `-----BEGIN RSA PRIVATE KEY-----  # pragma: allow-secret`
#       `"eyJhbGciOiJSUzI1NiJ9..."  # pragma: allow-secret`
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

# ── Build list of directories/files to scan ───────────────────────────────────
# Includes root-level docs (README.md, CLAUDE.md) and CI config (.github).
# practices/evals itself is excluded to prevent self-match on fixture content.
SCAN_DIRS=""
for d in backend/src frontend/src specs contracts blueprints practices/rules docs README.md CLAUDE.md .github; do
    [ -d "$d" ] && SCAN_DIRS="$SCAN_DIRS $d"
    [ -f "$d" ] && SCAN_DIRS="$SCAN_DIRS $d"
done
SCAN_DIRS="$(echo "$SCAN_DIRS" | xargs)"

# ── Allowlist: placeholder keywords for the MATCHED TOKEN VALUE ───────────────
# Applied only to the extracted matched value (not path, not the rest of the line).
# Prevents path-contamination (com/example package) and comment-contamination
# (// your-env) from silently suppressing real secrets.
ALLOWLIST_PATTERN='EXAMPLE|example|placeholder|your-|xxxx|REDACTED'

# Pragma pattern — checked against full content line (path stripped).
# Suppresses documentation examples annotated as safe.
PRAGMA_PATTERN='pragma[[:space:]]*:?[[:space:]]*allow[-_]?secret'

violations=0

report_violation() {
    local layer="$1"
    local pattern="$2"
    local matched_line="$3"
    echo "private_boundary_guard: VIOLATION [layer ${layer}] pattern='${pattern}'" >&2
    echo "  ${matched_line}" >&2
    violations=$((violations + 1))
}

# strip_prefix: Remove "path:linenum:" prefix from grep -rn output to get content only.
# Prevents path components (e.g. com/example) from triggering the allowlist.
strip_prefix() {
    sed 's/^[^:]*:[0-9]*://'
}

# ── LAYER 1: opt-in marker scan ───────────────────────────────────────────────
# Uses -i (case-insensitive) so "AcmeCorp" marker catches com.acmecorp package refs.
MARKERS_FILE=".ax-private-markers"
if [ -f "$MARKERS_FILE" ]; then
    while IFS= read -r pattern; do
        # Skip comments and blank lines
        [ -z "$pattern" ] && continue
        case "$pattern" in
            '#'*) continue ;;
        esac

        if [ -n "$SCAN_DIRS" ]; then
            # shellcheck disable=SC2086
            while IFS= read -r hit; do
                [ -z "$hit" ] && continue
                # Strip path:linenum: — get content only
                content="$(echo "$hit" | strip_prefix)"
                # Check pragma escape (full content line)
                if echo "$content" | grep -qiE "$PRAGMA_PATTERN"; then
                    continue
                fi
                # Extract the matched value only; suppress if the VALUE ITSELF is placeholder
                matched_value="$(echo "$content" | grep -oiE -e "$pattern" | head -1)"
                if [ -n "$matched_value" ] && echo "$matched_value" | grep -qE "$ALLOWLIST_PATTERN"; then
                    continue
                fi
                report_violation "1:marker" "$pattern" "$hit"
            done < <(grep -rnEi -e "$pattern" $SCAN_DIRS 2>/dev/null || true)
        fi
    done < "$MARKERS_FILE"
fi

# ── LAYER 2: generic secret heuristics ────────────────────────────────────────
if [ -n "$SCAN_DIRS" ]; then
    # Pattern a: PEM private key header
    SECRET_A='-----BEGIN [A-Z ]*PRIVATE KEY-----'
    # Pattern b: AWS access key
    SECRET_B='AKIA[0-9A-Z]{16}'
    # Pattern c: API key assignment (handles various quoting styles)
    SECRET_C='[Aa][Pp][Ii][_-]?[Kk][Ee][Yy]['"'"'"]?[[:space:]]*[:=][[:space:]]*['"'"'"][A-Za-z0-9_-]{20,}'
    # Pattern d: JWT token (three base64url-encoded segments)
    SECRET_D='eyJ[A-Za-z0-9_-]{10,}\.[A-Za-z0-9_-]{10,}\.'

    for secret_pattern in "$SECRET_A" "$SECRET_B" "$SECRET_C" "$SECRET_D"; do
        # shellcheck disable=SC2086
        # -e so patterns starting with '-' (PEM header) are not mistaken for flags.
        # src/test/ excluded: test code legitimately contains crafted tokens.
        while IFS= read -r hit; do
            [ -z "$hit" ] && continue
            # Strip path:linenum: prefix — content only for all further checks
            content="$(echo "$hit" | strip_prefix)"
            # 1. Pragma escape: suppress if line has allow-secret annotation
            if echo "$content" | grep -qiE "$PRAGMA_PATTERN"; then
                continue
            fi
            # 2. Value-only allowlist: extract the matched token; suppress if VALUE is placeholder
            #    This prevents path (com/example) and comment (// your-env) from suppressing
            #    real secrets.
            matched_value="$(echo "$content" | grep -oE -e "$secret_pattern" | head -1)"
            if [ -n "$matched_value" ] && echo "$matched_value" | grep -qE "$ALLOWLIST_PATTERN"; then
                continue
            fi
            report_violation "2:secret" "$secret_pattern" "$hit"
        done < <(grep -rnE -e "$secret_pattern" $SCAN_DIRS 2>/dev/null \
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
    echo "  Inline escape: append '# pragma: allow-secret' to documentation-only examples." >&2
    exit 1
fi

if [ -n "$SCAN_DIRS" ]; then
    SCANNED="$SCAN_DIRS"
else
    SCANNED="(no scan dirs found)"
fi
echo "private_boundary_guard: PASS — no private boundary violations (scanned: $SCANNED)"
exit 0
