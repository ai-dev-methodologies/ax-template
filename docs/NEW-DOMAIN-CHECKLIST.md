# New Backend Domain — Single-Entry Scaffold Checklist

> Closes IDW1 dogfood completeness gap #9: a newcomer had to reverse-engineer the
> required artifact set by reading a whole existing domain, and only learned a
> `ViolationProofTest` was mandatory when a guard failed. This is the discoverable
> single entry point. Follow it top-to-bottom for any new Spring Boot domain under
> `com.ax.template.authblueprint.<domain>`. The mechanical guards listed in [§Enforcement]
> will FAIL your build if you skip a step — that is intentional (zero-tolerance).

## ▶ STEP 0 — Plan first: `/ax-scaffold` → `/ax-plan` → implement (the G6 forcing wire)

You do **not** start a domain by writing code. The chain is mechanically forced:

1. `bash skills/ax-scaffold/scripts/new-domain.sh <domain>` — emits the empty Spec Trio
   skeleton (`specs/<domain>-l0.yaml` + frontend spec + contracts + manifests), each
   carrying a `# TODO: Add` marker.
2. **`/ax-plan <domain>`** — interview (domain_mode / standards / complexity) → fill the
   Spec Trio with real items → emit **1:1 RED `@Tag` stubs** (`emit-red-stubs.sh`) →
   register `domain_mode` in `trio_integrity_allowlist.yaml` + the `test<Domain>` Gradle
   task → `docs/blueprints/<domain>/{plan.md,progress.md}`. This is the **fill+map** stage.
3. Only now implement (RED → GREEN), top-to-bottom through this checklist.

**Why you cannot skip `/ax-plan`:** `spec_scaffold_unfilled_guard.sh` (hard gate [70])
BLOCKS the build while any spec still carries the `# TODO: Add` scaffold marker. The
trio + binding guards pass an *empty* skeleton vacuously; this guard is what makes the
unplanned skeleton fail. `bash skills/ax-plan/scripts/check-plan-complete.sh <domain>`
must exit 0 (PLAN_COMPLETE, all items RED-bound) before implementation is meaningful.

## 0. Reuse before you build (the catalog already ships this code)
Do **NOT** hand-roll these — import the real implementations from `common/`:

| Need | Use | Spec |
|---|---|---|
| Optimistic locking (ETag / If-Match / 428·412·409) | `common/OptimisticLockingSupport` | optimistic-locking-l0 |
| Validation / 415 / 405 / malformed → `application/problem+json` | `common/GlobalProblemDetailAdvice` (already global; do NOT re-handle `@Valid`) | problem-details-l0 |
| Idempotent create/mutation (`Idempotency-Key`) | `common/IdempotencyKeyStore` | idempotency-l0 |
| PII hashing in audit rows | `common/AuditPiiHelper` | — |
| List/collection responses | offset + body envelope `{data, pagination:{page,pageSize,totalElements,totalPages,hasMore}}` (cursor is opt-in) | pagination-l0 |
| Batch endpoints | per-item partial-success (207-style) report | bulk-operation-l0 |

Read a clean reference domain end-to-end first: `commentthread`, `approvalworkflow`, or `billing`.

> **Business roles ≠ security principals (IDW3).** Domain roles like `CUSTOMER`/`SELLER`/`RIDER`/`RESTAURANT`
> are NOT JWT authorities — the auth system only mints `UserRole` = {ADMIN, MANAGER, MEMBER, AUDITOR}, and
> `role_literal_guard` blocks any `@PreAuthorize("...ROLE_X")` outside that set (signup also rejects an unknown
> role with 400). Model a business role as a **relationship** derived in the service from the resource
> (use `common/CallerScope` + an owner/participant check), NOT as a `@PreAuthorize` authority or a new
> `UserRole` constant. Throw `common/ResourceNotFoundException` for the not-yours case — it maps to a
> 404 problem+json via `GlobalProblemDetailAdvice` (IDOR-safe; never 403-leak existence).

