---
title: Dogfood-ledger real_bug findings MUST reference closure_commit_sha
impact: MEDIUM
impactDescription: "A real_bug entry without a recorded closure_commit_sha leaves the ledger ↔ git boundary one-way: the prose says 'Closure: X' but a future maintainer cannot mechanically verify which git revision actually landed the fix. The R71 ledger-guard enforces the classification field; R85 enforces re-open conditions on scope_deferral entries; R86 closes the symmetric gap on real_bug entries."
tags:
  - dogfood
  - ledger
  - catalog-quality
  - real-bug
  - closure-traceability
  - git
spec_ref: "specs/spring-practices-l0.yaml#PRACTICES-DOGFOOD-LEDGER-002"
verification:
  source: "practices/evals/dogfood_finding_real_bug_closure_commit_guard.sh (R86b — 46th hard guard)"
  pattern: "Every docs/dogfood-ledger/*.yaml entry where classification=real_bug MUST carry a closure_commit_sha field whose value (a) is non-empty, (b) matches ^[0-9a-f]{7,40}$, AND (c) resolves to an existing commit in the local repository (git cat-file -e <sha>^{commit})."
upstream:
  - "https://www.kernel.org/doc/html/latest/process/submitting-patches.html"
  - "https://docs.github.com/en/issues/tracking-your-work-with-issues/linking-a-pull-request-to-an-issue"
evidence:
  - source_type: external
    citation: "GitHub Docs — Linking a pull request to an issue (verbatim): 'You can also use closing keywords in a commit message. The issue will be closed when you merge the commit into the default branch, but the pull request that contains the commit will not be listed as a linked pull request.' The supported keywords are 'close, closes, closed, fix, fixes, fixed, resolve, resolves, resolved'. This is the direct precedent for R86: a fix lives in a commit, and the issue / finding records the commit that closed it. The traceability direction matches — closure_commit_sha points FROM the ledger entry TO the commit that landed the fix, just as GitHub's closing-keyword binding points FROM the issue TO the merging PR's commit."
    url: "https://docs.github.com/en/issues/tracking-your-work-with-issues/linking-a-pull-request-to-an-issue"
    quoted_at: "2026-05-27"
  - source_type: external
    citation: "Linux Kernel — Submitting Patches, Fixes: trailer convention. Verbatim trailing segment chosen so the static-fetch advisory can substring-match around the typographic-quote-wrapped middle on docs.kernel.org: 'tag with at least the first 12 characters of the SHA-1 ID, and the one line summary.' The full sentence on docs.kernel.org reads (with typographic quote marks around git bisect and Fixes:): If your patch fixes a bug in a specific commit, e.g. you found an issue using git bisect, please use the Fixes: tag with at least the first 12 characters of the SHA-1 ID, and the one line summary. The static fetcher cannot match the full sentence because the middle phrases (git bisect, Fixes:) are wrapped in Unicode curly quotes in the Sphinx rendering, which introduce stray whitespace after our quote-stripping normalisation. This is a supporting precedent for the broader practice of mechanically-linkable commit references, NOT a direct shape match — the kernel Fixes: tag points FROM the fixing commit BACK to the buggy commit, while R86 closure_commit_sha points FROM the ledger entry FORWARD to the fixing commit. The kernel community uses Fixes: trailers + the parallel Tested-by:/Reported-by: trailer family as the documented convention for making commit-graph relationships programmatically auditable; R86 cites that family for precedent, while the actual direction-correct analogy is the GitHub closing-keyword binding above."
    url: "https://docs.kernel.org/process/submitting-patches.html"
    quoted_at: "2026-05-27"
---

## Dogfood-ledger real_bug findings MUST reference closure_commit_sha

**Impact: MEDIUM — without a recorded closure SHA, the ledger and git history drift apart and the catalog cannot self-verify its own closures.**

R71 `dogfood_ledger_guard.sh` enforces the classification field on every finding (real_bug / scope_deferral / methodology_gap). R85 enforces an explicit re-open condition on scope_deferral entries. R86 closes the symmetric gap on real_bug entries: every closure MUST carry a `closure_commit_sha` field whose value is a real, resolvable git commit hash.

The prose-only "Closure: …" pattern that the catalog has used so far (e.g., "Closure: caught DataIntegrityViolationException in FavoriteController.handleConcurrentDuplicate") tells the reader WHAT changed but does not tell them WHICH commit landed the change. A future maintainer reading the ledger six months later cannot mechanically confirm the fix shipped — they must search git history by hand, infer from commit messages, and hope no later commit undid the work.

The fix shape follows GitHub's closing-keywords convention (the direct precedent — see evidence below): pin every closure to a specific git revision so the ledger ↔ git boundary is bidirectional. The Linux Kernel's `Fixes:` trailer is cited as a supporting precedent for mechanically-linkable commit references but points in the opposite direction (fixing commit → buggy commit) and is not the analogy shape R86 adopts.

**Incorrect — closure described in prose, no SHA:**

```yaml
- persona: P1
  finding: "F12: processQueue summary AUDIT.info only fires when processed > 0... Closure: AUDIT.debug 'verb=PROCESS_QUEUE_EMPTY total=0' on empty branch."
  classification: real_bug
  references_artifact_path: backend/src/main/java/.../EmailOutboxService.java
```

The closure prose is informative but the ledger cannot answer "did this actually land?" without a git search.

**Correct — closure_commit_sha pins the closure:**

```yaml
- persona: P1
  finding: "F12: processQueue summary AUDIT.info only fires when processed > 0... Closure: AUDIT.debug 'verb=PROCESS_QUEUE_EMPTY total=0' on empty branch."
  classification: real_bug
  closure_commit_sha: b475685
  references_artifact_path: backend/src/main/java/.../EmailOutboxService.java
```

Reader and guard can now both confirm the closure: `git show b475685` shows the actual diff.

**Apply this rule to**: every `real_bug` entry in `docs/dogfood-ledger/*.yaml`. The SHA may be short (≥ 7 hex chars) or full (40 hex chars).

**When NOT to apply**: entries classified as `scope_deferral` (those carry expiry triggers per R85) or `methodology_gap` (those drive methodology change, not a single closure commit). Only `real_bug` carries the SHA requirement.

A pair-with rule: R85 enforces re-open conditions on scope_deferral entries; R86 enforces closure SHAs on real_bug entries. Together they make every ledger row mechanically auditable in both directions — has-this-been-closed and when-will-this-re-open.

Reference: [Linux Kernel — Submitting Patches](https://www.kernel.org/doc/html/latest/process/submitting-patches.html)

Reference: [GitHub Docs — Linking a pull request to an issue](https://docs.github.com/en/issues/tracking-your-work-with-issues/linking-a-pull-request-to-an-issue)
