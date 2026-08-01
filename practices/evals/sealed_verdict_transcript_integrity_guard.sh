#!/usr/bin/env bash
# practices/evals/sealed_verdict_transcript_integrity_guard.sh — BACKLOG P3-108.
#
# THE DEFECT. A sealed-verdict file that carries a real context-0 sub-agent transcript (e.g.
# scheduler-l4-verdict-v2.md → scheduler-l4-verdict-v2-transcript.md, skills/_tests/sealed-
# verdict/) binds to that transcript by a PATH STRING only: the verdict's prose names the
# transcript file, quotes excerpts from it, and nothing else. Editing the transcript's bytes after
# the verdict was scored — to make a sub-agent appear to have said something it didn't — is
# undetectable: the path still resolves, and no gate compares the transcript's CONTENT against
# anything recorded at scoring time.
#
# THE FIX. skills/_tests/sealed-verdict/TRANSCRIPT-MANIFEST.yaml records, for every (verdict,
# transcript) pair, the transcript's sha256 at scoring time. This guard recomputes that sha256 on
# every run and BLOCKS on mismatch (TRANSCRIPT_TAMPERED). It also enforces COMPLETENESS: every
# verdict `.md` file in the directory that textually references a `*-transcript.md` filename must
# have a matching registered binding, so a future verdict that introduces a transcript without
# registering its hash is a BLOCK (UNREGISTERED_TRANSCRIPT_REFERENCE), not a silent gap that only
# the registry's author would ever have noticed.
#
# v1/v2 in the real scheduler-l4 lineage are SEALED (never edited, per the "seals are history, not
# config" convention) — the registry is therefore the ONLY place the sha256 binding for a
# pre-existing transcript-carrying verdict can mechanically live. A NEW transcript-carrying verdict
# may additionally self-describe the binding in its own frontmatter (transcript_path /
# transcript_sha256), but this guard's ground truth is always the registry, so both cases are
# checked by one code path.
#
# WHAT IS AND ISN'T CLOSED. This closes "the transcript's bytes changed after the verdict was
# scored, undetected". It does NOT (and cannot, offline) prove the transcript was never doctored
# BEFORE being hashed into the registry in the first place — that trust boundary is the same one
# every other evidence-anchoring gate in this catalog draws (see practices/evals/evidence_guard.sh
# and its structure-vs-content-truth split): a registry entry is a claim, not a proof of origin.
# What this guard guarantees is that the claim, once recorded, cannot silently drift from the file
# it describes.
#
# Exit codes:
#   0 — every registered binding's sha256 matches disk, and no unregistered transcript reference
#       exists
#   1 — TRANSCRIPT_TAMPERED (a registered transcript's disk sha256 does not match the recorded
#       value) or UNREGISTERED_TRANSCRIPT_REFERENCE (a verdict references a transcript filename
#       with no matching registry entry)
#   2 — structural: missing directory/manifest/file, malformed manifest shape, zero bindings
#       (non-vacuity floor), no sha256 tool available
#
# Usage:
#   bash practices/evals/sealed_verdict_transcript_integrity_guard.sh
#   bash practices/evals/sealed_verdict_transcript_integrity_guard.sh --root DIR

set -uo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"

ROOT_OVERRIDE=""
while [ $# -gt 0 ]; do
    case "$1" in
        --root) ROOT_OVERRIDE="$2"; shift 2 ;;
        --root=*) ROOT_OVERRIDE="${1#--root=}"; shift ;;
        *) echo "sealed_verdict_transcript_integrity_guard: unknown arg: $1" >&2; exit 2 ;;
    esac
done
[ -n "$ROOT_OVERRIDE" ] && REPO_ROOT="$ROOT_OVERRIDE"

VERDICT_DIR="$REPO_ROOT/skills/_tests/sealed-verdict"
MANIFEST="$VERDICT_DIR/TRANSCRIPT-MANIFEST.yaml"

if [ ! -d "$VERDICT_DIR" ]; then
    echo "sealed_verdict_transcript_integrity_guard: missing directory $VERDICT_DIR" >&2
    exit 2
fi
if [ ! -f "$MANIFEST" ]; then
    echo "sealed_verdict_transcript_integrity_guard: missing manifest $MANIFEST" >&2
    exit 2
fi

sha256_of() {
    if command -v shasum >/dev/null 2>&1; then
        shasum -a 256 "$1" 2>/dev/null | awk '{print $1}'
    elif command -v sha256sum >/dev/null 2>&1; then
        sha256sum "$1" 2>/dev/null | awk '{print $1}'
    else
        return 1
    fi
}

if ! printf 'probe' | { command -v shasum >/dev/null 2>&1 || command -v sha256sum >/dev/null 2>&1; }; then
    echo "sealed_verdict_transcript_integrity_guard: no sha256 tool available (need shasum or sha256sum)" >&2
    exit 2
fi

