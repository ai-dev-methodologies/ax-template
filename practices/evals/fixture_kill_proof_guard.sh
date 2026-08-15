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
#       fixture_arg2: <OPTIONAL second CLI flag, e.g. --commit-msg-file>
#       fixture2: <OPTIONAL path (relative to repo root) for fixture_arg2's value>
#   fixture_arg2/fixture2 are only needed when the guard requires two distinct
#   inputs to reproduce a failure (e.g. private_boundary_guard's --repo-root
#   for the fixture's .ax-private-markers plus --commit-msg-file for the
#   message text). Omit both for single-input guards — unset by default,
#   zero behavior change for every item that predates this field pair.
#
#   OPTIONAL top-level key:
#     min_items: <N>   — the floor the UNIQUE item count may not fall below.
#
# REGISTRY INTEGRITY (2026-07-30, BACKLOG P2-50)
#   Three structural checks run BEFORE any mutation, because a registry that has been
#   shrunk or padded reports a green "all items non-vacuous" about the wrong set:
#     1. duplicate `id`            → BLOCK. The id is the report identity; two rows sharing
#                                    one make the PASS line ambiguous about which was proven.
#     2. duplicate (guard,fixture) → BLOCK. A COUNT IS NOT AN IDENTITY. Deleting the item
#                                    that matters and re-adding a cheap already-proven pair
#                                    under a fresh id keeps any floor satisfied while the
#                                    proof it stood for is gone — the attack the protected-
#                                    anchor ledger was hardened against. Consequence,
#                                    stated rather than hidden: registering a SECOND anchor
#                                    against an already-registered pair (defence in depth on
#                                    one fixture) becomes a reviewed edit to this guard. No
#                                    such item exists today; the restriction is what makes
#                                    the floor mean anything.
#     3. min_items floor           → BLOCK when the unique count is below the manifest's
#                                    declared `min_items`, and — for THIS guard's own default
#                                    manifest — when that declaration itself is missing or
#                                    below the pinned LIVE_MIN_ITEMS. The number is
#                                    deliberately duplicated (manifest declares, guard pins)
#                                    so emptying the gate takes two coordinated edits instead
#                                    of one silent deletion. Same ratchet as
#                                    evidence_protected_template_anchors.txt's min_entries /
#                                    LIVE_MIN_PROTECTED_ENTRIES pair.
#   An override manifest (--manifest) that declares no min_items is unconstrained by the
#   floor — that is what the one-item self-proof mini-manifests are, and forcing a floor on
#   them would only be a floor of zero. They are still duplicate-checked.
#
# SELF-PROOF
#   fixtures/fixture-kill-proof/ に two mini-manifests:
#     vacuous_manifest.yaml   — 항목 1개, neuter가 flip 안 됨 → meta-gate exit 1
#     nonvacuous_manifest.yaml — 항목 1개, neuter가 올바름  → meta-gate exit 0
#
# LIMITS
#   1. neuter 어휘는 고정 allowlist로 강제된다 (P2-14, 2026-07-13; 2026-07-14 cross-family
#      리뷰로 조임): no-op(true/:/comment 단독) · sentinel-string(anchor와 str_skeleton이
#      동일 — 따옴표 문자열 리터럴만 NEUTER_* 로 교체, 임의 코드에 NEUTER_ name-drop 불가) ·
#      condition-constant(False/false 단독) · truncation(neuter에서 `| head -N` 1회 제거 시
#      anchor와 byte-identical) · over-broad-glob(`*)` 단독) · variable-substitution(anchor와
#      skeleton 동일, $var만 교체) 6개 shape 중 하나에 매칭해야 하며,
#      neuter가 anchor 대비 **새로 도입**하는 escape 토큰(exit/return/kill/exec/eval/
#      source/trap·후행 &·명령치환 $( )·백틱·프로세스치환 <( ))이 있으면 shape 매칭과
#      무관하게 즉시 BLOCK된다(anchor에 이미 있는 <( ) 보존은 허용 — 카운트 비교).
#      차단 실증: `exit 0` / `exec true # NEUTER_BYPASS` / `SECRET_A="$(true) NEUTER_X"`
#      (따옴표 안 명령치환) / 파이프라인 중간 `| head -1 foo`. truncation은 head 절
#      제거 시 anchor와 byte-identical이고 그 절이 파이프라인 **마지막 단계**일 때만 인정.
#      또한 manifest가 0 항목으로 파싱되면 PASS 대신 BLOCK한다(공허 통과 차단).
#   2. 잔여 리스크(honest residual): 4개 shape(sentinel/truncation/variable-substitution은
#      anchor-skeleton 바인딩, 상수형은 단독-토큰 강제)이지만 여전히 토큰/구조 매칭이지
#      완전한 semantic 검증이 아니다 — 예: variable-substitution에서 엉뚱한 변수를 고르는
#      오도성 neuter는 통과 가능하며 사람 리뷰에 의존한다. shape 내부 의미론적 정확성
#      보장의 부재가 Java PIT [84] 대비 약한 지점이다 (allowlist "밖"이 아니라 "안"의
#      오도성이 잔여 리스크임을 명시 — 2026-07-14 리뷰 지적 반영).
#   3. exit 0은 원인 불문 "flip"으로 인정된다 (mutation 결과 판정 로직은 변경 없음).
#   4. Typed operator enum for neuter shapes was assessed 2026-07-14 and deferred
#      as net-negative: a typed schema would break every existing manifest item
#      and re-introduce the heredoc special-char hazard, for only marginal gain
#      over the documented author-responsibility residual noted in limit 2 above.
#
# USAGE
#   bash practices/evals/fixture_kill_proof_guard.sh
#       LIVE: fixture_kill_manifest.yaml에 등재된 모든 항목 kill 확인.
#
#   bash practices/evals/fixture_kill_proof_guard.sh --manifest PATH
#       OVERRIDE: 지정된 manifest 사용 (자기 비공허성 fixture 테스트용).
#
# EXIT: 0 = all items non-vacuous. 1 = vacuous item(s) found, or the registry itself failed
#       integrity (duplicate id / duplicate (guard,fixture) / min_items floor) — BLOCK.
#       2 = usage/tooling error.

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
# Columns: id | guard | fixture | fixture_arg | anchor | neuter | fixture_arg2 | fixture2
# fixture_arg2/fixture2 are OPTIONAL (default '') — a second flag+path pair passed
# to the guard alongside fixture_arg/fixture, for guards that need two inputs to
# reproduce a failure (e.g. private_boundary_guard's --repo-root + --commit-msg-file).
# Existing single-input items simply leave these two fields unset; behavior for
# them is unchanged (see invocation sites below, gated on fixture_arg2 non-empty).
ITEMS_PY="$(mktemp)"
cat <<'PY' > "$ITEMS_PY"
import os, sys, yaml
manifest_path = sys.argv[1]
default_manifest = sys.argv[2]
try:
    doc = yaml.safe_load(open(manifest_path, encoding='utf-8'))
