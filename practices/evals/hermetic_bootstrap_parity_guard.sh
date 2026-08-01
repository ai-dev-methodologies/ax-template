#!/usr/bin/env bash
# practices/evals/hermetic_bootstrap_parity_guard.sh — BACKLOG P2-64.
#
# THE HERMETIC BOOTSTRAP IS DUPLICATED IN EIGHT ENTRIES ON PURPOSE, AND THAT IS EXACTLY WHY IT
# NEEDS A PARITY GATE.
#
# The bootstrap (privileged re-exec → pure-keyword preflight → runtime scrub) cannot live in a
# sourced file: it is the code that decides whether `source` itself is safe, so it must run BEFORE
# anything is sourced. "The duplication IS the bootstrap" is written in every copy. But a
# deliberate duplication has a failure mode a shared function does not: UPDATE ONE COPY AND SEVEN
# GATES KEEP THE OLD BEHAVIOUR — silently, with no test failing, because each copy is
# independently valid shell. Every round that hardened the bootstrap (the round-5 scrub, the
# round-6 preflight, the round-7 privileged re-exec, P2-67's mktemp, P2-70's env allowlist) had to
# be applied eight times by hand, and nothing checked that it was.
#
# This guard extracts the three bootstrap blocks from all eight entries and asserts BYTE IDENTITY
# modulo exactly three per-file parameters — the label, _AX_HRM_EXIT and _AX_HRM_NEED_PY.
#
#   (A) PRIVILEGED BLOCK  `case $- in` … `unset AX_PRIV_REEXEC` (exec form)
#                         `case $- in` … `esac`                 (sourced-assert form)
#   (B) PREFLIGHT         `_AX_PF_LABEL="…` … `unset _AX_PF_ENV …`
#   (C) RUNTIME SCRUB     `_AX_HRM_LABEL="…` … `unset _ax_hn _ax_hb …`
#
# (B) and (C) must be identical across ALL EIGHT. (A) legitimately has THREE forms, so it is
# compared WITHIN A REGISTERED CLASS and the class membership is itself pinned:
#
#   · exec-hermetic-env  — re-execs through `env -i` with the P2-70 allowlist. For the entries
#                          whose process subtree execs NO foreign toolchain.
#   · exec-inheriting-env— re-execs inheriting the whole environment, with the reason recorded
#                          inline. For the entries that exec gradle/npm (directly or
#                          transitively), whose environment surface is not enumerable here.
#   · sourced-assert     — a SOURCED file cannot re-exec without replacing its caller, so it
#                          ASSERTS privileged mode instead.
#
# Pinning the class is the point of the roster: silently rewriting an `exec-hermetic-env` entry
# back to the inheriting form would otherwise be a legal-looking edit that removes a control from
# one gate and leaves the other two claiming it.
#
# THE ROSTER IS ALSO A CENSUS. Any file in the tree carrying `_AX_HRM_LABEL=` must be registered;
# a ninth entry that appears without being registered BLOCKS, and a registered entry that has lost
# its bootstrap BLOCKS. Otherwise "all copies agree" would be satisfiable by deleting copies.
#
# THIS GUARD DELIBERATELY DOES NOT CARRY THE BOOTSTRAP ITSELF. It would then be a ninth copy whose
# own drift nothing checks, and — worse — it would compare its own text against the thing it is
# comparing. It is a text-parity checker over committed files, not a ratchet: it reads no
# environment-derived policy and executes nothing it reads.
#
# Exit: 0 parity holds · 1 drift / roster mismatch (BLOCK) · 2 usage or harness error.
#
# Usage:
#   bash practices/evals/hermetic_bootstrap_parity_guard.sh
#   bash practices/evals/hermetic_bootstrap_parity_guard.sh --root DIR
#   bash practices/evals/hermetic_bootstrap_parity_guard.sh --fixtures
#
# NON-VACUITY (--fixtures). Committed fixture DIRECTORIES cannot express this subject: the subject
# IS the eight live multi-thousand-line entries, and a fixture copy of them would itself be a
# ninth set of duplicates to keep in sync — the very problem this guard exists to solve. So the
# self-proof follows the precedent set by pre_push_decision_guard's `scenario_selfproof_nonvacuous`
# (the [87] fixture-kill model cannot express a subject that is the tooling itself): the live
# entries are copied into a throwaway tree, ONE copy is mutated in each of the three blocks in
# turn, and the guard must BLOCK on each mutation and PASS on the unmutated copy.

