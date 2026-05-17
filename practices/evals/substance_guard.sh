#!/usr/bin/env bash
# practices/evals/substance_guard.sh — second hard gate that catches trivially-true rules.
#
# spec_ref_guard alone only checks the frontmatter pointer; a rule can satisfy spec_ref
# while saying nothing of substance (empty examples, placeholder URLs, etc.). This guard
# scores rule body substance and rejects rules below the threshold.
#
# Checks per rule.md:
#   - frontmatter has non-empty `title` and `impactDescription`
#   - Body contains an "Incorrect" code block whose payload is not a placeholder
#   - Body contains a "Correct"   code block whose payload is not a placeholder
#   - At least one Reference / upstream URL that is not example.com / placeholder
#
# Placeholder detection: the code block contains <= 1 substantive line, OR matches one of
# the placeholder phrases (`no real example`, `placeholder`, `TODO`, `example only`).
set -uo pipefail

cd "$(dirname "$0")/.."

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

# ── templates/ walk extension (§4.10) ────────────────────────────────────────
# Walk templates/**/*.{md,tsx,ts,yaml,java}.
# Zero-scan guard: if templates/ exists but produces zero matching files → FAIL ZERO_SCAN.

SCRIPT_DIR_ABS="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT_ABS="$(cd "$SCRIPT_DIR_ABS/../.." && pwd)"
TEMPLATES_DIR="$REPO_ROOT_ABS/templates"
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
    echo "substance_guard: templates/ walk found ${templates_count} file(s)"
fi

echo "substance_guard: all rules pass"
exit 0
