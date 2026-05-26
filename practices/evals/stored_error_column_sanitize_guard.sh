#!/usr/bin/env bash
# practices/evals/stored_error_column_sanitize_guard.sh
# R81 (43rd hard guard) — mechanises practices/rules/server-side-stored-error-sanitize.md
# (R61) by scanning every JPA entity that declares a stored-error column
# (`last_error`, `error_message`, `failure_reason`, `last_failure_reason`) and
# verifying that either:
#
#   (a) the same entity source file contains a call to
#       `AuditPiiHelper.sanitizeReason(` — i.e. the entity itself scrubs
#       PII at the storage boundary (R63 pattern, preferred for new code);
#       OR
#
#   (b) the field is annotated `@PiiSanitized(reason = "...")` — the
#       documented escape hatch for fields whose sanitize happens in an
#       upstream service-layer caller.
#
# A stored-error field that has neither evidence is flagged as a violation.
#
# Exit codes:
#   0 — every stored-error column is sanitized (entity-level) or annotated.
#   1 — at least one column declared without sanitize evidence.
#   2 — usage / environment error.
#
# Usage:
#   bash practices/evals/stored_error_column_sanitize_guard.sh
#   bash practices/evals/stored_error_column_sanitize_guard.sh --root DIR
#
# Bash 3.2 compatible (no associative arrays, no ${var,,}).
set -uo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"

ROOT_OVERRIDE=""
while [ $# -gt 0 ]; do
    case "$1" in
        --root) ROOT_OVERRIDE="$2"; shift 2 ;;
        --root=*) ROOT_OVERRIDE="${1#--root=}"; shift ;;
        *) echo "stored_error_column_sanitize_guard: unknown arg: $1" >&2; exit 2 ;;
    esac
done

[ -n "$ROOT_OVERRIDE" ] && REPO_ROOT="$ROOT_OVERRIDE"
cd "$REPO_ROOT" || { echo "cannot cd to $REPO_ROOT" >&2; exit 2; }

BACKEND_DIR="backend/src/main/java"
[ ! -d "$BACKEND_DIR" ] && exit 0

# Stored-error column name pattern. Anchored to '@Column(name = "<name>")'.
COLUMN_PATTERN='@Column\([^)]*name[[:space:]]*=[[:space:]]*"(last_error|error_message|failure_reason|last_failure_reason)"'

violations=0
violation_lines=""

# Step 1: candidate files contain BOTH the column pattern AND a top-level
# @Entity annotation — excludes javadoc-only matches (e.g. PiiSanitized.java)
# and other non-entity files that mention the pattern in comments.
candidate_files=$(grep -lE "$COLUMN_PATTERN" -r "$BACKEND_DIR" 2>/dev/null || true)
files=""
for f in $candidate_files; do
    if grep -qE '^[[:space:]]*@Entity\b' "$f" 2>/dev/null; then
        files="$files $f"
    fi
done

[ -z "$files" ] && {
    echo "stored_error_column_sanitize_guard: no stored-error columns found (vacuously PASS)"
    exit 0
}

for file in $files; do
    # Each entity file may declare multiple stored-error columns. Loop over
    # every @Column match line and resolve the Java field on the following
    # non-empty source line.

    # Find line numbers of @Column matches in this file.
    match_lines=$(grep -nE "$COLUMN_PATTERN" "$file" 2>/dev/null | cut -d: -f1)

    # Pre-compute the file-wide self-sanitize signal (option (a)).
    file_has_sanitize=0
    if grep -qE 'AuditPiiHelper\.sanitizeReason[[:space:]]*\(' "$file" 2>/dev/null; then
        file_has_sanitize=1
    fi

    for column_line in $match_lines; do
        # Resolve the Java field name on the next non-empty/non-comment line.
        field_name=""
        next_line_no=$((column_line + 1))
        total_lines=$(wc -l < "$file" | tr -d ' ')

        while [ "$next_line_no" -le "$total_lines" ]; do
            raw=$(sed -n "${next_line_no}p" "$file")
            stripped=$(echo "$raw" | sed 's/^[[:space:]]*//;s/[[:space:]]*$//')

            if [ -z "$stripped" ] || \
               echo "$stripped" | grep -qE '^(//|/\*|\*)'; then
                next_line_no=$((next_line_no + 1))
                continue
            fi

            # Match `private|protected|public ... <name>;` — capture the last
            # identifier before the semicolon.
            field_name=$(echo "$stripped" \
                | grep -oE '(private|protected|public)[[:space:]]+[^;]+;' \
                | sed -E 's/.*[[:space:]]+([A-Za-z_][A-Za-z0-9_]*)[[:space:]]*;.*/\1/')
            break
        done

        if [ -z "$field_name" ]; then
            violations=$((violations + 1))
            violation_lines="${violation_lines}
$file:$column_line — stored-error @Column not followed by a recognisable field declaration"
            continue
        fi

        # Option (b): @PiiSanitized annotation on or just before the @Column
        # declaration. Look back up to 8 lines so multi-line annotations
        # (e.g. @PiiSanitized(reason = "..."\n+ "...")) are still detected.
        annotated=0
        look_start=$((column_line - 8))
        [ "$look_start" -lt 1 ] && look_start=1
        if sed -n "${look_start},${column_line}p" "$file" 2>/dev/null \
            | grep -qE '@PiiSanitized\b'; then
            annotated=1
        fi

        # Option (a): file-level sanitize evidence.
        if [ "$file_has_sanitize" -eq 1 ] || [ "$annotated" -eq 1 ]; then
            continue
        fi

        violations=$((violations + 1))
        violation_lines="${violation_lines}
$file:$column_line — field '$field_name' has no AuditPiiHelper.sanitizeReason call in the entity and no @PiiSanitized annotation"
    done
done

if [ "$violations" -gt 0 ]; then
    echo "VIOLATION: stored-error columns missing R61 sanitize evidence:" >&2
    echo "$violation_lines" >&2
    echo "" >&2
    echo "Apply ONE of:" >&2
    echo "  (a) call AuditPiiHelper.sanitizeReason(value) inside the entity setter / mutator" >&2
    echo "      (preferred — see WebhookDelivery.truncate / AuditExportJob.markFailed for R63 pattern), OR" >&2
    echo "  (b) annotate the field @PiiSanitized(reason = \"upstream caller scrubs before set\")." >&2
    echo "stored_error_column_sanitize_guard: $violations violation(s) — merge BLOCKED" >&2
    exit 1
fi

echo "stored_error_column_sanitize_guard: every stored-error column has R61 sanitize evidence"
exit 0
