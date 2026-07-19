# consumer-proof — falsifiable proof that the catalog blocks violations in a consumer context

This is a **standalone, adversarial probe**. It demonstrates that ax-template's
catalog does not merely *describe* anti-patterns — it **mechanically blocks**
rule-violating code an AI would generate, and lets the correct rewrite through.

Run it:

```bash
bash practices/consumer-proof/run-consumer-proof.sh
```

Exit `0` ⇒ proof holds. Exit `1` ⇒ proof **falsified** (a violating fixture
slipped past its gate, or a clean fixture was wrongly blocked) — the harness
names the failing case.

---

## What this PROVES

For each rule under test there is a **violating** fixture (realistic AI output)
and a **clean** fixture (the correct rewrite). The proof is falsifiable, binary,
and **non-vacuous** — `exit 0` is reachable only when EVERY expected case
genuinely ran and each half held on its own terms, not by accident:

- every **violating** fixture MUST be blocked **by its intended defect**: its
  gate exits the **EXPECTED-BLOCK code** (ESLint lint-failure = exit `1`; shell
  guards = exit `1`) — explicitly **NOT** exit `2` (env/config/usage) and not any
  other code — **AND** the captured output carries the intended signature (React:
  the exact `ax/<rule-id>` with no fatal parse/crash message; Java: the guard's
  own violation signature). A non-zero exit alone is **not** credited.
- every **clean** fixture MUST (a) **exist** on disk, (b) be **actually scanned**
  (a positive scan count > 0 — a zero-file / "nothing to check → 0" run is
  rejected), and (c) drive its gate to **zero** with no violation.
- every **expected case MUST run** — a dropped case, an empty fixture list, or an
  un-started lane fails a **cardinality gate** (a partial run is not a full proof).

If any half breaks, the thesis "the catalog mechanically enforces" is false, and
`run-consumer-proof.sh` fails loudly naming the case.

### Lane A — React / ESLint

`@ax/eslint-plugin-ax` is installed as a plugin and its rules run in a flat
config — the entire integration is `react/eslint.config.mjs`. Path coupling is
**per-rule**, so state it honestly:

- **Path-agnostic (AST-shape) rules** — `ax/no-array-mutate-on-state`,
  `ax/prefer-functional-setstate`, `ax/no-server-state-in-local-state` — fire on
  React/TSX **anywhere**. The consumer's directory layout, package name, and
  build system are irrelevant to these three.
- **Route/layer rules** — `ax/no-god-route` (and its siblings) require the
  Next.js **App-Router path convention** `src/app/**/(page|layout).*`. They do
  **not** fire on arbitrary React placed outside that path shape. `no-god-route`
  additionally needs a `"use client"` directive **plus** the line threshold, so
  its fixture must live under a real `src/app/dashboard/` route path.

So "just install the plugin and every rule fires anywhere" is **true only for the
AST-shape rules**, not for the route/layer rules — do not overstate it.

| Rule | Violating fixture | Clean fixture |
|------|-------------------|---------------|
| `ax/no-array-mutate-on-state` | `.push` on a `useState` array, then `setItems(items)` | `setItems(prev => [...prev, x])` |
| `ax/prefer-functional-setstate` | `setCount(count + 1)` (references state) | `setCount(curr => curr + 1)` |
| `ax/no-god-route` | a `"use client"` `src/app/dashboard/page.tsx` > 100 lines with inline form/business logic | a thin route delegating to `@/features/orders` |
| `ax/no-server-state-in-local-state` | `useState(useSWR(...).data)` | `const { data } = useSWR(...)` |

> `no-god-route` keys on the App-Router path shape (`src/app/**/(page|layout).tsx`)
> **plus** a `"use client"` directive **plus** the line threshold — so its fixtures
> live under a real `src/app/dashboard/` route path.

### Lane B — Java / Spring shell guards (convention-path — honest about coupling)

The Java guards are text scanners that walk a fixed package path. They accept
`--root DIR`, but they reach the code **only when the consumer adopts the repo's
package convention** `com.ax.template.authblueprint`. So the Java fixtures sit
under two sibling roots:

```
java/violating-root/backend/src/main/java/com/ax/template/authblueprint/...
java/clean-root/backend/src/main/java/com/ax/template/authblueprint/...
```

| Guard | Violating fixture | Clean fixture |
|-------|-------------------|---------------|
| `controller_problemdetail_guard.sh` | `@ExceptionHandler` returns `Map<String,String>` | returns `ProblemDetail` (RFC 9457) |
| `controller_repository_shell_guard.sh` | a `*Controller` injects + calls a `*Repository` | routes through a `*Service` |
| `money_boundary_seam_guard.sh` | `BigDecimal.valueOf(order.getTotalAmount())` (raw minor→major) | `Money.toMajorUnits(minor, currency)` |
| `entity_migration_guard.sh` | `@Entity` mapped to a table with no `V*.sql` migration | `@Entity` backed by `V001__create_widget.sql` |
| `role_literal_guard.sh` | `@PreAuthorize("hasAuthority('ROLE_ADMINS')")` (typo — unsatisfiable) | `hasAuthority('ROLE_ADMIN')` (a real `UserRole`) |

