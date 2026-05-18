# Codex Critic Review — Functional Extension PRD (Iteration 3)

## Verdict: ITERATE

Iter3 closes the BaseEntity file-absence, migration enumeration, and 8-entity smoke-list defects at the PRD mechanics level, and the edit scope is acceptably narrow. However, the selected Hibernate 6.4 Option A is not binary-implementable for the PRD's required `deleted_at TIMESTAMP` semantics: Hibernate ORM 6.4 `@SoftDelete` is a boolean indicator mechanism, while iter3 requires `deleted_at` to be an `Instant`/`TIMESTAMP` populated on delete. This is a new SP25-specific blocker, so `/ralph` should not start until SP25 chooses a timestamp-capable path.

## Defect closure verification (4)

1. BaseEntity CREATE: CLOSED — §6.3 now states `templates/backend/BaseEntity.java` is absent and SP25 owns create-from-scratch, not edit-only, at lines 375, 382, and 390. The BaseEntity deliverable includes `@MappedSuperclass`, auditing listener/fields, `@Version`, soft-delete annotation/fallback, and `deletedAt` field at lines 391-397. §7.2 repeats CREATE ownership and rollback at line 764.
2. @SQLDelete option choice: PARTIAL — Iter3 explicitly documents Option A vs Option B, reads `backend/build.gradle.kts`, selects Option A, and adds R4 with owner/command/threshold/recovery at lines 376-380, 395-396, and 462-466. The remaining blocker is semantic: selected Option A does not satisfy the PRD's timestamp `deleted_at` contract. Hibernate ORM 6.4 docs list only `ACTIVE` and `DELETED` soft-delete strategies and define the conversion domain type as boolean; iter3 requires `@Column(name = "deleted_at") private Instant deletedAt` plus `TIMESTAMP NULL` migration and `deleted_at` populated in RestAssured at lines 397, 399-418, and 441.
3. Flyway migration for 8 tables: CLOSED — SP25 now adds `templates/backend/data/migrations/V202605181200__add_soft_delete_columns.sql` at lines 399-418, with all 8 `ALTER TABLE` statements and 8 partial indexes. It is explicitly template SQL for fork receivers at line 418 and owned in §7.2 at line 765.
4. RestAssured 8-entity smoke: CLOSED — §6.3 enumerates exactly 8 entities, including `NotificationPreferences`, at lines 432-441, and requires CREATE -> SOFT-DELETE -> verify `deleted_at` plus read filter. The SP25 verification matrix repeats all 8 names and the same assertion at line 733.

## Scope discipline check

- Out-of-scope sections untouched: yes. A diff against iter2 shows the substantive changes are header facts, §6.3 SP25, §6.9 SP25 matrix row, §7.2 BaseEntity/migration rows, §9's single iter3 open question, and §13.1 delta. Spot-check targets §SP23/SP24/SP26/SP27/SP28/SP29, §1 RALPLAN-DR, §8 pre-mortem scenarios, §10 honored constraints, and §11 out-of-scope remain unchanged in substance.

## Residual ambiguity verdict (iter 3 §9 OQ-1)

- Option A/B once-and-committed default: ACCEPTABLE — a one-time SP25 pre-flight decision committed to `templates/backend/data/README.md` is reasonable for template generation. The blocker is not the decision moment; it is that the current Option A decision is semantically wrong for timestamp deletes.
- iter 1 RALPLAN-DR Options residual "in-place" wording: ACCEPTABLE — the §6.3 CREATE language and §13.1 delta override are clear enough for this ultra-narrow pass, despite historical "in-place" text remaining in §1/§4/§5.1/§10.

## New attack on iter 3

- Specific criticism: Iter3's primary Option A says `@SoftDelete(columnName = "deleted_at")` on `@MappedSuperclass` will populate the PRD's `deleted_at TIMESTAMP` column. Hibernate ORM 6.4 does not support timestamp soft-delete semantics there: the 6.4 `SoftDeleteType` enum has only `ACTIVE` and `DELETED`, and the user guide says the soft-delete conversion domain type is always boolean. That conflicts with iter3's `Instant deletedAt`, `TIMESTAMP NULL` migration, and acceptance requiring `deleted_at` populated.
- Grade: BLOCKING
- Required mitigation (if BLOCKING): Revise SP25 only. Either (a) make Option B the selected current-workspace path even on Hibernate 6.4: per-subclass `@SQLDelete(sql = "UPDATE <table> SET deleted_at = CURRENT_TIMESTAMP WHERE id = ?")` for all 8 entities plus superclass/read-filter support, or (b) change the schema and acceptance to Hibernate's boolean indicator model and add a separate timestamp audit mechanism if `deletedAt` remains required. If Planner wants to keep a future native timestamp option, gate it on an explicit `SoftDeleteType.TIMESTAMP` availability check, not just `Hibernate >= 6.4.0`.

## Final verdict reasoning

The iteration should not be rejected wholesale: the scope was disciplined, BaseEntity creation is now explicit, the Flyway template names all 8 tables, and the RestAssured list includes `NotificationPreferences`. But the new Option A implementation path is currently a false green. It would create a timestamp column and an `Instant deletedAt` field while asking Hibernate 6.4 `@SoftDelete` to manage it as a boolean indicator. That means the first SP25 implementation would either fail at mapping time or fail the required `deleted_at` populated smoke. One surgical iter4 edit can resolve this by selecting a timestamp-capable path.

## ADR (FINAL — for Step 6 commit if APPROVE)

Not issued because verdict is ITERATE.

## Re-review trigger (only if ITERATE/REJECT)

- Re-review only SP25 lines covering Hibernate Option A/B selection, BaseEntity soft-delete annotations, migration/acceptance semantics, R4, SP25 verification matrix, and §13.1 delta.
- Required iter4 revision: remove "Hibernate ORM >= 6.4.0 implies timestamp `@SoftDelete` Option A is selected" unless the PRD also changes to boolean soft-delete. For the existing `deleted_at TIMESTAMP` contract, select per-subclass timestamp `@SQLDelete` as the current path and keep the 8-entity smoke unchanged.
