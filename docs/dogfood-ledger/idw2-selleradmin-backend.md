# IDW2 — E-commerce Seller-Admin (Spring Boot backend) 3-persona dogfood

**Date:** 2026-05-29 · **Workflow:** wf_64594591-6c3 (wz255uimm) · 4 agents, 810K tokens, 1383s
**Method:** 3 personas (박지영 Junior / 정도윤 Senior / 황태완 특급시니어) independently built the SAME
`selleradmin` API (Product+status SM, Inventory idempotent-adjust, SellerOrder fulfillment, bulk import)
in isolated worktrees on the **post-IMW1 (hardened) catalog**. All 3 reached `complete`.
This is the regression test that IMW1 actually took, AND the next gap-finding pass.

## Headline: IMW1 WORKED (decisive vs IDW1)
- **Completeness lift validated:** 3/3 personas REUSED all 3 IMW1-B helpers with **ZERO hand-rolls**
  (IDW1: 3/3 hand-rolled). All 3 used `OptimisticLockingSupport.etag/requireMatch` AND followed the
  saveAndFlush-before-ETag javadoc contract — **the exact @Version=0 stale-ETag bug the helper documents
  was avoided by all 3 because the javadoc named it.** All 3 used `IdempotencyKeyStore.findOrCreate` with
  identical per-seller scoping; all 3 relied on `GlobalProblemDetailAdvice` and did NOT re-hand-roll a
  `@Valid` handler. Discoverability met: the Junior found all 3 via `ls common/` + javadoc BEFORE coding.
- **Enforcement validated:** 3 of 4 hardened guards correctly BLOCKED P-staff's deliberate stress
  violations — unbounded raw `List` (ArchitectureUnboundedRepositoryListTest), orphan `@Entity` with no
  migration (entity_migration_guard), Controller→Repository (ArchitectureLayerBoundaryTest). Zero
  false-blocks on the personas' legitimate first-run builds.

## 🔴 Honesty finding (highest priority)
The catalog **over-claims an enforcement it does not have.** `controller_problemdetail` is still
unbuilt (IMW1-D pending): no ProblemDetail-return guard exists in `practices/evals/`, and P-staff's
deliberate `Map<String,String>`-returning `@ExceptionHandler` **passed all 51 guards + testPractices**.
The 11 pre-existing violations (auth/AuthExceptionHandler ×7, notification ×2, filestorage ×2) remain.
Verdict: "a false enforcement claim is worse than a known gap — it produces unjustified confidence."
→ Ship IMW1-D FIRST: an ArchUnit @PRACTICES test asserting every `@ExceptionHandler` under
`com.ax.template.authblueprint..` returns `ProblemDetail`/`ResponseEntity<ProblemDetail>`, wired to the
already-existing `base_controller` / `global_exception_handler` pass+fail fixtures, plus fixing the 11.

## IMW2 backlog (prioritized; triangulated by 3 independent builds)
### Enforcement
1. **[#1 — make the catalog honest] IMW1-D** controller-problemdetail ArchUnit guard + fix the 11.
2. **Cross-package simple-name collision guard.** `selleradmin.Product` vs `ecommerce.Product` →
   runtime `ConflictingBeanDefinitionException`/Hibernate `DuplicateMappingException`; ALL 3 hit it,
   no static guard predicts it. Guard: fail on duplicate `@Entity`/`@Service`/`@Repository`/`@RestController`
   simple names across packages without an explicit `name=`. + AGENTS.md domain-prefix convention.
3. **Role-honored-at-signup guard.** Unknown signup role silently downgraded to MEMBER (`UserRole.valueOf`
   catch → MEMBER); `SELLER` absent from the enum; surfaces as confusing 403s. Reject unknown roles 400,
   OR ArchUnit asserting every `@PreAuthorize("...ROLE_X")` literal maps to a `UserRole` constant.

### Completeness (prose→real code — the lift IMW1-B stopped short of)
4. **common/PageEnvelope + OffsetPageSupport.** pagination-l0 PAGE-OFFSET-001 pins the field set
   `{data, pagination:{page,pageSize,totalElements,totalPages,hasMore}}` but ships NO code; all 3 re-typed
   it; ecommerce/billing use a divergent `{items,total}` shape. THE most-used spec still prose-only.
   `PageEnvelope.from(Page<E>, mapper)` + `OffsetPageSupport.clamp/stableSort`. Highest completeness leverage.
5. **common/BulkResult<T> + per-item collector.** bulk-operation-l0 (207 partial / 413 / 400 / RFC9457
   per-item) is prose-only; all 3 hand-rolled. `BulkResult.partial(...)` + REQUIRES_NEW per-item collector.
6. **common/CallerScope.** Backend caller-identity/owner-scope primitive (the L0 kit has client-side
   use-caller-id but backend has none); every domain re-derives `Authentication.getName()`/ROLE_ADMIN +
   owner-scoped-404 by hand. `CallerScope.of(Authentication)` + `ownerScopedFindOrThrow`.
7. **IdempotencyKeyStore non-create overload.** `findOrCreate` is create-shaped (Supplier<UUID>);
   idempotency-l0 SCOPE-001 covers PUT/PATCH/DELETE which have no new id. Add `idempotent(scope,key,Runnable)`
   or a `wasFirst` boolean.

### Coherence
8. **money-l0 spec.** billing (long minor-units) vs payment (BigDecimal) is an internal contradiction all 3
   flagged; no canonical money contract + no no-float guard. Pin ONE representation, reconcile over time.
   (Sensitive — relates to the R108 money-currency exclusion; this RECONCILES the existing contradiction,
   not a new competing spec. Spec-first, guard-second.)

## Verdict
CONVERGING faster than IDW1. Completeness +1 (3 helpers at 3/3 reuse), enforcement +1 (3/4 guards block).
Gating risk now is **honesty of the enforcement surface**, not breadth — ship IMW1-D before adding anything,
then lift common/PageEnvelope (the most-repeated hand-roll). "Complete + enforced" is reachable.