## 1. Required artifact set (every domain)
Create these under `backend/src/main/java/com/ax/template/authblueprint/<domain>/`:

1. **Entity(ies)** — Builder pattern; package-private mutators (entity is its own sole mutator);
   `@Column(updatable=false)` on identity/immutable columns; `@Version Long version` if the
   resource is mutated concurrently; **no public setters**.
2. **Repository** — `extends JpaRepository<…>`; any method returning a collection **takes a
   `Pageable`** (an unbounded raw-`List` return fails `ArchitectureUnboundedRepositoryListTest`).
3. **Service** (`@Service`, `@Transactional` where it writes) — the **sole orchestrator**: all
   repository access, RBAC (`MEMBER`/`MANAGER`/`ADMIN` via `Authentication.getName()`, never a
   path/body param), and IDOR-safe `404` (never leak existence) live here.
4. **Controller** (`@RestController`) — **thin**; delegates to the service ONLY. It **must not
   inject or call any `*Repository`** (`ArchitectureLayerBoundaryTest` bans Controller→Repository
   across the whole tree). Put `@PreAuthorize(...)` for method-level RBAC here.
5. **State machine** (if the entity has a lifecycle) — sole mutator of `status`; an `EnumMap`
   ALLOWED-transition graph; an illegal edge throws a domain exception → `409`. (Mirror
   `ApprovalRequestStateMachine` / `SubscriptionStateMachine`.)
6. **Domain `@ExceptionHandler`** — ONLY for your domain-specific exceptions, and it **must return
   `ProblemDetail`** (RFC 9457). Framework exceptions (`@Valid`, 415, 405, malformed body) are
   already handled by `common/GlobalProblemDetailAdvice` — do not re-handle them.
7. **Flyway migration** `backend/src/main/resources/db/migration/V###__create_<domain>.sql` —
   **REQUIRED for every `@Entity`** (`entity_migration_guard`). Match the entity's columns; tests
   run on `ddl-auto=create-drop` so the migration is not auto-exercised — write it carefully.
8. **SecurityConfig** — add the domain's request-matcher (authenticated / permitAll / role).

## 1b. Aggregate decomposition (DDD — MANDATORY for every `@Entity`)
> Spec: `docs/superpowers/specs/2026-06-08-ddd-decomposition-rules-design.md`. An aggregate
> is a transactional consistency boundary (Vernon). A feature package = bounded context and
> MAY hold several aggregate roots (e.g. dispatch = Provider / Offer / ServiceRequest).

- [ ] **Tag the ROOT entity `@AggregateRoot`** (`common/AggregateRoot`) — the entry point for
      all mutation, the only entity with global (repository) access.
- [ ] **Tag every MEMBER entity `@AggregateMember(root = <Root>.class)`** (`common/AggregateMember`)
      — a composition part loaded/saved through its root (e.g. `OrderItem` → `Order`). The
      `root` attribute is mandatory. *Every `@Entity` must carry exactly one of these two*
      (`aggregate_tagging_completeness_guard` fails the build otherwise).
- [ ] **Repository only on roots** — a `@AggregateMember` must NOT have its own `*Repository`;
      mutate/load it through its root (`HG-AGG-REPO`). A genuine exception goes in
      `practices/evals/aggregate_boundary_allowlist.yaml` (`kind: member-repo`, with expiry).
- [ ] **Reference other aggregates by identity, not object pointer** — no entity field may be
      typed as a *different* aggregate's `@AggregateRoot` (`HG-AGG-REF`). Child→own-root
      back-references are fine; store a `Long <other>Id` for cross-aggregate links.
- [ ] **Mutate ONE aggregate per `@Transactional` method** — coordinate the rest via a published
      service / domain event, not a second `repo.save(...)` in the same method body
      (`HG-ANTI-GODSERVICE-TX`; governed exceptions in `governed_god_service`).
- [ ] **Keep members encapsulated** — never reference another feature's `@AggregateMember` from
      outside its package; expose the root (`HG-AGG-MEMBER-ENCAP`).
