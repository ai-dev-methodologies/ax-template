---
title: Dogfood-ledger real_bug findings MUST reference regression-test coverage
impact: MEDIUM
impactDescription: "R86 records WHICH commit landed a fix; R87 records HOW the fix is protected from regression. Without test-coverage evidence, a future refactor can silently re-open the same bug — the catalog has no mechanical signal that the closure ever had a test."
tags:
  - dogfood
  - ledger
  - catalog-quality
  - real-bug
  - regression-test
  - test-coverage
spec_ref: "specs/spring-practices-l0.yaml#PRACTICES-DOGFOOD-LEDGER-003"
verification:
  source: "practices/evals/dogfood_finding_real_bug_test_coverage_guard.sh (R87b — 47th hard guard)"
  pattern: "Every docs/dogfood-ledger/*.yaml entry where classification=real_bug MUST carry AT LEAST ONE of: closure_test_method (Java test method following practices_*_camelCase), closure_test_commit_sha (a valid git sha resolvable locally), closure_test_path (relative path to a regression test file that exists on disk), OR closure_verification_ref + closure_verification_reason (escape hatch for doc-only / comment-contract / config-only / external-system / fork-receiver-owned closures)."
upstream:
  - "https://www.sqlite.org/testing.html"
  - "https://www.kernel.org/doc/html/latest/dev-tools/kselftest.html"
evidence:
  - source_type: external
    citation: "SQLite — How SQLite Is Tested, §Regression Testing (verbatim): 'Whenever a bug is reported against SQLite, that bug is not considered fixed until new test cases that would exhibit the bug have been added to either the TCL or TH3 test suites. Over the years, this has resulted in thousands and thousands of new tests. These regression tests ensure that bugs that have been fixed in the past are not reintroduced into future versions of SQLite.' This is the direct precedent for R87: a fix is not 'done' until a test guards against the same defect returning. ax-template's dogfood ledger applies the same discipline — closure_test_method (or one of the alternate shapes) is the catalog's machine-readable equivalent of SQLite's 'new test cases that would exhibit the bug'."
    url: "https://www.sqlite.org/testing.html"
    quoted_at: "2026-05-27"
  - source_type: external
    citation: "Linux Kernel — Kselftest documentation (verbatim, rendered as docs.kernel.org with typographic-quote normalisation applied by the R85b advisory tool): 'The kernel contains a set of \"self tests\" under the tools/testing/selftests/ directory. These are intended to be small tests to exercise individual code paths in the kernel.' The same page documents that selftests are also used so 'when a new test gets added to test existing code to regression test a bug, we should be able to run that test on an older kernel.' Mirror for ax-template: closure_test_method points at the small targeted test that exercises the specific code path the bug was in; closure_test_commit_sha covers the case where the test landed in a distinct commit from the fix and the catalog wants both pinned."
    url: "https://docs.kernel.org/dev-tools/kselftest.html"
    quoted_at: "2026-05-27"
---

## Dogfood-ledger real_bug findings MUST reference regression-test coverage

**Impact: MEDIUM — without test coverage recorded alongside the closure commit, a future refactor can silently re-open the same bug.**

R71 enforces ledger schema. R85 enforces re-open conditions on `scope_deferral` entries. R86 enforces the closure_commit_sha on `real_bug` entries. R87 is the symmetric companion to R86: every closure that says "we fixed this" MUST also record HOW we know the fix is protected from regression.

The fix shape follows SQLite's testing policy (the direct precedent — see evidence below): a bug is not considered fixed until a regression test exists. The catalog records that regression test in one of four shapes, ordered by preference:

1. **`closure_test_method`** (preferred for Java backend closures): the test method name, following the established `practices_<CAT>_<NNN>_<camelCase>` convention or the matching `<DomainTestClass>#methodName` shape.
2. **`closure_test_commit_sha`**: a distinct git SHA where the regression test landed (used when the fix and the test are in separate commits).
3. **`closure_test_path`**: the file path to the regression test (used for non-Java tests like Playwright e2e or ad-hoc shell tests not tied to a class+method).
4. **`closure_verification_ref` + `closure_verification_reason`**: escape hatch for closures with no executable regression anchor. `closure_verification_reason` MUST be one of:
   - `doc-only` (closure is a comment / doc change with no behavior delta)
   - `comment-contract` (closure is a documented invariant clarification)
   - `config-only` (closure is an application.yml / build.gradle.kts toggle without test)
   - `external-system` (closure depends on an external system change verified out-of-band)
   - `fork-receiver-owned` (closure is a policy contract the catalog deliberately does NOT mandate test for)

**Executable test fields take precedence.** If a `closure_test_method` or `closure_test_path` is available, use that — `closure_verification_ref` is for closures that genuinely have no executable regression anchor.

**Incorrect — real_bug closure with no test evidence:**

```yaml
- persona: P2
  finding: "F4 (HIGH): note column stores user-owned free-text VERBATIM..."
  classification: real_bug
  closure_commit_sha: 8304c89
  references_artifact_path: backend/src/main/java/.../Favorite.java
```

The closure shipped, but the ledger gives a future maintainer no way to confirm a regression test guards the fix. A later refactor of the `Favorite` entity could re-introduce the PII leakage and the catalog would not catch it.

**Correct — closure_test_method pins the regression anchor:**

```yaml
- persona: P2
  finding: "F4 (HIGH): note column stores user-owned free-text VERBATIM..."
  classification: real_bug
  closure_commit_sha: 8304c89
  closure_test_method: "practices_DOGFOOD_LEDGER_002_realBugReferencesClosureCommit"
  references_artifact_path: backend/src/main/java/.../Favorite.java
```

**Correct — escape-hatch for a doc-only closure:**

```yaml
- persona: P2
  finding: "F4 (HIGH): note column stores user-owned free-text VERBATIM..."
  classification: real_bug
  closure_commit_sha: 8304c89
  closure_verification_ref: "Favorite.java javadoc + FavoriteService.add javadoc citing R61/R67/AuditPiiHelper.piiHash"
  closure_verification_reason: doc-only
  references_artifact_path: backend/src/main/java/.../Favorite.java
```

The escape-hatch path is permitted because R85's R85+R86 backfill on `F4` was javadoc-only — no behavior change, so no executable regression test could exist. The `closure_verification_reason: doc-only` makes that explicit.

**Apply this rule to**: every `real_bug` entry in `docs/dogfood-ledger/*.yaml`.

**When NOT to apply**: entries classified as `scope_deferral` (those carry expiry triggers per R85) or `methodology_gap` (those drive methodology change, not a single closure). Only `real_bug` carries the test-coverage requirement.

A pair-with rule: R86 pins WHICH commit landed the fix; R87 pins WHICH test guards against the bug returning. Together they form the closure-traceability triple (finding ↔ fix commit ↔ regression test) that makes every ledger row mechanically auditable end-to-end.

Reference: [SQLite — How SQLite Is Tested](https://www.sqlite.org/testing.html)

Reference: [Linux Kernel — Kselftest](https://www.kernel.org/doc/html/latest/dev-tools/kselftest.html)