except Exception as e:
    print(f"PARSE_ERROR: {e}", file=sys.stderr); sys.exit(2)
items = doc.get('items') or []

# ── registry integrity (BACKLOG P2-50) ───────────────────────────────────────
# Runs before any row is emitted: a shrunk or padded registry would otherwise report a
# green verdict about a set that is not the one the gate was earned on. Exit 3 is the
# structural-violation channel; the caller maps it to BLOCK (exit 1), distinct from the
# exit 2 tooling channel above.
#
# LIVE_MIN_ITEMS is the guard-pinned half of the floor and applies to THIS guard's own
# default manifest — identified by realpath, so naming the same file via --manifest does
# not opt out of it. MAY NOT BE REDUCED: lowering it without also lowering the manifest's
# `min_items` directive (or vice-versa) leaves the other half BLOCKING, which is the point.
# The VALUE below is the only authoritative statement of this floor in this file (BACKLOG
# P3-104): earlier revisions restated a number in prose ("the measured disk truth at
# introduction, 57") which then rotted independently of the constant it described, so the file
# simultaneously claimed 57 and enforced 62 — a doc-vs-disk lie inside the gate whose whole job
# is to reject those. Prose now refers to the CONSTANT, never to a literal, and the constant is
# re-measured from disk whenever items are appended.
# 2026-07-30 (PRD-final-4 W5b): 62 → 64. Two kill-proofs were appended for the new
# manifest_snapshot_integrity_guard (fail_diverged + fail_stale_allowlist), so the floor moves
# to the new disk truth in the same commit — otherwise the two new proofs would be removable
# without breaching the registry floor, which is exactly the shrink [87] exists to reject.
# 2026-08-02 (D-4): 72 → 73. One kill-proof was appended for the new
# derived_block_license_guard (fail_unregistered), same reasoning.
# 2026-08-10 (D-7): 73 → 74. One kill-proof was appended for doc_headline_count_guard's
# new plugin.json/marketplace.json version-sync check (fail_version_mismatch).
# 2026-08-14 (GH #92 registration): 74 → 86. Ten kill-proofs were appended for the three
# newly-registered guards: install_artifact_extractability_guard.sh [112] (5 —
# fail_missing_marker, fail_duplicate_id, fail_free_text_conditional,
# fail_wrong_comment_prefix, fail_unparseable_shell), cross_artifact_contract_guard.sh [113]
# (2 — fail_p_contract_drift, fail_rule_not_in_index), downstream_release_recency_guard.sh
# [114] (3 — fail_stale_head, fail_partial_assertions, fail_digest_mismatch). A 4th [114]
# fixture, fail_missing_log, was measured (not assumed) to be fail-closed-by-construction —
# every single-anchor neuter that bypasses its `not os.path.isfile(log_path)` pre-check still
# crashes at the following `open(log_path, ...)` with an uncaught FileNotFoundError, which
# CPython also exits with status 1, so no minimal mutation flips it to exit 0. Disclosed in
# fixture_kill_manifest.yaml's comment at that fixture's (absent) entry rather than registered
# with a fake anchor. Prior disk truth was 76 unique items (2 above the previous floor of 74);
# the floor now tracks the new disk truth of 86 exactly, so none of the 10 new proofs are
# removable without breaching the registry floor.
LIVE_MIN_ITEMS = 97

