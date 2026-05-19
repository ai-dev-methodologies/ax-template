# Codex Critic R7 iter 2

## Verdict

ITERATE.

Six of the seven named closures are materially closed, and Synthesis-B is accepted. One narrow blocker remains: the PRD still contains the exact literal `applied_recipes: []` five times, so the requested spot validation does not pass.

## Closure check (7 + Synthesis-B)

1. L1 count: CLOSED. Active count is corrected to 49 in the PRD (`:28`, `:90`, `:359`, `:412`), and disk count is 49. The remaining `48` mentions are historical correction/changelog context, not an active L1 baseline.

2. TD-022 plus empty-list literal: BLOCKING PARTIAL. TD-022 is deleted/struck through at `:145`, `:350`, `:368`, but the exact `applied_recipes: []` literal still appears at `:144`, `:367`, `:412`, `:421`, `:425`. This violates the stated "no literal anywhere" closure and makes `grep -c "applied_recipes: \[\]" ...` return 5, not 0.

3. Korean 5 host attempts: CLOSED. Section 4.4 records Toss, Naver D2, Kakao Tech, Woowahan Tech Blog, and LINE Engineering attempts with URL, fetch/http status, 2026-05-20 iter-2 timestamp text, and downgrade rationale at `:228-232`. The zero-verbatim-cycle exception is explicit at `:237`.

4. Reddit upgraded to external: CLOSED. Reddit is recorded as `external` through `github.com/reddit-archive/reddit/wiki/API` with two verbatim quotes at `:197` and `:221`. Alternative Reddit host attempts are recorded for PRAW and Devvit quickstart at `:222-223`, with prior blocked Reddit hosts retained at `:224-225`.

5. Three invariant refs: CLOSED with Architect's informational caveat. `NOTIF-PREF-001` exists on disk at `specs/notification-l0.yaml:131`; `ASVS-V2.2.1` exists at `specs/auth-asvs-l1.yaml:47`; INV-005 is reshaped as `co-shipped-rule:` at PRD `:193`. The existing integrity guard still only recognizes `spec_ref:` / `rule_ref:` and will self-detect this at SP41b.

6. Critic L `/ax-verify-domain scheduled-task`: CLOSED. The command is removed from the SP41 binary gate and replaced by `bash skills/_tests/L4/scheduler-domain.test.sh` at `:128`, `:148`, `:168-180`, and `:247`. Remaining mentions are negative/changelog references, not a gate.

7. Synthesis-B Option 4: CLOSED. The PRD now has SP41 scheduler-atomic, SP41b community-atomic-sequential, and SP42 partial-tag-aware structure at `:57-80` and `:243-253`.

## Disk validation

- `wc -l docs/superpowers/specs/2026-05-20-r7-scheduler-community-prd.iter2.md` -> 433.
- `ls templates/L1/components | wc -l` -> 49.
- `grep -c "applied_recipes: \[\]" docs/superpowers/specs/2026-05-20-r7-scheduler-community-prd.iter2.md` -> 5. BLOCKING.
- `grep -c "TD-2026-05-20-022" docs/superpowers/specs/2026-05-20-r7-scheduler-community-prd.iter2.md` -> 2. Acceptable because both are deletion/no-specialization mentions.
- `grep -c "NOTIF-PREF-001" specs/notification-l0.yaml` -> 1.
- `grep -c "ASVS-V2.2.1" specs/auth-asvs-l1.yaml` -> 1.

## Independent attack

INFORMATIONAL: stale section heading at `:154`.

The accepted structure is 3 SPs, and the actual matrix says so at `:243` with SP41 / SP41b / SP42 rows. But §4 still reads `Deliverable Inventory (2 deliverables, 2 atomic SPs)`. This is not blocking because the executable plan, tag policy, and SP linearization all use the correct 3-SP Option 4 shape (`:57-80`, `:243-253`, `:303-304`, `:406`). It should be fixed to avoid reader confusion, for example: `2 deliverables, 3 SPs`.

## Final reasoning

Do not approve until the literal `applied_recipes: []` strings are removed or rewritten so the requested grep returns 0. The content can still express the intended constraint without the exact forbidden literal, for example by saying "NO empty applied-recipes array syntax" or "scheduler omits the applied-recipes key".

After that mechanical fix, the remaining evidence supports approval: the SP41/SP41b boundary is clean, the partial tag policy is explicit for SP41 PASS + SP41b FAIL, Korean zero-verbatim is documented rather than hidden, and the scheduler-domain test anchor is concrete enough despite `skills/_tests/L4/` being net-new.

## ADR (if APPROVE)

N/A.

## Re-review trigger (if ITERATE/REJECT)

Re-review after:

1. All exact `applied_recipes: []` literals are removed from the PRD and `grep -c "applied_recipes: \[\]" docs/superpowers/specs/2026-05-20-r7-scheduler-community-prd.iter2.md` returns 0.
2. Optional cleanup: update §4 heading from `2 atomic SPs` to the accepted 3-SP Option 4 shape.
