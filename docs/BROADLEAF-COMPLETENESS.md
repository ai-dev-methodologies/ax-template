# Broadleaf module-set exhaustion ledger

The Broadleaf-absorption program is a BOUNDED sweep over a FINITE codebase: Broadleaf's modules
are countable, so this sweep TERMINATES. It is NOT a new-industry dogfood and does NOT reopen the
frozen IDW18+ loop or the retired "100% completeness" north-star — it is the honest closure of the
"absorb ALL Broadleaf features" directive, classifying every Broadleaf Maven module AND every
sub-package of each ABSORBED module (the core commerce engine + `common` + `profile`) so the program
closes with zero silent gaps **at every enumerated grain** (the bound is now sub-package, not module).

The clone is a **10-Maven-module** platform (10 built `jar` leaf modules under 3 `pom` aggregators),
so the sweep is **four-level**:
1. **Maven module-set** (`maven_module_count: 10`) — every top-level Maven module classified
   ABSORBED / RE-FIND / SKIP. An ABSORBED module's correctness is decomposed in the sub-package
   table(s) below (`core/broadleaf-framework` → level 2; `common` → level 3; `core/broadleaf-profile` → level 4).
2. **Core commerce-package set** (`module_count: 21`) — every sub-package of
   `core/broadleaf-framework/src/main/java/org/broadleafcommerce/core` (the commerce engine),
   classified at finer grain. This is where the bulk of the portable correctness invariants live.
3. **Common sub-package set** (`common_subpackage_count: 56`) — every sub-package of
   `common/src/main/java/org/broadleafcommerce/common`. Added 2026-06-27: the earlier ledger
   classified `common` only at module grain, letting a real invariant (e.g. an ID allocator) hide
   un-adjudicated under one ABSORBED row. Now every common sub-package is classified + disk-checked.
4. **Profile-core sub-package set** (`profile_subpackage_count: 8`) — every sub-package of
   `core/broadleaf-profile/src/main/java/org/broadleafcommerce/profile/core`.

"Absorb" means the correctness INVARIANT holds in ax + external anchoring — NOT Broadleaf
feature/behavior parity. A SKIP is legitimate ONLY where the module/package carries no portable
correctness invariant (web/admin/plumbing/SEO/multi-currency-exchange features). A RE-FIND is
legitimate ONLY where a named ax spec genuinely covers the Broadleaf invariant.

Enforced mechanically by `practices/evals/broadleaf_module_exhaustion_guard.sh` [80]: every row
(all four tables) has a valid classification + non-empty evidence; the row counts match the declared
`maven_module_count` / `module_count` / `common_subpackage_count` / `profile_subpackage_count`;
`residue_count` matches the RESIDUE rows; every RESIDUE references an existing spec + parity record
(no unledgered residue). **Disk-truth (live) check:** when the Broadleaf clone is present at
`../broadleaf-modernized`, the guard ALSO enumerates the real built Maven modules (`find -name pom.xml`,
packaging ≠ pom), the real `core` sub-packages, AND (since 2026-06-27) the real `common` + `profile/core`
sub-packages, and FAILS if any on-disk module/package is missing a classification row — so the counts are
disk-truthful, not self-asserted, at every grain (no invariant can hide one level below an ABSORBED row).

Reproducible disk-truth enumeration (clone present, outside git):
- Maven modules: `find ../broadleaf-modernized -name pom.xml -not -path '*/target/*'`
  → 13 poms = 3 aggregator poms (packaging `pom`, build no artifact: the reactor root, `core`, `admin`)
  + 10 built leaf modules (packaging `jar`) = `core/broadleaf-framework{,-web}`, `core/broadleaf-profile{,-web}`,
  `common`, `admin/broadleaf-{admin-module,open-admin-platform,contentmanagement-module,admin-functional-tests}`,
  `integration`. The 10 leaf modules are exactly the 10 classification rows below.
- Core commerce packages: `find ../broadleaf-modernized/core/broadleaf-framework/src/main/java/org/broadleafcommerce/core -mindepth 1 -maxdepth 1 -type d`
  → 21 sub-packages (catalog .. workflow).