structural = []


def _same_file(a, b):
    try:
        return os.path.samefile(a, b)
    except OSError:
        return os.path.realpath(a) == os.path.realpath(b)


is_default_manifest = _same_file(manifest_path, default_manifest)

seen_ids = {}
seen_pairs = {}
for idx, it in enumerate(items):
    item_id = str(it.get('id', '?'))
    pair = (str(it.get('guard', '')), str(it.get('fixture', '')))
    if item_id in seen_ids:
        structural.append(
            f"DUPLICATE_ID — id {item_id!r} appears at item #{seen_ids[item_id] + 1} and "
            f"#{idx + 1}. The id is the report identity; a repeat makes the PASS line "
            f"ambiguous about which registration was proven")
    else:
        seen_ids[item_id] = idx
    if pair in seen_pairs:
        structural.append(
            f"DUPLICATE_PROOF — (guard, fixture) pair {pair[0]} :: {pair[1]} is registered "
            f"twice (items #{seen_pairs[pair] + 1} and #{idx + 1}, ids "
            f"{items[seen_pairs[pair]].get('id')!r} / {item_id!r}). A count is not an "
            f"identity: re-adding an already-proven pair under a fresh id satisfies the "
            f"min_items floor while the deleted proof stays gone")
    else:
        seen_pairs[pair] = idx

unique_count = len(seen_pairs)
declared_min = doc.get('min_items')
if declared_min is not None:
    try:
        declared_min = int(declared_min)
    except (TypeError, ValueError):
        structural.append(f"MIN_ITEMS_NOT_A_NUMBER — min_items: {doc.get('min_items')!r}")
        declared_min = None
if is_default_manifest and declared_min is None:
    structural.append(
        "NO_MIN_ITEMS — the live manifest must declare `min_items: N` (the unique item "
        "count that may not be reduced). Without it the registry can shrink silently")
