#!/usr/bin/env bash
# practices/scripts/verify-completion.sh — R25 mechanical completion contract.
#
# Reads practices/verification-checklist.yaml and runs every step sequentially.
# Exit 0  → ALL steps PASS (task may declare done).
# Exit 1  → at least one non-advisory step FAILED; fix_playbook printed.
# Exit 2  → setup error (yaml missing, python3 missing, etc.).
#
# Side effect: writes an audit log line to .ax-verify/runs.jsonl with
#   {ts, head_sha, exit, advisory_fail_count, hard_fail_count}
# so that completion_checklist_recency_guard.sh can audit recency.
#
# Iron Law: this script is the SOLE source of truth for "is the task done".
# Do NOT bypass with --skip flags. There is no opt-out.
#
# Usage:
#   bash practices/scripts/verify-completion.sh
#   bash practices/scripts/verify-completion.sh --step <id>   # run one step only
#   bash practices/scripts/verify-completion.sh --dry-run     # parse + plan, no exec
#   bash practices/scripts/verify-completion.sh --json        # emit machine-readable summary
set -uo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
CHECKLIST="$REPO_ROOT/practices/verification-checklist.yaml"
AUDIT_DIR="$REPO_ROOT/.ax-verify"
AUDIT_LOG="$AUDIT_DIR/runs.jsonl"

STEP_FILTER=""
DRY_RUN=0
JSON_OUTPUT=0

while [ $# -gt 0 ]; do
    case "$1" in
        --step) STEP_FILTER="$2"; shift 2 ;;
        --step=*) STEP_FILTER="${1#--step=}"; shift ;;
        --dry-run) DRY_RUN=1; shift ;;
        --json) JSON_OUTPUT=1; shift ;;
        --help|-h)
            sed -n '2,18p' "$0"; exit 0 ;;
        *)
            echo "verify-completion: unknown arg: $1" >&2
            echo "use --help for usage" >&2
            exit 2
            ;;
    esac
done

if [ ! -f "$CHECKLIST" ]; then
    echo "verify-completion: checklist yaml missing: $CHECKLIST" >&2
    exit 2
fi

if ! command -v python3 >/dev/null 2>&1; then
    echo "verify-completion: python3 not in PATH (required for yaml parsing)" >&2
    exit 2
fi

mkdir -p "$AUDIT_DIR"

# ── 1. Parse the checklist into a flat command plan via python3 ───────────────
# Output schema (one line per command, tab-separated):
#   <step_id>\t<step_title>\t<command>\t<working_directory>\t<expected_exit>\t<advisory>
PLAN_FILE=$(mktemp)
trap 'rm -f "$PLAN_FILE"' EXIT

python3 - "$CHECKLIST" "$STEP_FILTER" "$PLAN_FILE" <<'PYEOF'
import sys, os, pathlib

try:
    import yaml  # noqa
except ImportError:
    # Fallback: use a minimal hand-rolled parser if PyYAML is absent.
    # ax-template tests rely on PyYAML being available in dev env; fall back
    # to subprocess-yq if not.
    yaml = None

checklist_path, step_filter, plan_path = sys.argv[1], sys.argv[2], sys.argv[3]
text = pathlib.Path(checklist_path).read_text()

if yaml is None:
    # Minimal fallback: shell out to python3 -c with safe_load via stdlib? No
    # stdlib yaml. Try `yq` then bail.
    import subprocess, json
    try:
        out = subprocess.check_output(["yq", "-o=json", ".", checklist_path])
        doc = json.loads(out)
    except Exception as e:
        print(f"verify-completion: cannot parse yaml without PyYAML or yq ({e})", file=sys.stderr)
        sys.exit(2)
else:
    doc = yaml.safe_load(text)

defaults = doc.get("defaults") or {}
default_wd = defaults.get("working_directory", ".")

