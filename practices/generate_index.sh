#!/usr/bin/env bash
# practices/generate_index.sh — produce <catalog>/INDEX.md (catalog ROOT, never under
# rules/ — the 4 hard gates glob `{catalog}/rules/*.md`, so an INDEX.md placed inside
# rules/ would be scanned as a rule file and BLOCK all four. See PRD d-track PM-2.)
#
# Usage:
#   bash practices/generate_index.sh                        # default: --catalog practices
#   bash practices/generate_index.sh --catalog practices-react
#
# Deterministic (2 consecutive runs against an unchanged rules/ tree diff 0):
# LC_ALL=C throughout, zero `date` calls, rule ids / tags / tag-membership lists all
# sorted. bash 3.2 compatible (no mapfile, no associative arrays) — same nullglob +
# IFS-array-sort idiom as practices/generate_agents.sh.
#
# Frontmatter is parsed with python3 + PyYAML, not regex/awk (O2-C — PRD d-track
# §0.3 F11/F13). Rule frontmatter has 12+ distinct real shapes on disk (block AND
# inline mapping for `verification:`, block AND inline-flow `tags: [...]`); a
# hand-rolled parser was measured against the live tree and found to misread 38+
# rules' tags alone. PyYAML is not a new dependency — evidence_guard.sh and
# spec_ref_guard.sh already fail-closed on its absence (pinned catalog-wide by
# pyyaml_preflight_coverage_guard.sh [95]) — this script requiring it too is
# conformance with an existing gate, not a new one.
set -euo pipefail
export LC_ALL=C

CATALOG="practices"
while [ $# -gt 0 ]; do
    case "$1" in
        --catalog) CATALOG="$2"; shift 2 ;;
        --catalog=*) CATALOG="${1#--catalog=}"; shift ;;
        *) echo "generate_index.sh: unknown arg: $1" >&2; exit 2 ;;
    esac
done

# Fail closed: without the parser there is nothing to trust — never degrade to a
# regex scraper (same policy as evidence_guard.sh / spec_ref_guard.sh).
if ! command -v python3 >/dev/null 2>&1 || ! python3 -c 'import yaml' >/dev/null 2>&1; then
    echo "generate_index.sh: BLOCK — cannot generate: python3 + PyYAML required (python3 -m pip install pyyaml)" >&2
    exit 2
fi

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
CATALOG_DIR="$REPO_ROOT/$CATALOG"
RULES_DIR="$CATALOG_DIR/rules"
OUT="$CATALOG_DIR/INDEX.md"

if [ ! -d "$RULES_DIR" ]; then
    echo "generate_index.sh: catalog '$CATALOG' has no rules/ dir at $RULES_DIR" >&2
    exit 2
fi

