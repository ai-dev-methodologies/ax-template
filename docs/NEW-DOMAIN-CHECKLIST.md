# New Backend Domain — Single-Entry Scaffold Checklist

> Closes IDW1 dogfood completeness gap #9: a newcomer had to reverse-engineer the
> required artifact set by reading a whole existing domain, and only learned a
> `ViolationProofTest` was mandatory when a guard failed. This is the discoverable
> single entry point. Follow it top-to-bottom for any new Spring Boot domain under
> `com.ax.template.authblueprint.<domain>`. The mechanical guards listed in [§Enforcement]
> will FAIL your build if you skip a step — that is intentional (zero-tolerance).

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

## 5. Verify (binary pass/fail)
```bash
cd backend && ./gradlew test<Domain>          # your domain, green
bash practices/evals/run-all-guards.sh        # all catalog guards green
bash practices/scripts/verify-completion.sh   # R25 Iron Law — full regression + audit
```
Exit 0 ⇒ done. The Iron Law (`CLAUDE.md` R25) requires `verify-completion.sh` before declaring done.
