#!/usr/bin/env bash
# practices/evals/fixture_kill_proof_guard.sh — shell-guard판 PIT: fail fixture non-vacuity [87]
#
# PURPOSE
#   guard의 fail fixture가 실제로 guard의 특정 탐지 로직에 의존하는지(= non-vacuous)를
#   mutation으로 기계 증명한다.
#
#   배경: private_boundary_guard의 fail_secret_multitoken fixture가 vacuous였음 —
#   해당 fixture가 원래대로 exit 1을 내지만, 그 fixture가 검증하려는 N1(token-loop) 로직을
#   실제로 테스트하지 않았다 (pre-fix guard도 같은 fixture에서 exit 1을 냈기 때문).
#   이를 사람이 적발했다; 이 guard는 그 적발을 기계화한다.
#
# ALGORITHM (per manifest item)
#   0. neuter 값이 allowlisted PIT-style operator shape에 매칭하는지 확인 (P2-14).
#      shape 밖이거나 control-flow escape 토큰(exit/return/kill)을 포함하면 → BLOCK.
#   1. anchor가 guard 소스에 정확히 1회 존재하는지 확인 (0회/2+회 → manifest stale → BLOCK)
#   2. 원본 guard를 fixture에 실행 → exit 1 확인 (fixture가 실패를 재현)
#   3. guard 소스에서 anchor를 neuter로 치환 → 임시 guard 생성
#   4. 임시 guard를 같은 fixture에 실행 → exit 0 확인 (fixture가 해당 로직에 의존 = non-vacuous)
#   4가 exit 0로 뒤집히지 않으면 → fixture가 VACUOUS → BLOCK (exit 1)
#
# MANIFEST FORMAT (fixture_kill_manifest.yaml)
#   items:
#     - id: <identifier>
#       guard: <path relative to repo root>
#       fixture: <path relative to repo root (directory)>
#       fixture_arg: <CLI flag to pass fixture dir, e.g. --repo-root>
#       anchor: <exact string appearing ONCE in guard source>
#       neuter: <replacement string that disables the targeted logic>
#
# SELF-PROOF
#   fixtures/fixture-kill-proof/ に two mini-manifests:
#     vacuous_manifest.yaml   — 항목 1개, neuter가 flip 안 됨 → meta-gate exit 1
#     nonvacuous_manifest.yaml — 항목 1개, neuter가 올바름  → meta-gate exit 0
#
# LIMITS
#   1. neuter 어휘는 고정 allowlist로 강제된다 (P2-14, 2026-07-13):
#      no-op(true/:/comment) · sentinel-string(NEUTER_*) · condition-constant(False/false) ·
#      truncation(| head -N) · over-broad-glob(*)) · variable-substitution(anchor와 skeleton이
#      동일하고 $var만 교체) 6개 shape 중 하나에 매칭해야 하며, exit/return/kill 또는
#      &&/||로 연결된 exit 같은 control-flow escape 토큰이 있으면 shape 매칭과 무관하게
#      즉시 BLOCK된다. `exit 0` 같은 short-circuit neuter는 이제 등재 시점에 차단된다.
#   2. 잔여 리스크(honest residual): 이 allowlist는 토큰/구조 매칭이지 완전한 semantic
#      검증이 아니다. 허용된 shape 안에서도 저자가 논리를 오도하는 neuter를 만들면
#      (예: 엉뚱한 변수를 골라 variable-substitution shape을 통과) 여전히 사람 리뷰에
#      의존한다 — **이 6-shape 어휘 밖의 적대적 neuter는 author-responsibility로 남는다.**
#      고정 mutator operator를 쓰는 Java PIT [84] 대비 약한 지점은 shape 내부 의미론적
#      정확성 보장의 부재다.
#   3. exit 0은 원인 불문 "flip"으로 인정된다 (mutation 결과 판정 로직은 변경 없음).
#
# USAGE
#   bash practices/evals/fixture_kill_proof_guard.sh
#       LIVE: fixture_kill_manifest.yaml에 등재된 모든 항목 kill 확인.
#
#   bash practices/evals/fixture_kill_proof_guard.sh --manifest PATH
#       OVERRIDE: 지정된 manifest 사용 (자기 비공허성 fixture 테스트용).
#
# EXIT: 0 = all items non-vacuous. 1 = vacuous item(s) found (BLOCK). 2 = usage/tooling error.

set -u

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
DEFAULT_MANIFEST="$SCRIPT_DIR/fixture_kill_manifest.yaml"

MANIFEST="$DEFAULT_MANIFEST"
while [ $# -gt 0 ]; do
    case "$1" in
        --manifest)   MANIFEST="$2"; shift 2 ;;
        --manifest=*) MANIFEST="${1#--manifest=}"; shift ;;
        *) echo "fixture_kill_proof_guard: unknown arg: $1" >&2; exit 2 ;;
    esac
