# Codex Critic Review - Functional Extension PRD (Iteration 2)

## Verdict: ITERATE

Iter2 closes the SP29 surface-area problem, the dependency serialization, the SP28 i18n contradiction, shared-artifact ownership, executable risk mitigations, and the SP27 polling default. One narrow SP25 issue remains: the PRD now chooses in-place BaseEntity soft-delete, but the acceptance and implementation mechanics are not yet binary-implementable for autonomous execution. The current repo has no `templates/backend/BaseEntity.java`, the PRD's concrete smoke list omits one of the eight impacted entities, and the proposed inherited `@SQLDelete(sql="UPDATE <table> ...")` needs either a proven table-expansion mechanism or a different Hibernate soft-delete strategy.

## Blocker closure verification (6 + 1)

1. SP29 -> /ax-verify subcommands: CLOSED - No new top-level `/ax-policy-check`, `/ax-evidence-fetch`, or `/ax-explain` surface appears in iter2. Tier-1 is frozen at 4 and F13/F14/F15 are `/ax-verify policy-check|evidence-fetch|explain` at lines 18, 111, 240-250, and 635-658. Options and ADR reject the three-new-Tier-1 path at lines 53-56 and 947-968.
2. Reserialized dependency graph: CLOSED - The graph is now `SP23 -> SP25 -> SP24 || SP26 -> SP27 || SP28 -> SP29` at lines 287-323. SP25 owns `BaseEntity`, `JobDispatcher`, and `PageRequestNormalizer` before SP24/SP26 consume them at lines 315-319. Rollback boundaries follow the same order at lines 700-710, and ADR "Why chosen" matches at lines 970-974.
3. BaseEntity in-place: PARTIAL - `BaseEntityWithSoftDelete` is explicitly forbidden at lines 130 and 920, and iter2 documents in-place `templates/backend/BaseEntity.java` changes with `@SQLDelete`, `@Where`, and `deletedAt` at lines 174 and 382-387. However, `rg --files` finds the eight existing extenders but no `templates/backend/BaseEntity.java`, so the PRD still needs an explicit "create/repair common BaseEntity if absent" step. Also, line 387 lists eight impacted entities, but line 400's concrete RestAssured coverage list has seven resources and omits `NotificationPreferences`.
4. SP28 i18n Option beta: CLOSED - The rule is scoped to files created after 2026-05-18 and excludes existing L4 domains at lines 122, 235, and 579-594. Acceptance runs only SP28-scoped fixtures and explicitly does not run existing L4 auth/crud/payment/practices/notification/audit-log/file-storage at lines 603-612. Out-of-scope and future P1 migration are consistent at lines 906 and 918.
5. Section 7.2 ownership: CLOSED - The ownership table now includes BaseEntity, JobDispatcher, PageRequestNormalizer, i18n L1 components, cache layer, domain registry, trio integrity allowlist, realtime/i18n/feature-flags manifests, and SP29 dispatcher/SKILL.md rows at lines 712-732. Race-safe protocol is explicit for `domain-registry.yaml` and `trio_integrity_allowlist.yaml` at lines 723-724.
6. Executable mitigations: CLOSED - Every SP risk is now owner/command/threshold/recovery shaped. Random sample: SP24 Resilience4j pin has command and version threshold at lines 457-461; SP24 POI memory has AST and heap-cap checks at lines 462-466; SP25 Flyway naming collision has a concrete fixture command at lines 411-415; SP27 serverless defaults to polling and gates SSE opt-in at lines 556-560.
P1. Polling default: CLOSED - `blueprints/realtime-policy-manifest.yaml` declares `default_transport: polling` at lines 528-540; acceptance and TDD anchor check `realtime_default_polling` / `realtime-default-polling.spec.ts` at lines 547-553; out-of-scope forbids SSE/WebSocket as default at line 921.

## Spot check (iter 1 closed material intact)

- Existing guard baseline remains stated as 19/19 GREEN at line 86, and SP23 keeps `run-all-guards.sh --include-fixtures` in the verification matrix at line 686.
- Existing auth/crud/payment/practices trio integrity is not reopened; only `search: full_trio` and `feature-flags: full_trio` are added at lines 119, 484, and 601.
- The 18-skill baseline is preserved at lines 81 and 250; SP29 adds subcommands inside `/ax-verify`, not Tier-2/3 changes.
- Public release, deployment, and CI/CD remain out of scope at lines 914-916.
- Existing `/ax-verify` legacy integration is protected by dispatcher backward-compat acceptance and `_legacy-call-compat.sh` at lines 642 and 671-675.
- Residual mecab-ko and telemetry defaults are documented well enough for execution: TTY-conditional/non-interactive default is line 880, and local JSON metrics via `--metrics` is line 881. Line 505 still says "interactive prompt" in the risk card, but line 880's default resolves the headless case.

## New attack on iter 2

- Specific criticism: SP25's BaseEntity soft-delete design is still not executable as written. Iter2 says a common `templates/backend/BaseEntity.java` gets `@SQLDelete(sql="UPDATE <table> SET deleted_at = ? WHERE id = ?")` and `@Where` at lines 382-385, and all eight existing entities inherit it at line 387. But a common superclass cannot safely carry one static table-specific SQL string for eight different tables unless the template engine expands `<table>` per concrete entity, and the PRD does not specify that expansion or a Hibernate alternative. It also adds a `deletedAt` field but does not name the Flyway migration that adds `deleted_at` to the eight existing tables, while acceptance requires DELETE to set `deleted_at` at line 400.
- Grade: BLOCKING
- Required mitigation: In iter3, add a short SP25 implementation decision that makes the soft-delete mechanism executable. It must explicitly cover: whether `templates/backend/BaseEntity.java` is created/normalized if absent; how table-specific soft-delete SQL is generated or replaced with a supported Hibernate soft-delete annotation; the Flyway migration adding `deleted_at` to all eight impacted tables; and a RestAssured smoke covering all eight entities, including `notification-preferences`.

## Final verdict reasoning

The original six hard blockers are mostly closed, but B3 remains partial because the PRD asserts an in-place file that is not present in the workspace and the acceptance list does not prove all eight impacted entities. This is narrow and fixable: no need to revisit the seven-SP plan, `/ax-verify` subcommand decision, SP ordering, or SP28 i18n scope.

## ADR (FINAL - for Step 6 commit if APPROVE)

Not final while verdict is ITERATE.

## Re-review trigger (only if ITERATE/REJECT)

- Revise SP25 only. Add/repair the common `templates/backend/BaseEntity.java` path if missing, specify the exact supported soft-delete mechanism, add the `deleted_at` Flyway migration for all eight impacted entity tables, and make the RestAssured coverage list explicitly include `NotificationPreferences` / `notification-preferences`.
- No broad rewrite requested. Re-review can be limited to SP25 lines 372-423 plus the SP25 verification matrix and rollback/ownership rows.
