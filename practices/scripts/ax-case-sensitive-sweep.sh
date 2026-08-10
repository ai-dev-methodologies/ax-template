#!/usr/bin/env bash
# practices/scripts/ax-case-sensitive-sweep.sh — BACKLOG P2-72 (periodic STANDING job).
#
# WHY THIS EXISTS
# ---------------
# P1-B of the 2026-08-01 seal round proved that a COMMITTED path STRING can name a file by a
# spelling git does not record, and that an aliasing filesystem (the default case-INSENSITIVE
# APFS every macOS checkout of this repo lives on) then serves it anyway: the command runs,
# the tree is clean, the fingerprint is the clean-tree constant, and R25 reports GREEN on
# evidence it never actually produced. `checklist_command_path_spelling_guard.sh` closed that
# for the ONE input it can enumerate — the checklist's own `command`/`working_directory`. The
# general family (shell `source`, gradle `apply from:`, node `require`, python `open()`,
# Dockerfile `COPY`, CI paths) is NOT enumerable by inspection: which substring of an
# arbitrary file is a path is undecidable, and pointing a regex at an undecidable problem and
# calling the family closed is the exact defect this catalog keeps catching.
#
# The only COMPLETE prescription is to stop relying on inspection and RUN THE SUITE ON A
# FILESYSTEM THAT DOES NOT ALIAS. That is what this script does, and it is the mechanism
# P2-72's adoption condition (b) named. Lane F discharged the one-time baseline by hand
# (case-sensitive APFS volume, clean clone of HEAD, full `run-all-guards.sh
# --include-fixtures` → 357/357 PASS in 937 s, nothing failing there that passes here). A
# baseline someone did once is not a standing property; this file is the repeatable form of
# that same run, so the next person does not have to reconstruct it.
#
# WHAT IT DOES
# ------------
#   1. creates a case-sensitive APFS sparse image and attaches it (`hdiutil`);
#   2. PROBES the mount — refuses to continue unless it is REALLY case-sensitive, and reports
#      (does not require) whether it is normalization-sensitive;
#   3. FORCES TMPDIR onto the volume and PROBES that directory too (BACKLOG P3-142) — otherwise
#      the guard suite's own TMPDIR-based probes (`ax-prove-hermetic-runtime.sh`'s $WORK) would
#      silently fall back to the boot volume and measure the wrong filesystem;
#   4. clones this repo onto the volume and checks out the exact revision under test;
#   5. runs `practices/evals/run-all-guards.sh --include-fixtures` INSIDE that clone, with the
#      volume-forced TMPDIR exported;
#   6. detaches the volume, deletes the image, and VERIFIES with `hdiutil info` that nothing
#      is left attached;
#   7. prints what it covered and, explicitly, WHAT IT DID NOT.
#
# IT FAILS LOUDLY, NEVER SILENTLY. No hdiutil, a volume that turns out to alias, a clone that
# will not check out, a leaked attachment — each is a distinct NON-ZERO exit with its reason
# printed. A standing job whose failure mode is "print nothing and exit 0" would be a worse
# lie than not having the job, because the row would read as closed.
#
# HONEST SCOPE — stated here and re-printed at the end of every run:
#   · It sweeps the GUARD suite. It does NOT run R25's gradle steps (backend-build,
#     per-domain tests) or its npm step (frontend-lint): those need a JDK 21 toolchain and a
#     populated frontend/node_modules on the volume, which this script deliberately does not
#     provision. Path-spelling defects reachable ONLY through gradle/npm are therefore still
#     unswept, and this run must never be cited as if they were.
#   · It sweeps a CLONE OF A COMMITTED REVISION. Uncommitted working-tree changes are not in
#     the clone; the script says so, loudly, when the tree is dirty.
#   · macOS only (it is `hdiutil`). On a Linux CI the root filesystem is normally already
#     case-sensitive, so the equivalent is to run `run-all-guards.sh --include-fixtures`
#     directly — the script says that instead of pretending to be portable.
#
# Usage:
#   bash practices/scripts/ax-case-sensitive-sweep.sh                 # sweep HEAD
#   bash practices/scripts/ax-case-sensitive-sweep.sh --rev <sha>     # sweep a given revision
#   bash practices/scripts/ax-case-sensitive-sweep.sh --size 6g       # bigger image
#   bash practices/scripts/ax-case-sensitive-sweep.sh --keep          # leave volume attached
#   bash practices/scripts/ax-case-sensitive-sweep.sh --probe-only    # volume lifecycle only
#
# FIRST STANDING RUN (2026-08-01, rev cd9210e): 357 passed / 0 failed, run-all-guards exit 0,
# 932 s wall — i.e. the same 357/357 Lane F measured by hand, reproduced by this script. No
# guard failed on the case-sensitive volume that passes on the default case-INSENSITIVE one.
#
# Exit codes:
#   0  sweep ran and every guard matched its expected exit
#   1  sweep ran and at least one guard did not (a REAL finding, or a case-sensitivity
#      difference — the operator classifies which, per P2-72's stated cost)
#   2  usage / repo resolution error
#   3  hdiutil unavailable (or image creation/attach failed) — NOT a skip
#   4  the attached volume is not case-sensitive — NOT a skip
#   5  the volume could not be detached, or is still attached afterwards (leak)
#   6  TMPDIR could not be forced onto the volume, or the forced directory itself failed the
#      case-sensitivity probe (BACKLOG P3-142) — NOT a skip
set -uo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"