- Common sub-packages: `find ../broadleaf-modernized/common/src/main/java/org/broadleafcommerce/common -mindepth 1 -maxdepth 1 -type d` → 56.
- Profile-core sub-packages: `find ../broadleaf-modernized/core/broadleaf-profile/src/main/java/org/broadleafcommerce/profile/core -mindepth 1 -maxdepth 1 -type d` → 8.

maven_module_count: 10
module_count: 21
common_subpackage_count: 56
profile_subpackage_count: 8
residue_count: 1

## Maven module-set (top level)

| maven_module | classification | evidence |
|---|---|---|
| core/broadleaf-framework | ABSORBED | the commerce engine — its org.broadleafcommerce.core package = the 21-row core-package table below |
| core/broadleaf-framework-web | SKIP | Spring MVC controllers/web plumbing; portable correctness invariants live in the framework domains (controllers are thin per problem-details-l0) |
| core/broadleaf-profile | ABSORBED | Customer/Address/CustomerPayment default singleton = default-member-singleton-l0 (G006); CustomerForgotPasswordSecurityToken single-use+expiry = auth-asvs-l1 (password reset) + identity-claim-on-auth-l0 (possession token); customer identity = user/auth domain |
| core/broadleaf-profile-web | SKIP | profile Spring MVC controllers/web plumbing — no portable correctness invariant |
| common | ABSORBED | invariant-bearing packages cross-referenced: money = payment-l0 MONEY family (+ backend common/Money.java); audit = audit-log-l0 + JpaAuditConfig; i18n = i18n-policy-l0; sandbox = approval-workflow-l0 + content-versioning-l0; notification/email/sms = notification-l0 + email-outbox-l0; time = business-day-deadline-arithmetic-l0; security = auth-asvs-l1. Plumbing pkgs (persistence/vendor/extensibility/weave/dao/classloader/dialect/jmx) carry no portable invariant; encryption is a self-declared no-op SPI (real crypto = secrets-management-l0); sitemap = SEO XML feature; currency = multi-currency exchange feature (money invariant covered) |
| admin/broadleaf-admin-module | SKIP | admin-UI scaffolding — out of scope per anti-pattern (no portable correctness invariant) |
| admin/broadleaf-open-admin-platform | SKIP | dynamic-entity admin framework — out of scope per anti-pattern |
| admin/broadleaf-contentmanagement-module | RE-FIND | CMS structured-content/page/asset = content-versioning-l0 + approval-workflow-l0 + temporal-validity-l0; media blob lifecycle = file-storage-l0 |
| admin/broadleaf-admin-functional-tests | SKIP | Selenium/functional admin-UI test harness — 0 main + 0 test Java files on disk (empty module shell, build artifact only); no shipped correctness invariant (same rationale as `integration`) |
| integration | SKIP | integration test harness — no shipped correctness invariant |

## Core commerce-package set (org.broadleafcommerce.core)

| module | classification | evidence |
|---|---|---|
| catalog | ABSORBED | commercecatalog + catalog-commerce-l0 (CAT-VARIANT/PRICING + CAT-INVENTORY-GATE-001) |
| checkout | ABSORBED | saga-orchestration-l0 SAGA-COMPENSATE-002 (register-before-act); rest re-find (parity/checkout.md) |
| config | SKIP | Spring FrameworkConfig classpath/cache wiring — not a correctness invariant; real config invariant = config-validation-l0 |
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

## Common sub-package set (org.broadleafcommerce.common)

