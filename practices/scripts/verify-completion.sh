#!/usr/bin/env bash
# practices/scripts/verify-completion.sh — R25/R27/R28 mechanical completion contract.
#
# Reads practices/verification-checklist.yaml and runs every step sequentially.
# Exit 0  → ALL steps PASS (task may declare done).
# Exit 1  → at least one non-advisory step FAILED; fix_playbook printed.
# Exit 2  → setup error (yaml missing, python3 missing, etc.).
#
# R28 surface upgrades (additive, no schema change):
#   • per-domain-tests step COLLAPSES its 15 mandatory ./gradlew testXxx tasks
#     into a SINGLE warm-daemon invocation (`--continue` so one fail does not
#     short-circuit). 2 advisory tasks (testIntegration, testPortability) run
#     afterwards individually so their RED is isolated to themselves.
#   • Real-time streaming: gradle `--console=plain` injected automatically so
#     `> Task :testCrud` lines surface immediately (R27 fix, retained).
#   • Per-step timeout (yaml `timeout_seconds`) implemented via background
#     watchdog with SIGTERM → 30s grace → SIGKILL. Race-free: watchdog killed
#     atomically once the command exits.
#   • `--resume` reads .ax-verify/last_run.jsonl and skips steps whose
#     {step_id, head_sha} matches and were PASS. HEAD drift wipes resume.
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
#   bash practices/scripts/verify-completion.sh --resume      # skip prior PASS steps (same HEAD)
#   bash practices/scripts/verify-completion.sh --no-collapse # disable per-domain collapse
set -uo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
CHECKLIST="$REPO_ROOT/practices/verification-checklist.yaml"
AUDIT_DIR="$REPO_ROOT/.ax-verify"
AUDIT_LOG="$AUDIT_DIR/runs.jsonl"
RESUME_LOG="$AUDIT_DIR/last_run.jsonl"
COLLAPSE_HELPER="$SCRIPT_DIR/_collapse_plan.py"

STEP_FILTER=""
DRY_RUN=0
JSON_OUTPUT=0
RESUME=0
COLLAPSE=1

while [ $# -gt 0 ]; do
    case "$1" in
        --step) STEP_FILTER="$2"; shift 2 ;;
        --step=*) STEP_FILTER="${1#--step=}"; shift ;;
        --dry-run) DRY_RUN=1; shift ;;
        --json) JSON_OUTPUT=1; shift ;;
        --resume) RESUME=1; shift ;;
        --no-collapse) COLLAPSE=0; shift ;;
        --help|-h)
            sed -n '2,30p' "$0"; exit 0 ;;
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

CURRENT_HEAD="$(git -C "$REPO_ROOT" rev-parse HEAD 2>/dev/null || echo "unknown")"

# ── 1. Parse the checklist into a flat command plan via python3 ───────────────
# Output schema (one line per command, tab-separated):
#   <step_id>\t<step_title>\t<command>\t<working_directory>\t<expected_exit>\t<advisory>\t<timeout_seconds>
PLAN_FILE=$(mktemp)
RESULTS_FILE=$(mktemp)
PLAYBOOK_DIR=$(mktemp -d)
RESUME_TMP=$(mktemp)
cleanup() {
    # RESUME_NEW is defined later (line ~226); guard with :- so an early-exit
    # trap before it is set does not trip `set -u`. The .failfast sidecar is
    # written next to PLAN_FILE by the python emitter — remove it here too.
    rm -f "$PLAN_FILE" "$PLAN_FILE.failfast" "$RESULTS_FILE" "$RESUME_TMP" "${RESUME_NEW:-}"
    rm -rf "$PLAYBOOK_DIR"
}
trap cleanup EXIT

python3 - "$CHECKLIST" "$STEP_FILTER" "$PLAN_FILE" "$PLAYBOOK_DIR" <<'PYEOF'
import sys, pathlib

try:
    import yaml
except ImportError:
    yaml = None