SIZE="4g"
REV=""
KEEP=0
PROBE_ONLY=0
while [ $# -gt 0 ]; do
    case "$1" in
        --size) SIZE="${2:?--size needs a value}"; shift 2 ;;
        --size=*) SIZE="${1#--size=}"; shift ;;
        --rev) REV="${2:?--rev needs a value}"; shift 2 ;;
        --rev=*) REV="${1#--rev=}"; shift ;;
        --keep) KEEP=1; shift ;;
        # --probe-only exercises everything EXCEPT the ~930 s sweep: create, attach, both
        # probes, the armed leak check, detach, leak verification. It exists so the VOLUME
        # LIFECYCLE can be re-verified in seconds after any edit to this file, instead of the
        # lifecycle being re-checked only as a side effect of a sixteen-minute run (which is
        # how a lifecycle bug survives). It sweeps NOTHING and says so.
        --probe-only) PROBE_ONLY=1; shift ;;
        -h|--help) sed -n '2,60p' "$0"; exit 0 ;;
        *) echo "ax-case-sensitive-sweep: unknown arg: $1" >&2; exit 2 ;;
    esac
done

if [ ! -d "$REPO_ROOT/.git" ] && [ ! -f "$REPO_ROOT/.git" ]; then
    echo "ax-case-sensitive-sweep: $REPO_ROOT is not a git checkout" >&2; exit 2
fi
REV="${REV:-$(git -C "$REPO_ROOT" rev-parse HEAD 2>/dev/null)}"
[ -n "$REV" ] || { echo "ax-case-sensitive-sweep: cannot resolve a revision to sweep" >&2; exit 2; }
REV_FULL="$(git -C "$REPO_ROOT" rev-parse "$REV" 2>/dev/null)" || {
    echo "ax-case-sensitive-sweep: '$REV' is not a revision in $REPO_ROOT" >&2; exit 2; }

DIRTY="$(git -C "$REPO_ROOT" status --porcelain | wc -l | tr -d ' ')"

echo "═══ ax-case-sensitive-sweep ═══"
echo "  repo      : $REPO_ROOT"
echo "  revision  : $REV_FULL"
if [ "$DIRTY" != "0" ]; then
    echo "  WARNING   : the working tree has $DIRTY uncommitted change(s). They are NOT in the"
    echo "              clone and are NOT swept — this run speaks only for the revision above."
fi

# ── volume lifecycle ────────────────────────────────────────────────────────────────
# Same posture as practices/scripts/ax-prove-hermetic-runtime.sh: the mountpoint is recorded
# BEFORE the probe, so teardown detaches even when the probe is what failed, and the detach
# happens BEFORE any rm, or the rm walks a mounted volume.
WORK="$(mktemp -d "${TMPDIR:-/tmp}/ax-cs-sweep.XXXXXX")" || {
    echo "ax-case-sensitive-sweep: cannot create a work directory" >&2; exit 2; }
IMG_BASE="$WORK/ax-cs"          # hdiutil -type SPARSE appends .sparseimage
IMG="$IMG_BASE.sparseimage"
MNT="$WORK/mnt"
ATTACHED=""
ATTACHED_REAL=""

