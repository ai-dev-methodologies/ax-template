#!/usr/bin/env bash
# practices/evals/substance_guard.sh — second hard gate that catches trivially-true rules.
#
# spec_ref_guard alone only checks the frontmatter pointer; a rule can satisfy spec_ref
# while saying nothing of substance (empty examples, placeholder URLs, etc.). This guard
# scores rule body substance and rejects rules below the threshold.
#
# Two DIALECTS, selected by --catalog (default: practices):
#
#   catalog=practices (Java) — checks per rule.md:
#     - frontmatter has non-empty `title` and `impactDescription`
#     - Body contains an "Incorrect" code block whose payload is not a placeholder
#     - Body contains a "Correct"   code block whose payload is not a placeholder
#     - At least one Reference / upstream URL that is not example.com / placeholder
#     Placeholder detection: the code block contains <= 1 substantive line, OR matches one
#     of the placeholder phrases (`no real example`, `placeholder`, `TODO`, `example only`).
#
#   catalog=practices-react — BACKLOG P2-37. Prior to this, `practices-react/evals/run.sh`
#   ran spec_ref/time_decay/evidence but never substance — the only gap, not a design
#   decision to leave React ungated. The Java dialect's body markers (`**Incorrect`,
#   `**Correct`, `Reference:`) do not port: a census over the 102 React rules found only
#   16/15/25 carry those exact markers. Rather than reuse a heuristic that silently no-ops
#   on 86/102 rules, this is a SEPARATE, FROZEN dialect — see "dialect=react-frozen-v1"
#   below. FORBIDDEN: a `--catalog` mode that is silently laxer than the Java dialect for
#   the sake of matching more rules; every clause below is checked, no exceptions.
#
#   dialect=react-frozen-v1 — four clauses, ALL must pass, exact parse semantics:
#     (1) frontmatter `impactDescription` is a non-empty scalar (quote-stripped, >=1
#         non-whitespace char).
#     (2) frontmatter `verification:` block has a non-empty `notes:` scalar, OR the body
#         has a heading matching `^##.*Verification` followed by >=1 non-blank prose line
#         before the next heading.
#     (3) body has >=1 fenced code block of >=3 non-blank lines whose text does NOT match
#         (case-insensitive) `TODO|FIXME|\.\.\.|placeholder|<your` — i.e. at least one
#         example is not a stub/elided/templated placeholder.
#     (4) body or frontmatter contains >=1 `https?://` URL.
#   A full census over all 102 rules at authoring time found 27 rules missing clause-2
#   `notes` and 7 missing a qualifying clause-3 block (mostly array/object spread `...`
#   tripping the ellipsis-placeholder detector) — every one was a REAL finding, remediated
#   in the rule body (added `notes:`, or added/adjusted one genuinely non-placeholder
#   example); the dialect itself was never weakened to route around a census result.
#
# Usage:
#   bash practices/evals/substance_guard.sh                          # default catalog=practices
#   bash practices/evals/substance_guard.sh --catalog practices-react
#   bash practices/evals/substance_guard.sh --catalog practices-react /abs/path/to/fixture-root
set -uo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"

