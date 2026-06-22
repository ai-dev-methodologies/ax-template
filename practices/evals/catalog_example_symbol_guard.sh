#!/usr/bin/env bash
# practices/evals/catalog_example_symbol_guard.sh
# 2026-06-22 catalog-example/impl-drift closure (78th hard guard).
#
# Rule code examples are AI-targeted: an agent that reads a rule's ```java
# fence treats the symbols in it as the canonical shape to write. When a
# fence names a class that does NOT exist (a fabricated store method, a
# divergent state-machine name, the wave-8 deadlock example whose listing
# diverged from the shipped service), the agent faithfully reproduces a
# broken pattern. Iterations 2-3 of this improvement track fixed two such
# drifts BY HAND (api-idempotency example realigned to the reference impl;
# persistence-state-machine example annotated as intentionally generic).
# Nothing mechanically caught them. This guard closes that class of defect.
#
# What it scans:
#   practices/rules/*.md (skipping _template.md). For each rule it walks
#   ONLY fenced ```java blocks — prose mentions of a class are out of scope
#   (a rule legitimately names PaymentStateMachine in its title without it
#   being a code example to reproduce).
#
# Two DENY checks per java fence:
#   (a) SEED DENY-LIST — a small set of known-fabricated store-method calls
#       (e.g. idempotencyStore.computeIfAbsent) that must NOT appear in any
#       java fence unless the rule carries a matching annotation:
#           <!-- catalog-example-ok: <symbol> ... -->
#       naming the offending symbol. The shipped IdempotencyKeyStore exposes
#       no computeIfAbsent; an example that calls one teaches a method that
#       does not exist.
#   (b) SYMBOL EXISTENCE — any identifier matching *StateMachine or *Store
#       used inside a java fence must EITHER resolve to a real
#       backend/src/main/java/**/<Symbol>.java file OR be named in a
#       catalog-example-ok annotation in that same rule (the escape hatch
#       for a deliberately generic illustration, e.g. WorkStateMachine in
#       persistence-state-machine-atomic.md whose reference impl is
#       PaymentStateMachine).
#
# The annotation is the single, auditable escape hatch — a rule author who
# means the divergence declares it inline; an unannotated divergence is a
# drift the build blocks.
#
# Exit codes:
#   0 — every in-scope java fence is clean (or every divergence annotated)
#   1 — at least one unannotated seed-deny call or unresolved *StateMachine/
#       *Store symbol
#   2 — usage / environment error (rules dir missing, python3 missing)
#
# Usage:
#   bash practices/evals/catalog_example_symbol_guard.sh
#   bash practices/evals/catalog_example_symbol_guard.sh --root DIR
#
# Bash 3.2 compatible. Fast: pure file scan, no gradle.
set -uo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"

ROOT_OVERRIDE=""
while [ $# -gt 0 ]; do
    case "$1" in
        --root) ROOT_OVERRIDE="$2"; shift 2 ;;
        --root=*) ROOT_OVERRIDE="${1#--root=}"; shift ;;
        *) echo "catalog_example_symbol_guard: unknown arg: $1" >&2; exit 2 ;;
    esac
done
[ -n "$ROOT_OVERRIDE" ] && REPO_ROOT="$ROOT_OVERRIDE"

# Live repo keeps rules under practices/rules; minimal fixtures put them at
# <root>/rules (mirroring the evidence-quote-spotcheck fixture convention,
# which itself uses <root>/practices/...). Accept either layout.
if [ -d "$REPO_ROOT/practices/rules" ]; then
    RULES_DIR="$REPO_ROOT/practices/rules"
elif [ -d "$REPO_ROOT/rules" ]; then
    RULES_DIR="$REPO_ROOT/rules"
else
    echo "catalog_example_symbol_guard: rules dir not found under $REPO_ROOT (looked for practices/rules and rules)" >&2
    exit 2
fi

if ! command -v python3 >/dev/null 2>&1; then
    echo "catalog_example_symbol_guard: python3 not in PATH (required for parsing)" >&2
    exit 2
fi

REPO_ROOT="$REPO_ROOT" RULES_DIR="$RULES_DIR" python3 - <<'PY'
import glob
import os
import re
import sys

repo_root = os.environ["REPO_ROOT"]
rules_dir = os.environ["RULES_DIR"]
java_root = os.path.join(repo_root, "backend", "src", "main", "java")