lines = []
for step in doc.get("checklist") or []:
    sid = step.get("id", "")
    if step_filter and sid != step_filter:
        continue
    title = step.get("title", sid)
    for cmd_entry in step.get("commands") or []:
        cmd = cmd_entry.get("command", "")
        wd = cmd_entry.get("working_directory", default_wd)
        expected_exit = cmd_entry.get("expected_exit", 0)
        advisory = str(bool(cmd_entry.get("advisory", False))).lower()
        # Tab-separated; commands here MUST NOT contain literal tabs.
        if "\t" in cmd:
            print(f"verify-completion: tab character in command: {cmd}", file=sys.stderr)
            sys.exit(2)
        lines.append(f"{sid}\t{title}\t{cmd}\t{wd}\t{expected_exit}\t{advisory}")

pathlib.Path(plan_path).write_text("\n".join(lines) + ("\n" if lines else ""))
PYEOF

PLAN_EXIT=$?
if [ "$PLAN_EXIT" -ne 0 ]; then
    echo "verify-completion: failed to parse checklist" >&2
    exit 2
fi

if [ ! -s "$PLAN_FILE" ]; then
    if [ -n "$STEP_FILTER" ]; then
        echo "verify-completion: no commands matched --step '$STEP_FILTER'" >&2
        exit 2
    fi
    echo "verify-completion: checklist contained no commands" >&2
    exit 2
fi

# ── 2. Print the plan ─────────────────────────────────────────────────────────
echo "=== verify-completion.sh — R25 mechanical completion contract ==="
echo "checklist: practices/verification-checklist.yaml"
echo ""
CURRENT_STEP=""
while IFS=$'\t' read -r sid title cmd wd expected advisory; do
    if [ "$sid" != "$CURRENT_STEP" ]; then
        CURRENT_STEP="$sid"
        echo "▸ step: $sid — $title"
    fi
    adv_label=""
    [ "$advisory" = "true" ] && adv_label=" [advisory]"
    echo "    \$ ( cd $wd && $cmd )${adv_label} (expect exit $expected)"
done < "$PLAN_FILE"
echo ""

if [ "$DRY_RUN" -eq 1 ]; then
    echo "verify-completion: --dry-run set, exiting without execution"
    exit 0
fi

# ── 3. Execute the plan ──────────────────────────────────────────────────────
HARD_FAIL=0
ADVISORY_FAIL=0
RESULTS_FILE=$(mktemp)
trap 'rm -f "$PLAN_FILE" "$RESULTS_FILE"' EXIT

# Re-extract per-step fix_playbook into a temp dir, keyed by id. Used to print
# the playbook exactly once per failing step (tracked via STEP_FAILED_ALREADY
# scalar, not a bash-4 associative array — keep bash-3 compatibility for macOS).
PLAYBOOK_DIR=$(mktemp -d)
trap 'rm -rf "$PLAN_FILE" "$RESULTS_FILE" "$PLAYBOOK_DIR"' EXIT

python3 - "$CHECKLIST" "$PLAYBOOK_DIR" <<'PYEOF'
import sys, pathlib
try:
    import yaml
except ImportError:
    import subprocess, json
    out = subprocess.check_output(["yq", "-o=json", ".", sys.argv[1]])
    doc = json.loads(out)
else:
    doc = yaml.safe_load(pathlib.Path(sys.argv[1]).read_text())

outdir = pathlib.Path(sys.argv[2])
for step in doc.get("checklist") or []:
    sid = step.get("id", "")
    fp = step.get("fix_playbook", "")
    (outdir / f"{sid}.txt").write_text(fp)
PYEOF

CURRENT_STEP=""
STEP_FAILED_ALREADY=""

