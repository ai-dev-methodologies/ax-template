# Broadleaf module-set exhaustion ledger

The Broadleaf-absorption program is a BOUNDED sweep over a FINITE codebase: Broadleaf's modules
are countable, so this sweep TERMINATES. It is NOT a new-industry dogfood and does NOT reopen the
frozen IDW18+ loop or the retired "100% completeness" north-star — it is the honest closure of the
"absorb ALL Broadleaf features" directive, classifying EVERY Broadleaf subsystem so the program
closes with ZERO silent gaps.

The clone is a **9-Maven-module** platform, so the sweep is **two-level**:
1. **Maven module-set** (`maven_module_count: 9`) — every top-level Maven module classified
   ABSORBED / RE-FIND / SKIP. An ABSORBED module's correctness lives in the package table below
   (for `core/broadleaf-framework`) or in the named cross-cutting ax specs (for `profile` / `common`).
2. **Core commerce-package set** (`module_count: 23`) — every sub-package of
   `core/broadleaf-framework/src/main/java/org/broadleafcommerce/core` (the commerce engine),
   classified at finer grain. This is where the bulk of the portable correctness invariants live.

"Absorb" means the correctness INVARIANT holds in ax + external anchoring — NOT Broadleaf
feature/behavior parity. A SKIP is legitimate ONLY where the module/package carries no portable
correctness invariant (web/admin/plumbing/SEO/multi-currency-exchange features). A RE-FIND is
legitimate ONLY where a named ax spec genuinely covers the Broadleaf invariant.

Enforced mechanically by `practices/evals/broadleaf_module_exhaustion_guard.sh` [80]: every row
(both tables) has a valid classification + non-empty evidence; the row counts match the declared
`maven_module_count` / `module_count`; `residue_count` matches the RESIDUE rows; every RESIDUE
references an existing spec + parity record (no unledgered residue). **Disk-truth (live) check:**
when the Broadleaf clone is present at `../broadleaf-modernized`, the guard ALSO enumerates the
real Maven modules (`find -name pom.xml`) and the real `core` sub-packages, and FAILS if any
on-disk module/package is missing a classification row — so the counts are disk-truthful, not
self-asserted.

Reproducible disk-truth enumeration (clone present, outside git):
- Maven modules: `find ../broadleaf-modernized -maxdepth 3 -name pom.xml -exec dirname {} \;`
  → 8 module dirs + the reactor root = `core/broadleaf-framework{,-web}`, `core/broadleaf-profile{,-web}`,
  `common`, `admin/broadleaf-{admin-module,open-admin-platform,contentmanagement-module}`, `integration`.
- Core commerce packages: `find ../broadleaf-modernized/core/broadleaf-framework/src/main/java/org/broadleafcommerce/core -maxdepth 1 -type d`
  → 21 sub-packages (catalog .. workflow).

maven_module_count: 9
module_count: 23
residue_count: 1

## Maven module-set (top level)

| maven_module | classification | evidence |
|---|---|---|
| core/broadleaf-framework | ABSORBED | the commerce engine — its org.broadleafcommerce.core package = the 23-row core-package table below |
| core/broadleaf-framework-web | SKIP | Spring MVC controllers/web plumbing; portable correctness invariants live in the framework domains (controllers are thin per problem-details-l0) |
| core/broadleaf-profile | ABSORBED | Customer/Address/CustomerPayment default singleton = default-member-singleton-l0 (G006); CustomerForgotPasswordSecurityToken single-use+expiry = auth-asvs-l1 (password reset) + identity-claim-on-auth-l0 (possession token); customer identity = user/auth domain |
| core/broadleaf-profile-web | SKIP | profile Spring MVC controllers/web plumbing — no portable correctness invariant |
| common | ABSORBED | invariant-bearing packages cross-referenced: money = payment-l0 MONEY family (+ backend common/Money.java); audit = audit-log-l0 + JpaAuditConfig; i18n = i18n-policy-l0; sandbox = approval-workflow-l0 + content-versioning-l0; notification/email/sms = notification-l0 + email-outbox-l0; time = business-day-deadline-arithmetic-l0; security = auth-asvs-l1. Plumbing pkgs (persistence/vendor/extensibility/weave/dao/classloader/dialect/jmx) carry no portable invariant; encryption is a self-declared no-op SPI (real crypto = secrets-management-l0); sitemap = SEO XML feature; currency = multi-currency exchange feature (money invariant covered) |
| admin/broadleaf-admin-module | SKIP | admin-UI scaffolding — out of scope per anti-pattern (no portable correctness invariant) |
| admin/broadleaf-open-admin-platform | SKIP | dynamic-entity admin framework — out of scope per anti-pattern |
| admin/broadleaf-contentmanagement-module | RE-FIND | CMS structured-content/page/asset = content-versioning-l0 + approval-workflow-l0 + temporal-validity-l0; media blob lifecycle = file-storage-l0 |
| integration | SKIP | integration test harness — no shipped correctness invariant |

