# Legacy Backfill Ledger

> Per ralplan R37 consensus (architect grandfathering + codex acceptance criterion #3).
> Pre-Appendix-C-in-force domains (R0..R28) are exempt from the
> `docs/blueprints/<domain>/{plan, progress, decisions}.md` requirement enforced
> by the new R37 guards, with explicit rationale captured here.
>
> Each entry MUST include rationale. Burn-down tracked per quarter.

## Schema

```
| domain | r_number | rationale | quarter_burn_down |
```

## Entries (18 older L4 backend domains)

| domain | r_number | rationale | quarter_burn_down |
|---|---|---|---|
| auth | R0 | pre-Appendix-C origin (genesis domain; methodology was being defined alongside this domain's implementation). Already has docs/blueprints/auth NOT REQUIRED in v1; promote to S1-S4 retrofit in Q3 2026. | Q3 2026 |
| crud | R0 | pre-Appendix-C origin (companion to auth). Same exemption. | Q3 2026 |
| payment | R-payment | **EXEMPT**: payment is the canonical S1-S12 reference. docs/blueprints/payment/ already exists with plan / progress / decisions / security-review / verification-log + acceptance/. This is the only domain fully compliant with METHODOLOGY Appendix C. | n/a — already compliant |
| rate-limit | R-rl | pre-Appendix-C. Methodology was extending. Q3 backfill. | Q3 2026 |
| notification | R15 | pre-Appendix-C-in-force. S1-S4 backfill deferred to Q3 2026. | Q3 2026 |
| audit-log | R14 | pre-Appendix-C. Q3 2026. | Q3 2026 |
| file-storage | R16 | pre-Appendix-C. Q3 2026. | Q3 2026 |
| search | R17 | pre-Appendix-C. Q3 2026. | Q3 2026 |
| scheduled-task | R18 | pre-Appendix-C. Q3 2026. | Q3 2026 |
| webhook | R19 | pre-Appendix-C. Q3 2026. | Q3 2026 |
| feature-flags | R20 | pre-Appendix-C. Q3 2026. | Q3 2026 |
| billing | R21 | pre-Appendix-C. Q3 2026. | Q3 2026 |
| identity-verification | R2 | pre-Appendix-C-in-force at original implementation timestamp; methodology was being authored. Q3 2026 burn-down. | Q3 2026 |
| ecommerce | R23 | pre-Appendix-C. recipes/e-commerce already documents the composition. Q3 2026 burn-down. | Q3 2026 |
| report-export | R29 | borderline: post-Appendix-C-in-force per git timestamp, but predates the explicit S1-S4 retrofit guard introduction at R37. Treat as legacy for this ledger; flag for Q3 priority. | Q3 2026 (priority) |
| api-key | R30 | same as report-export — borderline post-Appendix-C but pre-guard. Q3 priority. | Q3 2026 (priority) |
| approval-workflow | R31 | borderline post-Appendix-C; R31-iter1+2 substantive (found 3 real bugs). Q3 priority. | Q3 2026 (priority) |
| practices | n/a | meta-domain (the catalog enforces itself); no Spec Trio applies. Excluded. | n/a |

## Burn-down policy

- Q3 2026: prioritize R29-R31 (report-export, api-key, approval-workflow) — these are
  borderline post-Appendix-C and have the strongest "should have had docs" claim
- Q4 2026: 9 mid-tier (notification, audit-log, file-storage, search, scheduled-task,
  webhook, feature-flags, billing, identity-verification)
- Q1 2027: 5 genesis tier (auth, crud, rate-limit, ecommerce — these have the weakest
  S7 generalization signal since the patterns were being invented; backfill is mostly
  documentation, not auditing)

## Guard integration

The `l4_domain_reachability_guard.sh` and `composition_completeness_guard.sh`
introduced at R37 consult this ledger before requiring docs/blueprints/<domain>/
artifacts on any domain listed here. Future R37+ domains MUST have the artifacts
at S1 — they cannot be added to this ledger retroactively.