| common_package | classification | evidence |
|---|---|---|
| admin | SKIP | `admin/condition` = Spring `@ConditionalOnAdmin` bean-presence annotations; `admin/domain` = `AdminMainEntity` display-name interface; pure admin UI plumbing, no portable correctness invariant |
| audit | ABSORBED | audit-log-l0 — audit event capture + immutable record invariants absorbed by audit-log-l0 |
| breadcrumbs | SKIP | `BreadcrumbDTO` + `BreadcrumbService` build UI navigation trail; navigation rendering feature, no data-integrity invariant |
| cache | SKIP | `StatisticsService` + JCache/EhCache configuration + `AbstractCacheMissAware`; caching infrastructure and metrics, no correctness invariant |
| classloader | SKIP | Single `ThreadLocalManager.java`; classloader thread-local lifecycle infrastructure, no domain invariant |
| condition | SKIP | `ConditionalOnBroadleafModule` Spring conditional annotations; framework module-presence wiring plumbing |
| config | SKIP | Spring `@EnableBroadleafAutoConfiguration` + environment configurers; auto-configuration + environment wiring plumbing |
| copy | RE-FIND | multi-tenant-l0 — `MultiTenantCopier` + `EntityDuplicator` enforce tenant-scoped entity ownership (not owned by another site, not referenced in a derived catalog beyond standard override) = multi-tenant-l0 isolation invariant |
| crossapp | RE-FIND | auth-asvs-l1 — `CrossAppAuthService` single-use, time-sensitive token for cross-app admin auth; single-use + expiry = ASVS session token integrity (auth-asvs-l1) |
| currency | RE-FIND | payment-l0 — multi-currency exchange is a feature; money invariant covered by payment-l0 MONEY |
| dao | SKIP | `GenericEntityDao` JPA persistence DAO infrastructure, no domain invariant |
| demo | SKIP | `AutoImportSql` / `ImportSQLConfig`; demo seed-data SQL import infrastructure |
| dialect | SKIP | `BroadleafPostgreSQLDialect` Hibernate dialect configuration, DB infrastructure |
| domain | SKIP | Single `AdditionalFields.java` JAXB map extensibility interface; framework extension marker, no correctness invariant |
| email | ABSORBED | email-outbox-l0 — outbox queue + retry invariants absorbed by email-outbox-l0 |
| encryption | SKIP | self-declared no-op SPI stub; real crypto = secrets-management-l0 |
| entity | RE-FIND | multi-tenant-l0 — `EntityInformationService` resolves owningSiteId/catalogId/profileId; tenant-scoping fields powering isolation invariant = multi-tenant-l0 |
| enumeration | SKIP | `DataDrivenEnumeration` admin-managed lookup table; product-configuration constraint, not a portable correctness invariant |
| event | SKIP | Spring application event bus + order-lifecycle event DTOs; events are notification wires; state-transition invariants enforced in order/payment domain |
| exception | SKIP | Exception class hierarchy; error signals only, no invariant enforcement logic |
| expression | SKIP | `BroadleafExpressionParser` wrapping MVEL; expression evaluation infrastructure |
| extensibility | SKIP | Spring XML merge reader + JPA weaving extension points; framework plumbing |
| extension | SKIP | `ExtensionHandler`/`ExtensionManager` SPI framework; plugin architecture infrastructure |
| file | RE-FIND | file-storage-l0 — file storage invariants covered by file-storage-l0 |
| filter | SKIP | admin UI list-query filter DSL (DAO-layer filtering objects), no business correctness invariant |
| i18n | ABSORBED | i18n-policy-l0 — locale negotiation + message-source invariants absorbed by i18n-policy-l0 |
| id | SKIP | ax delegates identity to DB-native generation (@GeneratedValue IDENTITY/UUID) — duplicate-free + monotonic ID invariant holds by construction at the persistence layer; Broadleaf hi-lo IdGenerationServiceImpl batch allocator is a clustered-ID performance optimization, not a portable correctness invariant |
| io | SKIP | `AtomicMove`/`ConcurrentFileOutputStream` file I/O utilities; OS-level atomicity primitive, not a portable business invariant |
| jmx | SKIP | JMX MBean management infrastructure |
| locale | ABSORBED | i18n-policy-l0 — locale resolution invariants absorbed by i18n-policy-l0 |
| logging | SKIP | `SupportLogger` logging adapter infrastructure |
| media | RE-FIND | file-storage-l0 — media/file storage invariants covered by file-storage-l0 |
| module | SKIP | `BroadleafModuleRegistration` module presence detection utility; no domain invariant |
| money | ABSORBED | payment-l0 — money value + arithmetic invariants absorbed by payment-l0 MONEY family + backend/common/Money.java |
| notification | ABSORBED | notification-l0 — outbound notification delivery invariants absorbed by notification-l0 |
| page | RE-FIND | content-versioning-l0 — page draft/live versioning invariants covered by content-versioning-l0 |
| payment | RE-FIND | payment-l0 — payment processing invariants covered by payment-l0 |
| persistence | RE-FIND | content-versioning-l0 — `ArchiveStatus` soft-delete flag + `Previewable`/`PreviewStatus` draft-vs-live isolation (preview must not leak to production) = content-versioning-l0 |
| presentation | SKIP | `AdminPresentation` annotation metadata; admin UI rendering hints, zero domain logic |
| resource | SKIP | `ResourceBundlingService` + CSS/JS minification; static-asset bundling pipeline; web infrastructure |
| rest | SKIP | REST response wrapper infrastructure; framework plumbing |
| rule | SKIP | `MvelHelper`+`RuleProcessor`+`QuantityBasedRule` MVEL expression framework; invariant enforcement delegated to offer/pricing domain |
| sandbox | ABSORBED | approval-workflow-l0 — sandbox approval workflow + draft/live content versioning invariants absorbed by approval-workflow-l0 + content-versioning-l0 |
| security | ABSORBED | auth-asvs-l1 — authentication and authorization invariants absorbed by auth-asvs-l1 |
| service | SKIP | `GenericEntityService`/`PersistenceService` generic JPA entity management; persistence infrastructure |
| site | RE-FIND | multi-tenant-l0 — site/tenant isolation invariants covered by multi-tenant-l0 |
| sitemap | SKIP | SEO XML feature |
| sms | ABSORBED | notification-l0 — SMS delivery invariants absorbed by notification-l0 |
| structure | RE-FIND | content-versioning-l0 — structured content versioning invariants covered by content-versioning-l0 |
| template | RE-FIND | multi-tenant-l0 — `TemplateOverrideExtensionHandler` per-site template isolation (a site renders its own configured template) = multi-tenant-l0 |
| time | ABSORBED | business-day-deadline-arithmetic-l0 — business-day time arithmetic invariants absorbed by business-day-deadline-arithmetic-l0 |
| util | SKIP | General utilities (BLCArrayUtils/BLCDateUtils/ValidationUtil/TransactionalOperation); support library, no standalone portable invariant |
| value | SKIP | Single `ValueAssignable.java` marker interface; framework extension point only |
| vendor | SKIP | `AbstractVendorService` HTTP POST utility for third-party vendor calls; generic integration base class, no domain invariant |
| weave | SKIP | `ConditionalDirectCopyTransformersManager` JPA/bytecode weaving instrumentation; ORM infrastructure |
| web | SKIP | MVC request filter layer (BroadleafRequestFilter/SiteResolver/LocaleResolver); establishes request context consumed by domain layers; no standalone domain invariant |

