# Codex PR #2 RE-review

## Verdict: REQUEST CHANGES

## Fix 1 closure: CLOSED

`templates/backend/observability/MdcCorrelationIdInterceptor.java` now matches the runnable backend copy's servlet-filter shape:

- Extends `OncePerRequestFilter`, not `HandlerInterceptor`.
- Has `@Component` and `@Order(Ordered.HIGHEST_PRECEDENCE + 1)`.
- Uses the same MDC keys, correlation-id resolution, response header, and `finally` cleanup as `backend/src/main/java/com/ax/template/authblueprint/observability/MdcCorrelationIdInterceptor.java`.
- The `@ax-template-meta` usage block no longer instructs `WebMvcConfig.addInterceptors()` wiring; it says the component auto-registers as a servlet filter.

No remaining blocker found for Fix 1.

## Fix 2 closure: CLOSED

Both targeted integration tests were rewritten away from MockMvc:

- `backend/src/test/java/com/ax/template/observability/MdcCorrelationIdIT.java` uses `@SpringBootTest(webEnvironment = RANDOM_PORT)`, `@LocalServerPort`, and `io.restassured.RestAssured`.
- `backend/src/test/java/com/ax/template/integration/WebhookReceiverIT.java` uses the same random-port RestAssured pattern.
- `grep -r "MockMvc\|@AutoConfigureMockMvc" backend/src/test/java/com/ax/template/{observability,integration}/` returned empty.

Static setup spot-check: `/actuator/health` is explicitly `permitAll`, and `/api/test/webhooks` is explicitly `permitAll`; the RestAssured rewrites are not depending on MockMvc security-test bypass behavior.

No remaining blocker found for Fix 2.

## Fix 3 closure: NOT CLOSED

`templates/backend/data/migrations/V202605181201__create_job_queue_table.sql` now exists and creates `job_queue`, so the previous "no migration exists" defect is partially addressed.

However, the migration does not satisfy the requested closure criteria for the table shape:

- It creates `updated_at`, not `last_modified_at`.
- It omits `created_by` and `last_modified_by`.
- It omits `priority` and `next_run_at`.
- It has partial indexes for `deleted_at`, `status, created_at`, and `job_type, status`, but not the requested status/priority/next_run_at indexing.

Evidence: `templates/backend/data/migrations/V202605181201__create_job_queue_table.sql:13-41`.

Grade: BLOCKING, because the fix-cycle migration is present but incomplete relative to the stated schema/index contract.

## Regression spot-check: PARTIAL

- 19+ guards: PASS. `bash practices/evals/run-all-guards.sh --include-fixtures` reported `Total: 19 passed, 0 failed`.
- Tier-1 cap: PASS. `bash skills/_tests/tier1-topology.test.sh` reported `count=4` and all assertions PASS.
- `/ax-verify-java`: NOT PROVEN GREEN in this sandbox. First run failed on default `~/.gradle` lockfile permission. Retry with workspace-local `GRADLE_USER_HOME` reached Gradle wrapper download and failed with `UnknownHostException: services.gradle.org`.
- `/ax-verify all`: NOT PROVEN GREEN in this sandbox. Guards passed, then backend failed at the same Gradle wrapper download DNS failure.

The build-gate failures are environment/download blockers, not direct evidence of a code regression. They still mean the requested green verification could not be confirmed locally.

## New attack: BLOCKING

The new issue is specific to fix commit `ca7cf40`: the added migration closes the absence of `job_queue`, but it codifies the wrong/minimal queue schema for the requested closure contract. In particular, there is no `priority` or `next_run_at` column and no corresponding worker/dispatcher index, so a queue ordered by readiness/priority cannot be supported by this migration without another forward migration.

This is not the previous "no migration" objection; it is a new defect in the migration that was added to fix that objection.

## Final reasoning: REQUEST CHANGES

Fix 1 and Fix 2 are clean. Fix 3 is not clean: the PR now ships a `job_queue` migration, but the migration does not include the requested audit columns or readiness/priority scheduling columns/indexes. Because one of the three prior BLOCKING fixes remains incomplete, this PR is still not merge-ready.

No additional blocking issue was found in the OncePerRequestFilter ordering or in the RestAssured rewrites.

## Merge recommendation

- REQUEST CHANGES:
  - Add a forward migration that brings `job_queue` to the requested schema: `created_by`, `last_modified_at`, `last_modified_by`, `priority`, `next_run_at`, and the status/priority/next_run_at indexes, or update the owning `JobQueue`/review contract consistently if that schema is intentionally rejected.
  - Re-run `/ax-verify-java` and `/ax-verify all` in an environment with Gradle wrapper distribution access, because this sandbox could not prove those gates green.
