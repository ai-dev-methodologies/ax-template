# multi-tenant-aop-guard-skeleton — PASSING fixture

Fork-receiver simulation: every file under `passing/com/acme/multitenancy/`
is the canonical adoption of `blueprints/multi-tenant-manifest.yaml` with
`<root>` substituted by `acme`. This fixture demonstrates that the manifest
skeletons are concrete enough for mechanical substitution — no design
decision is left to the fork-receiver.

## Source mapping

| File | manifest anchor (`blueprints/multi-tenant-manifest.yaml#`) |
|---|---|
| `TenantContext.java` | `context-resolution.tenant_context_skeleton` (added R3) |
| `TenantOwned.java` | `aop-guard.marker_interface` |
| `TenantBoundaryViolationException.java` | `aop-guard.exception_skeleton` |
| `TenantContextMissingException.java` | `async-propagation.context_missing_exception_skeleton` |
| `MultiTenantProblemDetailAdvice.java` | `aop-guard.advice_scope` |
| `TenantAwareAsyncConfig.java` | `async-propagation.prerequisite_executor_bean` |
| `TenantContextAwareTaskDecorator.java` | `async-propagation.task_decorator_skeleton` |
| `TenantFilterActivationFilter.java` | `row-level-strategy.filter_activation` |
| `AuditEvent.java` | `ledger-audit-tenant-scope.java_skeleton` (added R4 — closes GAP-R3-3) |

**Missing from this fixture**: `AuthorizedTenantInterceptor.java` —
manifest `aop-guard.description` names it as a load-bearing component
(throws `TenantBoundaryViolationException`) but ships no `java_skeleton`
for the interceptor body or for the `@AuthorizedTenant` annotation it
reads. The actual cross-tenant check logic (reflection on method
parameters, comparison with `TenantContext.current()`) is undefined.
Logged as GAP-NEW-2 (R3) in the dogfooding report. Until closed,
fork-receivers must hand-write the most security-critical piece of
the skeleton — exactly the failure mode the rule was created to
prevent.

## R3 dogfooding finding — manifest sufficiency

P2 Round 3 sealed sub-agent attempted to write the three files
named in the original gap report (`TenantOwned`, `MultiTenantProblemDetailAdvice`,
`TenantContextAwareTaskDecorator`) from the dogfood-2 manifest alone.

Outcome:

- `TenantOwned.java` — wrote directly from skeleton. PASS.
- `MultiTenantProblemDetailAdvice.java` — wrote directly from skeleton. PASS.
- `TenantContextAwareTaskDecorator.java` — **stalled** on `TenantContext`
  class signature (manifest defined semantics but not the class body).
  Documented as GAP-NEW-1 (R3). Closed by `tenant_context_skeleton:` anchor
  added in dogfood-3.

After GAP-NEW-1 closure, all 8 files were written by mechanical substitution
of `<root>` with no further blueprint-side decisions needed.

## What this fixture does NOT prove

This is the PASS side. The mechanical guard that would BLOCK a recipe
declaring `tenant_model: multi` while missing any of these 8 files is
still deferred (see `verification.type: review` in the rule). The next
round should:

1. Add a `failing/` sibling that omits one file (e.g. no
   `MultiTenantProblemDetailAdvice` — exception bubbles as 500).
2. Add `practices/evals/multi_tenant_aop_guard_skeleton/` bash guard that
   scans the fork-receiver's tree.
3. Wire the guard into `practices/evals/run.sh` as the 30th hard guard.

Until then this fixture demonstrates the manifest is concrete enough to
copy — it is the PASS half of an eventual pass/fail pair.
