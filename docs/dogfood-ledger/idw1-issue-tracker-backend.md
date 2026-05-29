# IDW1 — B2B Issue Tracker (Spring Boot backend) 3-persona dogfood

**Date:** 2026-05-29 · **Workflow:** wf_f18a4bc1-ce4 (w2gkc9web) · 4 agents, 742K tokens, 2012s
**Method:** 3 personas (박지영 Junior / 정도윤 Senior / 황태완 특급시니어) independently built the
SAME issue-tracker API (Project/Issue/Comment + status state machine + RBAC + optimistic lock +
pagination) in isolated git worktrees, each using + stress-testing the ax catalog/automation.
All 3 builds reached `status: complete` (testIssueTracker GREEN: 23/25/22 tests; run-all-guards 49/49).

This is the FIRST industry-app dogfood of the standardization pivot (spec-only catalog → real
implementation proven by dogfood). It empirically confirmed the project thesis and produced a
prioritized improvement backlog toward the north star (100% completeness + zero-tolerance enforcement).

## Headline finding (thesis confirmed)
- **COMPLETENESS:** the canonical shape is highly CONVERGENT — 3/3 independently produced byte-equivalent
  entity immutability, the EnumMap sole-mutator state machine, optimistic-lock + ETag/If-Match semantics,
  IDOR-safe 404, RFC9457 mapping, ViolationProofTest, per-domain gradle task. The standardization is REACHABLE.
  BUT the catalog is NOT complete: the two richest cross-cutting specs ship as **prose with ZERO reusable
  code**, so "use verbatim" was literally impossible and forced hand-rolling in every build.
- **ENFORCEMENT (decisive negative result):** the guard surface enforces META-STRUCTURE strongly
  (reachability, tag casing, no package cycles, mandatory ViolationProofTest) but enforces **NONE of the
  SUBSTANTIVE contracts on real domains** — `ArchitectureLayerBoundaryTest` imports only the demo
  `…practices` package. 4-5 behavioral deviations (Controller→Repository bypass, non-RFC9457 error body,
  missing migration, unbounded List query) slipped past 49/49 guards.

## Confirmed pre-existing violations (probe of the live tree, not the worktrees)
Re-scoping ArchUnit to the whole tree will surface these 10 — they must be fixed first:
- **Controller→Repository (9):** webhook/WebhookAdminController, auth/AdminController, auth/AuthSessionController,
  payment/PaymentAdminController, payment/PaymentCallbackController, scheduledtask/ScheduledTaskController,
  identityverification/IdentityVerificationAdminController, crud/ItemController, practices/PracticesDemoController.
- **Service→Controller (1):** payment/PaymentService → PaymentCallbackController (already banned, but scoped to practices).

## Improvement backlog (prioritized; the IMW1 program)
### Enforcement (toward zero-tolerance)
1. **[#1 leverage] Re-scope `ArchitectureLayerBoundaryTest` from `…practices` to `com.ax.template.authblueprint..`
   + add the Controller→Repository ban.** Requires fixing the 10 violations above first. One change converts a
   demo-only net into repo-wide layering enforcement across all 40 controllers / 45 repos / 40 services.
2. Guard: every 4xx/5xx from a domain controller returns `application/problem+json` (ArchUnit: @ExceptionHandler/
   @ControllerAdvice in a domain package must return ProblemDetail, not Map/String).
3. Bash guard: every `@Entity`/@Table in a domain package has a referencing `V###*.sql` (ddl-auto=create-drop
   hides entity↔migration drift; JVM tests can't catch it).
4. ArchUnit: forbid Repository methods returning raw `List`/`Collection` without `Pageable`/`Slice` (enforces
   PAGE-LIMIT-001 mechanically; allowlist small finders by annotation).
5. Guard: any `specs/*.yaml` path mentioned in backend code/comments must resolve (extend
   recipe_spec_referential_integrity_guard to code references).

### Completeness (ship REAL reference implementations, not prose)
6. `common/` ETag/If-Match optimistic-locking helper (closes optimistic-locking-l0's zero-code gap +
   the recurring real bug: `save()` vs `saveAndFlush()` leaving @Version=0). 3/3 hand-rolled the same thing.
7. Shared cross-cutting RFC9457 `@ControllerAdvice` covering `authblueprint.*` (PracticesProblemDetailAdvice is
   scoped to `practices`, so every domain hand-rolls MethodArgumentNotValidException→400). 3/3 hit this.
8. Lift idempotency to `common/` (only impl is locked in `payment.IdempotencyKeyStore`; cross-domain reuse = boundary violation).
9. Single-entry new-domain scaffold/checklist (entity+repo+service+controller+advice+migration+test+gradle-task+
   SecurityConfig+ViolationProofTest); ViolationProofTest mandatory is only learned when a guard fails.
10. project-membership primitive (MEMBER=own+assigned needed a membership concept the catalog lacks; 2/3 improvised).

### Coherence
11. **[done this wave] Reconcile pagination-l0:** 3/3 personas REJECTED the cursor/HMAC headline contract and
    converged on offset+envelope `{data, pagination:{page,pageSize,totalElements,totalPages,hasMore}}`, matching
    the existing ecommerce reference. The spec contradicted the reference code. Reconciled: offset+envelope is the
    canonical DEFAULT tier; cursor is an opt-in high-throughput tier.

## Divergences needing a canonical decision (from synthesis)
- Per-project seq allocator concurrency: adopt P-senior's pessimistic-lock allocator (others have a latent race).
- MEMBER-scoped list: visibility predicate MUST be in the repository query, never a post-fetch filter (else short pages).
- Every state-changing mutation requires If-Match (P-staff posture — strictly safer).
- New backend-only domain SHOULD still ship a spec YAML for traceability.

## Automation validation verdict
Build/test loop + run-all-guards worked reliably across all 3 (no false positives, no flakiness).
l4_domain_reachability_guard + test_tag_naming_convention_guard proved their value as zero-off-template ratchets.
The gap is purely that SUBSTANTIVE contracts are unenforced on real domains — addressed by backlog items 1-5.