done

[ -f "$MANIFEST" ] || {
    echo "fixture_kill_proof_guard: manifest not found: $MANIFEST" >&2; exit 2; }

command -v python3 >/dev/null 2>&1 || {
    echo "fixture_kill_proof_guard: python3 required" >&2; exit 2; }

# ── Parse manifest into tab-separated rows ────────────────────────────────────
# Columns: id | guard | fixture | fixture_arg | anchor | neuter
ITEMS="$(python3 - "$MANIFEST" <<'PY'
import sys, yaml
manifest_path = sys.argv[1]
try:
    doc = yaml.safe_load(open(manifest_path, encoding='utf-8'))
except Exception as e:
    print(f"PARSE_ERROR: {e}", file=sys.stderr); sys.exit(2)
items = doc.get('items') or []
for it in items:
    fields = [
        str(it.get('id', '?')),
        str(it.get('guard', '')),
        str(it.get('fixture', '')),
        str(it.get('fixture_arg', '')),
        str(it.get('anchor', '')),
        str(it.get('neuter', '')),
    ]
    print('\t'.join(fields))
PY
)"

parse_rc=$?
if [ "$parse_rc" -ne 0 ]; then
    echo "fixture_kill_proof_guard: failed to parse manifest: $MANIFEST" >&2; exit 2
fi

if [ -z "$ITEMS" ]; then
    echo "fixture_kill_proof_guard: PASS — manifest has no items (nothing to prove)"
    exit 0
fi

FAIL=0
PROVEN=0

while IFS=$'\t' read -r item_id guard_rel fixture_rel fixture_arg anchor neuter; do
    [ -n "$item_id" ] || continue

    GUARD_PATH="$REPO_ROOT/$guard_rel"
    FIXTURE_PATH="$REPO_ROOT/$fixture_rel"

    # ── field validation ──────────────────────────────────────────────────────
    [ -f "$GUARD_PATH" ] || {
        echo "fixture_kill_proof_guard: FAIL [$item_id] guard not found: $GUARD_PATH" >&2
        FAIL=1; continue; }

    [ -d "$FIXTURE_PATH" ] || {
        echo "fixture_kill_proof_guard: FAIL [$item_id] fixture dir not found: $FIXTURE_PATH" >&2
        FAIL=1; continue; }

    [ -n "$anchor" ] || {
        echo "fixture_kill_proof_guard: FAIL [$item_id] anchor is empty" >&2
        FAIL=1; continue; }

    # ── (0) neuter vocabulary validation (P2-14) ──────────────────────────────
    # neuter must match one of 6 allowlisted PIT-style operator shapes, and must
    # not contain a control-flow escape token (exit/return/kill) regardless of shape.
    neuter_verdict="$(python3 - "$anchor" "$neuter" <<'PY'
import re, sys
anchor = sys.argv[1]
neuter = sys.argv[2]

# control-flow escape tokens are rejected unconditionally (any shape).
reject_patterns = [
    r'\bexit\b',
    r'\breturn\b',
    r'\bkill\b',
    r'(&&|\|\|)\s*exit\b',
]
for pat in reject_patterns:
    if re.search(pat, neuter):
        print(f"REJECT: neuter contains control-flow escape token (pattern {pat!r}): {neuter!r}")
        sys.exit(0)

def skeleton(s):
    # collapse $var / ${var} references to a single placeholder so a pure
    # variable-identifier swap can be recognized independent of naming.
    return re.sub(r'\$\{?[A-Za-z_][A-Za-z0-9_]*\}?', 'VAR', s)

shapes = []
if re.match(r'^\s*(true|:)\s*(#.*)?$', neuter):
    shapes.append('no-op')
if re.search(r'NEUTER_[A-Z0-9_]+', neuter):
    shapes.append('sentinel-string')
if neuter.strip() in ('False', 'false'):
    shapes.append('condition-constant')
if re.search(r'\|\s*head\s+-?\d+', neuter):
    shapes.append('truncation')
if neuter.strip() == '*)':
    shapes.append('over-broad-glob')
if neuter != anchor and skeleton(neuter) == skeleton(anchor):
    shapes.append('variable-substitution')

if shapes:
    print("PASS: " + ",".join(shapes))
else:
    print("UNKNOWN: neuter does not match any allowlisted PIT-style operator shape: " + repr(neuter))