# LEAK PREDICATE. `hdiutil info` prints the mountpoint in its RESOLVED spelling
# (/private/var/folders/…) while `$MNT` is the mktemp spelling (/var/folders/…, and with a
# doubled slash when TMPDIR ends in one). MEASURED on the first run of this script:
# `hdiutil info | grep -c "$MNT"` was 0 while `mount` showed the volume attached under the
# /private spelling — i.e. the single-spelling grep this started as would have reported
# "detached cleanly" for a volume that was still attached. Both spellings, both sources.
mnt_visible() {
    local scan
    scan="$({ hdiutil info 2>/dev/null; mount 2>/dev/null; })"
    case "$scan" in *"$ATTACHED"*) return 0 ;; esac
    [ -n "$ATTACHED_REAL" ] && case "$scan" in *"$ATTACHED_REAL"*) return 0 ;; esac
    return 1
}

teardown() {
    if [ -n "$ATTACHED" ]; then
        if [ "$KEEP" = "1" ]; then
            echo "  (volume left attached on request: $ATTACHED)"
        else
            hdiutil detach -force "$ATTACHED" >/dev/null 2>&1
            # LEAK CHECK — the point of a standing job is that it can be run again tomorrow,
            # and a volume left attached every run is how "run it periodically" turns into a
            # machine with forty stale mounts. Asking hdiutil is the only honest check;
            # trusting detach's own exit code is not (it can succeed while the volume is
            # still listed, e.g. when a process holds it open).
            if mnt_visible; then
                echo "  LEAK: $ATTACHED is STILL attached after detach — detach it by hand:" >&2
                echo "        hdiutil detach -force '$ATTACHED'" >&2
                # Exiting from inside the EXIT trap is what makes the leak OBSERVABLE in the
                # exit status; setting a variable here would be read after the trap has
                # already returned, i.e. never.
                rm -rf "$WORK" 2>/dev/null
                exit 5
            else
                echo "  volume detached and verified gone from \`hdiutil info\`/\`mount\`" \
                     "under BOTH spellings: $ATTACHED"
            fi
        fi
    fi
    [ "$KEEP" = "1" ] || rm -rf "$WORK"
}
trap teardown EXIT

if ! command -v hdiutil >/dev/null 2>&1; then
    echo "ax-case-sensitive-sweep: hdiutil NOT AVAILABLE — this job cannot run here." >&2
    echo "  This is a FAILURE, not a skip: the whole point of P2-72 is that the suite has" >&2
    echo "  actually been executed on a non-aliasing filesystem, and it has not been." >&2
    echo "  On Linux the root filesystem is normally already case-sensitive; run" >&2
    echo "  'bash practices/evals/run-all-guards.sh --include-fixtures' directly there." >&2
    exit 3
fi

echo "  creating $SIZE case-sensitive APFS image …"
if ! hdiutil create -quiet -size "$SIZE" -fs "Case-sensitive APFS" -volname AXCSSWEEP \
        -type SPARSE -ov "$IMG_BASE" >/dev/null 2>&1; then
    echo "ax-case-sensitive-sweep: hdiutil could not CREATE a Case-sensitive APFS image." >&2
    echo "  FAILURE, not a skip — see the note above." >&2
    exit 3
fi
[ -f "$IMG" ] || IMG="$IMG_BASE"     # in case hdiutil kept the name verbatim
mkdir -p "$MNT"
if ! hdiutil attach -quiet -nobrowse -mountpoint "$MNT" "$IMG" >/dev/null 2>&1; then
    echo "ax-case-sensitive-sweep: hdiutil could not ATTACH $IMG at $MNT." >&2
    exit 3
fi
ATTACHED="$MNT"
ATTACHED_REAL="$(cd "$MNT" 2>/dev/null && pwd -P)"
# POSITIVE CONTROL, on every run: the leak predicate must SEE a volume that is demonstrably
# attached right now. Without this, a predicate that matches nothing would silently report
# "detached cleanly" forever — a check that cannot fail is not a check, and this script's
# whole claim to being runnable repeatedly rests on it.
if ! mnt_visible; then
    echo "ax-case-sensitive-sweep: LEAK CHECK IS VACUOUS — the volume is attached at $MNT" >&2
    echo "  but neither \`hdiutil info\` nor \`mount\` reports it under either spelling, so a" >&2
    echo "  'detached cleanly' claim from this run would be unearned. Refusing to continue." >&2
    exit 5