CATALOG="${CATALOG:-practices}"
CATALOG_DIR_OVERRIDE=""
while [ $# -gt 0 ]; do
    case "$1" in
        --catalog) CATALOG="$2"; shift 2 ;;
        --catalog=*) CATALOG="${1#--catalog=}"; shift ;;
        /*) CATALOG_DIR_OVERRIDE="$1"; shift ;;
        *) echo "substance_guard: unknown arg: $1" >&2; exit 2 ;;
    esac
done

# ── Fail closed: this guard verifies through PyYAML ──────────────────────────
# Without the parser there is nothing to report, so exit 2 ("cannot verify") — NEVER 0.
# A skip that shares its exit code with a pass is a green gate that checked nothing,
# which is the failure class this catalog exists to prevent. Pinned mechanically by
# practices/evals/pyyaml_preflight_coverage_guard.sh [95].
if ! command -v python3 >/dev/null 2>&1 || ! python3 -c 'import yaml' >/dev/null 2>&1; then
    echo "substance_guard: BLOCK — cannot verify: python3 + PyYAML required (python3 -m pip install pyyaml)" >&2
    exit 2
fi

CATALOG_DIR="${CATALOG_DIR_OVERRIDE:-$REPO_ROOT/$CATALOG}"

if [[ "$CATALOG" == "practices-react" ]]; then
    # ── React dialect (dialect=react-frozen-v1, BACKLOG P2-37) ──────────────
    if [[ ! -d "$CATALOG_DIR/rules" ]]; then
        echo "substance_guard: catalog '$CATALOG' has no rules/ dir at $CATALOG_DIR/rules — nothing to check"
        exit 0
    fi
    RULES_DIR="$CATALOG_DIR" python3 - << 'PY'
import glob, os, re, sys
import yaml

rules_dir = os.environ["RULES_DIR"]
placeholder_re = re.compile(r'(TODO|FIXME|\.\.\.|placeholder|<your)', re.I)
url_re = re.compile(r'https?://')
heading_re = re.compile(r'^##.*Verification')
fence_re = re.compile(r'```[a-zA-Z0-9_+-]*\n(.*?)```', re.S)

paths = sorted(glob.glob(os.path.join(rules_dir, "rules", "*.md")))
paths = [p for p in paths if os.path.basename(p) not in ("_template.md", ".gitkeep")]

if not paths:
    print("substance_guard: dialect=react-frozen-v1 — rules/*.md is empty, nothing to check")
    sys.exit(0)

total_violations = 0
for path in paths:
    text = open(path, encoding="utf-8", errors="replace").read()
    m = re.match(r'^---\n(.*?)\n---\n(.*)$', text, re.S)
    findings = []
    if not m:
        findings.append("no YAML frontmatter block")
        fm, body = {}, ""
    else:
        fm_text, body = m.group(1), m.group(2)
        try:
            fm = yaml.safe_load(fm_text) or {}
        except yaml.YAMLError as e:
            fm = {}
            findings.append(f"frontmatter YAML parse error: {e}")

    # Clause 1 — impactDescription non-empty scalar.
    impact_desc = fm.get("impactDescription", "") if isinstance(fm, dict) else ""
    impact_desc = str(impact_desc).strip().strip('"').strip("'")
    if not impact_desc:
        findings.append("clause 1: frontmatter `impactDescription` is empty or missing")

    # Clause 2 — verification.notes non-empty, OR a "## ... Verification" heading
    # followed by >=1 non-blank prose line before the next heading.
    clause2_ok = False
    ver = fm.get("verification") if isinstance(fm, dict) else None
    if isinstance(ver, dict) and str(ver.get("notes", "")).strip():
        clause2_ok = True
    if not clause2_ok:
        lines = body.split("\n")
        for i, l in enumerate(lines):
            if heading_re.match(l):
                for j in range(i + 1, len(lines)):
                    if lines[j].startswith("#"):
                        break
                    if lines[j].strip():
                        clause2_ok = True
                        break
                if clause2_ok:
                    break
    if not clause2_ok:
        findings.append("clause 2: no frontmatter `verification.notes` and no `## ... Verification` "
                         "heading with a following prose line")

    # Clause 3 — >=1 fenced code block, >=3 non-blank lines, no placeholder pattern.
    clause3_ok = False
    for cb in fence_re.findall(body):
        nonblank = [l for l in cb.split("\n") if l.strip() != ""]
        if len(nonblank) >= 3 and not placeholder_re.search(cb):
            clause3_ok = True
            break
    if not clause3_ok:
        findings.append("clause 3: no fenced code block has >=3 non-blank lines free of "
                         "TODO|FIXME|...|placeholder|<your")

    # Clause 4 — >=1 URL in frontmatter or body.
    fm_text_for_url = m.group(1) if m else ""
    if not (url_re.search(fm_text_for_url) or url_re.search(body)):
        findings.append("clause 4: no https?:// URL in frontmatter or body")

    if findings:
        total_violations += 1
        rel = os.path.relpath(path, rules_dir)
        print(f"VIOLATION [{rel}] (dialect=react-frozen-v1):")
        for f in findings:
            print(f"  - {f}")

print(f"substance_guard: dialect=react-frozen-v1 — {len(paths)} rule(s) scanned, "
      f"{total_violations} violation(s)")
sys.exit(1 if total_violations > 0 else 0)
PY
    react_exit=$?
    if [[ $react_exit -ne 0 ]]; then
        echo "substance_guard: react rule(s) failed substance check (dialect=react-frozen-v1) — merge BLOCKED" >&2
        exit 1
    fi
else
    # ── Java dialect (default, catalog=practices) — unchanged behavior ──────
    if [[ ! -d "$CATALOG_DIR/rules" ]]; then
        echo "substance_guard: catalog '$CATALOG' has no rules/ dir at $CATALOG_DIR/rules — nothing to check"
        exit 0
    fi
    cd "$CATALOG_DIR"

    violations=0
    shopt -s nullglob

    for rule in rules/*.md; do
        [[ "$(basename "$rule")" == "_template.md" ]] && continue
        [[ "$(basename "$rule")" == ".gitkeep" ]] && continue

        awk -v F="$rule" '
            BEGIN {
                in_fm = 0; fm_done = 0
                title = ""; impact_desc = ""
                in_code = 0; code_block = ""
                section = ""; incorrect_payload = ""; correct_payload = ""
                ref_lines = ""
            }
            /^---/ {
                if (!fm_done) { if (in_fm) fm_done = 1; in_fm = !in_fm; next }
            }
            in_fm && /^title:/ { sub(/^title:[ \t]*/,""); gsub(/"/,""); title = $0; next }
            in_fm && /^impactDescription:/ { sub(/^impactDescription:[ \t]*/,""); gsub(/"/,""); impact_desc = $0; next }
            # Section markers in body
            fm_done && /^\*\*Incorrect/ { section = "incorrect"; next }
            fm_done && /^\*\*Correct/   { section = "correct"; next }
            fm_done && /^Reference:/    { ref_lines = ref_lines "\n" $0; section = "ref"; next }
            # Code-fence handling
            fm_done && /^```/ {
                if (in_code) {
                    # close fence — assign accumulated block to the active section
                    if (section == "incorrect" && incorrect_payload == "") incorrect_payload = code_block
                    else if (section == "correct" && correct_payload == "") correct_payload = code_block
                    in_code = 0; code_block = ""
                } else {
                    in_code = 1; code_block = ""
                }
                next
            }
            in_code { code_block = code_block "\n" $0; next }
            fm_done && section == "ref" { ref_lines = ref_lines "\n" $0; next }
            END {
                v = 0
                if (title == "")        { print "  - empty title"; v++ }
                if (impact_desc == "")  { print "  - empty impactDescription"; v++ }

                # placeholder heuristics
                placeholder_re = "no real example|placeholder|TODO|example only"

                if (incorrect_payload == "") {
                    print "  - missing Incorrect example"; v++
                } else if (incorrect_payload ~ placeholder_re) {
                    print "  - Incorrect block looks like a placeholder"; v++
                } else {
                    # count non-comment, non-empty lines
                    n = split(incorrect_payload, lines, "\n"); real = 0
                    for (i = 1; i <= n; i++) {
                        L = lines[i]; gsub(/^[ \t]+|[ \t]+$/,"",L)
                        if (L == "") continue
                        if (L ~ /^\/\//) continue
                        real++
                    }
                    if (real < 2) { print "  - Incorrect block has < 2 substantive lines (" real ")"; v++ }
                }

                if (correct_payload == "") {
                    print "  - missing Correct example"; v++
                } else if (correct_payload ~ placeholder_re) {
                    print "  - Correct block looks like a placeholder"; v++
                } else {
                    n = split(correct_payload, lines, "\n"); real = 0
                    for (i = 1; i <= n; i++) {
                        L = lines[i]; gsub(/^[ \t]+|[ \t]+$/,"",L)
                        if (L == "") continue
                        if (L ~ /^\/\//) continue
                        real++
                    }
                    if (real < 2) { print "  - Correct block has < 2 substantive lines (" real ")"; v++ }
                }

                if (ref_lines !~ /https?:\/\//) {
                    print "  - missing Reference URL"; v++
                } else if (ref_lines ~ /example\.com/) {
                    print "  - Reference URL points to example.com (placeholder)"; v++
                }

                if (v > 0) {
                    print "VIOLATION [" F "]: " v " substance issue(s)"
                    exit 1
                }
                exit 0
            }
        ' "$rule"
        [[ $? -ne 0 ]] && violations=$((violations + 1))
    done

    if [[ $violations -gt 0 ]]; then
        echo "substance_guard: $violations rule(s) failed substance check — merge BLOCKED" >&2
        exit 1
    fi
fi

# ── templates/ walk extension (§4.10) — REACHABILITY ONLY, NOT A SUBSTANCE CHECK ─────────
# Walk templates/**/*.{md,tsx,ts,yaml,java}.
# Zero-scan guard: if templates/ exists but produces zero matching files → FAIL ZERO_SCAN.
#
# BACKLOG P2-43 — WHAT THIS WALK DOES AND DOES NOT DO. It counts reachable template files
# and fails if the tree became unreachable (ZERO_SCAN). It applies NO substance clause to
# any of them, and the summary line below now says so out loud instead of leaving the §4.10
# banner to imply a check that was never written.
#
# WHY THE CLAIM IS NARROWED RATHER THAN PROMOTED (the sibling walk in evidence_guard.sh WAS
# promoted to real structural verification, because its evidence contract does port). Both
# dialects above score a RULE DOCUMENT: an Incorrect example, a Correct example, a Reference
# URL, a non-empty impactDescription. A template is source code, not a rule document, and a
# census of every candidate porting of that contract found each one needs an allowlist to
# survive first contact — i.e. it would encode exceptions, not an invariant:
#   • "body has >= 3 substantive lines" → 5 DELIBERATE re-export shells fail
#     (templates/L2/blocks/conditional-field.tsx and 3 siblings are deprecated back-compat
#     re-exports; templates/L4/search/.../results/page.tsx re-exports its L3 page by design).
#   • "no TODO/FIXME/placeholder token" → 58 files match, overwhelmingly the React
#     `placeholder` prop and shadcn's own "shows a placeholder while content is loading".
#   • "evidence payload is not trivially short" → the shortest live payloads are real,
#     correctly-cited Korean-language quotes (17-18 chars).
#   • "frontmatter declares a non-empty provenance_class" → 6 live files declare none.
# Inventing any of those here would add a new contract to 500+ files under a gate whose
# stated job is scoring rule bodies. The honest posture is: this walk asserts reachability,
# evidence_guard.sh's §4.10 walk asserts evidence structure, and neither pretends to the
# other's coverage. If a template substance contract is ever wanted it belongs in its own
# guard with its own census — not smuggled into a summary line here.

# BACKLOG P3-73 — the templates root is resolved from $REPO_ROOT, which is captured at
# the TOP of this script (before any `cd`), NOT re-derived from ${BASH_SOURCE[0]} here.
# The old code re-derived it at this point, i.e. AFTER the Java dialect's
# `cd "$CATALOG_DIR"`, so under a RELATIVE invocation — `bash practices/evals/substance_guard.sh`,
# which is exactly how .githooks/pre-commit calls it — the re-`cd` failed
# ("cd: practices/evals: No such file or directory"), the command substitution collapsed
# to "", TEMPLATES_DIR became "/templates", the `-d` test failed and the ENTIRE ZERO_SCAN
# subgate silently no-opped while the guard still exited 0. Whether the catalog is checked
# must never depend on how the caller spelled the path. Fail closed (exit 2, "cannot
# verify") if the root is unusable, and say so out loud if templates/ is absent — a skip
# that is indistinguishable from a pass is the failure class this catalog exists to prevent.
if [[ -z "$REPO_ROOT" || "$REPO_ROOT" == "/" || ! -d "$REPO_ROOT" ]]; then
    echo "substance_guard: BLOCK — cannot verify: repo root unresolved (REPO_ROOT='${REPO_ROOT}') — the templates/ walk would scan nothing" >&2
    exit 2
fi
TEMPLATES_DIR="$REPO_ROOT/templates"
if [[ ! -d "$TEMPLATES_DIR" ]]; then
    echo "substance_guard: templates/ walk SKIPPED — no templates/ directory at $TEMPLATES_DIR"
fi
if [[ -d "$TEMPLATES_DIR" ]]; then
    templates_count=0
    while IFS= read -r f; do
        [[ -f "$f" ]] && templates_count=$((templates_count + 1))
    done < <(find "$TEMPLATES_DIR" \
        -name "*.md" -o -name "*.tsx" -o -name "*.ts" -o -name "*.yaml" -o -name "*.java" 2>/dev/null)

    if [[ $templates_count -eq 0 ]]; then
        echo "substance_guard: ZERO_SCAN — templates/ exists but no scannable files found — merge BLOCKED" >&2
        exit 1
    fi
    echo "substance_guard: templates/ walk found ${templates_count} file(s) — reachability only; NO substance clause is applied to templates/** here (see P2-43 note above; evidence structure is gated by evidence_guard.sh's §4.10 walk)"
fi

echo "substance_guard: all rules pass"
exit 0