if declared_min is not None and unique_count < declared_min:
    structural.append(
        f"REGISTRY_SHRUNK — {unique_count} unique (guard, fixture) item(s) < declared "
        f"min_items={declared_min}. Items were removed; a smaller registry proves less "
        f"while still printing PASS")
if is_default_manifest and declared_min is not None and declared_min < LIVE_MIN_ITEMS:
    structural.append(
        f"MIN_ITEMS_FLOOR — declared min_items={declared_min} is below the guard-pinned "
        f"floor {LIVE_MIN_ITEMS} (LIVE_MIN_ITEMS may not be reduced). Lowering the gate "
        f"takes two coordinated edits, and only one of them has been made")

if structural:
    for msg in structural:
        print(f"fixture_kill_proof_guard: BLOCK — {msg}", file=sys.stderr)
    sys.exit(3)

for it in items:
    fields = [
        str(it.get('id', '?')),
        str(it.get('guard', '')),
        str(it.get('fixture', '')),
        str(it.get('fixture_arg', '')),
        str(it.get('anchor', '')),
        str(it.get('neuter', '')),
        str(it.get('fixture_arg2', '')),
        str(it.get('fixture2', '')),
    ]
    print('\t'.join(fields))
PY
ITEMS="$(python3 "$ITEMS_PY" "$MANIFEST" "$DEFAULT_MANIFEST")"
parse_rc=$?
rm -f "$ITEMS_PY"

if [ "$parse_rc" -eq 3 ]; then
    # registry integrity violation (duplicate id / duplicate proof / min_items floor).
    # Reported above by the parser; a BLOCK, not a tooling error.
    echo "" >&2
    echo "fixture_kill_proof_guard: FAIL — manifest registry integrity violated ($MANIFEST)." >&2
    exit 1
elif [ "$parse_rc" -ne 0 ]; then
    echo "fixture_kill_proof_guard: failed to parse manifest: $MANIFEST" >&2; exit 2
fi

# ZERO-ITEM VACUITY GUARD (2026-07-14): an empty item set must never report PASS —
# a broken parse or an emptied manifest would otherwise "prove" every fail fixture
# non-vacuous while proving nothing. The live manifest is required to be non-empty;
# only an explicitly-empty --manifest override is tolerated, and even then it reports
# a distinct non-PASS status.
if [ -z "$ITEMS" ]; then
    echo "fixture_kill_proof_guard: BLOCK — manifest yielded zero items ($MANIFEST). A zero-item run cannot prove non-vacuity." >&2
    exit 1
fi

FAIL=0
PROVEN=0

while IFS=$'\t' read -r item_id guard_rel fixture_rel fixture_arg anchor neuter fixture_arg2 fixture2_rel; do
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

    # ── optional second fixture input (fixture_arg2/fixture2) ─────────────────
    FIXTURE2_PATH=""
    if [ -n "$fixture_arg2" ]; then
        [ -n "$fixture2_rel" ] || {
            echo "fixture_kill_proof_guard: FAIL [$item_id] fixture_arg2 set but fixture2 is empty" >&2
            FAIL=1; continue; }
        FIXTURE2_PATH="$REPO_ROOT/$fixture2_rel"
        [ -e "$FIXTURE2_PATH" ] || {
            echo "fixture_kill_proof_guard: FAIL [$item_id] fixture2 not found: $FIXTURE2_PATH" >&2
            FAIL=1; continue; }
    fi

    # ── (0) neuter vocabulary validation (P2-14) ──────────────────────────────
    # neuter must match one of 6 allowlisted PIT-style operator shapes, and must
    # not contain a control-flow escape token (exit/return/kill) regardless of shape.
    NEUTER_VERDICT_PY="$(mktemp)"
    cat <<'PY' > "$NEUTER_VERDICT_PY"
import re, sys
anchor = sys.argv[1]
neuter = sys.argv[2]

