# multi-tenant-aop-guard-skeleton — FAILING fixture

Fork-receiver simulation that adopts 10 of the 11 canonical multi-tenant
skeleton files (`com.acme.multitenancy/`) but **omits
`AuthorizedTenantInterceptor.java`** — the service-boundary AOP guard.

This is the most security-critical omission. Row-level `@Filter` (via
`TenantFilterActivationFilter` + `TenantOwned`) still catches
SQL-bound leaks for repository queries. But any service method that
takes a `UUID` parameter directly (e.g. a controller forwarding a
path-parameter UUID into a service before any repository call) sees
no cross-tenant check — the AOP guard was the second defense layer
that catches exactly this case.

## Expected guard behaviour

`practices/evals/multi_tenant_aop_guard_skeleton_guard.sh --fixtures`
MUST:

- exit with non-zero status against this directory
- name `AuthorizedTenantInterceptor.java` in the violation message
- detect the omission even though the package compiles cleanly
  (the missing file does not cause a build break — that is exactly
  the silent-leak threat model the guard exists to catch)

If the guard exits 0 against this fixture, it has regressed and the
catalog's protection against fork-receivers shipping a half-adopted
multi-tenant skeleton is gone.
