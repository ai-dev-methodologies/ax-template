# Codex PR #2 Review

## Verdict: REQUEST CHANGES

The PR is broadly traceable to SP23-SP29 and most deliverables are present, but it is not merge-ready: required local gates are not green in this sandbox, the SP23 catalog template still violates the approved `OncePerRequestFilter` contract, new PR tests include MockMvc-only coverage contrary to the PRD, and the new `JobQueue` template references a `job_queue` table/`deleted_at` column without a matching migration.

## Verification gate status
- `gh pr view 2 --json statusCheckRollup`: FAIL to fetch in sandbox (`api.github.com` connection failed).
- `run-all.sh`: FAIL. Guards PASS, then backend stops at Gradle wrapper download. First run hit `/Users/kyjin/.gradle/...gradle-8.5-bin.zip.lck` permission denial; retry with workspace `GRADLE_USER_HOME` hit `UnknownHostException: services.gradle.org`.
- `/ax-fork-receiver --bundle-only`: PASS. Produced `dist/ax-template-catalog-6de9fd9.tar.gz`, 2 MB, SHA256 `86a26a4c7b31e6c483807459dc238318e780b97a6d14c855fa445b8e97208a83`.
- `/ax-verify-domain x9`: FAIL as a gate. Structural allowlist/evidence/trio/cross-trio steps passed for all domains checked. `auth`, `crud`, `payment`, `notification`, `audit-log`, `file-storage`, `search`, `feature-flags` fail at Gradle download (`services.gradle.org`). `practices` reaches Playwright and fails Next build with `Cannot find module 'react'` from `templates/L3/pages/forgot-password/page.tsx:21`.
- `/ax-verify all`: FAIL, same `run-all.sh` backend blocker.
- SP29 subcommand tests: PASS. `policy-check-fp-rate` evaluated 50 fixtures with FP 0.0% / FN 0.0%; `evidence-fetch-stale-detect` passed 11/11 assertions; `explain-rule-lookup` passed 27/27 assertions.

## PRD traceability spot-check (per SP)
SP23: PARTIAL. Cache templates/rule are present (`templates/backend/cache/CaffeineConfig.java:3`, `templates/backend/cache/CaffeineConfig.java:76`, `templates/backend/cache/RedisCacheConfig.java:101`, `practices/rules/cacheable-requires-explicit-ttl.md:2`). Observability exists, but the catalog template is wrong: `templates/backend/observability/MdcCorrelationIdInterceptor.java:31` imports `HandlerInterceptor` and `:58` implements it.

SP24: PRESENT with test anti-pattern. External integration and import/export are present (`templates/backend/integration/WebClientConfig.java:3`, `templates/backend/integration/WebhookReceiver.java:72`, `templates/backend/import-export/CsvImportService.java:51`, `templates/L2/blocks/ImportPreview.tsx:3`). HMAC and chunked import rules are present (`practices/rules/webhook-hmac-required.md:2`, `practices/rules/chunked-import-required-when-rowcount-gt-1000.md:2`).

SP25: PRESENT. Foundation files exist (`templates/backend/BaseEntity.java:85`, `templates/backend/data/PageRequestNormalizer.java:59`, `templates/backend/jobs/JobDispatcher.java:48`). Migration exists with 8 `ALTER TABLE` blocks (`templates/backend/data/migrations/V202605181200__add_soft_delete_columns.sql:13`, `:20`, `:27`, `:34`, `:41`, `:48`, `:55`, `:62`).

SP26: PRESENT. Search full trio and backend/L2 surfaces exist (`specs/search-l0.yaml`, `contracts/search-openapi.yaml:13`, `blueprints/search-manifest.yaml`, `templates/backend/search/SearchController.java:53`, `templates/L2/blocks/typeahead-search.tsx:49`). Charts are present (`templates/L2/blocks/time-series-chart.tsx:3`, `templates/L2/blocks/bar-chart.tsx:3`, `templates/L2/blocks/pie-chart.tsx:3`, `templates/L2/blocks/kpi-card.tsx:3`, `templates/L2/blocks/sparkline.tsx:3`, `templates/L2/blocks/heatmap.tsx:3`).

SP27: PRESENT. Realtime default policy is present (`blueprints/realtime-policy-manifest.yaml:17`) with backend realtime templates (`templates/backend/realtime/RealtimeEventBus.java:52`, `templates/backend/realtime/SseEmitterConfig.java:57`) and L2 blocks (`templates/L2/blocks/live-presence.tsx:55`, `templates/L2/blocks/event-stream.tsx:64`, `templates/L2/blocks/optimistic-update.tsx:48`). Form orchestration blocks are present (`templates/L2/blocks/field-array-extended.tsx:62`, `templates/L2/blocks/dependent-field.tsx:45`, `templates/L2/blocks/dirty-guard.tsx:48`).

SP28: PRESENT. i18n components/rule are present (`templates/L1/components/locale-switcher.tsx:3`, `templates/L2/blocks/locale-provider.tsx:64`, `templates/L2/blocks/translation-boundary.tsx:69`, `practices-react/rules/no-hardcoded-user-facing-string-in-l4.md:16`). Feature flags full trio/backend/L2 are present (`practices/evals/trio_integrity_allowlist.yaml:26`, `templates/backend/feature-flags/FeatureFlag.java:43`, `templates/L2/blocks/feature-gate.tsx:63`, `templates/L2/blocks/feature-flag-toggle.tsx:66`).

