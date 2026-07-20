#!/usr/bin/env bash
# practices/evals/randomport_contextcache_dirtiescontext_guard.sh
# Codifies the R22 ContextCache lever as a mechanical, pre-commit-checkable contract.
#
# THE HAZARD (Spring TestContext ContextCache): the framework caches loaded
# ApplicationContexts in an LRU keyed by context configuration, capped by default at
# 32 (spring.test.context.cache.maxSize). A suite that boots more than 32 distinct
# @SpringBootTest contexts churns the LRU: a sibling context can be evicted (its Hikari
# pool shut down) BEFORE its own test methods run, surfacing as failures in a class that
# is itself correct. The failure is order-dependent and only manifests in the full
# `./gradlew test` aggregate — a developer running a single per-domain task never sees it.
#
# THE LEVER: annotate the affected class with @DirtiesContext (classMode = BEFORE_CLASS
# or AFTER_CLASS) to force a fresh context boot and remove it from cache reuse. Applied
# this session to BillingFlowIT, FeatureFlagFlowIT, ApiKeyComplianceTest,
# I18nPolicyComplianceTest, ReportExportComplianceTest, SessionRevocationCheckTest, and
# the RealtimePolicyComplianceTest that triggered the latest eviction.
#
# THE INVARIANT (generic, self-describing, NARROWED): any backend test source that
# BOTH (a) actually loads a Spring context (carries `@SpringBootTest`) AND (b) NAMES
# the ContextCache hazard (contains the literal token `ContextCache`) MUST also carry
# `@DirtiesContext`. Naming the hazard without mitigating it is the exact regression
# that returns the spurious aggregate failures. The guard generalizes to any
# fork-receiver whose @SpringBootTest suite grows past the cache cap: it turns a
# hard-won, invisible lesson into a binary contract.
#
# FQN RECOGNITION (Fix, 2026-07-20): both `@SpringBootTest` and `@DirtiesContext`
# are recognized with an OPTIONAL fully-qualified prefix — e.g.
# `@org.springframework.boot.test.context.SpringBootTest(webEnvironment = ...)`
# — using the same FQN-tolerant shape as admin_preauthorize_guard.sh's mapping
# recognizer (`@(?:[A-Za-z_][\w.]*\.)?AnnotationName\b`). Prior to this fix the
# check was a literal substring match on `@SpringBootTest` / `@DirtiesContext`,
# so a fully-qualified `@SpringBootTest` that named the hazard but carried no
# `@DirtiesContext` escaped the guard entirely (real R22 hole — the class is
# genuinely exposed to ContextCache eviction regardless of which import style
# it uses).
#
# SCOPE NOTE (narrowed 2026-07-20): a plain Jackson/unit test with NO
# `@SpringBootTest` cannot suffer ContextCache eviction pressure at all — there is
# no Spring context to cache or evict. Such a class may legitimately mention
# `ContextCache` in a comment (typically Javadoc, e.g. "Plain Jackson unit test —
# no {@code @SpringBootTest}, zero ContextCache pressure") to document WHY it
# deliberately stays plain-Jackson (avoiding the hazard, not exposed to it).
# Demanding `@DirtiesContext` on a non-Spring class is meaningless (the annotation
# has no effect without a context to dirty) and would falsely flag accurate
# documentation. Critically, that documentation sentence itself CONTAINS the
# literal token `@SpringBootTest` (naming what the class is NOT), so a naive
# raw-text search for `@SpringBootTest` would wrongly treat the file as a real
# Spring test — comments/Javadoc are stripped before the `@SpringBootTest` /
# `@DirtiesContext` checks so only actual annotation usage counts.
#
# Evidence (external):
#   Spring Framework Reference — "Context Caching" (ContextCache, default maxSize 32):
#     https://docs.spring.io/spring-framework/reference/testing/testcontext-framework/ctx-management/caching.html
#   Spring Framework Reference — "@DirtiesContext":
#     https://docs.spring.io/spring-framework/reference/testing/annotations/integration-spring/annotation-dirtiescontext.html
#
# Exit: 0 PASS · 1 a @SpringBootTest names ContextCache without @DirtiesContext · 2 usage/setup error.
#
# Usage:
#   bash practices/evals/randomport_contextcache_dirtiescontext_guard.sh
#   bash practices/evals/randomport_contextcache_dirtiescontext_guard.sh --test-root DIR
#   bash practices/evals/randomport_contextcache_dirtiescontext_guard.sh --fixtures   # self-test

set -uo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"

TEST_ROOT="$REPO_ROOT/backend/src/test"
RUN_FIXTURES=0
while [ $# -gt 0 ]; do
    case "$1" in
        --test-root) TEST_ROOT="$2"; shift 2 ;;
        --test-root=*) TEST_ROOT="${1#--test-root=}"; shift ;;
        --fixtures) RUN_FIXTURES=1; shift ;;
        *) echo "randomport_contextcache_dirtiescontext_guard: unknown arg: $1" >&2; exit 2 ;;
    esac
done

