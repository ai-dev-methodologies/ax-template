---
recipe: internal-it
verdict_version: "1"
recorded_at: "2026-05-22"
agent_context: "context-0 — given only recipes/internal-it/RECIPE.md + practices/AGENTS.md"
result:
  must_score: null
  must_total: 12
  should_score: null
  should_total: 8
  verdict: PENDING
  threshold: "≥10/12 MUST + ≥5/8 SHOULD"
---

# Sealed Verdict — internal-it recipe (SP46 will execute)

## Sealed Context (sub-agent input)

The sub-agent will receive **only** these two files at spawn time:

1. `recipes/internal-it/RECIPE.md`
2. `practices/AGENTS.md`

No other codebase context. No `specs/recipes/internal-it-recipe-l0.yaml` body,
no L4-composition, no L2-block-recipe. The RECIPE.md must be self-describing
enough that a context-0 sub-agent can re-derive the internal-it recipe's
composition + 5 invariants from it alone (cross-referencing catalog rules from
`practices/AGENTS.md` only).

## Sub-Agent Prompt (SP46 will execute)

```
You are given two files:
  1. recipes/internal-it/RECIPE.md — the internal-it recipe manifest
  2. practices/AGENTS.md — the ax-template practices catalog

Using ONLY these two files, describe the internal-it recipe. Your answer must
cover:

a) The enabled L4 domains (alphabetical) wired by the recipe
b) The 5 business invariants and what each binds to (spec_ref or
   co-shipped-rule + invariant_test)
c) Why INV-005 uses co-shipped-rule rather than spec_ref + rule_ref (the
   R7-community escape-hatch framing)
d) The first-consumer relationship with the webhook L4 (SP45 → SP45b binding)
e) At least one external ITSM platform documented as evidence
f) At least one Korean enterprise platform documented as evidence
g) The override_allowed scenarios (auth skip / webhook skip / feature-flags skip)

Do not use any information outside the two provided files. If you cannot
answer a sub-question from those two files, say "not derivable from sealed
context".
```

## MUST Rubric (12 items — to be executed by SP46)

| # | Criterion | Expected Source in RECIPE.md | Pass? |
|---|-----------|------------------------------|-------|
| M1 | Lists all 6 enabled L4 domains (audit-log, auth, crud, notification, scheduled-task, webhook) | Frontmatter `enabled_l4_domains:` alphabetical + §"Enabled L4 Domains" table | PENDING |
| M2 | Identifies `internal-it` as the closing recipe for the R6 Synthesis-A deferred queue | §"Business context" paragraph + cross-ref to TD-2026-05-22-026 | PENDING |
| M3 | Names INV-001 audit-log binding (`AUDIT-RECORD-001/002`) for ticket-state transitions | §"Business Invariants" table INV-001 row | PENDING |
| M4 | Names INV-002 scheduled-task lock + idempotency binding for SLA breach reminders | §"Business Invariants" table INV-002 row | PENDING |
| M5 | Names INV-003 webhook HMAC-SHA256 + exponential backoff binding for ITSM relay | §"Business Invariants" table INV-003 row | PENDING |
| M6 | Names INV-004 notification preferences + at-least-once delivery binding | §"Business Invariants" table INV-004 row | PENDING |
| M7 | Names INV-005 co-shipped-rule `webhook-secret-encryption` for per-endpoint KMS encryption | §"Business Invariants" table INV-005 row + §"INV-005 disambiguation" paragraph | PENDING |
| M8 | Explains INV-005 escape-hatch framing (R7 community precedent — no existing rule anchor) | §"INV-005 disambiguation (deliberate framing)" paragraph | PENDING |
| M9 | Identifies internal-it as FIRST CONSUMER of the webhook L4 primitive | §"Business context" + L4 Domains table notes | PENDING |
| M10 | Names ≥1 ITSM platform as external evidence (Jira / PagerDuty) | §"Evidence" yaml block | PENDING |
| M11 | Names ≥1 Korean platform as external evidence (Toss / Naver Works) | §"Evidence" yaml block | PENDING |
| M12 | Identifies the override_allowed scenarios (auth / webhook / feature-flags skip) | Frontmatter `override_allowed:` comment block | PENDING |

## SHOULD Rubric (8 items — to be executed by SP46)

| # | Criterion | Expected Source in RECIPE.md | Pass? |
|---|-----------|------------------------------|-------|
| S1 | Names the four ticket lifecycle states (open → in-progress → resolved → closed) | §"Business context" paragraph + INV-001 row | PENDING |
| S2 | Names ≥3 ITSM platforms as relay targets (Jira, ServiceNow, PagerDuty, Slack-incoming) | §"Enabled L4 Domains" webhook row + §"Business context" | PENDING |
| S3 | Names the per-endpoint signing-secret storage rule (KMS-managed AES-256 envelope) | INV-005 statement + spec-trio notes cross-ref | PENDING |
| S4 | Names the M5 "deferred indefinitely" promotion policy for webhook-secret-encryption | §"INV-005 disambiguation" promotion criterion paragraph | PENDING |
| S5 | Names the RFC 2104 + OWASP ASVS V13.2.6 anchor reuse with the receiver rule | INV-003 binding notes + cross-ref to TD-2026-05-22-025 | PENDING |
| S6 | Identifies SP45 as the cycle that shipped the webhook L4 NET-NEW Spec Trio | §"Business context" cross-ref to TD-2026-05-22-025 | PENDING |
| S7 | Names TD-2026-05-22-026 (deferred-queue closure ADR) | §"Business context" + INV-005 cross-refs | PENDING |
| S8 | Identifies the partial-tag inline annotation pattern on the webhook README | §"Applied Recipe Annotation" in L4-composition.md cross-ref (M6 Architect fix) | PENDING |

## Verdict

```
MUST:   PENDING / 12  (threshold: ≥10)
SHOULD: PENDING / 8   (threshold: ≥5)
VERDICT: PENDING (SP46 will execute the sealed sub-agent simulation and update this row)
```

**§7 Pre-Mortem mitigation honored:** RECIPE.md is fully self-describing —
enables all 12 MUST rubric items to be answered from the file alone. INV-005
disambiguation paragraph is explicit about the catalog-novel escape hatch and
the M5 deferred-indefinitely promotion criterion. The Evidence block carries
all 4 verbatim PASS rows (Jira + PagerDuty + Toss + Naver Works) plus the
documented downgrades (ServiceNow × 4, Atlassian Cloud × 3, PagerDuty
developer × 3, Kakao × 6).

**Evidence density note:** 4 English verbatim (Jira webhooks × 2 quotes +
PagerDuty webhooks × 2 quotes) + 4 Korean verbatim (Toss × 2 + Naver Works ×
2) = 8 total quote rows. Exceeds 1-floor with 3x buffer. 2 consecutive non-
zero-Korean cycles preserved (R8 lms classting + cms brunch → R9 internal-it
toss + naverworks).