checklist_path, step_filter, plan_path, playbook_dir = sys.argv[1:5]
text = pathlib.Path(checklist_path).read_text()

if yaml is None:
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
default_timeout = int(defaults.get("timeout_seconds", 900))

lines = []
fail_fast_sids = []
playbook_out = pathlib.Path(playbook_dir)
for step in doc.get("checklist") or []:
    sid = step.get("id", "")
    if step_filter and sid != step_filter:
        continue
    if step.get("fail_fast", False):
        fail_fast_sids.append(sid)
    title = step.get("title", sid)
    step_timeout = int(step.get("timeout_seconds", default_timeout))
    (playbook_out / f"{sid}.txt").write_text(step.get("fix_playbook", ""))
    for cmd_entry in step.get("commands") or []:
        cmd = cmd_entry.get("command", "")
        wd = cmd_entry.get("working_directory", default_wd)
        expected_exit = cmd_entry.get("expected_exit", 0)
        advisory = str(bool(cmd_entry.get("advisory", False))).lower()
        cmd_timeout = int(cmd_entry.get("timeout_seconds", step_timeout))
        if "\t" in cmd:
            print(f"verify-completion: tab character in command: {cmd}", file=sys.stderr)
            sys.exit(2)
        lines.append(f"{sid}\t{title}\t{cmd}\t{wd}\t{expected_exit}\t{advisory}\t{cmd_timeout}")

pathlib.Path(plan_path).write_text("\n".join(lines) + ("\n" if lines else ""))
# Sidecar: step ids marked `fail_fast: true` — a HARD_FAIL in one of these short-circuits
# the remaining steps (so a structural pre-gate FAIL does not still pay the ~18-min per-domain suite).
pathlib.Path(plan_path + ".failfast").write_text("\n".join(fail_fast_sids) + ("\n" if fail_fast_sids else ""))
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

# Steps marked `fail_fast: true` short-circuit the run on a HARD_FAIL (e.g. backend-build,
# structural-pregate) — so a compile/structural break does not still pay the ~18-min per-domain suite.
FAIL_FAST_SIDS=$(cat "$PLAN_FILE.failfast" 2>/dev/null || true)

# ── 1b. Resume preload (atomic): map step_id → PASS|FAIL only when HEAD matches.
declare_resume_pass() {
    grep -F "$1" "$RESUME_TMP" >/dev/null 2>&1
}
if [ "$RESUME" -eq 1 ] && [ -f "$RESUME_LOG" ]; then
    python3 - "$RESUME_LOG" "$CURRENT_HEAD" "$RESUME_TMP" <<'PYEOF'
import sys, pathlib, json
resume_path, head, out_path = sys.argv[1], sys.argv[2], sys.argv[3]
keep = []
for line in pathlib.Path(resume_path).read_text().splitlines():
    line = line.strip()
    if not line:
        continue
    try:
        rec = json.loads(line)
    except Exception:
        continue
    if rec.get("head_sha") != head:
        continue
    if rec.get("status") != "PASS":
        continue
    sid = rec.get("step_id")
    if sid:
        keep.append(sid)
pathlib.Path(out_path).write_text("\n".join(sorted(set(keep))) + ("\n" if keep else ""))
PYEOF
fi

# ── 2. Print the plan ─────────────────────────────────────────────────────────
echo "=== verify-completion.sh — R25/R28 mechanical completion contract ==="
echo "checklist: practices/verification-checklist.yaml"
echo "head_sha : $CURRENT_HEAD"
[ "$RESUME" -eq 1 ] && echo "resume   : enabled (skip PASS steps with matching head_sha)"
[ "$COLLAPSE" -eq 0 ] && echo "collapse : disabled (per-domain tasks will run separately)"
echo ""
CURRENT_STEP=""
while IFS=$'\t' read -r sid title cmd wd expected advisory timeout_s; do
    if [ "$sid" != "$CURRENT_STEP" ]; then
        CURRENT_STEP="$sid"
        echo "▸ step: $sid — $title  (timeout ${timeout_s}s)"
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

