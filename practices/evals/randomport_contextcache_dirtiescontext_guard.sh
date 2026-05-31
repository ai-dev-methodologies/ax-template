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
# THE INVARIANT (generic, self-describing): any backend test source that NAMES the
# ContextCache hazard (contains the literal token `ContextCache`) MUST also carry
# `@DirtiesContext`. Naming the hazard without mitigating it is the exact regression that
# returns the spurious aggregate failures. The guard generalizes to any fork-receiver
# whose @SpringBootTest suite grows past the cache cap: it turns a hard-won, invisible
# lesson into a binary contract.
#
# Evidence (external):
#   Spring Framework Reference — "Context Caching" (ContextCache, default maxSize 32):
#     https://docs.spring.io/spring-framework/reference/testing/testcontext-framework/ctx-management/caching.html
#   Spring Framework Reference — "@DirtiesContext":
#     https://docs.spring.io/spring-framework/reference/testing/annotations/integration-spring/annotation-dirtiescontext.html
#
# Exit: 0 PASS · 1 a test names ContextCache without @DirtiesContext · 2 usage/setup error.
#
# Usage:
#   bash practices/evals/randomport_contextcache_dirtiescontext_guard.sh
#   bash practices/evals/randomport_contextcache_dirtiescontext_guard.sh --test-root DIR

set -uo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"

TEST_ROOT="$REPO_ROOT/backend/src/test"
while [ $# -gt 0 ]; do
    case "$1" in
        --test-root) TEST_ROOT="$2"; shift 2 ;;
        --test-root=*) TEST_ROOT="${1#--test-root=}"; shift ;;
        *) echo "randomport_contextcache_dirtiescontext_guard: unknown arg: $1" >&2; exit 2 ;;
    esac
done

if [ ! -d "$TEST_ROOT" ]; then
    # No backend test tree (e.g. a fresh fork that has not added tests yet) — nothing to check.
    echo "randomport_contextcache_dirtiescontext_guard: no test root at $TEST_ROOT — nothing to check"
    exit 0
fi

violations=0
checked=0
guarded=0

# Every *.java test that names the ContextCache hazard MUST also carry @DirtiesContext.
while IFS= read -r f; do
    [ -z "$f" ] && continue
    checked=$((checked + 1))
    if grep -q '@DirtiesContext' "$f"; then
        guarded=$((guarded + 1))
    else
        echo "VIOLATION: $(basename "$f") names the ContextCache hazard but carries no @DirtiesContext to mitigate it" >&2
        violations=$((violations + 1))
    fi
done < <(grep -rl --include='*.java' 'ContextCache' "$TEST_ROOT" 2>/dev/null)

if [ "$violations" -gt 0 ]; then
    echo "" >&2
    echo "randomport_contextcache_dirtiescontext_guard: $violations test(s) name the ContextCache hazard without @DirtiesContext — BLOCKED" >&2
    echo "Fix: add @DirtiesContext(classMode = ClassMode.BEFORE_CLASS) (or AFTER_CLASS) to force a fresh context — the sanctioned R22 lever." >&2
    exit 1
fi

echo "randomport_contextcache_dirtiescontext_guard: PASS — $guarded/$checked ContextCache-aware test(s) carry @DirtiesContext"
exit 0