fi
echo "  leak check armed (positive control: the live attachment IS visible to hdiutil/mount)"

# ── probes ──────────────────────────────────────────────────────────────────────────
# cs_probe is byte-for-byte the shape ax-prove-hermetic-runtime.sh uses (round 11): write one
# spelling, ask for the other. A volume that answers is aliasing and this run would be a lie.
cs_probe() {   # cs_probe <dir> → 0 when <dir> is on a CASE-SENSITIVE filesystem
    local d="$1/.axcase.$$"
    mkdir -p "$d" || return 1
    : > "$d/CaseProbe" || { rm -rf "$d"; return 1; }
    if [ -e "$d/caseprobe" ]; then rm -rf "$d"; return 1; fi
    rm -rf "$d"; return 0
}
# Normalization is the OTHER half of P2-72's "aliasing" (NFC vs NFD spellings of the same
# name). It is REPORTED, not required: APFS preserves normalization but the guarantee differs
# by macOS version, and silently claiming normalization coverage we did not verify is the
# same defect as claiming case coverage we did not verify.
norm_probe() {  # norm_probe <dir> → 0 when NFC and NFD spellings are DISTINCT files
    local d="$1/.axnorm.$$"
    mkdir -p "$d" || return 1
    : > "$d/$(printf 'e\xcc\x81')" || { rm -rf "$d"; return 1; }   # NFD  e + U+0301
    if [ -e "$d/$(printf '\xc3\xa9')" ]; then rm -rf "$d"; return 1; fi  # NFC  U+00E9
    rm -rf "$d"; return 0
}

if ! cs_probe "$MNT"; then
    echo "ax-case-sensitive-sweep: the attached volume at $MNT is NOT case-sensitive." >&2
    echo "  Refusing to run: a sweep on an aliasing filesystem measures nothing and would" >&2
    echo "  report a green that means the opposite of what the row asks for." >&2
    exit 4
fi
echo "  probe: volume IS case-sensitive (verified, not assumed)"
if norm_probe "$MNT"; then
    echo "  probe: volume IS normalization-sensitive (NFC and NFD are distinct files)"
    NORM_NOTE="covered (probe passed)"
else
    echo "  probe: volume is NORMALIZATION-INSENSITIVE — unicode-normalization aliases are NOT"
    echo "         swept by this run. Reported, not fatal: the case half is still measured."
    NORM_NOTE="NOT covered (this volume folds NFC/NFD)"
fi

# ── force TMPDIR onto the volume ────────────────────────────────────────────────────
# BACKLOG P3-142 — `run-all-guards.sh --include-fixtures` shells out to
# `ax-prove-hermetic-runtime.sh`, which builds its ENTIRE attack topology under
# `${TMPDIR:-/tmp}` ($WORK in that script), not under $MNT. Without forcing TMPDIR onto THIS
# volume, the invocation below silently tests the BOOT volume's (folding) semantics instead of
# the case-sensitive one just attached and probed above — every case-folding-dependent premise
# would construct on the boot volume's own case-INSENSITIVE filesystem and "pass" there, having
# never touched the volume this script exists to test. MEASURED: this was true of every prior
# green from this sweep (357/0 ×2) — the TMPDIR-based probes inside those runs were partially
# VACUOUS. Probed here, in the same style as the volume's own case probe above (build, ask,
# refuse loudly), so a TMPDIR that turns out not to be on the volume cannot pass silently.
SWEEP_TMPDIR="$MNT/tmp"
if ! mkdir -p "$SWEEP_TMPDIR"; then
    echo "ax-case-sensitive-sweep: cannot create $SWEEP_TMPDIR — TMPDIR cannot be forced onto" >&2
    echo "  the volume, so the guard-suite invocation would fall back to the boot volume." >&2
    exit 6
fi
if ! cs_probe "$SWEEP_TMPDIR"; then
    echo "ax-case-sensitive-sweep: the forced TMPDIR ($SWEEP_TMPDIR) is NOT case-sensitive, even" >&2
    echo "  though the volume it sits on IS (probed above). Something is wrong with this" >&2
    echo "  directory specifically — refusing to run a sweep whose TMPDIR premise is unverified." >&2
    exit 6
