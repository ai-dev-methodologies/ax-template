#!/usr/bin/env bash
# practices/evals/adversarial/run.sh — adversarial cases. Each case temporarily injects a
# crafted rule into practices/rules/, runs the relevant guard, then restores. Returns
# BLOCK when the guard correctly rejected the bad rule, FAIL when the guard let it through.
set -uo pipefail

cd "$(dirname "$0")/../.."

CASE=""
while [[ $# -gt 0 ]]; do
    case "$1" in
        --case) CASE="$2"; shift 2 ;;
        *) echo "usage: $0 --case <name>" >&2; exit 2 ;;
    esac
done

if [[ -z "$CASE" ]]; then
    echo "ERROR: --case <name> is required" >&2
    exit 2
fi

# ── Fail closed: the cases below parse yaml through PyYAML ───────────────────
# Without the parser a case cannot be adjudicated, so exit 2 ("cannot verify") — NEVER 0.
# Pinned mechanically by practices/evals/pyyaml_preflight_coverage_guard.sh [95] (static
# assertion: this runner mutates practices/rules/ while running, so [95] does not execute it).
if ! command -v python3 >/dev/null 2>&1 || ! python3 -c 'import yaml' >/dev/null 2>&1; then
    echo "adversarial/run.sh: BLOCK — cannot verify: python3 + PyYAML required (python3 -m pip install pyyaml)" >&2
    exit 2
fi

CASE_DIR="evals/adversarial/cases/$CASE"
CASE_RULE="$CASE_DIR/rule.md"
# time_decay is a manifest-level case; it does not require a rule.md fixture.
if [[ "$CASE" != "time_decay" && ! -f "$CASE_RULE" ]]; then
    echo "ERROR: case file not found: $CASE_RULE" >&2
    exit 2
fi

# Special case: time_decay tests the time_decay_guard against a temporarily stale manifest.
if [[ "$CASE" == "time_decay" ]]; then
    MANIFEST="upstream/_MANIFEST.yaml"
    if [[ ! -f "$MANIFEST" ]]; then
        echo "FAIL — no $MANIFEST to mutate for time_decay case" >&2
        exit 1
    fi
    BACKUP="$(mktemp)"
    cp "$MANIFEST" "$BACKUP"
    trap 'cp "$BACKUP" "$MANIFEST"; rm -f "$BACKUP"' EXIT

    python3 - "$MANIFEST" <<'PY'
import yaml, datetime, pathlib, sys
mf = pathlib.Path(sys.argv[1])
d = yaml.safe_load(mf.read_text()) or {}
stale_when = (datetime.datetime.now(datetime.timezone.utc) - datetime.timedelta(days=120)).strftime("%FT%TZ")
d.setdefault("snapshots", []).append({
    "id": "_adversarial_time_decay",
    "tier": 3,
    "source": "https://example.com/stale",
    "via": "test-injected",
    "fetched_at": stale_when,
    "sha": "0" * 64,
})
mf.write_text(yaml.safe_dump(d, default_flow_style=False))
PY
    bash evals/time_decay_guard.sh
    td_exit=$?
    if [[ $td_exit -ne 0 ]]; then
        echo "BLOCK — case 'time_decay' was correctly rejected by: time_decay_guard"
        exit 0
    fi
    echo "FAIL — case 'time_decay' slipped past time_decay_guard" >&2
    exit 1
fi

# Standard cases: inject the case rule.md into rules/ and run spec_ref + substance guards.
TARGET="rules/_adversarial_${CASE}.md"
cp "$CASE_RULE" "$TARGET"
trap 'rm -f "$TARGET"' EXIT

bash evals/spec_ref_guard.sh
spec_exit=$?
bash evals/substance_guard.sh
subst_exit=$?

if [[ $spec_exit -ne 0 || $subst_exit -ne 0 ]]; then
    by=""
    [[ $spec_exit  -ne 0 ]] && by="${by}spec_ref_guard "
    [[ $subst_exit -ne 0 ]] && by="${by}substance_guard "
    echo "BLOCK — case '$CASE' was correctly rejected by: ${by% }"
    exit 0
else
    echo "FAIL — case '$CASE' slipped past all guards" >&2
    exit 1
fi