SP29: PRESENT. `/ax-verify` subcommands are implemented as existing-skill extensions (`skills/ax-verify/SKILL.md:130`, `:145`, `:146`, `:147`; `skills/ax-verify/scripts/policy-check.sh:2`, `skills/ax-verify/scripts/evidence-fetch.sh:2`, `skills/ax-verify/scripts/explain.sh:2`) and their focused tests passed locally.

## Critical contracts
- `BaseEntity.java`: PASS (`templates/backend/BaseEntity.java:85`).
- 8-entity `@SQLDelete`: PASS for the named 8 (`Notification.java:48`, `NotificationPreferences.java:43`, `AuditLog.java:54`, `StoredFile.java:29`, `EmailOutbox.java:49`, `EmailTemplate.java:47`, `ScheduledTask.java:49`, `JobHistory.java:48`). No `@SoftDelete` implementation found.
- Soft-delete migration: PASS for the required 8 ALTER TABLE blocks (`templates/backend/data/migrations/V202605181200__add_soft_delete_columns.sql:13-63`).
- `MdcCorrelationIdInterceptor` template: FAIL. Catalog template still uses `HandlerInterceptor` (`templates/backend/observability/MdcCorrelationIdInterceptor.java:31`, `:58`). Runnable backend copy is fixed (`backend/src/main/java/com/ax/template/authblueprint/observability/MdcCorrelationIdInterceptor.java:47` extends `OncePerRequestFilter`), but the shipped template violates SP23.
- SP28 i18n rule scope: PASS (`practices-react/rules/no-hardcoded-user-facing-string-in-l4.md:16`).
- SP29 Tier-1 cap 4: PASS (`skills/ax-verify/SKILL.md:132-134`).
- SP27 default transport polling: PASS (`blueprints/realtime-policy-manifest.yaml:17`).

## Anti-pattern check
- No MockMvc-only tests: FAIL. New PR files `backend/src/test/java/com/ax/template/observability/MdcCorrelationIdIT.java:8`, `:10`, `:33`, `:37` and `backend/src/test/java/com/ax/template/integration/WebhookReceiverIT.java:8`, `:11`, `:40`, `:47` use `@AutoConfigureMockMvc`/`MockMvc` with no RestAssured.
- No `--no-verify` / hook bypass in commits: PASS. `git log main..HEAD` search found no bypass markers.
- No deployment/release/CI workflow files: PASS. Diff name scan found no `.github/workflows`, deploy, release, or CI files.
- Composition kit framing preserved: PASS. Changes remain in templates, practices, specs, contracts, blueprints, and skills. Only existing build/package files changed (`backend/build.gradle.kts`, `frontend/package.json`); no single publishable package framing was introduced.

## Branch hygiene
- Commit count: PASS. `git rev-list --count main..HEAD` = 24.
- Main hygiene: PASS. `git rev-list --count b5b159f..main` = 0, so local `main` is still at the stated base.
- Secrets / `.env` / credentials in diff: PASS. No `.env`, credential, private-key, or workflow secret files in the diff. Regex hits were placeholders/test values such as `${ax.webhook.secret:ax-template-dev-secret}` and `${ax.search.meilisearch.api-key:}`.
- `.gitignore` excludes `dist/`: PASS (`.gitignore:16`).
- Working tree note: pre-existing local dirt remains outside this review artifact (`.omc/state/.../prd.json`, untracked `practices/evals/fixtures/spring-modulith-example`). I did not modify or revert those.

## My independent attack
- Specific issue: `templates/backend/jobs/JobQueue.java` tells users to run a Flyway migration before using `job_queue` (`:20`), maps `@Table(name = "job_queue")` (`:56-57`), extends `BaseEntity` (`:63`), and defines `@SQLDelete` against `deleted_at` (`:55`), but no migration creates or alters `job_queue`. The only shipped soft-delete migration explicitly covers the 8 pre-existing tables and has no `job_queue` entry (`templates/backend/data/migrations/V202605181200__add_soft_delete_columns.sql:3`, `:13-63`). Search found no `CREATE TABLE job_queue` or `ALTER TABLE job_queue`.
- Grade: BLOCKING.

## Final reasoning
The implementation is close in breadth: SP23-SP29 surfaces are mostly present and several local structural checks pass. However, merge-readiness requires the shipped templates and tests to satisfy the PRD contracts. A catalog template still ships the rejected MDC interceptor design, two new integration tests violate the RestAssured-only rule, and a new persistent jobs entity is unusable without a migration. These are implementation defects, not spec-review disagreements.

## Merge recommendation
- REQUEST CHANGES:
  1. Convert `templates/backend/observability/MdcCorrelationIdInterceptor.java` to the same `OncePerRequestFilter` pattern as the runnable backend copy and update usage comments away from `WebMvcConfig.addInterceptors()`.
  2. Replace the new MockMvc-only PR tests with RestAssured/random-port coverage.
  3. Add a `job_queue` Flyway migration (including inherited `BaseEntity` columns, `deleted_at`, and indexes) or remove `JobQueue` persistence claims until a migration exists.
  4. Re-run `bash skills/ax-verify/scripts/run-all.sh`, `bash skills/ax-fork-receiver/scripts/run.sh --bundle-only`, and all 9 `ax-verify-domain` commands in an environment with Gradle/Node dependencies available.
