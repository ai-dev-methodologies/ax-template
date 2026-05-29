# IDW3 — Food-Delivery (Spring Boot backend) 3-persona dogfood — CONVERGENCE CONFIRMED

**Date:** 2026-05-29 · **Workflow:** wf_50425829-4c1 · 4 agents, 790K tokens, 1386s
**Method:** 3 personas built the SAME `fooddelivery` API (FoodOrder+status SM, delivery assignment,
review, 4-role RBAC) in isolated worktrees on the FULLY-hardened catalog (54 guards, 12 common/ helpers).
All 3 `complete`, 23-30 tests each GREEN, 54/54 guards GREEN per build.

## Headline: the loop is CONVERGING toward the north star (~85-90% complete, ~90% enforced)
- **Hand-roll trajectory collapsed:** IDW1 (3/3 hand-rolled EVERYTHING) → IDW2 (3/3 reused 3 IMW1-B helpers,
  but pagination/bulk still prose → hand-rolled) → **IDW3 (3/3 reused 7 helpers; only 2-3 NARROW residuals,
  none a catalog-owned primitive)**. Reuse:handroll ≈ 7:2. First real main-code consumer of all 4 IMW2-B helpers.
- **Gap severity dropped a full tier:** "primitive doesn't exist" (IDW1) → "primitive is prose-only" (IDW2) →
  **"primitive exists + is reused but has a sharp edge"** (IDW3). That progression = approaching the north star.
- **Prior debts CLOSED + validated:** scaffold checklist made onboarding "decisively obvious" to all 3 (the
  IDW1 ViolationProofTest-discovered-via-red-build gap is closed); controller_problemdetail_guard — the IDW2
  FALSE-enforcement claim — now actually BLOCKS a Map-returning handler (P-staff verified). 6/7 stress probes caught.

## IMW3 backlog (smaller + finer-grained than IMW1/IMW2 — convergence)
### #1 (highest value) — make the documented IDOR-safe-404 default correct-by-default
`common/ResourceNotFoundException` (@ResponseStatus 404) actually yields **403** under the reference
SecurityConfig: `sendError(404)` re-dispatches to `/error`, re-enters the filter chain, hits
`anyRequest().denyAll()`. 2/3 personas hit it; NO guard catches it (only a black-box 404 test does). The
documented primitive (CallerScope.ownerScopedOrThrow → ResourceNotFoundException) is a trap. **Fix:** add a
GLOBAL `@ExceptionHandler(ResourceNotFoundException) → 404 ProblemDetail` to `common/GlobalProblemDetailAdvice`
(verified absent) so the documented default just works + removes a per-domain hand-roll.
### #2 — page-size out-of-range → 400 (not 403)
`OffsetPageSupport.clamp` throws `IllegalArgumentException`; nothing maps it → same 403 trap. Map it to 400
PAGE_SIZE_INVALID (prefer a typed exception OffsetPageSupport throws + a GlobalProblemDetailAdvice handler,
over a broad IllegalArgumentException→400 which could mask real bugs).
### #3 — entity_migration_guard regex false-negative (G4, the ONLY new real enforcement hole)
`ENTITY_RE = ^\s*@Entity\s*(\(|$)` misses inline `@Entity @Table(...)` on one line → an un-migrated entity
ships GREEN. P-staff reproduced (own-line=EXIT1, same-line=EXIT0). Fix to a token-boundary match
`(?m)^\s*@Entity\b`; add a same-line regression fixture; audit other guards for the same own-line-anchor class.
### #4 (doc) — NEW-DOMAIN-CHECKLIST §0 business-role note
Domain roles (CUSTOMER/RESTAURANT/RIDER) are NOT security principals — model them as CallerScope-style row
relationships derived in the service, NOT @PreAuthorize authorities (role_literal_guard + signup
InvalidRoleException block adding them to UserRole). All 3 discovered this only by reading guard source.
### deferred — common/ParticipantScope (multi-actor / owner-SET authz)
CallerScope is single-owner; food-delivery needs a 3-axis visibility OR + an actor-may-transition axis. All 3
hand-rolled; P-senior shipped an OR-vs-AND bug. Lift candidate as marketplace/multi-party domains grow.

## Verdict
CONVERGING STRONGLY. The IMW3 backlog (4-5 completeness, 1 enforcement, finer-grained) is SMALLER than
IMW1 (~9+5) and IMW2 — the backlog itself is shrinking. Single highest-value next action: the global
ResourceNotFoundException→404 handler (maximal complete+enforced leverage from one change), + the G4 regex fix.
