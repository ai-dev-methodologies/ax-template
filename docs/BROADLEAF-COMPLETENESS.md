# Broadleaf module-set exhaustion ledger

The Broadleaf-absorption program is a BOUNDED sweep over a FINITE codebase: Broadleaf's modules
are countable, so this sweep TERMINATES. It is NOT a new-industry dogfood and does NOT reopen the
frozen IDW18+ loop or the retired "100% completeness" north-star — it is the honest closure of the
"absorb ALL Broadleaf features" directive, classifying EVERY Broadleaf core subsystem so the program
closes with ZERO silent gaps.

Every core subsystem is classified ABSORBED / RE-FIND / SKIP / RESIDUE with a one-line evidence pointer.
Enforced mechanically by `practices/evals/broadleaf_module_exhaustion_guard.sh` [80]: every row has a
valid classification + non-empty evidence; the row count matches `module_count`; `residue_count` matches
the RESIDUE rows; every RESIDUE references an existing spec + parity record (no unledgered residue).

Reproducible disk-truth enumeration (with the Broadleaf clone present, outside git):
`find ../broadleaf-modernized/core/broadleaf-framework/src/main/java/org/broadleafcommerce/core -maxdepth 1 -type d`
→ 22 core subsystems (catalog .. workflow) + the admin module group.

module_count: 23
residue_count: 1

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
