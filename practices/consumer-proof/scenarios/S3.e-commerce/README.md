# S3.e-commerce — checkout vertical slice (consumer-proof scenario)

Korean-enterprise commerce CHECKOUT slice: **cart -> order -> payment -> receipt**
(buyer), **list/refund** (ADMIN). Thin composition of catalog assets, proven with
the same adversarial harness contract as `practices/consumer-proof/run-consumer-proof.sh`.

Run it:

```bash
bash practices/consumer-proof/scenarios/S3.e-commerce/run-scenario-proof.sh
```

Exit `0` ⇒ proof holds (all 4 cases blocked-by-signature + clean-passes,
non-vacuously). Exit `1` ⇒ falsified, names the failing case.

## Composition (catalog assets used)

| Concern | Catalog asset composed | How |
|---|---|---|
| Cart/Product/Order CRUD | `templates/L4/crud` pattern + `specs/crud-security.yaml` shape | `Order`/`OrderRepository`/`OrderService`/`OrderController` follow the thin-controller -> service -> repository layering (`docs/NEW-DOMAIN-CHECKLIST.md`) |
| Money (minor-units storage <-> PG-edge major-units) | `common/Money.toMajorUnits` seam (money-l0) | `OrderService.chargeAmount()` composes the seam instead of a raw conversion |
| Payment / Notification / Audit-log L4 | `templates/L4/{payment,notification,audit-log}` (interfaces referenced in `OrderService` javadoc) | Thin slice — the full PG charge / receipt email / state-change audit call sites are named but not reimplemented here (would duplicate the real backend/ecommerce + payment/notification/audit-log modules; this scenario proves the *seam*, not a second full implementation) |
| Bounded admin list | `api-pagination-pageable.md` pattern (`Page<Order>`/`Pageable`) | `OrderAdminController` + `OrderRepository.findAllByBuyerId(buyerId, Pageable)` |
| Layering (controller never touches repository) | `controller_repository_shell_guard.sh` (IMW5) | `OrderController`/`OrderAdminController` inject `OrderService` only |
| Receipt page shape | `ax/no-god-route` thin-route pattern (Lane A convention: `src/app/**/page.tsx`) + `templates/L2/blocks/billing-history.tsx` concept | `react/clean/.../checkout/receipt/page.tsx` — thin route, would delegate to a `billing-history`-style feature component in a full build |
| Locale-aware money/date display | **no catalog asset — see gap below** | hand-rolled |

## Named violations proven BLOCKED

| # | Violation | Guard | Reuse type |
|---|---|---|---|
| 1 | `BigDecimal.valueOf(order.getTotalAmount())` — raw minor-unit value read as PG major-units (100x over-charge for 2-decimal currencies) | `practices/evals/money_boundary_seam_guard.sh` | **catalog-reused** |
| 2 | `OrderAdminController` injects `OrderRepository` directly instead of routing through `OrderService` | `practices/evals/controller_repository_shell_guard.sh` | **catalog-reused** |
| 3 | `OrderRepository.findAllByBuyerId(buyerId)` returns the buyer's *entire* order history as `List<Order>`, no `Pageable` — unbounded read | `scenario-guards/unbounded_repository_read_guard.sh` | **hand-rolled** (see gap) |
| 4 | Receipt page uses raw `.toLocaleString()` + manual date-string concatenation instead of `Intl.NumberFormat`/`Intl.DateTimeFormat` | `scenario-guards/locale_format_guard.sh` | **hand-rolled** (see gap) |

All 4 harness cases PASS: each violating fixture is blocked by its **intended
signature** at **exactly exit 1** (never a bare non-zero, never exit 2), and
each clean fixture is **actually scanned** (positive file count) and **exits 0**
— the same non-vacuity contract as the parent consumer-proof harness.

## Capability gaps found (assets_handrolled — signal for the catalog, not a defect in this slice)

1. **`unbounded_repository_read_guard.sh`** — the catalog states the
   "no unbounded findAll" invariant as a rule
   (`practices/rules/api-pagination-pageable.md`) and enforces it at the JVM
   layer via ArchUnit, which requires a full `./gradlew test` compile. There is
   **no standalone `--root`-parameterized shell guard** in `practices/evals/`
   for this invariant (checked: `ls practices/evals/*.sh | grep -iE
   'page|list|unbounded'` returns nothing that text-scans `*Repository.java`).
   So proving this violation for an isolated, uncompiled fixture tree required
   a hand-rolled scanner, modeled on the style of
   `money_boundary_seam_guard.sh`/`controller_repository_shell_guard.sh`.

2. **`locale_format_guard.sh`** — this was the dogfood brief's explicit
   additional requirement (FE locale-aware `Intl.NumberFormat`/
   `Intl.DateTimeFormat` enforcement). It is a **confirmed** gap, not merely
   "not found by this search": `practices/consumer-proof/engine/canary-gaps.yaml`
   `CANARY-001` already planted this exact need and verified it absent at
   2026-07-19 (`grep -rlE "locale.{0,20}(number|date).{0,25}format|
   toLocaleString.{0,100}(require|enforce|must|MUST)"
   practices-react/rules/*.md` → 0 matches). No `@ax/eslint-plugin-ax` rule id
   exists for this shape either. This scenario's grep-based guard is a
   scenario-local stand-in, not a proposed catalog rule — a real fix upstream
   would be a proper AST-shape ESLint rule (`ax/require-intl-number-date-format`
   or similar), out of scope for a scenario probe.

Both gaps corroborate the same finding via independent evidence (an
inventoried absence-of-asset search + a previously-planted, dated canary) —
this is not a one-off oversight in this scenario's search.

## Isolation

Everything here lives under `practices/consumer-proof/scenarios/S3.e-commerce/`
plus **read-only** invocations of the real `practices/evals/*.sh` catalog
guards (via `--root` pointed at this scenario's own fixture trees). Nothing
here touches `backend/src`, `frontend/src`, or `practices/evals/run-all-guards.sh`
— this scenario is not wired into R25 or any ax-template own gate.

## Directory layout

```
S3.e-commerce/
├── README.md                    (this file)
├── run-scenario-proof.sh        (harness driver — reuses the consumer-proof contract)
├── java/
│   ├── clean-root/backend/src/main/java/com/ax/template/authblueprint/
│   │   ├── checkout/  (Order, OrderStatus, OrderRepository, OrderService,
│   │   │               OrderController, OrderAdminController — all CLEAN)
│   │   └── common/Money.java   (seam stub, for realism only — not compiled)
│   └── violating-root/... (same shape, cases 1-3 injected)
├── react/
│   ├── clean/src/app/checkout/receipt/page.tsx      (Intl.*, thin route)
│   └── violating/src/app/checkout/receipt/page.tsx  (toLocaleString + concat)
└── scenario-guards/
    ├── unbounded_repository_read_guard.sh   (hand-rolled)
    └── locale_format_guard.sh               (hand-rolled)
```