# scan_root <dir> → prints diagnostics, returns 0 PASS / 1 violation(s) found.
# Uses python3 to strip // and /* */ (incl. Javadoc) comments before checking for
# @SpringBootTest / @DirtiesContext — otherwise a Javadoc sentence like "no
# {@code @SpringBootTest}" (documenting the ABSENCE of a Spring context) would be
# misread as the annotation being present. `ContextCache` candidate-detection stays
# on raw text: that token is expected to live in prose/Javadoc either way.
scan_root() {
    local scan_dir="$1"
    if [ ! -d "$scan_dir" ]; then
        # No backend test tree (e.g. a fresh fork that has not added tests yet) — nothing to check.
        echo "randomport_contextcache_dirtiescontext_guard: no test root at $scan_dir — nothing to check"
        return 0
    fi
    if ! command -v python3 >/dev/null 2>&1; then
        echo "randomport_contextcache_dirtiescontext_guard: python3 not in PATH (required for comment-aware parsing)" >&2
        return 2
    fi

    python3 - "$scan_dir" <<'PYEOF'
import pathlib
import re
import sys

scan_dir = pathlib.Path(sys.argv[1])

# FQN-tolerant annotation recognizers (same shape as
# admin_preauthorize_guard.sh's MAPPING_START_RE): an optional dotted package
# prefix followed by the simple annotation name and a word boundary, so both
# `@SpringBootTest` and `@org.springframework.boot.test.context.SpringBootTest`
# are recognized — see FQN RECOGNITION note in the header.
SPRING_BOOT_TEST_RE = re.compile(r"@(?:[A-Za-z_][\w.]*\.)?SpringBootTest\b")
DIRTIES_CONTEXT_RE = re.compile(r"@(?:[A-Za-z_][\w.]*\.)?DirtiesContext\b")


def strip_comments(text: str) -> str:
    text = re.sub(r"/\*.*?\*/", "", text, flags=re.S)
    text = re.sub(r"//[^\n]*", "", text)
    return text


checked = 0
guarded = 0
violations = []

for jf in sorted(scan_dir.rglob("*.java")):
    raw = jf.read_text(encoding="utf-8", errors="replace")
    if "ContextCache" not in raw:
        continue  # does not name the hazard at all — not a candidate
    code = strip_comments(raw)
    if not SPRING_BOOT_TEST_RE.search(code):
        # Hazard named only in prose/Javadoc (e.g. explaining why this class stays
        # plain-Jackson) — no real Spring context is loaded, so @DirtiesContext
        # would be meaningless here. Not a candidate. See SCOPE NOTE above.
        continue
    checked += 1
    if DIRTIES_CONTEXT_RE.search(code):
        guarded += 1
    else:
        violations.append(jf.name)

if violations:
    for name in violations:
        print(
            f"VIOLATION: {name} is a @SpringBootTest that names the ContextCache "
            "hazard but carries no @DirtiesContext to mitigate it",
            file=sys.stderr,
        )
    print("", file=sys.stderr)
    print(
        f"randomport_contextcache_dirtiescontext_guard: {len(violations)} "
        "@SpringBootTest(s) name the ContextCache hazard without @DirtiesContext "
        "— BLOCKED",
        file=sys.stderr,
    )
    print(
        "Fix: add @DirtiesContext(classMode = ClassMode.BEFORE_CLASS) (or "
        "AFTER_CLASS) to force a fresh context — the sanctioned R22 lever.",
        file=sys.stderr,
    )
    sys.exit(1)

print(
    f"randomport_contextcache_dirtiescontext_guard: PASS — {guarded}/{checked} "
    "ContextCache-aware @SpringBootTest(s) carry @DirtiesContext"
)
sys.exit(0)
PYEOF
    return $?
}

# ── --fixtures self-test mode ────────────────────────────────────────────────
if [ "$RUN_FIXTURES" -eq 1 ]; then
    FIX="$SCRIPT_DIR/fixtures/randomport_contextcache_dirtiescontext"
    rc=0
    echo "[randomport_contextcache_dirtiescontext] pass/ (expect 0 — plain Jackson test naming the hazard, no @SpringBootTest)"
    scan_root "$FIX/pass"; got=$?
    if [ "$got" -ne 0 ]; then echo "  FAIL: pass/ exited $got, expected 0" >&2; rc=1; fi
    echo "[randomport_contextcache_dirtiescontext] fail/ (expect 1 — @SpringBootTest(RANDOM_PORT) naming the hazard, no @DirtiesContext)"
    scan_root "$FIX/fail" >/dev/null; got=$?
    if [ "$got" -ne 1 ]; then echo "  FAIL: fail/ exited $got, expected 1" >&2; rc=1; fi
    echo "[randomport_contextcache_dirtiescontext] pass_fqn/ (expect 0 — FQN @org...SpringBootTest(RANDOM_PORT) naming the hazard, mitigated by FQN @org...DirtiesContext)"
    scan_root "$FIX/pass_fqn"; got=$?
    if [ "$got" -ne 0 ]; then echo "  FAIL: pass_fqn/ exited $got, expected 0" >&2; rc=1; fi
    echo "[randomport_contextcache_dirtiescontext] fail_fqn/ (expect 1 — FQN @org...SpringBootTest(RANDOM_PORT) naming the hazard, no @DirtiesContext — the FQN escape this fix closes)"
    scan_root "$FIX/fail_fqn" >/dev/null; got=$?
    if [ "$got" -ne 1 ]; then echo "  FAIL: fail_fqn/ exited $got, expected 1" >&2; rc=1; fi
    if [ "$rc" -eq 0 ]; then
        echo "randomport_contextcache_dirtiescontext_guard --fixtures: PASS (pass→0, fail→1)"
    else
        echo "randomport_contextcache_dirtiescontext_guard --fixtures: FAIL" >&2
    fi
    exit "$rc"
fi

# ── live mode ─────────────────────────────────────────────────────────────────
scan_root "$TEST_ROOT"
exit $?