fi
echo "  TMPDIR forced onto the volume: $SWEEP_TMPDIR (probed case-sensitive, not assumed)"

if [ "$PROBE_ONLY" = "1" ]; then
    echo ""
    echo "═══ RESULT (--probe-only) ═══"
    echo "  volume lifecycle exercised: create → attach → case probe → normalization probe →"
    echo "  TMPDIR forced onto the volume + probed → armed leak check → (teardown below). NO"
    echo "  CLONE, NO SWEEP, NOTHING VERIFIED ABOUT THE CATALOG. This mode measures the harness,"
    echo "  not the tree."
    exit 0
fi

# ── clone + sweep ───────────────────────────────────────────────────────────────────
CLONE="$MNT/ax-template"
echo "  cloning $REV_FULL onto the volume …"
if ! git clone --quiet --local --no-hardlinks "$REPO_ROOT" "$CLONE" >/dev/null 2>&1; then
    echo "ax-case-sensitive-sweep: git clone onto the volume failed" >&2; exit 2
fi
if ! git -C "$CLONE" checkout --quiet --detach "$REV_FULL" >/dev/null 2>&1; then
    echo "ax-case-sensitive-sweep: cannot check out $REV_FULL in the clone" >&2; exit 2
fi
CLONE_HEAD="$(git -C "$CLONE" rev-parse HEAD)"
if [ "$CLONE_HEAD" != "$REV_FULL" ]; then
    echo "ax-case-sensitive-sweep: clone HEAD $CLONE_HEAD != requested $REV_FULL" >&2; exit 2
fi
echo "  clone HEAD verified: $CLONE_HEAD"

LOG="$WORK/run-all-guards.log"
echo "  running run-all-guards.sh --include-fixtures on the case-sensitive volume (TMPDIR=$SWEEP_TMPDIR) …"
START=$(date +%s)
( cd "$CLONE" && TMPDIR="$SWEEP_TMPDIR" bash practices/evals/run-all-guards.sh --include-fixtures ) \
    >"$LOG" 2>&1
SWEEP_RC=$?
ELAPSED=$(( $(date +%s) - START ))
TALLY="$(grep -E '^Total: [0-9]+ passed, [0-9]+ failed' "$LOG" | tail -1)"
[ -n "$TALLY" ] || TALLY="(no tally line — the sweep did not reach its summary)"

echo ""
echo "═══ RESULT ═══"
echo "  $TALLY"
echo "  run-all-guards exit: $SWEEP_RC   wall: ${ELAPSED}s"
if [ "$SWEEP_RC" != "0" ]; then
    echo "  MISMATCHING GUARDS:"
    # run-all-guards records a failure as `FAIL [<label>] expected exit X, got Y` (or
    # `... EXCEEDED THE PER-GUARD CAP …`). Grepping for anything else prints nothing on a
    # real failure, which is the silent-standing-job failure mode this file argues against.
    grep -E "^FAIL \[|EXCEEDED THE PER-GUARD CAP" "$LOG" | head -40 | sed 's/^/    /'
    if cp "$LOG" "${TMPDIR:-/tmp}/ax-cs-sweep-failure.log" 2>/dev/null; then
        echo "  full log kept at ${TMPDIR:-/tmp}/ax-cs-sweep-failure.log"
    else
        echo "  full log: $LOG (removed on exit unless --keep)"
    fi
fi

echo ""
echo "═══ COVERAGE — what this run does NOT say ═══"
echo "  · gradle steps (backend-build, per-domain test tasks) were NOT run: no JDK 21 was"
echo "    provisioned on the volume. Path-spelling defects reachable only through gradle are"
echo "    UNSWEPT by this run."
echo "  · the npm step (frontend-lint) was NOT run: no node_modules was provisioned on the"
echo "    volume. React-side path aliases are UNSWEPT by this run."
echo "  · R25 (verify-completion.sh) as a whole was NOT run here; this is the guard-suite"
echo "    half of it only."
echo "  · unicode normalization: $NORM_NOTE"
echo "  · only the committed revision $REV_FULL was swept."

if [ "$SWEEP_RC" != "0" ]; then exit 1; fi
exit 0
