# Adversarial case: inter_rater (stub)

Purpose: validate that two independent reviewers score the same rule within a tolerance.

Status: **stub** — automation requires two human reviewer signals (e.g., GitHub PR review
checkbox states or external rating). Logic and threshold belong in P2-A's substance pass.

Skeleton for the eventual runner (`practices/evals/adversarial/run.sh --case inter_rater`):

1. Read two reviewer scores from a fixture file or PR metadata.
2. Compute absolute difference per dimension.
3. Return BLOCK if any dimension difference exceeds the threshold (e.g., 0.20).
4. Return PASS otherwise.

Not implemented in P1; deferred to P2-A.
