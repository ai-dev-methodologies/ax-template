# Codex Critic Review — Functional Extension PRD (Iteration 4)

## Verdict: APPROVE

Iter4 closes the single iter3 blocker at the PRD mechanics level: SP25 now rejects Hibernate 6.4 `@SoftDelete` for timestamp deletes, selects per-subclass `@SQLDelete` unconditionally, preserves the `deleted_at TIMESTAMP` contract, and adds an ArchUnit guard for future drift. The previously closed iter3 items did not regress. One stale wording issue remains, but the normative SP25 implementation and verification rows are clear enough to proceed to /ralph.

## Blocker closure verification

1. @SoftDelete removed from BaseEntity: CLOSED — BaseEntity's soft-delete block now says Option B is selected, `@SoftDelete` is not used, no `@SoftDelete` annotation belongs on BaseEntity, and concrete subclasses must carry `@SQLDelete` at PRD lines 390-396. Residual `@SoftDelete` mentions are explanatory/rejection text at lines 379-380, 395, and 453; one stale `SoftDeleteAspect` phrase at line 386 is noted under the informational attack below.
2. Option B (per-subclass @SQLDelete) selected: CLOSED — the pre-flight decision collapses the iter3 version branch and selects per-subclass `@SQLDelete` plus inherited `@Where` for the timestamp contract at lines 376-380. The explicit subclass instruction is repeated at lines 418-419 and in the final handoff at line 1163.
3. 8-entity literal mapping: CLOSED — Iter4 adds the mandatory per-subclass block and entity-to-table mapping for all 8 entities at lines 419-444, with example SQL for `notifications` and `notification_preferences` at lines 421-431. The mapped table names match the current `@Table` annotations in the template files spot-checked for Notification, NotificationPreferences, AuditLog, StoredFile, EmailOutbox, EmailTemplate, ScheduledTask, and JobHistory.
4. Timestamp contract preserved: CLOSED — BaseEntity keeps `Instant deletedAt` with `@Column(name = "deleted_at")` at line 396; the Flyway template adds `deleted_at TIMESTAMP NULL` to all 8 tables at lines 398-415; the RestAssured smoke requires DELETE to populate `deleted_at` and hide soft-deleted rows at lines 457-466; the SP25 matrix repeats the same assertion at line 758.
5. ArchUnit rule added: CLOSED — R4 defines `BaseEntitySubclassMustCarrySqlDelete`, requires every `BaseEntity` subclass to carry `@SQLDelete` whose SQL references the `@Table` name or snake_case fallback, and wires it to `BaseEntitySoftDeleteArchTest` at lines 487-491. The verification matrix includes the test file and RED reason at line 758.

## Scope discipline check

- Out-of-scope sections untouched: yes. The iter3->iter4 diff is limited to header facts, SP25 Sec. 6.3, SP25 Sec. 6.9 matrix row, Sec. 9 open-question closure, and the appended Sec. 13.2/14 closure record. Spot checks confirm the requested unchanged surfaces: SP23 remains at lines 330-369, SP24 at 498-542, SP26 at 544-587, SP27 at 589-641, SP28 at 643-704, SP29 at 706-749; RALPLAN-DR options remain at lines 14-64; pre-mortem scenarios remain at lines 833-944; honored constraints at lines 968-984; out-of-scope at lines 988-1001; ADR template at lines 1005-1078; iter3 delta historical record at lines 1098-1115.

## Previously CLOSED defects (no regression)

- BaseEntity CREATE: still CLOSED — SP25 still owns create-from-absent `templates/backend/BaseEntity.java` at lines 375 and 390, with rollback ownership repeated at lines 789-790.
- Flyway migration 8 tables: still CLOSED — the migration template still enumerates all 8 `ALTER TABLE ... deleted_at TIMESTAMP NULL` statements and partial indexes at lines 398-417.
- RestAssured 8-entity smoke: still CLOSED — all 8 entities, including `NotificationPreferences`, remain listed at lines 457-466, and the SP25 matrix repeats the all-8 assertion at line 758.

## Open Questions count

- Sec. 9 = 0 confirmed — line 952 states "Iter4 open question count: 0"; the iter3 Option A/B question is explicitly resolved by unconditional Option B at lines 954-955.

## New attack on iter 4

- Specific criticism: Iter4 leaves two stale summary phrases after the Option B flip. `SoftDeleteAspect.java` is described as working alongside Hibernate `@SoftDelete` at line 386 even though the selected PRD path does not use `@SoftDelete`; Sec. 5.1 also summarizes the BaseEntity extension as having `@SQLDelete` directly on BaseEntity at line 177, which is less precise than the normative per-subclass instruction in Sec. 6.3.
- Grade: INFORMATIONAL
- Required mitigation (if any): During /ralph/SP25 execution or final PRD polish, rewrite those summary phrases to say `SoftDeleteAspect` works with per-subclass timestamp `@SQLDelete`, and that BaseEntity owns only `@Where` plus `deletedAt` while concrete entities own `@SQLDelete`. No iter5 required because the binary implementation path is governed by lines 376-396, 419-444, 487-491, and 758.

## Final verdict reasoning

The iter3 blocking failure was semantic, not stylistic: Hibernate 6.4 `@SoftDelete` could not satisfy a `deleted_at TIMESTAMP` delete-time contract. Iter4 fixes that by rejecting Option A, selecting timestamp-capable per-subclass SQL, preserving the Flyway/RestAssured timestamp assertions, and adding CI enforcement for new or missed subclasses. The remaining stale wording does not create a competing executable path because the detailed SP25 deliverables, acceptance criteria, R4, and verification matrix all point to Option B.

## ADR (FINAL — for Step 6 commit)

- Decision: Approve Iter4 as the canonical PRD for Functional Capability Extension SP23-SP29, with SP25 soft-delete implemented through per-subclass timestamp `@SQLDelete` plus BaseEntity `@Where` and `deletedAt`.
- Drivers: Preserve `deleted_at TIMESTAMP` semantics; avoid Hibernate 6.4 boolean-only `@SoftDelete`; keep SP25 binary-verifiable across all 8 existing entities; prevent future subclass drift with ArchUnit.
- Alternatives considered: Hibernate 6.4 `@SoftDelete` on BaseEntity (rejected: boolean indicator only); change the schema to boolean soft delete (rejected: violates the PRD's timestamp contract); per-subclass `@SQLDelete` without a guard (rejected: drift risk).
- Why chosen: Option B is timestamp-capable, explicit per table, compatible with the current contract, and now guarded by `BaseEntitySoftDeleteArchTest`.
- Consequences: SP25 has more repetitive entity edits, but the repetition is enumerated and tested. Future `BaseEntity` subclasses must declare matching `@SQLDelete`.
- Follow-ups: Clean the stale summary wording at lines 177 and 386 during execution; keep the Hibernate snapshot evidence because it records why `@SoftDelete` is intentionally not used.