## Core commerce-package set (org.broadleafcommerce.core)

| module | classification | evidence |
|---|---|---|
| catalog | ABSORBED | commercecatalog + catalog-commerce-l0 (CAT-VARIANT/PRICING + CAT-INVENTORY-GATE-001) |
| checkout | ABSORBED | saga-orchestration-l0 SAGA-COMPENSATE-002 (register-before-act); rest re-find (parity/checkout.md) |
| config | SKIP | Spring FrameworkConfig classpath/cache wiring — not a correctness invariant; real config invariant = config-validation-l0 |
| core | SKIP | base framework plumbing (no portable correctness invariant) |
| demo | SKIP | demo seed data — not a correctness invariant |
| event | SKIP | ExtensionHandler/Manager SPI for sandbox-clone purge; event-publishing invariants = RE-FIND transactional-outbox-l0 + activity-feed-l0 |
| geolocation | SKIP | external IP-geo lookup adapter; outbound-call resilience = RE-FIND resilience-l0 / in-doubt-outbound-call-l0 |
| inventory | ABSORBED | CAT-INVENTORY-GATE-001 (tri-state policy); decrement/increment conservation = RE-FIND two-axis-inventory-reservation-l0 INVRES-COMMIT/RELEASE |
| media | RE-FIND | Media/asset blob lifecycle = file-storage-l0 + multipart-upload-l0 + storage-reconciliation-l0 |
| offer | ABSORBED | promotion-l0 (conserving proration / deterministic order / atomic max-uses / clamp) (parity/promotion.md) |
| order | ABSORBED | order-l0 (cart→order spine: snapshot / immutable-after-submit / merge / fulfillment-conserves) (parity/order.md) |
| payment | ABSORBED | payment-l0 + PAYMENT-SPLIT-001 split-tender coverage (parity/payment.md) |
| pricing | ABSORBED | pricing-l0 (discount-before-tax / conserving total closure) (parity/pricing.md) |
| promotionMessage | RE-FIND | priority-ordered + rule-gated + time-windowed banner = ordered-collection-l0 + temporal-validity-l0 + content-versioning-l0; discount math = promotion-l0 |
| rating | ABSORBED | derived-aggregate-consistency-l0 (denormalized MEAN recompute/eligibility/empty) (parity/rating.md) |
| registration | RESIDUE | identity-claim-on-auth-l0 (anonymous→registered atomic idempotent guarded claim) — the one genuine residue (parity/identity-claim.md) |
| rule | SKIP | MVEL rule-builder DTO + embedded expression engine = framework plumbing; rule correctness = RE-FIND promotion-l0 + temporal-validity-l0 |
| search | RE-FIND | faceted search criteria/ranges/redirect = search-l0 (SEARCH-QUERY/RANK) + query-field-allowlist-l0 + pagination-l0 + temporal-validity-l0 |
| social | RE-FIND | OAuth UserConnection token storage = auth-asvs-l1 V2.8.x + secrets-management-l0 |
| store | SKIP | brick-and-mortar store-locator (geo radius search) — niche, not multi-tenant; tenancy = multi-tenant-l0 |
| util | SKIP | DistributedLock/Queue infra; protected invariants = RE-FIND scheduled-task-l0 (SCHED-LOCK/RETENTION) + soft-delete-l0 (PURGE) |
| workflow | ABSORBED | saga-orchestration-l0 (ordered local-tx sequence + reverse-order compensation + register-before-act) (parity/checkout.md) |
| admin (admin-module / open-admin-platform / contentmanagement) | SKIP | admin-UI scaffolding + dynamic-entity framework (out of scope per anti-pattern); CMS content correctness = RE-FIND content-versioning-l0 + approval-workflow-l0 |
