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
#   두 단계로 suppress 여부를 판정한다 — 경로·주석 부수 등장이나 다중 토큰의
#   첫 번째만 검사하는 오류로 real secret이 묻히는 것을 방지한다:
#
#   1) pragma 에스케이프 (문서 경로 한정):
#      같은 라인에 "pragma: allow-secret"이 있으면 suppress하되,
#      docs/ 또는 practices/rules/ 경로이거나 *.md 파일일 때만 유효.
#      backend/src·frontend/src·specs·contracts 등 코드/스펙 트리에서는
#      pragma가 있어도 무시한다 — real credential은 코드에서 pragma로 숨길 수 없다.
#      예: docs/crypto-guide.md 내 `-----BEGIN RSA PRIVATE KEY-----  # pragma: allow-secret`
#
#   2) 모든 매칭 토큰 값 순회 (N1 fix — head -1 제거):
#      같은 라인에 여러 토큰이 있으면 ALL 토큰을 순회해서, 하나라도
#      non-placeholder(ALLOWLIST_PATTERN 불일치)이면 violation으로 보고한다.
#      placeholder decoy 토큰이 첫 번째로 오더라도 뒤따르는 real 시크릿을 놓치지 않음.
#      예: `"AKIAEXAMPLEXXXXXXXXX", "AKIA0123456789ABCDEF"` → 두 번째 real AKIA → exit 1
#      예: `api_key = "EXAMPLE_KEY"` → 단일 토큰이 placeholder → suppress
#
#   3) value-only 판정 (경로·주석 오염 방지):
#      ALLOWLIST_PATTERN은 파일 경로·줄번호·라인의 다른 텍스트(주석 etc.)가 아닌
#      실제 매칭된 토큰 값에 포함될 때만 suppress한다.
#      예: `AKIA0123456789ABCDEF  // your-env` → real token, comment `your-env`은 무시
#          `backend/src/com/example/Real.java` → 경로 example로 suppress 안 함
#
#   4) src/test/ 경로 제외 (층2만):
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
#   - docs/practices/rules 이외 markdown fenced-block 내부는 자동 제외 안 됨 →
#     문서화용 PEM/JWT 예시는 docs/ 또는 practices/rules/ 경로의 파일에 넣거나
#     pragma: allow-secret 주석으로 라인 단위 이스케이프 필요.
#   - 스캔은 위 SCAN TARGETS 경로로 한정. 루트 레벨 .env·.envrc·기타 dotfile은
#     현재 커버리지 밖 (silent gap이 아닌 honest gap).
#   - ax-template HEAD 자체: markers 비어 있어 층1 0-match, 실 시크릿 없어 층2 0-match.
#   - fork-receiver 차단은 .ax-private-markers opt-in 활성화로 작동.
#
# PRAGMA ESCAPE (문서 경로 한정)
#   docs/ 또는 practices/rules/ 하위 파일, 또는 *.md 파일의 라인에
#   `pragma: allow-secret` 주석을 추가하면 해당 라인의 시크릿 탐지를 suppress한다.
#   backend/src·frontend/src·specs·contracts 등 코드 트리에서는 pragma가 무시된다.
#   예(유효):   `-----BEGIN RSA PRIVATE KEY-----  # pragma: allow-secret`  in docs/
#   예(무효):   `String K="AKIA..."; // pragma: allow-secret`              in backend/src/
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
# Applied only to extracted matched token values (not path, not surrounding text).
# Every token on a line is checked; a single real token triggers violation even if
# placeholder tokens also appear on the same line.
ALLOWLIST_PATTERN='EXAMPLE|example|placeholder|your-|xxxx|REDACTED'

# Pragma pattern — checked against full content line (path stripped).
# Only valid in documentation paths; ignored in code/spec trees.
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

# is_doc_path: Return 0 if the grep hit's filepath is a documentation path where
# pragma: allow-secret is valid. Code/spec paths (backend/src, frontend/src, etc.)
# return 1 — pragma is ignored there so real credentials cannot be hidden with a comment.
is_doc_path() {
    local hit="$1"
    local filepath
    filepath="$(echo "$hit" | sed 's/:[0-9]*:.*//')"
    case "$filepath" in
        docs/* | practices/rules/* | *.md)
            return 0 ;;
    esac
    return 1
}

# ── LAYER 1: opt-in marker scan ───────────────────────────────────────────────
# Uses -i (case-insensitive) so "AcmeCorp" marker catches com.acmecorp package refs.
# Checks ALL matched tokens per line (N1 fix) — placeholder decoy first does not hide
# a real match later on the same line.
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
                # Pragma escape: only valid in documentation paths
                if is_doc_path "$hit" && echo "$content" | grep -qiE "$PRAGMA_PATTERN"; then
                    continue
                fi
                # Loop over ALL matched tokens (N1 fix: head -1 removed).
                # Suppress only when every token on this line is a placeholder.
                all_placeholder=1
                found_any=0
                while IFS= read -r tok; do
                    [ -z "$tok" ] && continue
                    found_any=1
                    if ! echo "$tok" | grep -qiE "$ALLOWLIST_PATTERN"; then
                        all_placeholder=0
                        break
                    fi
                done < <(echo "$content" | grep -oiE -e "$pattern")
                # If all tokens are placeholders (and at least one was found), suppress
                [ "$found_any" -eq 1 ] && [ "$all_placeholder" -eq 1 ] && continue
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
            # 1. Pragma escape: only valid in documentation paths (docs/, practices/rules/, *.md).
            #    In code/spec trees, pragma is ignored — real credentials cannot be hidden.
            if is_doc_path "$hit" && echo "$content" | grep -qiE "$PRAGMA_PATTERN"; then
                continue
            fi
            # 2. Loop over ALL matched tokens (N1 fix: head -1 removed).
            #    Value-only allowlist: suppress if EVERY token on the line is a placeholder.
            #    A single non-placeholder token triggers violation even if others are dummies.
            all_placeholder=1
            found_any=0
            while IFS= read -r tok; do
                [ -z "$tok" ] && continue
                found_any=1
                if ! echo "$tok" | grep -qE "$ALLOWLIST_PATTERN"; then
                    all_placeholder=0
                    break
                fi
            done < <(echo "$content" | grep -oE -e "$secret_pattern")
            # If all tokens are placeholders (and at least one was found), suppress
            [ "$found_any" -eq 1 ] && [ "$all_placeholder" -eq 1 ] && continue
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
    echo "  Pragma escape is valid only in docs/ or practices/rules/ or *.md files." >&2
    exit 1
fi

if [ -n "$SCAN_DIRS" ]; then
    SCANNED="$SCAN_DIRS"
else
    SCANNED="(no scan dirs found)"
fi
echo "private_boundary_guard: PASS — no private boundary violations (scanned: $SCANNED)"
exit 0