# ── 3. Atomic resume-log writer ──────────────────────────────────────────────
# .ax-verify/last_run.jsonl is rewritten atomically every step so a Ctrl-C or
# SIGKILL never leaves a half-line. Pattern: write tmp → mv -f.
RESUME_NEW=$(mktemp)
# Seed RESUME_NEW with any PASS lines for OTHER head_shas we'd want to overwrite.
# Simpler: start fresh each run; resume only consults prior file. So no seed.
: > "$RESUME_NEW"

emit_resume() {
    local sid="$1" status="$2"
    local ts
    ts="$(date -u +%Y-%m-%dT%H:%M:%SZ)"
    printf '{"step_id":"%s","status":"%s","ts":"%s","head_sha":"%s"}\n' \
        "$sid" "$status" "$ts" "$CURRENT_HEAD" >> "$RESUME_NEW"
    # Atomic publish: copy to a sibling tmp then rename. mv on same filesystem
    # is atomic per POSIX. Crash mid-rename ⇒ caller sees either old or new.
    cp "$RESUME_NEW" "$RESUME_LOG.tmp.$$"
    mv -f "$RESUME_LOG.tmp.$$" "$RESUME_LOG"
}

# ── 4. Watchdog wrapper: run command with timeout, line-buffered streaming ──
# We background the command (so its stdout/stderr stay attached to OUR fds —
# stream live), then background a watchdog that SIGTERMs on timeout and SIGKILL
# after a 30s grace period. Watchdog is killed atomically once the command
# exits. No `timeout` binary dependency.
run_with_timeout() {
    local exec_wd="$1" exec_cmd="$2" timeout_s="$3"

    # Background the user command. Critical: redirect stdin from /dev/null so
    # it does NOT inherit the parent's stdin (which, when the caller is e.g.
    # `while ... done < PLAN_FILE`, would be a file descriptor that keeps the
    # subshell's read-end open and breaks bash 3.2's wait/SIGCHLD coupling —
    # observed as "gradle exits, BUILD SUCCESSFUL printed, but parent never
    # advances past `wait $cmd_pid`"). We deliberately do NOT use `set -m`:
    # job control under non-interactive shells corrupts wait() semantics here.
    ( cd "$exec_wd" && bash -c "$exec_cmd" ) </dev/null &
    local cmd_pid=$!

    # Watchdog: a background sleeper that signals the child if it overruns,
    # and exits silently if killed by the parent on natural completion.
    (
        trap 'exit 0' TERM
        sleep "$timeout_s"
        if kill -0 "$cmd_pid" 2>/dev/null; then
            echo "" >&2
            echo "  [watchdog] step exceeded ${timeout_s}s — sending SIGTERM to pid $cmd_pid" >&2
            kill -TERM "$cmd_pid" 2>/dev/null
            # 30s graceful shutdown window.
            local i=0
            while [ "$i" -lt 30 ] && kill -0 "$cmd_pid" 2>/dev/null; do
                sleep 1
                i=$((i + 1))
            done
            if kill -0 "$cmd_pid" 2>/dev/null; then
                echo "  [watchdog] grace expired — sending SIGKILL to pid $cmd_pid" >&2
                kill -KILL "$cmd_pid" 2>/dev/null
            fi
        fi
    ) &
    local wd_pid=$!
    # Disown the watchdog from the job table so bash never prints
    # "Killed: 9" on its termination. `disown` is bash-3.2 safe.
    disown "$wd_pid" 2>/dev/null || true

    # Wait for the user command. `wait <pid>` propagates the child's exit code.
    wait "$cmd_pid" 2>/dev/null
    local actual_exit=$?

    # Tear down the watchdog. We DELIBERATELY do not `wait $wd_pid` afterwards:
    # bash 3.2 (macOS default) can hang on `wait` for a backgrounded subshell
    # that we just signalled. Instead, brief polling reap + SIGKILL fallback.
    kill -TERM "$wd_pid" 2>/dev/null
    local i=0
    while [ "$i" -lt 5 ]; do
        kill -0 "$wd_pid" 2>/dev/null || break
        sleep 0.1 2>/dev/null || sleep 1
        i=$((i + 1))
    done
    kill -KILL "$wd_pid" 2>/dev/null

    return "$actual_exit"
}