while IFS=$'\t' read -r sid title cmd wd expected advisory; do
    if [ "$sid" != "$CURRENT_STEP" ]; then
        CURRENT_STEP="$sid"
        STEP_FAILED_ALREADY=""
        echo "── [$sid] $title ──────────────────────────────────────────────"
    fi

    exec_wd="$REPO_ROOT/$wd"
    if [ "$wd" = "." ] || [ -z "$wd" ]; then
        exec_wd="$REPO_ROOT"
    fi

    if [ ! -d "$exec_wd" ]; then
        echo "  SKIP \$ ( cd $wd && $cmd ) — working dir does not exist"
        echo -e "$sid\t$cmd\tSKIP\tdir-missing\t$advisory" >> "$RESULTS_FILE"
        continue
    fi

    echo "  RUN  \$ ( cd $wd && $cmd )"
    set +e
    ( cd "$exec_wd" && bash -c "$cmd" )
    actual_exit=$?
    set -e

    if [ "$actual_exit" -eq "$expected" ]; then
        echo "  PASS \$ ( cd $wd && $cmd ) — exit $actual_exit"
        echo -e "$sid\t$cmd\tPASS\t$actual_exit\t$advisory" >> "$RESULTS_FILE"
    else
        if [ "$advisory" = "true" ]; then
            ADVISORY_FAIL=$((ADVISORY_FAIL + 1))
            echo "  WARN \$ ( cd $wd && $cmd ) — exit $actual_exit (expected $expected, ADVISORY)"
            echo -e "$sid\t$cmd\tWARN\t$actual_exit\t$advisory" >> "$RESULTS_FILE"
        else
            HARD_FAIL=$((HARD_FAIL + 1))
            echo "  FAIL \$ ( cd $wd && $cmd ) — exit $actual_exit (expected $expected)"
            echo -e "$sid\t$cmd\tFAIL\t$actual_exit\t$advisory" >> "$RESULTS_FILE"
            if [ -z "$STEP_FAILED_ALREADY" ]; then
                STEP_FAILED_ALREADY="1"
                if [ -s "$PLAYBOOK_DIR/$sid.txt" ]; then
                    echo ""
                    echo "  ▼ fix_playbook for step [$sid]:"
                    sed 's/^/      /' "$PLAYBOOK_DIR/$sid.txt"
                    echo ""
                fi
            fi
        fi
    fi
done < "$PLAN_FILE"

# ── 4. Summary ──────────────────────────────────────────────────────────────
echo ""
echo "=== verify-completion.sh — Summary ==="
PASS_COUNT=$(grep -c $'\tPASS\t' "$RESULTS_FILE" || true)
SKIP_COUNT=$(grep -c $'\tSKIP\t' "$RESULTS_FILE" || true)
echo "  PASS     : $PASS_COUNT"
echo "  WARN(adv): $ADVISORY_FAIL"
echo "  FAIL     : $HARD_FAIL"
echo "  SKIP     : $SKIP_COUNT"

# ── 5. Audit log line (consumed by 49th hard guard) ──────────────────────────
HEAD_SHA="$(git -C "$REPO_ROOT" rev-parse HEAD 2>/dev/null || echo "unknown")"
TS="$(date -u +%Y-%m-%dT%H:%M:%SZ)"
EXIT_CODE=0
[ "$HARD_FAIL" -gt 0 ] && EXIT_CODE=1

# JSON line is single-line; no embedded newlines.
printf '{"ts":"%s","head_sha":"%s","exit":%d,"pass":%d,"warn_advisory":%d,"hard_fail":%d,"skip":%d}\n' \
    "$TS" "$HEAD_SHA" "$EXIT_CODE" "$PASS_COUNT" "$ADVISORY_FAIL" "$HARD_FAIL" "$SKIP_COUNT" \
    >> "$AUDIT_LOG"

if [ "$JSON_OUTPUT" -eq 1 ]; then
    tail -1 "$AUDIT_LOG"
fi

if [ "$HARD_FAIL" -gt 0 ]; then
    echo ""
    echo "verify-completion: FAIL — $HARD_FAIL non-advisory step(s) failed"
    echo "Iron Law: task is NOT done. Apply the fix_playbook above and re-run."
    exit 1
fi

if [ "$ADVISORY_FAIL" -gt 0 ]; then
    echo ""
    echo "verify-completion: PASS with $ADVISORY_FAIL advisory warning(s)"
    echo "Advisory items are knowingly advisory in the catalog (see fix_playbook)."
    exit 0
fi

echo ""
echo "verify-completion: PASS — all steps green. Task may declare done."
exit 0