## Profile-core sub-package set (org.broadleafcommerce.profile.core)

| profile_package | classification | evidence |
|---|---|---|
| config | SKIP | Single `ProfileConfig.java` Spring configuration class; framework wiring plumbing |
| dao | SKIP | `CustomerDao`/`AddressDao`/`CustomerPaymentDao` JPA DAO implementations; persistence infrastructure |
| demo | SKIP | `ImportSQLConfig.java`; demo seed-data SQL import configuration |
| domain | ABSORBED | default-member-singleton-l0 — `Customer`/`CustomerImpl` singleton member (unique email, G006); `CustomerForgotPasswordSecurityToken` reset credential (ASVS AUTHN-CRED, auth-asvs-l1); `CustomerPayment` stored instrument (payment-l0); `CustomerAddress` member address — absorbed under default-member-singleton-l0 + auth-asvs-l1 |
| dto | SKIP | `CustomerRuleHolder.java` DTO supplying customer attributes to MVEL rule context; rule engine plumbing |
| event | RE-FIND | notification-l0 — account-lifecycle event listeners (ForgotPassword/ForgotUsername/RegisterCustomer) wire to outbound notification delivery = notification-l0 |
| extension | SKIP | `PostUpdateCustomerExtensionHandler` SPI hook for post-update callbacks; framework extension plumbing |
| service | ABSORBED | default-member-singleton-l0 — `CustomerService` (member uniqueness, password lifecycle), `CustomerPaymentService`, `AddressVerificationProvider` SPI — member-singleton + credential-management invariants absorbed by default-member-singleton-l0 + auth-asvs-l1 |