# (a) Seed deny-list: known-fabricated store-method calls. A java fence that
# contains any of these must carry a catalog-example-ok annotation naming the
# offending receiver symbol, or it is a drift.
#   pattern -> (display, symbol-the-annotation-must-name)
SEED_DENY = [
    (re.compile(r"\bidempotencyStore\.computeIfAbsent\b"),
     "idempotencyStore.computeIfAbsent", "IdempotencyKeyStore"),
]

# (b) *StateMachine / *Store class identifiers used in a code block.
SYM_RE = re.compile(r"\b[A-Z][A-Za-z0-9]*(?:StateMachine|Store)\b")

# catalog-example-ok annotation: collect every whitespace-delimited token it
# names so both seed-deny symbols and *StateMachine/*Store symbols can opt out.
ANN_RE = re.compile(r"<!--\s*catalog-example-ok:\s*(.*?)\s*-->")


def annotated_symbols(text):
    syms = set()
    for m in ANN_RE.finditer(text):
        for tok in re.split(r"[\s,]+", m.group(1)):
            tok = tok.strip()
            if tok:
                syms.add(tok)
    return syms


def java_fence_lines(text):
    """Yield (lineno, line) for every line INSIDE a ```java fenced block."""
    in_java = False
    for idx, line in enumerate(text.splitlines(), start=1):
        s = line.strip()
        if s.startswith("```"):
            lang = s[3:].strip().lower()
            in_java = (lang == "java") if not in_java else False
            continue
        if in_java:
            yield idx, line


# Build the set of backing java symbols once (basename without .java).
java_symbols = set()
for root, _dirs, files in os.walk(java_root):
    for fn in files:
        if fn.endswith(".java"):
            java_symbols.add(fn[:-len(".java")])

violations = []
fences_scanned = 0
rules_scanned = 0

for rule_path in sorted(glob.glob(os.path.join(rules_dir, "*.md"))):
    if os.path.basename(rule_path) == "_template.md":
        continue
    text = open(rule_path, encoding="utf-8", errors="replace").read()
    rules_scanned += 1
    ann = annotated_symbols(text)
    rel = os.path.relpath(rule_path, repo_root)

    saw_fence = False
    for lineno, line in java_fence_lines(text):
        saw_fence = True
        # (a) seed deny-list
        for pat, display, need_sym in SEED_DENY:
            if pat.search(line) and need_sym not in ann:
                violations.append(
                    f"{rel}:{lineno} — fabricated store call '{display}' in a "
                    f"java fence and no '<!-- catalog-example-ok: {need_sym} ... -->' "
                    f"annotation (the shipped {need_sym} exposes no such method)"
                )
        # (b) *StateMachine / *Store symbol existence
        for m in SYM_RE.finditer(line):
            sym = m.group(0)
            if sym in java_symbols or sym in ann:
                continue
            violations.append(
                f"{rel}:{lineno} — code-example symbol '{sym}' has no backing "
                f"backend/src/main/java/**/{sym}.java and no "
                f"'<!-- catalog-example-ok: {sym} ... -->' annotation (rule "
                f"example drifted from the reference impl)"
            )
    if saw_fence:
        fences_scanned += 1

if violations:
    print(
        "VIOLATION: rule code example(s) reference symbols that neither exist "
        "nor are annotated (catalog-example/impl-drift):",
        file=sys.stderr,
    )
    # de-dup while preserving order (a symbol can recur on many lines)
    seen = set()
    for v in violations:
        if v in seen:
            continue
        seen.add(v)
        print(f"  {v}", file=sys.stderr)
    print("", file=sys.stderr)
    print(
        "Either realign the example to the shipped class, or declare the "
        "divergence inline with '<!-- catalog-example-ok: <Symbol> reason -->' "
        "naming the symbol (see persistence-state-machine-atomic.md for the "
        "intentionally-generic pattern).",
        file=sys.stderr,
    )
    print(
        f"catalog_example_symbol_guard: {len(seen)} violation(s) across "
        f"{rules_scanned} rule(s) — merge BLOCKED",
        file=sys.stderr,
    )
    sys.exit(1)

print(
    f"catalog_example_symbol_guard: all java fences clean "
    f"({fences_scanned} fence-bearing rule(s) of {rules_scanned} scanned; "
    f"{len(java_symbols)} backing java symbols)"
)
sys.exit(0)
PY