# control-flow / process escape tokens are rejected unconditionally, any shape.
# CAUTION: this python body lives inside a bash command substitution — literal
# parens, backticks and apostrophes in THIS block break the bash scanner, so such
# characters are built via chr and comments avoid them.
OPEN = chr(40)      # opening paren
BT = chr(96)        # backtick
reject_patterns = [
    r'\bexit\b',
    r'\breturn\b',
    r'\bkill\b',
    r'\bexec\b',
    r'\beval\b',
    r'\bsource\b',
    r'\btrap\b',
    r'&\s*$',
    # command / process substitution: executable code smuggled INSIDE what would
    # otherwise look like a plain literal swap. 2026-07-14 리뷰 F1 재지적:
    # SECRET_A="$" + OPEN + "true..." passed the sentinel shape because quoted
    # literals are collapsed by str_skeleton — yet bash EXECUTES it.
    r'\$' + '\\' + OPEN,
    BT,
    '<\\' + OPEN,
    '>\\' + OPEN,
]
# A token is an escape only if the NEUTER INTRODUCES it — an anchor may legitimately
# contain process substitution, and preserving it while truncating the pipeline is
# surgical. Reject when the neuter occurrence count for a pattern EXCEEDS the anchor
# occurrence count.
for pat in reject_patterns:
    if len(re.findall(pat, neuter)) > len(re.findall(pat, anchor)):
        print(f"REJECT: neuter introduces control-flow escape token (pattern {pat!r}): {neuter!r}")
        sys.exit(0)

def skeleton(s):
    # collapse $var / ${var} references to a single placeholder so a pure
    # variable-identifier swap can be recognized independent of naming.
    return re.sub(r'\$\{?[A-Za-z_][A-Za-z0-9_]*\}?', 'VAR', s)

def str_skeleton(s):
    # collapse quoted string literals so a pure string-literal swap can be
    # recognized: everything OUTSIDE the quotes must be byte-identical to the
    # anchor. Blocks arbitrary code that merely name-drops a NEUTER_ token,
    # e.g. an exec/true one-liner carrying a NEUTER_ comment.
    s2 = re.sub(r"'[^']*'", 'STR', s)
    return re.sub(r'"[^"]*"', 'STR', s2)

shapes = []
if re.match(r'^\s*(true|:)\s*(#.*)?$', neuter):
    shapes.append('no-op')
if (re.search(r'NEUTER_[A-Z0-9_]+', neuter) and neuter != anchor
        and str_skeleton(neuter) == str_skeleton(anchor)):
    shapes.append('sentinel-string')
if neuter.strip() in ('False', 'false'):
    shapes.append('condition-constant')
# truncation: removing exactly one head clause must yield the anchor verbatim, AND that
# clause must be the LAST stage of its pipeline — what follows may only be whitespace or
# closers. A mid-pipeline head that is followed by another command re-shapes the
# pipeline instead of truncating it, and is rejected. 2026-07-14 리뷰 F1 재지적.
CLOSERS = set(' \t' + chr(41) + chr(39) + chr(34))
for m in re.finditer(r'\s*\|\s*head\s+-?\d+', neuter):
    if neuter[:m.start()] + neuter[m.end():] != anchor:
        continue
    if all(ch in CLOSERS for ch in neuter[m.end():]):
        shapes.append('truncation')
        break
if neuter.strip() == '*)':
    shapes.append('over-broad-glob')
if neuter != anchor and skeleton(neuter) == skeleton(anchor):
    shapes.append('variable-substitution')

if shapes:
    print("PASS: " + ",".join(shapes))
else:
    print("UNKNOWN: neuter does not match any allowlisted PIT-style operator shape: " + repr(neuter))
PY
    neuter_verdict="$(python3 "$NEUTER_VERDICT_PY" "$anchor" "$neuter")"
    rm -f "$NEUTER_VERDICT_PY"
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
    ANCHOR_COUNT_PY="$(mktemp)"
    cat <<'PY' > "$ANCHOR_COUNT_PY"