Each guard has a **different `--root` convention** (verified by reading each
script), and the harness passes the correct argument shape per guard:

- repo-root style (guard appends the package suffix, or `cd`s + uses relative
  paths): `controller_problemdetail_guard`, `money_boundary_seam_guard`,
  `entity_migration_guard`, `role_literal_guard` → passed the **root** dir.
- scan-dir style (guard globs `*Controller.java` under `--root` directly):
  `controller_repository_shell_guard` → passed the **package** dir.

The Java fixtures are **structural** — they are realistic enough for each
guard's text parser but are **not compiled** (the shell guards never invoke a
compiler or gradle). `UserRole.java` / `ApiKeyScope.java` are included in both
roots because `role_literal_guard` derives its allowed-authority set from them.

**Lane B guards used: all 5 candidates (none skipped).** All five genuinely
support an external `--root` and are self-contained under the fixture tree —
none cross-references a hardcoded file in the *real* ax source tree, so no guard
was skipped for coupling reasons.

---

## What this CANNOT claim (stated plainly)

- It does **not** prove that arbitrary, *differently-packaged* consumer Spring
  code is gated. The Lane B guards reach code **only** when the consumer adopts
  the `com.ax.template.authblueprint` package path (or writes new
  importPath-parameterized tests). A consumer using their own package gets **no**
  Java shell-guard coverage from this proof.
- **ArchUnit JVM rules are out of scope.** Those hardcode `importPackages(...)`
  and would need compilation plus new import-path tests to exercise on foreign
  code. This probe does not touch them.
- The fixtures represent **typical AI-generated violations**, not a **live AI
  run**. Each violating fixture is handwritten to trip its intended gate, and
  the harness credits a block only when that intended rule/guard signature
  actually appears in the tool's output (a fixture may incidentally also trip
  a sibling rule — e.g. `array-mutate.tsx` trips both `ax/no-array-mutate-on-state`
  and `ax/prefer-functional-setstate` — the proof still targets the one named
  signature). They are evidence that the intended gate *fires on that shape*,
  not a measurement of real-world AI output frequency.
- Within Lane A the coupling is **per-rule**: the AST-shape rules
  (`no-array-mutate-on-state`, `prefer-functional-setstate`,
  `no-server-state-in-local-state`) are genuinely path-agnostic, but the
  route/layer rules (`no-god-route`, …) require the App-Router path convention —
  so Lane A is convention-free **only for the AST-shape rules**, not wholesale.
  Lane B is honest about its whole-package-path coupling — that asymmetry is the
  point, not a defect.

---

## How to run

```bash
# Full proof (installs Lane A deps on first run if node/npm present):
bash practices/consumer-proof/run-consumer-proof.sh

# Lane A only, manually:
cd practices/consumer-proof/react
npm install
npx eslint fixtures/violating   # expect: errors (non-zero)
npx eslint fixtures/clean       # expect: clean (zero)

# Lane B only, manually (example — one guard, both roots):
J=practices/consumer-proof/java
PKG=backend/src/main/java/com/ax/template/authblueprint
bash practices/evals/money_boundary_seam_guard.sh --root "$PWD/$J/violating-root"  # expect exit 1
bash practices/evals/money_boundary_seam_guard.sh --root "$PWD/$J/clean-root"      # expect exit 0
```

If `node`/`npm` are unavailable the harness **skips Lane A with a clear message
and exits non-zero** (a partial run is not a full proof).

## Wiring as a CI probe (optional)

Mirror the `practices-react` sentinel job — a dedicated, non-required (or
required, your call) job that runs the harness on every PR:

```yaml
# .github/workflows/consumer-proof.yml
name: consumer-proof
on:
  pull_request:
  push:
    branches: [main]
jobs:
  consumer-proof:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-node@v4
        with: { node-version: "20" }
      - uses: actions/setup-python@v5     # Lane B guards use python3
        with: { python-version: "3.12" }
      - run: bash practices/consumer-proof/run-consumer-proof.sh
```

## Relationship to R25

This probe is **NOT wired into R25** (`verify-completion.sh`) on purpose. R25
stays fast and dependency-light (no `npm install` in the completion loop). The
consumer-proof is a standalone/CI probe you run when you want to re-validate
that the catalog still mechanically enforces in a consumer context.

The probe is also **isolated from ax's own guards**: its deliberately-bad
fixtures live under `practices/consumer-proof/`, which ax's own default-root
guards never scan (they target the real `backend/src/.../authblueprint`). Verified:
`bash practices/evals/run-all-guards.sh --include-fixtures` still passes with the
consumer-proof fixtures present.