set -uo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"

ROOT_OVERRIDE=""
FIXTURES=0
while [ $# -gt 0 ]; do
    case "$1" in
        --root) ROOT_OVERRIDE="$2"; shift 2 ;;
        --root=*) ROOT_OVERRIDE="${1#--root=}"; shift ;;
        --fixtures) FIXTURES=1; shift ;;
        *) echo "hermetic_bootstrap_parity_guard: unknown arg: $1" >&2; exit 2 ;;
    esac
done
ROOT="${ROOT_OVERRIDE:-$REPO_ROOT}"
[ -d "$ROOT" ] || { echo "hermetic_bootstrap_parity_guard: root not found: $ROOT" >&2; exit 2; }

run_parity() {
    python3 - "$1" <<'PYEOF'
import hashlib, os, re, sys

root = sys.argv[1]

# ── THE ROSTER — path -> privileged-block class ───────────────────────────────────────
ROSTER = {
    "practices/evals/completion_checklist_recency_guard.sh":  "exec-hermetic-env",
    "practices/evals/evidence_quote_spotcheck_guard.sh":      "exec-hermetic-env",
    "practices/evals/manifest_snapshot_integrity_guard.sh":   "exec-hermetic-env",
    ".githooks/pre-push":                                     "exec-inheriting-env",
    "practices/evals/run-all-guards.sh":                      "exec-inheriting-env",
    "practices/scripts/verify-completion.sh":                 "exec-inheriting-env",
    ".githooks/pre-push-lib.sh":                              "sourced-assert",
    "practices/scripts/lib/release_anchor.sh":                "sourced-assert",
}

PF_START = '_AX_PF_LABEL="'
PF_END = "unset _AX_PF_ENV _AX_PF_NULL _AX_PF_DIE _AX_PF_LABEL"
HRM_START = '_AX_HRM_LABEL="'
HRM_END = "unset _ax_hn _ax_hb _ax_hdir _ax_hver _AX_HRM_BAD _AX_HRM_PATH"
PV_START = "\ncase $- in\n"
PV_EXEC_END = "\nunset AX_PRIV_REEXEC\n"

violations = []


def fail(msg):
    violations.append(msg)


def segment(text, start, end, path, what):
    i = text.find(start)
    if i < 0:
        fail("%s: the %s block was not found (looked for %r)" % (path, what, start))
        return None
    j = text.find(end, i)
    if j < 0:
        fail("%s: the %s block has no terminator (looked for %r)" % (path, what, end))
        return None
    return text[i:j + len(end)]


def privileged_segment(text, path, cls):
    i = text.find(PV_START)
    if i < 0:
        fail("%s: no privileged block (`case $- in`) found" % path)
        return None
    i += 1                                    # keep the leading newline out of the segment
    if cls == "sourced-assert":
        j = text.find("\nesac\n", i)
        if j < 0:
            fail("%s: the sourced-assert privileged block has no terminator" % path)
            return None
        return text[i:j + len("\nesac\n")]
    j = text.find(PV_EXEC_END, i)
    if j < 0:
        fail("%s: the exec-form privileged block has no `unset AX_PRIV_REEXEC` terminator" % path)
        return None
    return text[i:j + len(PV_EXEC_END)]


def norm_label(seg):
    """Replace the THREE sanctioned per-file parameters with placeholders."""
    seg = re.sub(r'^_AX_PF_LABEL="[^"]*"', '_AX_PF_LABEL="<LABEL>"', seg)
    seg = re.sub(r'^_AX_HRM_LABEL="[^"]*"; _AX_HRM_EXIT=\d+; _AX_HRM_NEED_PY=\d+',
                 '_AX_HRM_LABEL="<LABEL>"; _AX_HRM_EXIT=<EXIT>; _AX_HRM_NEED_PY=<NEEDPY>', seg)
    # the label also appears inside the message strings of both blocks
    seg = re.sub(r'"[^"\n]*?: HERMETIC_PRIVILEGED_UNREACHABLE', '"<LABEL>: HERMETIC_PRIVILEGED_UNREACHABLE', seg)
    seg = re.sub(r'\$_AX_HRM_LABEL', '$<LABELVAR>', seg)
    return seg


# ── CENSUS: every file carrying the scrub must be registered, and vice versa ──────────
SKIP_DIRS = {".git", "node_modules", "build", ".gradle", "__pycache__"}
found = set()
for dirpath, dirnames, filenames in os.walk(root):
    dirnames[:] = [d for d in dirnames if d not in SKIP_DIRS]
    for fn in filenames:
        p = os.path.join(dirpath, fn)
        rel = os.path.relpath(p, root)
        if not (fn.endswith(".sh") or fn == "pre-push"):
            continue
        try:
            with open(p, "r", errors="replace") as fh:
                head = fh.read()
        except OSError:
            continue
        # The census marker is the ASSIGNMENT AT COLUMN 0, not the string anywhere in the file:
        # this guard's own source mentions `_AX_HRM_LABEL="` as a parser constant (indented), and
        # a census that matched that would report the parity checker as a ninth bootstrap copy.
        if re.search(r'^_AX_HRM_LABEL="', head, re.M):
            found.add(rel)

# Fixture/sandbox copies of an entry are not a ninth entry; they are the SAME entry under a
# different path, and several guards deliberately relocate one. Only paths that are not under a
# fixtures/ directory participate in the census.
found = {r for r in found if "/fixtures/" not in ("/" + r)}

unregistered = sorted(found - set(ROSTER))
missing = sorted(set(ROSTER) - found)
for rel in unregistered:
    fail("%s carries the hermetic bootstrap but is NOT in this guard's roster. A ninth copy that "
         "nobody registered is a copy nobody keeps in sync — register it (and its privileged-block "
         "class) or remove the bootstrap from it." % rel)
for rel in missing:
    fail("%s is registered as a hermetic entry but no longer carries the bootstrap. 'All copies "
         "agree' must not be satisfiable by deleting copies." % rel)

# ── PARITY ───────────────────────────────────────────────────────────────────────────
pf, hrm, pv = {}, {}, {}
for rel, cls in sorted(ROSTER.items()):
    path = os.path.join(root, rel)
    if not os.path.isfile(path):
        continue                              # already reported by the census
    text = open(path, "r", errors="replace").read()
    s = segment(text, PF_START, PF_END, rel, "pure-keyword preflight")
    if s is not None:
        pf[rel] = norm_label(s)
    s = segment(text, HRM_START, HRM_END, rel, "hermetic runtime scrub")
    if s is not None:
        hrm[rel] = norm_label(s)
    s = privileged_segment(text, rel, cls)
    if s is not None:
        pv[rel] = (cls, norm_label(s))

    # class pinning: the shape of the block must MATCH the registered class.
    if s is not None:
        is_exec = "exec /usr/bin/env" in s
        is_env_i = "exec /usr/bin/env -i" in s
        actual = ("exec-hermetic-env" if is_env_i else
                  "exec-inheriting-env" if is_exec else "sourced-assert")
        if actual != cls:
            fail("%s is registered as %s but its privileged block is %s. The class is pinned "
                 "because rewriting an env-i entry back to the inheriting form removes a control "
                 "from ONE gate while the others keep claiming it." % (rel, cls, actual))


def group(name, table, keyfn=lambda rel: ""):
    buckets = {}
    for rel, val in table.items():
        k = (keyfn(rel), hashlib.sha256(val.encode("utf-8", "replace")).hexdigest())
        buckets.setdefault(k, []).append(rel)
    by_class = {}
    for (cls, digest), rels in buckets.items():
        by_class.setdefault(cls, []).append((digest, sorted(rels)))
    for cls, entries in sorted(by_class.items()):
        if len(entries) > 1:
            entries.sort(key=lambda e: -len(e[1]))
            majority = entries[0]
            odd = [(d, r) for d, r in entries[1:]]
            label = (" (class %s)" % cls) if cls else ""
            fail("%s block DRIFT%s — the copies are not byte-identical. Majority (%s): %s. "
                 "Divergent: %s. The bootstrap is duplicated on purpose; a copy that drifts is a "
                 "gate running the OLD behaviour with nothing failing."
                 % (name, label, majority[0][:12], ", ".join(majority[1]),
                    "; ".join("%s -> %s" % (", ".join(r), d[:12]) for d, r in odd)))
    return by_class


group("PREFLIGHT", pf)
group("SCRUB", hrm)
group("PRIVILEGED", {r: v[1] for r, v in pv.items()}, keyfn=lambda rel: ROSTER[rel])

if violations:
    for v in violations:
        print("VIOLATION [hermetic_bootstrap_parity]: %s" % v, file=sys.stderr)
    print("hermetic_bootstrap_parity_guard: %d violation(s) — BLOCKED" % len(violations),
          file=sys.stderr)
    sys.exit(1)

classes = {}
for rel, cls in ROSTER.items():
    classes[cls] = classes.get(cls, 0) + 1
print("hermetic_bootstrap_parity_guard: PASS — %d entries, preflight + scrub byte-identical "
      "(modulo label/EXIT/NEED_PY); privileged block identical within each class (%s)"
      % (len(ROSTER), ", ".join("%s=%d" % (c, n) for c, n in sorted(classes.items()))))
PYEOF
}