import sys
src = open(sys.argv[1], encoding='utf-8').read()
print(src.count(sys.argv[2]))
PY
    anchor_count="$(python3 "$ANCHOR_COUNT_PY" "$GUARD_PATH" "$anchor")"
    rm -f "$ANCHOR_COUNT_PY"
    if [ "$anchor_count" -ne 1 ]; then
        echo "fixture_kill_proof_guard: FAIL [$item_id] anchor appears ${anchor_count} time(s) in $guard_rel" >&2
        echo "  (expected exactly 1 — manifest is stale; update anchor to match current guard source)" >&2
        FAIL=1; continue
    fi

    # ── (2) original guard on fixture → expect exit 1 ────────────────────────
    if [ -n "$fixture_arg" ]; then
        if [ -n "$fixture_arg2" ]; then
            bash "$GUARD_PATH" "$fixture_arg" "$FIXTURE_PATH" "$fixture_arg2" "$FIXTURE2_PATH" >/dev/null 2>&1
        else
            bash "$GUARD_PATH" "$fixture_arg" "$FIXTURE_PATH" >/dev/null 2>&1
        fi
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
    # The neutered copy runs from a BARE TEMP PATH, so any helper a guard sources by
    # repo-relative path (practices/scripts/lib/release_anchor.sh, added 2026-07-30 for the
    # P1-X/P1-Y anchor authentication) cannot resolve from there. Naming it explicitly keeps
    # this harness measuring what it is supposed to measure — whether the ANCHOR LOGIC is
    # load-bearing — instead of reporting "neuter broke the guard" for a missing dependency.
    # The guards consult this variable ONLY when their committed helper path is absent, so it
    # is inert on every real tree and cannot substitute a weakened helper into a live run.
    export AX_RELEASE_ANCHOR_LIB="$REPO_ROOT/practices/scripts/lib/release_anchor.sh"
    # Same affordance for the ax_markers.py-importing guards (install_artifact_
    # extractability_guard.sh [112], cross_artifact_contract_guard.sh [113],
    # downstream_release_recency_guard.sh [114], added GH #92): all three resolve
    # practices/scripts/lib/ax_markers.py, and [112] additionally resolves the consumer-e2e
    # fixture's ax.config.json AND (P2-109) the committed ax.config.schema.json, via REPO_ROOT,
    # which collapses to "/" from a bare temp path. Consulted ONLY when the committed path is
    # absent AND the root is not a git work tree — see each guard's own comment at the point of
    # use.
    export AX_MARKERS_LIB_DIR="$REPO_ROOT/practices/scripts/lib"
    export AX_CONSUMER_E2E_CONFIG="$REPO_ROOT/practices/evals/fixtures/consumer-e2e/project/ax.config.json"
    export AX_CONFIG_SCHEMA="$REPO_ROOT/practices-react/eslint-plugin-ax/schemas/ax.config.schema.json"
    if [ -n "$fixture_arg" ]; then
        if [ -n "$fixture_arg2" ]; then
            bash "$TMP_GUARD" "$fixture_arg" "$FIXTURE_PATH" "$fixture_arg2" "$FIXTURE2_PATH" >/dev/null 2>&1
        else
            bash "$TMP_GUARD" "$fixture_arg" "$FIXTURE_PATH" >/dev/null 2>&1
        fi
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

# Print the floor alongside the proven count. A ratchet that is never displayed is a ratchet
# nobody notices moving; the census guards in this suite print their floors for the same
# reason. Display only — the enforcing comparison already happened in the parser above.
DECLARED_MIN_PY="$(mktemp)"
cat <<'PY' > "$DECLARED_MIN_PY"
import sys, yaml
doc = yaml.safe_load(open(sys.argv[1], encoding='utf-8')) or {}
print(doc.get('min_items', 'none'))
PY
DECLARED_MIN="$(python3 "$DECLARED_MIN_PY" "$MANIFEST" 2>/dev/null)"
rm -f "$DECLARED_MIN_PY"
echo "fixture_kill_proof_guard: PASS — $PROVEN item(s) all non-vacuous (every fail fixture is killed by its targeted neuter) [min_items floor: ${DECLARED_MIN:-none}]"
exit 0
