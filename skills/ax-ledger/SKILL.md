---
name: ax-ledger
description: >
  Per-project usage + violation ledger for ax-template, with a retrospective→improvement loop. Every
  interaction with the enforcement (gate runs, rule violations, bypass attempts, requests refused for
  breaking a rule, progress) is captured to a per-project, per-user JSONL trace; a review surface turns
  recurring friction into concrete catalog improvements that make the next fork stronger.
metadata:
  tier: 2
  axis: feedback
retrieval:
  aliases:
    - ax-ledger
    - usage log
    - violation log
    - retrospective
    - 복기
    - improvement loop
    - review ledger
---

# ax-ledger — capture → review (복기) → improve → feedback

ax-template enforces rules mechanically. **ax-ledger makes that enforcement observable and
self-improving.** When a fork-receiver uses ax-template, every meaningful interaction with the gates
leaves a trace, sliced by `{project, user}`, so the team can review what happened in the current
session or across the project and turn recurring friction into catalog changes.

This closes the virtuous cycle in the project vision: *rule → enforcement → friction recorded →
catalog improved → next fork stronger → loop.*

## The model

```
 capture            review (복기)              improve                 feedback
 ───────            ────────────              ───────                 ────────
 every event   →    aggregate + surface   →   classify + file a   →   next fork inherits
 to .ax-ledger      improvement direction     catalog change          the answered rule
```

Events are append-only JSONL at `.ax-ledger/events.jsonl` (gitignored, machine-local — like
`.ax-verify/`). Each event: `{ts, project, user, head_sha, kind, severity, reviewed, …}` where
`project` = the git remote (the fork) and `user` = git `user.email`.

`kind` ∈
- `progress` — a step advanced (domain added, `test{Domain}` GREEN, spec item bound)
- `gate_run` — a verification gate ran (`outcome=pass|fail`)
- `violation` — an enforced rule/guard **blocked** something (`gate=`, `rule=`)
- `bypass_attempt` — someone tried to skip a gate (the Iron Law forbids this; it is still **logged**)
- `request_rejected` — a request was refused because it would break an enforced rule/method
- `dogfood_finding` — a persona/agent dogfood (IDW/FDW) confirmed a real gap/bug (carry `area=`, `severity=`)

**Dogfood integration (the catalog's gap-finder feeds the ledger).** Every persona/agent dogfood run
MUST log each confirmed finding as a `dogfood_finding` event, and the dogfood protocol MUST end by
running `ax-ledger-review.sh` to decide what to feed back. A dogfood finding is exactly a catalog gap
to answer, so `review` counts `dogfood_finding` toward the improvement-direction threshold alongside
gate violations. This is how the persona/agent dogfood (which finds the gaps) and the ledger (which
turns them into catalog change) are wired into one loop — they are no longer separate systems.

## 1. Capture — `ax-ledger-log.sh`

Automatic where possible, explicit where it needs judgment:

- **Gate runs** are auto-captured: `verify-completion.sh` logs `gate_run` (pass) or `violation`
  (fail) on every run — no wiring needed by the fork-receiver.
- **Rule-violating requests + bypass attempts** are logged by the agent the moment it refuses /
  detects one (see CLAUDE.md — this is a standing directive, not optional):

```bash
bash practices/scripts/ax-ledger-log.sh request_rejected rule=R25 \
     detail="asked to declare done without verify-completion" severity=block actor=user
bash practices/scripts/ax-ledger-log.sh bypass_attempt \
     detail="tried to skip the pre-push recency guard" severity=block actor=agent
bash practices/scripts/ax-ledger-log.sh progress gate=testCaching outcome=pass detail="caching 7/7 GREEN"
```

The logger never fails the caller (always exits 0) — observability must not block real work.

## 2. Review — `ax-ledger-review.sh` (복기)

```bash
bash practices/scripts/ax-ledger-review.sh                 # whole project
bash practices/scripts/ax-ledger-review.sh --since 2026-06-05   # this session (pass today)
bash practices/scripts/ax-ledger-review.sh --user dev@x.com --unreviewed
```

Prints counts by kind, the **top recurring rule/gate friction**, open bypass attempts, and — for any
rule/gate that crossed `AX_LEDGER_IMPROVE_THRESHOLD` (default 3) occurrences — a concrete **개선 방향
(improvement candidate)** with the four classifications to choose from.

## 3. Improve + feedback — `ax-ledger-resolve.sh`

Classify each recurring friction, file the catalog change, then record the resolution so the events
are answered and the next fork benefits:

| classification | meaning | the catalog change |
|---|---|---|
| `gap`    | a real hole | add/fix a rule or guard + record in `practices/DECISIONS.md` |
| `relax`  | over-strict | relax the rule **with external evidence** (RFC/OWASP/vendor) |
| `doc`    | unclear     | clarify the rule's rationale + its `fix_playbook` |
| `oneoff` | legitimate  | note it; no catalog change |

```bash
bash practices/scripts/ax-ledger-resolve.sh --match <gate-or-rule> --classification gap \
     --resolution "Closed by <change>" --decision "<DECISIONS anchor or commit>"
```

This marks every matching unreviewed event `reviewed=true` with the resolution + decision reference —
the audit trail of WHY the catalog changed.

## When to run it

- **End of a session / before a fork hand-off** — run `ax-ledger-review` to surface the session's
  friction and decide what (if anything) to feed back.
- **Periodically per project** — review unreviewed `block`-severity events; let recurring ones drive
  the next catalog wave.

The ledger is never a merge gate (reviewing is a human/agent judgment, not a binary) — but capture is
always-on, so nothing is lost.