# ── 5. Inject `--console=plain` into gradle invocations (R27 streaming fix) ──
inject_gradle_plain_console() {
    local in="$1"
    if [[ "$in" == *"./gradlew"* ]] && [[ "$in" != *"--console="* ]]; then
        echo "${in/.\/gradlew/.\/gradlew --console=plain}"
    else
        echo "$in"
    fi
}

# ── 6. Execute the plan ──────────────────────────────────────────────────────
HARD_FAIL=0
ADVISORY_FAIL=0
SKIP_RESUME_COUNT=0

# Iterate by step. For each step:
#   1. If --resume and step is in resume cache, SKIP and emit PASS.
#   2. If COLLAPSE && step has multiple gradle ./gradlew commands in the same wd,
#      collapse non-advisory ones into a single `./gradlew taskA taskB --continue`
#      and run advisory commands separately after.
#   3. Else run each command sequentially with watchdog.

# Group plan by step_id while preserving order.
STEP_ORDER=$(awk -F'\t' '!seen[$1]++ { print $1 }' "$PLAN_FILE")

for sid in $STEP_ORDER; do
    # Step header.
    title=$(awk -F'\t' -v s="$sid" '$1==s { print $2; exit }' "$PLAN_FILE")
    echo "── [$sid] $title ──────────────────────────────────────────────"

    # Resume short-circuit.
    if [ "$RESUME" -eq 1 ] && [ -s "$RESUME_TMP" ] && grep -Fxq "$sid" "$RESUME_TMP"; then
        echo "  SKIP (resume): step PASS for head $CURRENT_HEAD already recorded"
        echo -e "$sid\tRESUME-SKIP\tPASS\t0\tfalse" >> "$RESULTS_FILE"
        SKIP_RESUME_COUNT=$((SKIP_RESUME_COUNT + 1))
        emit_resume "$sid" "PASS"
        continue
    fi

    # Try collapse for this step.
    COLLAPSED_PLAN=$(mktemp)
    if [ "$COLLAPSE" -eq 1 ]; then
        python3 "$COLLAPSE_HELPER" "$PLAN_FILE" "$sid" > "$COLLAPSED_PLAN" 2>/dev/null || true
    fi

    STEP_HARD_FAIL_BEFORE=$HARD_FAIL

    if [ -s "$COLLAPSED_PLAN" ]; then
        # ── Collapsed path: one warm gradle daemon for all non-advisory tasks ─
        # First line: <wd>\t<collapsed_cmd>\t<advisory_count>
        # Subsequent lines: <wd>\t<advisory_cmd>\tadvisory
        first_line=$(head -n 1 "$COLLAPSED_PLAN")
        coll_wd=$(echo "$first_line" | cut -f1)
        coll_cmd=$(echo "$first_line" | cut -f2)

        if [ "$coll_wd" = "." ] || [ -z "$coll_wd" ]; then
            exec_wd="$REPO_ROOT"
        else
            exec_wd="$REPO_ROOT/$coll_wd"
        fi

        # Use the timeout of the step (max across commands). Re-read first cmd row.
        step_timeout=$(awk -F'\t' -v s="$sid" '$1==s { if($7+0 > max) max=$7+0 } END { print (max?max:900) }' "$PLAN_FILE")

        if [ -n "$coll_cmd" ]; then
            # Append --continue if not already there.
            exec_cmd="$coll_cmd"
            [[ "$exec_cmd" != *"--continue"* ]] && exec_cmd="$exec_cmd --continue"
            exec_cmd=$(inject_gradle_plain_console "$exec_cmd")

            echo "  RUN (collapsed)  \$ ( cd $coll_wd && $exec_cmd )"
            run_with_timeout "$exec_wd" "$exec_cmd" "$step_timeout"
            actual_exit=$?

            if [ "$actual_exit" -eq 0 ]; then
                echo "  PASS (collapsed) — exit 0"
                echo -e "$sid\t$exec_cmd\tPASS\t0\tfalse" >> "$RESULTS_FILE"
            else
                HARD_FAIL=$((HARD_FAIL + 1))
                echo "  FAIL (collapsed) — exit $actual_exit"
                echo -e "$sid\t$exec_cmd\tFAIL\t$actual_exit\tfalse" >> "$RESULTS_FILE"
            fi
        fi

        # Advisory commands run individually so each RED stays scoped.
        tail -n +2 "$COLLAPSED_PLAN" | while IFS=$'\t' read -r adv_wd adv_cmd _adv_tag; do
            [ -z "$adv_cmd" ] && continue
            if [ "$adv_wd" = "." ] || [ -z "$adv_wd" ]; then
                exec_wd_adv="$REPO_ROOT"
            else
                exec_wd_adv="$REPO_ROOT/$adv_wd"
            fi
            exec_cmd_adv=$(inject_gradle_plain_console "$adv_cmd")
            echo "  RUN (advisory)   \$ ( cd $adv_wd && $exec_cmd_adv )"
            run_with_timeout "$exec_wd_adv" "$exec_cmd_adv" "$step_timeout"
            adv_exit=$?
            if [ "$adv_exit" -eq 0 ]; then
                echo "  PASS (advisory)  — exit 0"
                echo -e "$sid\t$exec_cmd_adv\tPASS\t0\ttrue" >> "$RESULTS_FILE"
            else
                # advisory FAIL is a WARN; do NOT bump HARD_FAIL. We can't
                # mutate ADVISORY_FAIL from subshell, so log a sentinel line.
                echo "  WARN (advisory)  — exit $adv_exit (advisory, continuing)"
                echo -e "$sid\t$exec_cmd_adv\tWARN\t$adv_exit\ttrue" >> "$RESULTS_FILE"
            fi
        done

        # Recompute ADVISORY_FAIL from the results file for THIS step.
        step_adv_fails=$(awk -F'\t' -v s="$sid" '$1==s && $3=="WARN" { c++ } END { print c+0 }' "$RESULTS_FILE")
        # Subtract any previously-counted advisory fails for prior steps to avoid double-count.
        ADVISORY_FAIL=$(awk -F'\t' '$3=="WARN" { c++ } END { print c+0 }' "$RESULTS_FILE")

        # Determine PASS/FAIL of step.
        if [ "$HARD_FAIL" -gt "$STEP_HARD_FAIL_BEFORE" ]; then
            emit_resume "$sid" "FAIL"
            if [ -s "$PLAYBOOK_DIR/$sid.txt" ]; then
                echo ""
                echo "  ▼ fix_playbook for step [$sid]:"
                sed 's/^/      /' "$PLAYBOOK_DIR/$sid.txt"
                echo ""
            fi
        else
            emit_resume "$sid" "PASS"
        fi
    else
        # ── Sequential path: run each command with watchdog ──────────────────
        STEP_FAILED_ALREADY=""
        while IFS=$'\t' read -r p_sid _p_title cmd wd expected advisory timeout_s; do
            [ "$p_sid" = "$sid" ] || continue
            if [ "$wd" = "." ] || [ -z "$wd" ]; then
                exec_wd="$REPO_ROOT"
            else
                exec_wd="$REPO_ROOT/$wd"
            fi

            if [ ! -d "$exec_wd" ]; then
                echo "  SKIP \$ ( cd $wd && $cmd ) — working dir does not exist"
                echo -e "$sid\t$cmd\tSKIP\tdir-missing\t$advisory" >> "$RESULTS_FILE"
                continue
            fi

            exec_cmd=$(inject_gradle_plain_console "$cmd")
            echo "  RUN  \$ ( cd $wd && $cmd )  (timeout ${timeout_s}s)"
            run_with_timeout "$exec_wd" "$exec_cmd" "$timeout_s"
            actual_exit=$?

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

        if [ "$HARD_FAIL" -gt "$STEP_HARD_FAIL_BEFORE" ]; then
            emit_resume "$sid" "FAIL"
        else
            emit_resume "$sid" "PASS"
        fi
    fi

    rm -f "$COLLAPSED_PLAN"

    # fail-fast: a HARD_FAIL in a `fail_fast: true` step short-circuits the remaining steps.
    if [ "$HARD_FAIL" -gt "$STEP_HARD_FAIL_BEFORE" ] && printf '%s\n' "$FAIL_FAST_SIDS" | grep -qxF "$sid"; then
        echo ""
        echo "  ⛔ fail-fast: step [$sid] FAILED — short-circuiting the remaining steps"
        echo "     (a fail_fast pre-gate failed; the heavy downstream steps are skipped — fix + re-run)."
        break
    fi