# ── Parse the flat "bindings:" list — grep-friendly by construction (see the manifest's own
#    header), so no YAML library dependency is needed for this guard. ──────────────────────────
verdicts="$(grep -E '^[[:space:]]*-[[:space:]]*verdict:' "$MANIFEST" | sed -E 's/^[[:space:]]*-[[:space:]]*verdict:[[:space:]]*//; s/^"//; s/"$//')"
transcripts="$(grep -E '^[[:space:]]*transcript:' "$MANIFEST" | sed -E 's/^[[:space:]]*transcript:[[:space:]]*//; s/^"//; s/"$//')"
shas="$(grep -E '^[[:space:]]*transcript_sha256:' "$MANIFEST" | sed -E 's/^[[:space:]]*transcript_sha256:[[:space:]]*//; s/^"//; s/"$//')"

n_v="$(printf '%s\n' "$verdicts" | grep -c . || true)"
n_t="$(printf '%s\n' "$transcripts" | grep -c . || true)"
n_s="$(printf '%s\n' "$shas" | grep -c . || true)"

if [ "$n_v" -eq 0 ] || [ "$n_t" -eq 0 ] || [ "$n_s" -eq 0 ]; then
    echo "sealed_verdict_transcript_integrity_guard: MANIFEST_EMPTY — 0 bindings parsed from $MANIFEST (verdict=$n_v transcript=$n_t sha=$n_s). Zero is never a pass — it means the manifest lost its shape, not that there is nothing to check." >&2
    exit 2
fi
if [ "$n_v" -ne "$n_t" ] || [ "$n_v" -ne "$n_s" ]; then
    echo "sealed_verdict_transcript_integrity_guard: MANIFEST_SHAPE — mismatched field counts (verdict=$n_v transcript=$n_t transcript_sha256=$n_s). Every binding must have exactly one of each key, in order." >&2
    exit 2
fi

violations=0
checked=0
registered_pairs=""   # newline-delimited "verdict|transcript" for the completeness scan below

i=0
while [ "$i" -lt "$n_v" ]; do
    i=$((i + 1))
    v="$(printf '%s\n' "$verdicts" | sed -n "${i}p")"
    t="$(printf '%s\n' "$transcripts" | sed -n "${i}p")"
    s="$(printf '%s\n' "$shas" | sed -n "${i}p")"

    vpath="$VERDICT_DIR/$v"
    tpath="$VERDICT_DIR/$t"

    if [ ! -f "$vpath" ]; then
        echo "VIOLATION: binding #$i names verdict '$v' which does not exist at $vpath" >&2
        violations=$((violations + 1))
        continue
    fi
    if [ ! -f "$tpath" ]; then
        echo "VIOLATION: binding #$i names transcript '$t' which does not exist at $tpath" >&2
        violations=$((violations + 1))
        continue
    fi

    actual="$(sha256_of "$tpath")"
    if [ -z "$actual" ]; then
        echo "sealed_verdict_transcript_integrity_guard: could not compute sha256 of $tpath" >&2
        exit 2
    fi
    checked=$((checked + 1))
    if [ "$actual" != "$s" ]; then
        echo "VIOLATION: TRANSCRIPT_TAMPERED — '$t' (bound to '$v') recorded transcript_sha256 '$s' but the disk sha256 is '$actual'. The transcript's bytes changed since the verdict was scored against it." >&2
        violations=$((violations + 1))
    fi
    registered_pairs="$registered_pairs
$v|$t"
done

# ── Completeness: every verdict `.md` that textually references a `*-transcript.md` filename
#    must have a matching registered binding. This is what keeps a FUTURE verdict from
#    introducing a transcript reference without ever registering its hash. ─────────────────────
referenced_unregistered=0
for f in "$VERDICT_DIR"/*.md; do
    [ -f "$f" ] || continue
    base="$(basename "$f")"
    case "$base" in
        *-transcript.md) continue ;;   # a transcript file itself, not a verdict
    esac
    refs="$(grep -oE '[A-Za-z0-9_-]+-transcript\.md' "$f" 2>/dev/null | sort -u || true)"
    [ -z "$refs" ] && continue
    while IFS= read -r ref; do
        [ -z "$ref" ] && continue
        case "$registered_pairs" in
            *"
$base|$ref"*) ;;   # registered — fine
            *)
                echo "VIOLATION: UNREGISTERED_TRANSCRIPT_REFERENCE — '$base' references transcript '$ref' but no binding for this exact (verdict, transcript) pair exists in $MANIFEST" >&2
                violations=$((violations + 1))
                referenced_unregistered=$((referenced_unregistered + 1))
                ;;
        esac
    done <<< "$refs"
done

if [ "$violations" -gt 0 ]; then
    echo "sealed_verdict_transcript_integrity_guard: $violations violation(s) — BLOCKED" >&2
    exit 1
fi

echo "sealed_verdict_transcript_integrity_guard: PASS — $checked binding(s) verified sha256-match against disk; 0 unregistered transcript reference(s)"
exit 0