# File discovery + sort is bash's job (O2-C splits responsibility: bash owns
# glob/sort, python owns YAML) — same idiom as practices/generate_agents.sh.
shopt -s nullglob
RULE_FILES=()
for f in "$RULES_DIR"/*.md; do
    base="$(basename "$f")"
    [[ "$base" == ".gitkeep" ]] && continue
    [[ "$base" == "_template.md" ]] && continue
    RULE_FILES+=("$f")
done
IFS=$'\n' SORTED=($(printf '%s\n' "${RULE_FILES[@]}" | sort)); unset IFS
COUNT="${#SORTED[@]}"

if [ "$COUNT" -eq 0 ]; then
    echo "generate_index.sh: catalog '$CATALOG' has 0 rule files at $RULES_DIR — nothing to index" >&2
    exit 2
fi

python3 - "$CATALOG" "$OUT" "${SORTED[@]}" <<'PYEOF'
import sys
import hashlib
import re
import yaml

catalog = sys.argv[1]
out_path = sys.argv[2]
rule_paths = sys.argv[3:]


def rule_id(path):
    return path.rsplit('/', 1)[-1][:-3]  # strip .md


def parse_frontmatter(path, raw_text):
    lines = raw_text.split('\n')
    if not lines or lines[0].strip() != '---':
        return None, "no opening '---' delimiter"
    end_idx = None
    for i in range(1, len(lines)):
        if lines[i].strip() == '---':
            end_idx = i
            break
    if end_idx is None:
        return None, "no closing '---' delimiter"
    fm_text = '\n'.join(lines[1:end_idx])
    try:
        data = yaml.safe_load(fm_text)
    except yaml.YAMLError as exc:
        return None, "yaml parse error: %s" % exc
    if not isinstance(data, dict):
        return None, "frontmatter is not a mapping"
    return data, None


def classify_verification(data):
    v = data.get('verification')
    if not isinstance(v, dict):
        v = {}
    if v.get('type') == 'review':
        return 'review'
    if v.get('gradle_task'):
        return 'gradle:%s' % v.get('gradle_task')
    if v.get('guard') or v.get('guard_script'):
        return 'guard:%s' % (v.get('guard') or v.get('guard_script'))
    if v.get('type'):
        return str(v.get('type'))
    return 'unclassified'


def esc_title(title):
    title = str(title)
    title = title.replace('|', '\\|')
    title = re.sub(r'\s*\n\s*', ' ', title)
    return title.strip()


rows = []             # (id, impact_cell, kind, title)
tag_index = {}         # tag -> set(ids)
file_content_concat = []

for path in rule_paths:
    rid = rule_id(path)
    with open(path, 'r', encoding='utf-8') as fh:
        raw_text = fh.read()
    file_content_concat.append(raw_text)

    data, err = parse_frontmatter(path, raw_text)
    if err is not None:
        sys.stderr.write("generate_index.sh: %s: %s\n" % (path, err))
        sys.exit(1)

    title = data.get('title')
    if title is None or str(title).strip() == '':
        sys.stderr.write(
            "generate_index.sh: %s: missing 'title' in frontmatter — the INDEX "
            "cannot represent a rule without a title\n" % path
        )
        sys.exit(1)
    title = esc_title(title)

    impact = data.get('impact')
    impact_cell = str(impact).strip() if impact not in (None, '') else '-'

    kind = classify_verification(data)

    tags = data.get('tags')
    if isinstance(tags, list):
        for t in tags:
            if not isinstance(t, str) or not t.strip():
                continue
            tag_index.setdefault(t.strip(), set()).add(rid)

    rows.append((rid, impact_cell, kind, title))

# ── self-assertion 1+2: row count and id-set match the input file list ──────
input_ids = set(rule_id(p) for p in rule_paths)
row_ids = set(r[0] for r in rows)
if len(rows) != len(rule_paths):
    sys.stderr.write(
        "generate_index.sh: INTERNAL: row count %d != input file count %d\n"
        % (len(rows), len(rule_paths))
    )
    sys.exit(1)
if row_ids != input_ids:
    sys.stderr.write(
        "generate_index.sh: INTERNAL: rule-id set mismatch (missing=%s extra=%s)\n"
        % (sorted(input_ids - row_ids), sorted(row_ids - input_ids))
    )
    sys.exit(1)

rows.sort(key=lambda r: r[0])

# ── render "## By tag" ───────────────────────────────────────────────────────
tag_lines = []
for tag in sorted(tag_index.keys()):
    ids_sorted = sorted(tag_index[tag])
    tag_lines.append("- **%s** (%d) — %s" % (tag, len(ids_sorted), ", ".join(ids_sorted)))

# ── render "## Rules" ────────────────────────────────────────────────────────
rule_lines = ["| id | impact | verification | title |", "|---|---|---|---|"]
for rid, impact_cell, kind, title in rows:
    rule_lines.append("| %s | %s | %s | %s |" % (rid, impact_cell, kind, title))

# ── self-assertion 3: reparse own rendered "## By tag" text and confirm it
#    reconstructs the exact same (tag, id) pair set collected while parsing ──
rendered_pairs = set()
tag_line_re = re.compile(r'^- \*\*(.+?)\*\* \((\d+)\) — (.*)$')
for line in tag_lines:
    m = tag_line_re.match(line)
    if not m:
        sys.stderr.write("generate_index.sh: INTERNAL: unparseable tag line: %r\n" % line)
        sys.exit(1)
    tag_name, n, ids_csv = m.group(1), int(m.group(2)), m.group(3)
    ids_list = [i.strip() for i in ids_csv.split(',')] if ids_csv else []
    if len(ids_list) != n:
        sys.stderr.write("generate_index.sh: INTERNAL: tag %s count mismatch\n" % tag_name)
        sys.exit(1)
    for i in ids_list:
        rendered_pairs.add((tag_name, i))
source_pairs = set()
for tag, ids in tag_index.items():
    for i in ids:
        source_pairs.add((tag, i))
if rendered_pairs != source_pairs:
    sys.stderr.write("generate_index.sh: INTERNAL: tag index reconstruction mismatch\n")
    sys.exit(1)

# ── sentinel sha covers the rule concat, same semantics as generate_agents.sh:
#    cat each sorted file + a trailing newline, then sha256 the concatenation ──
concat = "".join(c + "\n" for c in file_content_concat)
sha = hashlib.sha256(concat.encode('utf-8')).hexdigest()
rule_count = len(rows)

# ── self-assertion 4: sentinel fields match what was actually indexed ───────
if rule_count != len(rule_paths):
    sys.stderr.write("generate_index.sh: INTERNAL: sentinel rule_count mismatch\n")
    sys.exit(1)

body = []
body.append('---')
body.append('sentinel:')
body.append('  source_concat_sha256: "%s"' % sha)
body.append('  rule_count: %d' % rule_count)
body.append('  generated_by: "practices/generate_index.sh"')
body.append('---')
body.append('')
body.append('# %s — Rule INDEX (auto-generated)' % catalog)
body.append('')
body.append('## By tag')
body.extend(tag_lines)
body.append('')
body.append('## Rules')
body.extend(rule_lines)
body.append('')

with open(out_path, 'w', encoding='utf-8') as fh:
    fh.write('\n'.join(body))

# ── census (non-vacuity, PRD F11): count unclassified rows, stderr ONLY —
#    never write "unclassified" into the index body for explanatory purposes,
#    or the census's own grep on the file would find its own output (false-RED). ──
unclassified_count = sum(1 for r in rows if r[2] == 'unclassified')
if unclassified_count != 0:
    sys.stderr.write(
        "generate_index.sh: BLOCK — %d unclassified verification row(s) in %s; "
        "a new frontmatter 'verification:' shape was silently absorbed — add a "
        "classify_verification() branch for it\n" % (unclassified_count, catalog)
    )
    sys.exit(1)
sys.stderr.write(
    "generate_index.sh: %s: 0 unclassified verification rows (%d total)\n"
    % (catalog, rule_count)
)
PYEOF

echo "wrote $OUT — $COUNT rules"