done

# ── 7. Summary ──────────────────────────────────────────────────────────────
echo ""
echo "=== verify-completion.sh — Summary ==="
PASS_COUNT=$(grep -c $'\tPASS\t' "$RESULTS_FILE" 2>/dev/null || true)
SKIP_COUNT=$(grep -c $'\tSKIP\t' "$RESULTS_FILE" 2>/dev/null || true)
PASS_COUNT=${PASS_COUNT:-0}
SKIP_COUNT=${SKIP_COUNT:-0}
echo "  PASS         : $PASS_COUNT"
echo "  WARN(advisory): $ADVISORY_FAIL"
echo "  FAIL         : $HARD_FAIL"
echo "  SKIP         : $SKIP_COUNT"
[ "$SKIP_RESUME_COUNT" -gt 0 ] && echo "  RESUME-SKIP  : $SKIP_RESUME_COUNT"

# ── 8. Audit log line (consumed by 49th hard guard) ─────────────────────────
TS="$(date -u +%Y-%m-%dT%H:%M:%SZ)"
EXIT_CODE=0
[ "$HARD_FAIL" -gt 0 ] && EXIT_CODE=1

# Atomic append: write to tmp + sed concat is overkill — line-buffered >> is
# atomic for writes under PIPE_BUF (4096 on linux/macOS) and our line is < 200
# bytes. Append is safe.
printf '{"ts":"%s","head_sha":"%s","exit":%d,"pass":%d,"warn_advisory":%d,"hard_fail":%d,"skip":%d}\n' \
    "$TS" "$CURRENT_HEAD" "$EXIT_CODE" "$PASS_COUNT" "$ADVISORY_FAIL" "$HARD_FAIL" "$SKIP_COUNT" \
    >> "$AUDIT_LOG"

# ── ax-ledger capture — every verify run leaves a per-project usage trace (progress / violation),
# so a fork-receiver's gate history is reviewable (복기) and improvable. Never fails the gate. ──
_AX_LEDGER="$(dirname "${BASH_SOURCE[0]:-$0}")/ax-ledger-log.sh"
if [ -f "$_AX_LEDGER" ]; then
    if [ "$HARD_FAIL" -gt 0 ]; then
        bash "$_AX_LEDGER" violation gate=verify-completion outcome=fail "pass=$PASS_COUNT" "fail=$HARD_FAIL" \
            severity=block detail="R25 gate FAILED at HEAD" >/dev/null 2>&1 || true
    else
        bash "$_AX_LEDGER" gate_run gate=verify-completion outcome=pass "pass=$PASS_COUNT" "fail=0" \
            severity=info detail="R25 gate PASSED" >/dev/null 2>&1 || true
    fi
fi

# Finalize resume log atomically.
mv -f "$RESUME_NEW" "$RESUME_LOG"

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
