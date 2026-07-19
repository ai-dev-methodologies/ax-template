# S2.AUDIT-PII.FE — consumer-proof scenario

DOGFOOD cell: **FE AUDIT-PII**. The frontend parse-error PII deny-list
(`templates/L0/fork-receiver-kit/parse-error.ts`) existed but was completely
**untested** and, once tested, turned out to be **partial**: it redacted the
text/html fallback branch of `parseError()` but never the JSON
`body.detail` / `body.message` branch — the branch every RFC 9457
ProblemDetail response actually takes. This scenario surfaces both that gap
and a distinct, still-live consumer-level bypass, and adds the
**ADDITIONAL REQUIREMENT**: a runtime multi-tenant `TenantContext` primitive
that scopes every repository query to the caller tenant.

## What this proves

| # | Defect | Guard / test | Asset |
|---|--------|-------|-------|
| 1a (CLOSED) | `parseError()`'s JSON-body branch skipped the PII deny-list entirely | `frontend/tests/parse-error-denylist.vitest.ts` | **catalog closure** — `templates/L0/fork-receiver-kit/parse-error.ts` patched |
| 1b | A FE component reads the ProblemDetail body directly (`res.json().detail`) instead of calling `parseError()`, bypassing the deny-list regardless of 1a's fix | `scenario-guards/fe_error_display_pii_guard.sh` | **hand-rolled** |
| 2 | The ADDITIONAL REQUIREMENT: a `*Service` that imports `TenantContext` but calls the plain `findById`/`findAll` finders instead of tenant-scoped ones — cross-tenant data leak | `scenario-guards/tenant_scope_missing_guard.sh` | **hand-rolled** |

### Defect 1a — closed in the catalog (not just this scenario)

`sanitizeStoredError()` (the deny-list itself) worked correctly wherever it
was called directly. But `parseError()`'s JSON branch built its returned
`Error.message` straight from `body.detail`/`body.message` and **never
called it**:

```ts
// BEFORE (templates/L0/fork-receiver-kit/parse-error.ts)
const message =
  (body?.detail && String(body.detail)) ||
  (body?.message && String(body.message)) ||
  ''
```

Only the *text/html fallback* path a few lines below ran the same regexes
inline. A backend that echoes a submitted RRN/email/phone back into `detail`
for a validation error (a very common shape — "resident number
900101-1234567 is already registered") reached the browser verbatim.

Confirmed via a real RED→GREEN cycle, not asserted:

```bash
cd frontend
npx vitest run tests/parse-error-denylist.vitest.ts
```

- **Before the fix**: 3/10 tests failed (RRN, email, and code-preservation
  cases all leaked PII through the JSON branch).
- **After the fix** (`message` now passes through `sanitizeStoredError()`
  before being wrapped in `Error`/`CodedError`): **10/10 pass**.
- No regression: `npx vitest run tests/fmw2-primitives.vitest.ts
  tests/fmw4-primitives.vitest.ts` — 28/28 still pass.

This closure touches exactly the one catalog file the gap lives in
(`parse-error.ts`) plus its new colocated test
(`frontend/tests/parse-error-denylist.vitest.ts`, following the existing
`frontend/tests/fmw*-primitives.vitest.ts` convention for testing L0-kit
primitives) — nothing under `backend/src` or `frontend/src` was touched.

### Defect 1b — why it's a real, DISTINCT gap (not covered by 1a's fix)

Fixing `parseError()` protects every caller that *uses* it. It does nothing
for a caller that never calls it — and nothing in the catalog stops that. A
very natural AI-generated shortcut is to hand-read the failure body:

```ts
const body = await res.json()
setErrorMsg(body.detail || body.message || 'Upload failed')
```

Confirmed absent: `grep -ril "res.json\|parseError" practices/evals/*.sh
practices-react/eslint-plugin-ax/rules/*.js` returns 0 hits that check a FE
error-display call site for a `parseError()`/`sanitizeStoredError()`
wrapper — no ESLint `ax/*` rule and no shell guard targets this seam.

### Defect 2 — why it's a real gap, not a strawman

The catalog is not empty-handed on multi-tenancy — `specs/multi-tenant-l0.yaml`
+ `blueprints/multi-tenant-manifest.yaml` define the row-level isolation
*policy* in detail, down to a `java_skeleton:` block for a
`TenantFilterActivationFilter` / `TenantContext` pair. But that skeleton is
prose inside a YAML manifest, not code. The only `TenantContext.java` that
exists as an actual file lives under
`practices/evals/fixtures/multi-tenant-aop-guard-skeleton/{passing,failing}/`
— a **test double** for `multi_tenant_aop_guard_skeleton_guard.sh` to scan
for class-name presence, not an importable `common/` class any real domain
can `import`. Confirmed: `find backend/src/main/java/.../common -iname
'TenantContext*'` returns 0 hits, and every domain under `backend/src` today
is single-tenant. Nor does the existing guard check repository *call sites*
— it only checks that the skeleton's class names exist somewhere in the
tree, so a `*Service` that imports `TenantContext` but never calls a
tenant-scoped finder passes that guard silently.

## Capability-gap signals (assets_handrolled)

1. **`fe_error_display_pii_guard.sh`** — no ESLint rule or shell guard checks
   that a FE error-display call site routes through `parseError()`.
2. **`tenant_scope_missing_guard.sh`** — no guard checks repository call
   sites for tenant-scoped finder usage (the existing AOP-skeleton guard only
   checks class-name presence).
3. **`java/{violating,clean}-root/.../tenantdocfe/TenantContext.java`** — a
   faithful, minimal port of the blueprint's own `java_skeleton:` (ThreadLocal
   holder + `current()`/`set()`/`clear()`), since no runtime `common/`
   primitive exists to import. This is the scenario's stand-in for the
   ADDITIONAL REQUIREMENT's missing catalog asset — it is NOT wired into
   `common/` and does not modify the real backend.

## Isolation

Everything except the tracked closure (`templates/L0/fork-receiver-kit/
parse-error.ts` + `frontend/tests/parse-error-denylist.vitest.ts`) lives
under this scenario dir (`java/`, `react/`, `scenario-guards/`). Nothing here
edits `backend/src`, and this scenario is NOT wired into
`run-all-guards.sh` or R25 — it is a standalone probe, run manually.

## Run it

```bash
bash practices/consumer-proof/scenarios/S2.AUDIT-PII.FE/run-scenario-proof.sh
```

Exit 0 = proof holds (every violating fixture BLOCKED by its intended
signature, every clean fixture scanned + PASS, cardinality gate satisfied).
Exit 1 = proof falsified or a case could not run.

Separately (the catalog closure, not part of this harness run):

```bash
cd frontend && npx vitest run tests/parse-error-denylist.vitest.ts
```