if [ "$FIXTURES" -eq 1 ]; then
    # SELF-PROOF: copy the live entries into a throwaway tree, mutate ONE copy in each block in
    # turn, and require a BLOCK every time (and a PASS on the unmutated tree).
    WORK="$(mktemp -d "${TMPDIR:-/tmp}/ax-bootparity.XXXXXX")" || exit 2
    trap 'rm -rf "$WORK"' EXIT
    pass=0; fail=0
    build() {
        rm -rf "$WORK/t"; mkdir -p "$WORK/t"
        ( cd "$REPO_ROOT" && for rel in .githooks/pre-push .githooks/pre-push-lib.sh \
              practices/evals/completion_checklist_recency_guard.sh \
              practices/evals/evidence_quote_spotcheck_guard.sh \
              practices/evals/manifest_snapshot_integrity_guard.sh \
              practices/evals/run-all-guards.sh \
              practices/scripts/lib/release_anchor.sh \
              practices/scripts/verify-completion.sh; do
              mkdir -p "$WORK/t/$(dirname "$rel")"; cp "$rel" "$WORK/t/$rel"
          done )
    }
    check() {   # <label> <expected-exit>
        local label="$1" want="$2" rc
        run_parity "$WORK/t" >/dev/null 2>&1; rc=$?
        if [ "$rc" -eq "$want" ]; then echo "PASS [hermetic_bootstrap_parity/$label]"; pass=$((pass+1))
        else echo "FAIL [hermetic_bootstrap_parity/$label] — expected exit $want, got $rc"; fail=$((fail+1)); fi
    }
    mutate() {  # <file> <python-expression-file-edit>
        python3 - "$WORK/t/$1" "$2" <<'PY'
import sys
p, kind = sys.argv[1], sys.argv[2]
t = open(p).read()
if kind == "scrub":
    old = "_AX_HRM_DEPS_Q=\"[ [[\""
    assert old in t, "scrub anchor drifted"
    t = t.replace(old, old + "   # DRIFT", 1)
elif kind == "preflight":
    # inside the block, not at its terminator: a change after the terminator is by definition
    # outside the compared segment and would make this self-proof pass for the wrong reason.
    old = '_AX_PF_ENV="$(/usr/bin/env)"'
    assert old in t, "preflight anchor drifted"
    t = t.replace(old, '_AX_PF_ENV="$(/usr/bin/env)"   # DRIFT', 1)
elif kind == "privileged":
    old = 'exec /usr/bin/env -i "${_AX_PV_ENV[@]}" "$BASH" -p "$0" "$@"'
    assert old in t, "privileged anchor drifted"
    t = t.replace(old, 'exec /usr/bin/env AX_PRIV_REEXEC=1 "$BASH" -p "$0" "$@"', 1)
elif kind == "roster":
    pass
else:
    raise SystemExit("unknown mutation " + kind)
open(p, "w").write(t)
PY
    }

    build; check "pass_unmutated_copies" 0
    build; mutate practices/evals/run-all-guards.sh scrub || exit 2
    check "fail_scrub_drift" 1
    build; mutate practices/scripts/verify-completion.sh preflight || exit 2
    check "fail_preflight_drift" 1
    build; mutate practices/evals/manifest_snapshot_integrity_guard.sh privileged || exit 2
    check "fail_privileged_class_downgrade" 1
    build; rm -f "$WORK/t/practices/evals/run-all-guards.sh"
    check "fail_entry_lost_bootstrap" 1
    build; cp "$WORK/t/practices/evals/run-all-guards.sh" "$WORK/t/practices/evals/ninth_entry.sh"
    check "fail_unregistered_ninth_copy" 1

    echo ""
    echo "hermetic_bootstrap_parity_guard: fixtures $pass PASS / $fail FAIL"
    [ "$fail" -gt 0 ] && exit 1
    exit 0
fi

run_parity "$ROOT"