- [ ] **Cross-feature access = published API only** — to call another feature, depend on a
      `@PublishedApi` type (or a `shared_kernel` package), never its `@Entity`/`*Repository`
      (`HG-FEAT-ISOLATION`, default-deny). Composition/grandfather edges go in the allowlist.
- [ ] **Controller is resource-oriented** — one thin `/api/<resource>` controller per root, never
      verb-per-endpoint (`Create*Controller`/`List*Controller`/`Get*Controller`) and never a
      top-level `controllers/`/`services/`/`repositories/` package (`HG-ANTI-SPLIT-ENDPOINT`,
      `HG-FEAT-TOPLEVEL-TECH`).
- [ ] **State machine is the sole state mutator** — if root `X` has an `XStateMachine`, only it
      may call `X.setStatus/setState` (`HG-STATE-SOLE-MUTATOR`; governed callers in
      `governed_state_mutators`).

## 2. Required tests
Under `backend/src/test/java/com/ax/template/authblueprint/<domain>/`:

9. **`<Domain>ComplianceTest`** (RestAssured, black-box HTTP) — happy-path CRUD + RBAC denials +
   cross-entity IDOR `404` + illegal transition `409` + optimistic conflict (`428` missing
   If-Match / `412` stale, via `OptimisticLockingSupport`) + pagination envelope + `problem+json`
   error shape. (MockMvc-only tests are forbidden — not portable.)
10. **`<Domain>ViolationProofTest`** — **MANDATORY** (`l4_domain_reachability_guard` fails the build
    without it): reflection-based structural negatives — immutable columns reject writes, `@Version`
    present + non-null, no public setters, state machine rejects skip/reverse edges.

## 3. Build wiring
11. **Per-domain gradle task** in `backend/build.gradle.kts`:
    ```kotlin
    tasks.register<Test>("test<Domain>") {
        useJUnitPlatform { includeTags("<DOMAIN>") }   // UPPERCASE tag (test_tag_naming_convention_guard)
        group = "verification"
    }
    ```
    Tag every test class `@Tag("<DOMAIN>")` (+ specific `@Tag("<DOMAIN>-XXX-001")`), all UPPERCASE.

## 4. Enforcement (these guards mechanically check your work)
| Guard | Catches |
|---|---|
| `ArchitectureLayerBoundaryTest` | Controller→Repository, Service→Controller, Repository→upper |
| `ArchitectureUnboundedRepositoryListTest` | raw `List` repo return without `Pageable` |
| `entity_migration_guard` | an `@Entity` with no `V*.sql` migration |
| `controller_problemdetail_guard` *(IMW1-D)* | `@ExceptionHandler` returning non-`ProblemDetail` |
| `spec_ref_code_guard` | a `specs/*.yaml` path mentioned in code that does not resolve |
| `test_tag_naming_convention_guard` | a non-UPPERCASE `@Tag` |
| `l4_domain_reachability_guard` | a domain shipped without a `ViolationProofTest` |
| `aggregate_tagging_completeness_guard` | an `@Entity` not tagged `@AggregateRoot`/`@AggregateMember` |
| `aggregate_boundary_allowlist_guard` | an unresolved / expired DDD allowlist exception |
| `DddDecompositionTierZeroTest` | by-layer package, verb controller, kernel→feature dep, cross-feature `@Entity`/`*Repository` |
| `DddDecompositionTierOneTest` | member-repo, cross-aggregate object pointer, leaked member |
| `DddDecompositionHeuristicsTest` | god-service `@Transactional`, state setter bypassing the state machine |

## 5. Verify (binary pass/fail)
```bash
cd backend && ./gradlew test<Domain>          # your domain, green
bash practices/evals/run-all-guards.sh        # all catalog guards green
bash practices/scripts/verify-completion.sh   # R25 Iron Law — full regression + audit
```
Exit 0 ⇒ done. The Iron Law (`CLAUDE.md` R25) requires `verify-completion.sh` before declaring done.