PY
)"
    case "$neuter_verdict" in
        PASS:*) : ;;
        REJECT:*|UNKNOWN:*)
            echo "fixture_kill_proof_guard: FAIL [$item_id] neuter vocabulary rejected — ${neuter_verdict}" >&2
            FAIL=1; continue ;;
        *)
            echo "fixture_kill_proof_guard: FAIL [$item_id] neuter vocabulary check errored: ${neuter_verdict}" >&2
            FAIL=1; continue ;;
    esac

    # ── (1) anchor uniqueness in guard source ─────────────────────────────────
    anchor_count="$(python3 - "$GUARD_PATH" "$anchor" <<'PY'
import sys
src = open(sys.argv[1], encoding='utf-8').read()
print(src.count(sys.argv[2]))
PY
)"
    if [ "$anchor_count" -ne 1 ]; then
        echo "fixture_kill_proof_guard: FAIL [$item_id] anchor appears ${anchor_count} time(s) in $guard_rel" >&2
        echo "  (expected exactly 1 — manifest is stale; update anchor to match current guard source)" >&2
        FAIL=1; continue
    fi

    # ── (2) original guard on fixture → expect exit 1 ────────────────────────
    if [ -n "$fixture_arg" ]; then
        bash "$GUARD_PATH" "$fixture_arg" "$FIXTURE_PATH" >/dev/null 2>&1
        orig_rc=$?
    else
        bash "$GUARD_PATH" "$FIXTURE_PATH" >/dev/null 2>&1
        orig_rc=$?
    fi
    if [ "$orig_rc" -ne 1 ]; then
        echo "fixture_kill_proof_guard: FAIL [$item_id] original guard exited $orig_rc on fixture (expected 1 — fixture no longer reproduces the failure)" >&2
        FAIL=1; continue
    fi

    # ── (3) create neutered guard via anchor→neuter substitution ─────────────
    TMP_GUARD="$(mktemp)"
    python3 - "$GUARD_PATH" "$anchor" "$neuter" "$TMP_GUARD" <<'PY'
import sys
src = open(sys.argv[1], encoding='utf-8').read()
anchor = sys.argv[2]
neuter = sys.argv[3]
out_path = sys.argv[4]
neutered = src.replace(anchor, neuter, 1)
with open(out_path, 'w', encoding='utf-8') as f:
    f.write(neutered)
PY
    neuter_gen_rc=$?
    if [ "$neuter_gen_rc" -ne 0 ]; then
        echo "fixture_kill_proof_guard: FAIL [$item_id] manifest error: python3 failed to generate neutered guard (exit $neuter_gen_rc)" >&2
        rm -f "$TMP_GUARD"
        FAIL=1; continue
    fi
    if [ ! -s "$TMP_GUARD" ]; then
        echo "fixture_kill_proof_guard: FAIL [$item_id] manifest error: neutered guard is empty" >&2
        rm -f "$TMP_GUARD"
        FAIL=1; continue
    fi
    if cmp -s "$GUARD_PATH" "$TMP_GUARD"; then
        echo "fixture_kill_proof_guard: FAIL [$item_id] manifest error: neutered guard is identical to original (anchor not replaced — anchor mismatch?)" >&2
        rm -f "$TMP_GUARD"
        FAIL=1; continue
    fi

    # ── (4) neutered guard on fixture → expect exit 0 (flipped) ─────────────
    if [ -n "$fixture_arg" ]; then
        bash "$TMP_GUARD" "$fixture_arg" "$FIXTURE_PATH" >/dev/null 2>&1
        neuter_rc=$?
    else
        bash "$TMP_GUARD" "$FIXTURE_PATH" >/dev/null 2>&1
        neuter_rc=$?
    fi
    rm -f "$TMP_GUARD"

    if [ "$neuter_rc" -eq 1 ]; then
        echo "fixture_kill_proof_guard: FAIL [$item_id] neutered guard exited 1 on fixture (expected 0)" >&2
        echo "  VACUOUS fixture: fixture exit 1 does not depend on the anchor logic — it fires regardless." >&2
        echo "  anchor: $anchor" >&2
        FAIL=1; continue
    elif [ "$neuter_rc" -ne 0 ]; then
        echo "fixture_kill_proof_guard: FAIL [$item_id] manifest error: neuter broke the guard (exit $neuter_rc) — anchor/neuter 치환이 bash 문법을 깼다" >&2
        echo "  anchor: $anchor" >&2
        FAIL=1; continue
    fi

    echo "fixture_kill_proof_guard: PASS [$item_id] non-vacuous (original=exit1, neutered=exit0)"
    PROVEN=$((PROVEN + 1))
done <<< "$ITEMS"

if [ "$FAIL" -ne 0 ]; then
    echo "" >&2
    echo "fixture_kill_proof_guard: FAIL — $FAIL item(s) are vacuous or have a stale manifest anchor." >&2
    exit 1
fi

echo "fixture_kill_proof_guard: PASS — $PROVEN item(s) all non-vacuous (every fail fixture is killed by its targeted neuter)"
exit 0