## 6. REGULATED domains (PHI / personal data) — extra checklist

> Skip this section for an ordinary CRUD domain. Apply it when your domain stores or
> returns **protected health information** (diagnosis, medication, vitals, clinical
> notes) or other **regulated personal data** subject to consent / data-subject-rights
> obligations. The IDW4 EMR-lite dogfood proved all three personas hand-rolled these
> identically (rule-of-three) — so the catalog ships the primitives; do **NOT** invent
> your own. A regulated domain is a §1–§5 domain **plus** every box below.

- [ ] **Tag every PHI member with `@Phi`** (`common/Phi.java`) — on the entity field, DTO
      record component, or getter. This is the single intent-bearing tag the regulated
      guards key on (never a name heuristic). Forward-enforcing: tagging real PHI is what
      *activates* the two guards below.
- [ ] **Audit every PHI read** — a read method whose return type exposes a `@Phi` member
      MUST reference `AuditLogService.record(...)` on that path (HIPAA §164.312(b)).
      `audit_on_read_guard` fails the build otherwise.
- [ ] **Never log raw PHI** — no `log.{info,debug,warn,error,trace}(...)` may interpolate a
      `@Phi` getter. Use `common/AuditPiiHelper.piiHash(value)` for a non-recoverable
      correlation token instead, and `AuditPiiHelper.sanitizeReason(msg)` before storing any
      exception message. `phi_in_logs_guard` fails the build otherwise.
- [ ] **`Cache-Control: no-store` on every PHI endpoint** — set it on the controller method/
      response (mirror `IdentityVerificationAdminController` / `EmailOutboxAdminController`)
      so PHI never lands in a browser or proxy cache.
- [ ] **Gate data-sharing with `ConsentGate`** — before disclosing personal data for a
      purpose, check the subject's recorded opt-in via `ConsentGate` (purpose-scoped, with
      withdrawal + audit record). Anchors `specs/consent-management-l0.yaml`
      (`#CONSENT-CAPTURE-001` / `#CONSENT-WITHDRAW-001` / `#CONSENT-PURPOSE-001`). Do NOT
      hard-code "consent granted"; resolve it per request.
- [ ] **Model multi-actor roles with `ParticipantScope`** — when several real-world actors
      touch one resource (patient · clinician · admin · guardian; or subject · processor ·
      controller), derive the caller's relationship to the resource via `ParticipantScope`
      (built on `common/CallerScope`), NOT a `@PreAuthorize` authority or a new `UserRole`.
      Throw `common/ResourceNotFoundException` → 404 for the not-a-participant case
      (IDOR-safe; never 403-leak existence). See §0's business-role note.
- [ ] **Provide an audited `BreakGlass` path** — emergency override access to PHI MUST go
      through `BreakGlass`, which records an explicit high-severity audit entry (actor,
      resource, justification, timestamp) via `AuditLogService.record(...)`. Break-glass is
      logged-and-allowed, never silent.
- [ ] **Honor data-subject rights** — if the domain holds personal data, wire access /
      rectify / erase / restrict / portability through the DSR contract
      (`specs/data-subject-rights-l0.yaml` `#DSR-ACCESS-001` … `#DSR-SLA-001`, 30-day SLA).

| Regulated guard / primitive | Enforces |
|---|---|
| `common/Phi` + `audit_on_read_guard` | PHI read without `AuditLogService.record` → build fails |
| `common/Phi` + `phi_in_logs_guard` | raw `@Phi` getter inside a `log.*(...)` → build fails |
| `common/AuditPiiHelper` | `piiHash` / `sanitizeReason` — no raw PII in logs or stored-error columns |
| `ConsentGate` (`consent-management-l0`) | purpose-scoped opt-in checked before data-sharing |
| `ParticipantScope` (built on `common/CallerScope`) | multi-actor relationship check (404, not 403) |
| `BreakGlass` | emergency PHI access is audited, never silent |
