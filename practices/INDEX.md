---
sentinel:
  source_concat_sha256: "94fc42149d28ce6d347644242bd8c80bf6376b5b422d0c65e35a4ee1b6301c81"
  rule_count: 233
  generated_by: "practices/generate_index.sh"
---

# practices — Rule INDEX (auto-generated)

## By tag
- **2fa** (1) — two-factor-auth-totp-webauthn
- **a11y** (2) — error-message-not-in-native-title-attribute, mutation-in-flight-uses-aria-busy
- **abstraction** (1) — messaging-publisher-interface
- **accept** (1) — http-content-negotiation-rfc9110
- **access-control** (6) — bfla-privileged-endpoint-authz-presence, consume-path-consults-shared-fail-closed-blocking-gate, containment-scope-authz-tree-derived-downward-cascade, ownership-transfer-authz-audit-atomic, reproducible-procedure-recorded-seed-replay-and-blinding, time-bounded-access-grant-rebac-window-and-eligibility-gate
- **accessibility** (1) — background-poll-must-show-refresh-state
- **account-lifecycle** (1) — owner-deprovision-reassigns-not-orphans
- **account-suspension** (1) — consume-path-consults-shared-fail-closed-blocking-gate
- **accrual** (1) — time-proportional-accrual-prorates-partial-period
- **accumulator** (1) — accumulator-consume-is-atomic-non-rejecting
- **actuator** (3) — actuator-build-info, actuator-kubernetes-probes, actuator-restrict-exposure
- **acyclicity** (1) — self-referential-tree-reparent-rejects-cycle
- **admin** (2) — bfla-privileged-endpoint-authz-presence, destructive-action-confirm-with-side-effects
- **admin-surface** (1) — stored-server-error-sanitize-at-render-layer
- **advice** (1) — error-controller-advice
- **aeo** (1) — customs-e-trade-hs-edi-aeo
- **aggregate** (2) — composite-bundle-price-is-conserving-rollup-of-children, derived-aggregate-consistency-recompute-eligibility-empty
- **aggregation** (1) — lang-bigdecimal-for-measured-decimals
- **alerting** (1) — observability-slo-error-budget-convention
- **alg-confusion** (1) — externally-verifiable-artifact-uses-asymmetric-signature
- **allocation** (4) — accumulator-consume-is-atomic-non-rejecting, banded-pricing-segments-qty-per-band-and-conserves, rounded-split-conserves-total-largest-remainder, transformation-conserves-with-classified-residual
- **anonymous-access** (1) — public-lookup-token-is-unguessable-and-enumeration-resistant
- **aop** (3) — core-aop-proxy-no-final, multi-tenant-aop-guard-skeleton, transaction-no-self-invocation
- **api** (6) — api-idempotency-key-required, api-no-entity-leak, api-pagination-pageable, api-versioning-uri-prefix, caller-authentication-only-no-userid-param, http-delete-idempotency-rfc9110
- **api-contract** (1) — error-rfc7807-problem-detail
- **api-key** (1) — secret-shown-once-uses-beforeunload-guard
- **append-only** (4) — immutable-record-corrected-by-reversal-not-edit, provenance-dag-append-only-acyclic-rollup, published-edition-immutable-copy-on-write, tamper-evident-log-hashchain
- **approval-workflow** (1) — e-government-approval-gpki-approval-line
- **architecture** (4) — business-domain-must-declare-applied-recipe, prefer-recipe-composition-over-l4-cross-import, testing-archunit-layer-boundary, testing-archunit-no-cyclic-packages
- **archunit** (3) — testing-archunit-layer-boundary, testing-archunit-no-cyclic-packages, testing-archunit-repository-shape
- **aria** (1) — mutation-in-flight-uses-aria-busy
- **aria-busy** (1) — background-poll-must-show-refresh-state
- **aria-live** (1) — error-message-not-in-native-title-attribute
- **asymmetric** (1) — externally-verifiable-artifact-uses-asymmetric-signature
- **async** (3) — async-completablefuture-return-type, async-scheduled-fixed-delay-vs-fixed-rate, async-virtual-thread-executor
- **at-least-once** (3) — async-job-queue-at-least-once-dlq, monotonic-ingest-reject-stale-event, transactional-outbox-no-dual-write
- **atomic-claim** (1) — shared-counter-claim-must-be-atomic
- **atomicity** (2) — bulk-operation-partial-success-207, ownership-transfer-authz-audit-atomic
- **attribution** (1) — referral-disclosure-and-fraud-controls
- **audit** (39) — admin-cannot-rewrite-user-content, attested-governed-edit-carries-reason-and-preimage, audit-log-pii-hash-required, authorization-parity-executed-matches-authorized-four-eyes-positive-gates, business-day-deadline-arithmetic-calendar-vs-business-roll-and-versioned-holidays, client-must-not-fabricate-audit-timestamps, completion-reset-recurring-interval, computed-decision-versioned-basis-and-four-eyes-override, consent-explicit-optin-withdrawable-recorded, deadline-obligation-grounded-multi-axis-ladder-closed-loop, derived-key-idempotent-statement-generation, dimensional-uom-conversion-compatibility-bridge-and-basis, dunning-collections-one-way-ladder-aging-and-cure, external-reconciliation-classify-dispose-resolve-and-idempotent-rerun, immutable-record-corrected-by-reversal-not-edit, machine-computed-value-tracks-override-provenance, mandate-fanout-conserved-recall-check-battery-and-deemed-election, material-divisibility-reject-not-round-integer-vs-fractional, order-multiple-quantization-non-conserving-ceiling-to-moq, ownership-transfer-authz-audit-atomic, period-close-reject-late-write, pii-masked-at-dto-boundary, record-linkage-banded-verdict-and-survivorship-merge, remeasurement-supersession-versioned-recompute-and-trueup, reproducible-procedure-recorded-seed-replay-and-blinding, saturating-clamped-running-balance, sealed-period-watermark-monotonic-close, security-token-transfer-compliance-gate, self-reported-input-plausibility-range-rate-and-unverified-provenance, sensitive-read-audit-record-before-return-mask-and-purpose, settlement-finality-dvp-novation-and-fail-ladder, soft-delete-audit-trail, state-conditional-mutation-authority-is-a-declared-monotone-table, subscription-state-machine-explicit, tamper-evident-log-hashchain, timed-offer-exclusive-assignment-and-reoffer-ladder, transaction-propagation-requires-new, valuation-run-projection-as-of-snapshot-conserving-fan-out-and-rebase, variance-tolerance-band-asymmetric-gate-and-disposition
- **audit-trail** (1) — business-domain-must-declare-applied-recipe
- **authentication** (1) — two-factor-auth-totp-webauthn
- **authn** (1) — password-reset-success-invalidates-token-family
- **authorization** (10) — authorization-parity-executed-matches-authorized-four-eyes-positive-gates, containment-scope-authz-tree-derived-downward-cascade, facet-count-scope-parity-and-allowlist, negative-copresence-gate-is-set-evaluated-graded-failclosed, offer-eligibility-predicate-evaluated-fail-closed-from-declared-criteria, ownership-transfer-authz-audit-atomic, presigned-url-signature-required, query-field-allowlist-sort-filter-bound, state-conditional-mutation-authority-is-a-declared-monotone-table, time-bounded-access-grant-rebac-window-and-eligibility-gate
- **authz** (10) — admin-cannot-rewrite-user-content, bfla-privileged-endpoint-authz-presence, caller-authentication-only-no-userid-param, field-level-projection-authz-omits-not-nulls, owner-deprovision-reassigns-not-orphans, public-lookup-token-is-unguessable-and-enumeration-resistant, rbac-stub-default-fail-closed, relationship-scoped-authz-via-grant-lookup, role-hierarchy-subsumes-lower-tiers, time-gated-decisions-read-injected-clock
- **automated-decision** (1) — machine-computed-value-tracks-override-provenance
- **background-poll** (2) — background-poll-must-show-refresh-state, incident-dashboard-background-poll-plus-refresh
- **balance** (1) — balance-reservation-is-two-phase-and-conserving
- **base-entity** (1) — soft-delete-only-on-base-entity
- **batch** (1) — bulk-operation-partial-success-207
- **bcp-47** (1) — i18n-default-and-supported-locales-declared
- **bean-validation** (1) — config-validation-fail-fast-typed
- **beforeunload** (1) — secret-shown-once-uses-beforeunload-guard
- **bfla** (3) — bfla-privileged-endpoint-authz-presence, rbac-stub-default-fail-closed, role-hierarchy-subsumes-lower-tiers
- **bigdecimal** (14) — accumulator-consume-is-atomic-non-rejecting, balance-reservation-is-two-phase-and-conserving, banded-pricing-segments-qty-per-band-and-conserves, cumulative-register-is-value-monotone-with-governed-reset, lang-bigdecimal-for-measured-decimals, lang-bigdecimal-for-money, limit-crossing-drives-irreversible-terminal-and-blocks-derived-use, multilateral-netting-conserves-per-node-and-set-wide, net-meter-signed-net-is-derived-from-two-monotone-direction-registers, rounded-split-conserves-total-largest-remainder, time-proportional-accrual-prorates-partial-period, transformation-conserves-with-classified-residual, unit-of-measure-conversion-is-exact-and-pinned, value-transfer-must-be-balanced
- **billing** (11) — billing-event-idempotent, business-day-deadline-arithmetic-calendar-vs-business-roll-and-versioned-holidays, currency-amount-precision-explicit, dimensional-uom-conversion-compatibility-bridge-and-basis, dunning-collections-one-way-ladder-aging-and-cure, external-reconciliation-classify-dispose-resolve-and-idempotent-rerun, korean-vat-10-percent-calculation, no-billing-cross-import-from-payment, order-multiple-quantization-non-conserving-ceiling-to-moq, subscription-state-machine-explicit, valuation-run-projection-as-of-snapshot-conserving-fan-out-and-rebase
- **bitemporal** (1) — temporal-as-of-point-in-time-query
- **blast-radius** (1) — provenance-dag-traversal-is-bounded-and-cycle-safe
- **blocking-status** (1) — consume-path-consults-shared-fail-closed-blocking-gate
- **bola** (2) — polymorphic-entity-ref-path-segment-guard, relationship-scoped-authz-via-grant-lookup
- **bom-explosion** (1) — provenance-dag-traversal-is-bounded-and-cycle-safe
- **boundary** (1) — no-billing-cross-import-from-payment
- **bounded-labels** (1) — domain-metrics-bounded-cardinality
- **bounded-traversal** (1) — provenance-dag-traversal-is-bounded-and-cycle-safe
- **brn** (1) — korean-brn-format
- **build** (4) — actuator-build-info, build-java-toolchain-explicit, build-no-snapshot-dependencies, build-spring-boot-bom
- **bulk-operation** (1) — bulk-operation-partial-success-207
- **bulkhead** (1) — resilience-circuit-breaker-retry-bulkhead
- **business-logic** (3) — monetary-arithmetic-fails-closed-across-currencies-absent-explicit-recorded-conversion, offer-eligibility-predicate-evaluated-fail-closed-from-declared-criteria, order-tax-application-skips-exempt-scope-and-recompute-converges-to-one-record
- **cache** (4) — cache-caffeine-expiration, cache-explicit-name-key-sync, cache-not-on-controllers, cacheable-requires-explicit-ttl
- **cache-coherence** (1) — optimistic-update-snapshot-rollback
- **caffeine** (3) — cache-caffeine-expiration, cache-explicit-name-key-sync, cacheable-requires-explicit-ttl
- **calculation** (2) — material-divisibility-reject-not-round-integer-vs-fractional, order-multiple-quantization-non-conserving-ceiling-to-moq
- **capability-token** (1) — public-lookup-token-is-unguessable-and-enumeration-resistant
- **capacity** (1) — shared-counter-claim-must-be-atomic
- **cardinality** (1) — domain-metrics-bounded-cardinality
- **catalog** (1) — catalog-variant-resolves-unique-active-sku-and-purchasability-gated
- **catalog-meta** (2) — promote-on-third-use, spec-domain-mode-gates-frontend-trio
- **catalog-quality** (3) — dogfood-finding-must-have-expiry-trigger, dogfood-finding-real-bug-must-reference-closure-commit, dogfood-finding-real-bug-must-reference-test-coverage
- **chain-of-custody** (1) — tamper-evident-log-hashchain
- **chat** (1) — chat-message-delivery-receipts-presence
- **checksum** (1) — stored-blob-carries-content-digest-verified-on-read
- **chunking** (1) — chunked-import-required-when-rowcount-gt-1000
- **circuit-breaker** (1) — resilience-circuit-breaker-retry-bulkhead
- **cleanup** (1) — resumable-upload-tus-offset
- **clock** (1) — time-gated-decisions-read-injected-clock
- **closure-traceability** (1) — dogfood-finding-real-bug-must-reference-closure-commit
- **collection** (2) — default-member-singleton-exactly-one-default-clear-then-set, ordered-siblings-reorder-atomic
- **compensating-transaction** (1) — saga-compensating-transactions
- **compliance** (3) — customs-e-trade-hs-edi-aeo, e-government-approval-gpki-approval-line, electronic-tax-invoice-vat-pki-retention
- **composition** (1) — composite-bundle-price-is-conserving-rollup-of-children
- **composition-kit** (1) — prefer-recipe-composition-over-l4-cross-import
- **concurrency** (40) — accumulator-consume-is-atomic-non-rejecting, async-virtual-thread-executor, authorization-parity-executed-matches-authorized-four-eyes-positive-gates, balance-reservation-is-two-phase-and-conserving, completion-reset-recurring-interval, containment-scope-authz-tree-derived-downward-cascade, core-singleton-no-mutable-state, cumulative-register-is-value-monotone-with-governed-reset, deadline-obligation-grounded-multi-axis-ladder-closed-loop, default-member-singleton-exactly-one-default-clear-then-set, dunning-collections-one-way-ladder-aging-and-cure, external-reconciliation-classify-dispose-resolve-and-idempotent-rerun, identity-claim-on-auth-atomic-idempotent-guarded, limit-crossing-drives-irreversible-terminal-and-blocks-derived-use, mandate-fanout-conserved-recall-check-battery-and-deemed-election, multilateral-netting-conserves-per-node-and-set-wide, negative-copresence-gate-is-set-evaluated-graded-failclosed, net-meter-signed-net-is-derived-from-two-monotone-direction-registers, ordered-siblings-reorder-atomic, persistence-optimistic-locking, persistence-state-machine-atomic, promotion-offer-engine-conserves-determinism-atomic-cap, quorum-weighted-tally-with-quorum-gate-and-frozen-policy, quota-atomic-tenant-claim, record-linkage-banded-verdict-and-survivorship-merge, remeasurement-supersession-versioned-recompute-and-trueup, reproducible-procedure-recorded-seed-replay-and-blinding, saturating-clamped-running-balance, security-token-transfer-compliance-gate, self-reported-input-plausibility-range-rate-and-unverified-provenance, settlement-finality-dvp-novation-and-fail-ladder, shared-counter-claim-must-be-atomic, state-conditional-mutation-authority-is-a-declared-monotone-table, time-bounded-access-grant-rebac-window-and-eligibility-gate, timed-offer-exclusive-assignment-and-reoffer-ladder, timeout-sweep-is-a-concurrent-mutator, two-axis-inventory-reservation-reserve-commit-release-hold, valuation-run-projection-as-of-snapshot-conserving-fan-out-and-rebase, variance-tolerance-band-asymmetric-gate-and-disposition, waitlist-promotion-is-atomic-fifo
- **config** (3) — config-no-secret-in-yaml, config-profile-isolation, config-typed-properties
- **configuration** (3) — config-validation-fail-fast-typed, i18n-default-and-supported-locales-declared, realtime-single-protocol-declared
- **configuration-properties** (1) — config-typed-properties
- **confirm-dialog** (1) — destructive-action-confirm-with-side-effects
- **consent** (1) — consent-explicit-optin-withdrawable-recorded
- **conservation** (19) — accumulator-consume-is-atomic-non-rejecting, balance-reservation-is-two-phase-and-conserving, banded-pricing-segments-qty-per-band-and-conserves, composite-bundle-price-is-conserving-rollup-of-children, cumulative-register-is-value-monotone-with-governed-reset, derived-key-idempotent-statement-generation, limit-crossing-drives-irreversible-terminal-and-blocks-derived-use, multilateral-netting-conserves-per-node-and-set-wide, net-meter-signed-net-is-derived-from-two-monotone-direction-registers, payment-split-tender-coverage-sums-to-total-and-capture-bounded-by-auth, remeasurement-supersession-versioned-recompute-and-trueup, rounded-split-conserves-total-largest-remainder, saturating-clamped-running-balance, settlement-finality-dvp-novation-and-fail-ladder, time-proportional-accrual-prorates-partial-period, transformation-conserves-with-classified-residual, two-axis-inventory-reservation-reserve-commit-release-hold, unit-of-measure-conversion-is-exact-and-pinned, value-transfer-must-be-balanced
- **consistency** (1) — derived-aggregate-consistency-recompute-eligibility-empty
- **constants** (1) — messaging-topic-name-constant
- **content-negotiation** (2) — http-content-negotiation-rfc9110, web-explicit-produces
- **content-versioning** (1) — published-edition-immutable-copy-on-write
- **controller** (2) — cache-not-on-controllers, web-rest-controller-annotation
- **conversion** (1) — unit-of-measure-conversion-is-exact-and-pinned
- **copy-on-write** (1) — published-edition-immutable-copy-on-write
- **core** (3) — core-aop-proxy-no-final, core-constructor-injection, core-singleton-no-mutable-state
- **correction** (1) — immutable-record-corrected-by-reversal-not-edit
- **correlation** (1) — distributed-tracing-w3c-context-propagation
- **cors** (1) — cors-allowlist-and-preflight
- **counter** (1) — denormalized-counter-reconcilable
- **credential-lifecycle** (1) — secret-shown-once-uses-beforeunload-guard
- **credential-recovery** (1) — password-reset-success-invalidates-token-family
- **credentials** (1) — cors-allowlist-and-preflight
- **cross-cutting** (1) — multi-tenant-aop-guard-skeleton
- **csrf** (1) — security-csrf-scoped-disable
- **currency** (4) — currency-amount-precision-explicit, korean-vat-10-percent-calculation, monetary-arithmetic-fails-closed-across-currencies-absent-explicit-recorded-conversion, payment-iso-4217-currency
- **custom-constraint** (1) — validation-custom-constraint
- **customs** (1) — customs-e-trade-hs-edi-aeo
- **cwe-362** (1) — shared-counter-claim-must-be-atomic
- **cwe-367** (1) — temporal-validity-record-non-overlap
- **cwe-640** (1) — password-reset-success-invalidates-token-family
- **cycle-detection** (1) — provenance-dag-traversal-is-bounded-and-cycle-safe
- **dag** (2) — provenance-dag-append-only-acyclic-rollup, provenance-dag-traversal-is-bounded-and-cycle-safe
- **dashboards-as-code** (1) — observability-slo-error-budget-convention
- **data-exposure** (2) — field-level-projection-authz-omits-not-nulls, pii-masked-at-dto-boundary
- **data-freshness** (2) — background-poll-must-show-refresh-state, incident-dashboard-background-poll-plus-refresh
- **data-integrity** (12) — attested-governed-edit-carries-reason-and-preimage, catalog-variant-resolves-unique-active-sku-and-purchasability-gated, default-member-singleton-exactly-one-default-clear-then-set, denormalized-counter-reconcilable, derived-aggregate-consistency-recompute-eligibility-empty, destructive-remove-checks-inbound-references, identity-claim-on-auth-atomic-idempotent-guarded, machine-computed-value-tracks-override-provenance, order-cart-spine-price-snapshot-immutable-after-submit-merge-and-fulfillment-conserves, owner-deprovision-reassigns-not-orphans, soft-delete-only-on-base-entity, temporal-as-of-point-in-time-query
- **data-quality** (1) — self-reported-input-plausibility-range-rate-and-unverified-provenance
- **data-retention** (2) — erasure-and-purge-consult-legal-hold-gate, soft-delete-audit-trail
- **ddd** (1) — no-billing-cross-import-from-payment
- **dead-letter-queue** (1) — async-job-queue-at-least-once-dlq
- **default-deny** (1) — consume-path-consults-shared-fail-closed-blocking-gate
- **default-flag** (1) — default-member-singleton-exactly-one-default-clear-then-set
- **defense-in-depth** (3) — polymorphic-entity-ref-path-segment-guard, server-side-stored-error-sanitize, stored-server-error-sanitize-at-render-layer
- **delivery-receipt** (1) — chat-message-delivery-receipts-presence
- **denormalization** (2) — denormalized-counter-reconcilable, derived-aggregate-consistency-recompute-eligibility-empty
- **dependency-management** (2) — build-no-snapshot-dependencies, build-spring-boot-bom
- **destructive-action** (2) — destructive-action-confirm-with-side-effects, destructive-remove-checks-inbound-references
- **determinism** (3) — pricing-pipeline-orders-discount-before-tax-and-total-conserves, promotion-offer-engine-conserves-determinism-atomic-cap, reproducible-procedure-recorded-seed-replay-and-blinding
- **dev-stub** (1) — rbac-stub-default-fail-closed
- **di** (1) — core-constructor-injection
- **digital-signature** (1) — document-signing-pki-timestamp-revocation
- **distributed-lock** (1) — mutation-skipped-outcome-surfaces-reason
- **distributed-systems** (1) — in-doubt-outbound-call-holding-state
- **distributed-tracing** (1) — distributed-tracing-w3c-context-propagation
- **distributed-transaction** (1) — saga-compensating-transactions
- **dogfood** (3) — dogfood-finding-must-have-expiry-trigger, dogfood-finding-real-bug-must-reference-closure-commit, dogfood-finding-real-bug-must-reference-test-coverage
- **domain-isolation** (1) — prefer-recipe-composition-over-l4-cross-import
- **domain-mode** (1) — spec-domain-mode-gates-frontend-trio
- **domain-separation** (1) — no-billing-cross-import-from-payment
- **double-entry** (1) — value-transfer-must-be-balanced
- **dry** (1) — promote-on-third-use
- **dsr** (1) — erasure-and-purge-consult-legal-hold-gate
- **dto** (6) — api-no-entity-leak, field-level-projection-authz-omits-not-nulls, lang-records-for-dtos, pii-masked-at-dto-boundary, validation-jakarta-bean-constraints, validation-mass-assignment-guard
- **dual-write** (2) — storage-reclaim-must-be-reconciled, transactional-outbox-no-dual-write
- **e-commerce** (8) — catalog-variant-resolves-unique-active-sku-and-purchasability-gated, identity-claim-on-auth-atomic-idempotent-guarded, offer-eligibility-predicate-evaluated-fail-closed-from-declared-criteria, order-cart-spine-price-snapshot-immutable-after-submit-merge-and-fulfillment-conserves, order-tax-application-skips-exempt-scope-and-recompute-converges-to-one-record, payment-split-tender-coverage-sums-to-total-and-capture-bounded-by-auth, pricing-pipeline-orders-discount-before-tax-and-total-conserves, promotion-offer-engine-conserves-determinism-atomic-cap
- **e-government** (1) — e-government-approval-gpki-approval-line
- **e-tax-invoice** (1) — electronic-tax-invoice-vat-pki-retention
- **edifact** (1) — customs-e-trade-hs-edi-aeo
- **effective-dated** (1) — temporal-validity-record-non-overlap
- **eligibility-gate** (1) — consume-path-consults-shared-fail-closed-blocking-gate
- **email-outbox** (1) — idempotency-key-on-mutations
- **embargo** (1) — consume-path-consults-shared-fail-closed-blocking-gate
- **encapsulation** (1) — lang-no-public-mutable-fields
- **endorsement-disclosure** (1) — referral-disclosure-and-fraud-controls
- **entity-graph** (1) — persistence-entity-graph
- **erasure** (1) — erasure-and-purge-consult-legal-hold-gate
- **error** (5) — error-controller-advice, error-no-stacktrace-leak, error-rfc7807-problem-detail, traceid-in-error-response, validation-error-envelope
- **error-budget** (1) — observability-slo-error-budget-convention
- **error-handling** (4) — domain-rejection-uses-rfc9457-problem-detail, error-message-not-in-native-title-attribute, server-side-stored-error-sanitize, stored-server-error-sanitize-at-render-layer
- **escalation** (1) — deadline-obligation-grounded-multi-axis-ladder-closed-loop
- **event-ingest** (1) — monotonic-ingest-reject-stale-event
- **event-sourcing** (2) — billing-event-idempotent, published-edition-immutable-copy-on-write
- **eventual-consistency** (2) — saga-compensating-transactions, transactional-outbox-no-dual-write
- **evidence-chain** (1) — recipe-invariants-must-resolve
- **exception** (1) — transaction-rollback-on-checked
- **exception-handling** (1) — async-completablefuture-return-type
- **exif** (1) — uploaded-image-metadata-stripped-on-ingest
- **existence-hiding** (1) — public-lookup-token-is-unguessable-and-enumeration-resistant
- **expiry-trigger** (1) — dogfood-finding-must-have-expiry-trigger
- **external-anchor** (1) — tamper-evident-log-external-anchor
- **fail-closed** (6) — consume-path-consults-shared-fail-closed-blocking-gate, erasure-and-purge-consult-legal-hold-gate, monetary-arithmetic-fails-closed-across-currencies-absent-explicit-recorded-conversion, negative-copresence-gate-is-set-evaluated-graded-failclosed, offer-eligibility-predicate-evaluated-fail-closed-from-declared-criteria, stored-blob-carries-content-digest-verified-on-read
- **fail-fast** (1) — config-validation-fail-fast-typed
- **fallback** (1) — resilience-circuit-breaker-retry-bulkhead
- **federation** (1) — externally-verifiable-artifact-uses-asymmetric-signature
- **field-projection** (1) — field-level-projection-authz-omits-not-nulls
- **fifo** (1) — waitlist-promotion-is-atomic-fifo
- **file-storage** (2) — presigned-url-signature-required, resumable-upload-tus-offset
- **file-upload** (1) — uploaded-image-metadata-stripped-on-ingest
- **finality** (1) — settlement-finality-dvp-novation-and-fail-ladder
- **findOrCreate** (1) — per-subject-marker-entity-idempotent
- **flyway** (3) — migration-forward-only, migration-no-baseline-on-migrate, migration-versioned-naming
- **foreign-key** (1) — destructive-remove-checks-inbound-references
- **forensic** (1) — client-must-not-fabricate-audit-timestamps
- **four-eyes** (1) — computed-decision-versioned-basis-and-four-eyes-override
- **fraud-prevention** (1) — referral-disclosure-and-fraud-controls
- **frontend-trio** (1) — spec-domain-mode-gates-frontend-trio
- **ftc** (1) — referral-disclosure-and-fraud-controls
- **fx** (1) — derived-value-pins-its-time-varying-input
- **garbage-collection** (1) — storage-reconciliation-sweeps
- **gdpr** (4) — consent-explicit-optin-withdrawable-recorded, erasure-and-purge-consult-legal-hold-gate, machine-computed-value-tracks-override-provenance, soft-delete-audit-trail
- **git** (1) — dogfood-finding-real-bug-must-reference-closure-commit
- **governance** (23) — authorization-parity-executed-matches-authorized-four-eyes-positive-gates, business-day-deadline-arithmetic-calendar-vs-business-roll-and-versioned-holidays, computed-decision-versioned-basis-and-four-eyes-override, containment-scope-authz-tree-derived-downward-cascade, dimensional-uom-conversion-compatibility-bridge-and-basis, dunning-collections-one-way-ladder-aging-and-cure, external-reconciliation-classify-dispose-resolve-and-idempotent-rerun, mandate-fanout-conserved-recall-check-battery-and-deemed-election, material-divisibility-reject-not-round-integer-vs-fractional, order-multiple-quantization-non-conserving-ceiling-to-moq, quorum-weighted-tally-with-quorum-gate-and-frozen-policy, record-linkage-banded-verdict-and-survivorship-merge, remeasurement-supersession-versioned-recompute-and-trueup, reproducible-procedure-recorded-seed-replay-and-blinding, security-token-transfer-compliance-gate, self-reported-input-plausibility-range-rate-and-unverified-provenance, sensitive-read-audit-record-before-return-mask-and-purpose, state-conditional-mutation-authority-is-a-declared-monotone-table, time-bounded-access-grant-rebac-window-and-eligibility-gate, timed-offer-exclusive-assignment-and-reoffer-ladder, two-axis-inventory-reservation-reserve-commit-release-hold, valuation-run-projection-as-of-snapshot-conserving-fan-out-and-rebase, variance-tolerance-band-asymmetric-gate-and-disposition
- **gpki** (1) — e-government-approval-gpki-approval-line
- **graceful-shutdown** (1) — health-probes-liveness-readiness-startup
- **gradle** (2) — build-java-toolchain-explicit, build-spring-boot-bom
- **grant** (1) — relationship-scoped-authz-via-grant-lookup
- **graph-traversal** (1) — provenance-dag-traversal-is-bounded-and-cycle-safe
- **hash-chain** (2) — tamper-evident-log-external-anchor, tamper-evident-log-hashchain
- **headers** (1) — security-default-headers
- **health-check** (1) — health-probes-liveness-readiness-startup
- **hibernate** (1) — soft-delete-only-on-base-entity
- **hierarchy** (1) — self-referential-tree-reparent-rejects-cycle
- **hmac** (2) — presigned-url-signature-required, webhook-hmac-required
- **hooks** (1) — hooks-before-conditional-return
- **hs-code** (1) — customs-e-trade-hs-edi-aeo
- **http** (7) — api-idempotency-key-required, cors-allowlist-and-preflight, http-content-negotiation-rfc9110, http-delete-idempotency-rfc9110, http-explicit-timeouts, http-restclient-over-resttemplate, http-shared-client-singleton
- **http-status** (1) — domain-rejection-uses-rfc9457-problem-detail
- **human-override** (1) — machine-computed-value-tracks-override-provenance
- **i18n** (1) — i18n-default-and-supported-locales-declared
- **idempotency** (12) — api-idempotency-key-required, async-job-queue-at-least-once-dlq, billing-event-idempotent, derived-key-idempotent-statement-generation, http-delete-idempotency-rfc9110, idempotency-key-on-mutations, in-doubt-outbound-call-holding-state, monotonic-ingest-reject-stale-event, order-tax-application-skips-exempt-scope-and-recompute-converges-to-one-record, per-subject-marker-entity-idempotent, storage-reclaim-must-be-reconciled, storage-reconciliation-sweeps
- **identity** (3) — identity-claim-on-auth-atomic-idempotent-guarded, korean-brn-format, no-rrn-collection-without-legal-basis
- **idor** (2) — caller-authentication-only-no-userid-param, public-lookup-token-is-unguessable-and-enumeration-resistant
- **image-processing** (1) — uploaded-image-metadata-stripped-on-ingest
- **immutability** (6) — attested-governed-edit-carries-reason-and-preimage, core-constructor-injection, immutable-record-corrected-by-reversal-not-edit, messaging-payload-record, migration-forward-only, published-edition-immutable-copy-on-write
- **import** (1) — chunked-import-required-when-rowcount-gt-1000
- **in-doubt** (1) — in-doubt-outbound-call-holding-state
- **incident-prevention** (1) — destructive-action-confirm-with-side-effects
- **incident-response** (1) — incident-dashboard-background-poll-plus-refresh
- **information-disclosure** (1) — error-no-stacktrace-leak
- **injection** (1) — query-field-allowlist-sort-filter-bound
- **input-validation** (3) — facet-count-scope-parity-and-allowlist, query-field-allowlist-sort-filter-bound, self-reported-input-plausibility-range-rate-and-unverified-provenance
- **integer-minor-units** (1) — currency-amount-precision-explicit
- **integration** (2) — chunked-import-required-when-rowcount-gt-1000, webhook-hmac-required
- **integrity** (4) — resumable-upload-tus-offset, stored-blob-carries-content-digest-verified-on-read, tamper-evident-log-external-anchor, tamper-evident-log-hashchain
- **international-trade** (1) — customs-e-trade-hs-edi-aeo
- **interval-overlap** (1) — temporal-validity-record-non-overlap
- **invariants** (1) — recipe-invariants-must-resolve
- **inventory** (1) — two-axis-inventory-reservation-reserve-commit-release-hold
- **invoicing** (1) — electronic-tax-invoice-vat-pki-retention
- **iso-4217** (2) — monetary-arithmetic-fails-closed-across-currencies-absent-explicit-recorded-conversion, payment-iso-4217-currency
- **jakarta** (1) — validation-jakarta-bean-constraints
- **jdk21** (1) — async-virtual-thread-executor
- **job-queue** (1) — async-job-queue-at-least-once-dlq
- **jpa** (7) — ordered-siblings-reorder-atomic, persistence-batch-inserts, persistence-entity-graph, persistence-no-n-plus-1, persistence-optimistic-locking, persistence-state-machine-atomic, published-edition-immutable-copy-on-write
- **junction-entity** (1) — per-subject-marker-entity-idempotent
- **jws** (1) — externally-verifiable-artifact-uses-asymmetric-signature
- **jwt** (1) — security-stateless-session-policy
- **keyboard-nav** (1) — mutation-in-flight-uses-aria-busy
- **korean-compliance** (3) — korean-brn-format, korean-vat-10-percent-calculation, no-rrn-collection-without-legal-basis
- **korean-enterprise** (2) — audit-log-pii-hash-required, server-side-stored-error-sanitize
- **kubernetes** (2) — actuator-kubernetes-probes, health-probes-liveness-readiness-startup
- **l4-layer** (3) — business-domain-must-declare-applied-recipe, multi-tenant-aop-guard-skeleton, prefer-recipe-composition-over-l4-cross-import
- **lang** (8) — lang-bigdecimal-for-measured-decimals, lang-bigdecimal-for-money, lang-no-public-mutable-fields, lang-records-for-dtos, lang-sealed-result-hierarchies, rounded-split-conserves-total-largest-remainder, time-proportional-accrual-prorates-partial-period, unit-of-measure-conversion-is-exact-and-pinned
- **largest-remainder** (1) — rounded-split-conserves-total-largest-remainder
- **leak-prevention** (1) — multi-tenant-aop-guard-skeleton
- **least-privilege** (3) — field-level-projection-authz-omits-not-nulls, rbac-stub-default-fail-closed, relationship-scoped-authz-via-grant-lookup
- **ledger** (4) — dogfood-finding-must-have-expiry-trigger, dogfood-finding-real-bug-must-reference-closure-commit, dogfood-finding-real-bug-must-reference-test-coverage, value-transfer-must-be-balanced
- **legal-hold** (1) — erasure-and-purge-consult-legal-hold-gate
- **lifecycle** (3) — catalog-variant-resolves-unique-active-sku-and-purchasability-gated, http-shared-client-singleton, order-cart-spine-price-snapshot-immutable-after-submit-merge-and-fulfillment-conserves
- **lineage** (1) — provenance-dag-traversal-is-bounded-and-cycle-safe
- **liveness** (1) — health-probes-liveness-readiness-startup
- **locale** (1) — i18n-default-and-supported-locales-declared
- **localization** (1) — i18n-default-and-supported-locales-declared
- **locked_constraint** (3) — korean-brn-format, no-rrn-collection-without-legal-basis, no-rrn-logging
- **logging** (3) — audit-log-pii-hash-required, observability-structured-logging, quality-no-system-streams
- **manufacturing** (1) — transformation-conserves-with-classified-residual
- **mapping** (1) — web-specific-mapping-methods
- **marker-entity** (1) — per-subject-marker-entity-idempotent
- **mass-assignment** (1) — validation-mass-assignment-guard
- **mass-balance** (1) — transformation-conserves-with-classified-residual
- **matching** (1) — record-linkage-banded-verdict-and-survivorship-merge
- **mdc** (1) — observability-mdc-trace-propagation
- **media-type** (1) — http-content-negotiation-rfc9110
- **messaging** (5) — chat-message-delivery-receipts-presence, messaging-payload-record, messaging-publisher-interface, messaging-topic-name-constant, transactional-outbox-no-dual-write
- **metadata** (2) — business-domain-must-declare-applied-recipe, uploaded-image-metadata-stripped-on-ingest
- **metering** (3) — cumulative-register-is-value-monotone-with-governed-reset, limit-crossing-drives-irreversible-terminal-and-blocks-derived-use, net-meter-signed-net-is-derived-from-two-monotone-direction-registers
- **metrics** (1) — domain-metrics-bounded-cardinality
- **mfa** (1) — two-factor-auth-totp-webauthn
- **micrometer** (1) — domain-metrics-bounded-cardinality
- **migration** (3) — migration-forward-only, migration-no-baseline-on-migrate, migration-versioned-naming
- **moderation** (2) — admin-cannot-rewrite-user-content, chat-message-delivery-receipts-presence
- **money** (7) — derived-value-pins-its-time-varying-input, lang-bigdecimal-for-money, monetary-arithmetic-fails-closed-across-currencies-absent-explicit-recorded-conversion, order-tax-application-skips-exempt-scope-and-recompute-converges-to-one-record, payment-split-tender-coverage-sums-to-total-and-capture-bounded-by-auth, pricing-pipeline-orders-discount-before-tax-and-total-conserves, promotion-offer-engine-conserves-determinism-atomic-cap
- **monotonic** (4) — cumulative-register-is-value-monotone-with-governed-reset, net-meter-signed-net-is-derived-from-two-monotone-direction-registers, period-close-reject-late-write, sealed-period-watermark-monotonic-close
- **multi-status** (1) — bulk-operation-partial-success-207
- **multi-tenant** (2) — multi-tenant-aop-guard-skeleton, quota-atomic-tenant-claim
- **mutation** (3) — mutation-in-flight-uses-aria-busy, mutation-skipped-outcome-surfaces-reason, optimistic-update-snapshot-rollback
- **n-plus-one** (1) — persistence-no-n-plus-1
- **netting** (1) — multilateral-netting-conserves-per-node-and-set-wide
- **no-oversell** (1) — shared-counter-claim-must-be-atomic
- **no-pii** (1) — domain-metrics-bounded-cardinality
- **non-repudiation** (2) — document-signing-pki-timestamp-revocation, tamper-evident-log-external-anchor
- **notification** (1) — idempotency-key-on-mutations
- **object-property-level-authz** (1) — field-level-projection-authz-omits-not-nulls
- **object-store** (3) — storage-reclaim-must-be-reconciled, storage-reconciliation-sweeps, stored-blob-carries-content-digest-verified-on-read
- **observability** (10) — actuator-build-info, distributed-tracing-w3c-context-propagation, domain-metrics-bounded-cardinality, health-probes-liveness-readiness-startup, no-rrn-logging, observability-mdc-trace-propagation, observability-no-pii-in-logs, observability-slo-error-budget-convention, observability-structured-logging, traceid-in-error-response
- **offboarding** (1) — owner-deprovision-reassigns-not-orphans
- **on-conflict-do-nothing** (1) — per-subject-marker-entity-idempotent
- **one-time-reveal** (1) — secret-shown-once-uses-beforeunload-guard
- **opentelemetry** (1) — distributed-tracing-w3c-context-propagation
- **opt-in** (1) — consent-explicit-optin-withdrawable-recorded
- **optimistic-locking** (2) — ordered-siblings-reorder-atomic, timeout-sweep-is-a-concurrent-mutator
- **optimistic-update** (2) — client-must-not-fabricate-audit-timestamps, optimistic-update-snapshot-rollback
- **optional** (1) — quality-optional-only-as-return
- **orchestration** (1) — saga-compensating-transactions
- **order** (1) — order-cart-spine-price-snapshot-immutable-after-submit-merge-and-fulfillment-conserves
- **ordering** (1) — ordered-siblings-reorder-atomic
- **out-of-order** (1) — monotonic-ingest-reject-stale-event
- **outcome-surfacing** (1) — mutation-skipped-outcome-surfaces-reason
- **owner-scoped** (1) — caller-authentication-only-no-userid-param
- **ownership-transfer** (2) — owner-deprovision-reassigns-not-orphans, ownership-transfer-authz-audit-atomic
- **pagination** (3) — api-pagination-pageable, facet-count-scope-parity-and-allowlist, query-field-allowlist-sort-filter-bound
- **partial-success** (1) — bulk-operation-partial-success-207
- **password-reset** (1) — password-reset-success-invalidates-token-family
- **path-injection** (1) — polymorphic-entity-ref-path-segment-guard
- **pattern-matching** (1) — lang-sealed-result-hierarchies
- **payment** (4) — idempotency-key-on-mutations, no-billing-cross-import-from-payment, payment-iso-4217-currency, payment-split-tender-coverage-sums-to-total-and-capture-bounded-by-auth
- **pci-dss** (1) — observability-no-pii-in-logs
- **percentage** (1) — lang-bigdecimal-for-measured-decimals
- **performance** (5) — chunked-import-required-when-rowcount-gt-1000, http-shared-client-singleton, persistence-batch-inserts, retention-delete-on-high-volume-table-must-be-bounded, transaction-readonly-queries
- **period-close** (2) — period-close-reject-late-write, sealed-period-watermark-monotonic-close
- **persistence** (10) — api-no-entity-leak, persistence-batch-inserts, persistence-entity-graph, persistence-no-n-plus-1, persistence-optimistic-locking, persistence-state-machine-atomic, retention-delete-on-high-volume-table-must-be-bounded, soft-delete-only-on-base-entity, testing-archunit-repository-shape, timeout-sweep-is-a-concurrent-mutator
- **pii** (8) — audit-log-pii-hash-required, no-rrn-collection-without-legal-basis, no-rrn-logging, observability-no-pii-in-logs, sensitive-read-audit-record-before-return-mask-and-purpose, server-side-stored-error-sanitize, stored-server-error-sanitize-at-render-layer, uploaded-image-metadata-stripped-on-ingest
- **pii-minimization** (1) — public-lookup-token-is-unguessable-and-enumeration-resistant
- **pii-side-channel** (1) — error-message-not-in-native-title-attribute
- **pki** (2) — document-signing-pki-timestamp-revocation, e-government-approval-gpki-approval-line
- **pki-signature** (1) — electronic-tax-invoice-vat-pki-retention
- **point-in-time** (1) — temporal-as-of-point-in-time-query
- **polymorphic-entity** (1) — polymorphic-entity-ref-path-segment-guard
- **portability** (1) — testing-restassured-blackbox
- **postgres-exclude** (1) — temporal-validity-record-non-overlap
- **postgresql** (2) — provenance-dag-append-only-acyclic-rollup, temporal-as-of-point-in-time-query
- **precision** (5) — currency-amount-precision-explicit, lang-bigdecimal-for-measured-decimals, lang-bigdecimal-for-money, rounded-split-conserves-total-largest-remainder, unit-of-measure-conversion-is-exact-and-pinned
- **preflight** (1) — cors-allowlist-and-preflight
- **presence** (1) — chat-message-delivery-receipts-presence
- **presigned-url** (1) — presigned-url-signature-required
- **pricing** (3) — banded-pricing-segments-qty-per-band-and-conserves, composite-bundle-price-is-conserving-rollup-of-children, pricing-pipeline-orders-discount-before-tax-and-total-conserves
- **privacy** (6) — consent-explicit-optin-withdrawable-recorded, no-rrn-collection-without-legal-basis, no-rrn-logging, pii-masked-at-dto-boundary, referral-disclosure-and-fraud-controls, uploaded-image-metadata-stripped-on-ingest
- **probes** (1) — actuator-kubernetes-probes
- **problem-details** (1) — domain-rejection-uses-rfc9457-problem-detail
- **production-safety** (1) — migration-no-baseline-on-migrate
- **profiles** (1) — config-profile-isolation
- **promotion** (2) — offer-eligibility-predicate-evaluated-fail-closed-from-declared-criteria, promotion-offer-engine-conserves-determinism-atomic-cap
- **propagation** (2) — distributed-tracing-w3c-context-propagation, transaction-propagation-requires-new
- **proration** (1) — time-proportional-accrual-prorates-partial-period
- **protocol** (1) — realtime-single-protocol-declared
- **provenance** (5) — attested-governed-edit-carries-reason-and-preimage, derived-value-pins-its-time-varying-input, machine-computed-value-tracks-override-provenance, provenance-dag-append-only-acyclic-rollup, provenance-dag-traversal-is-bounded-and-cycle-safe
- **proxy** (1) — core-aop-proxy-no-final
- **purpose-limitation** (1) — consent-explicit-optin-withdrawable-recorded
- **quality** (3) — quality-no-system-streams, quality-optional-only-as-return, quality-utility-class-shape
- **quality-hold** (1) — consume-path-consults-shared-fail-closed-blocking-gate
- **quorum** (1) — quorum-weighted-tally-with-quorum-gate-and-frozen-policy
- **quota** (2) — denormalized-counter-reconcilable, quota-atomic-tenant-claim
- **race-condition** (2) — ordered-siblings-reorder-atomic, shared-counter-claim-must-be-atomic
- **range-types** (1) — temporal-as-of-point-in-time-query
- **rbac** (3) — bfla-privileged-endpoint-authz-presence, rbac-stub-default-fail-closed, role-hierarchy-subsumes-lower-tiers
- **re-derivability** (1) — derived-value-pins-its-time-varying-input
- **react** (1) — hooks-before-conditional-return
- **readiness** (1) — health-probes-liveness-readiness-startup
- **real-bug** (2) — dogfood-finding-real-bug-must-reference-closure-commit, dogfood-finding-real-bug-must-reference-test-coverage
- **realtime** (2) — chat-message-delivery-receipts-presence, realtime-single-protocol-declared
- **rebac** (1) — relationship-scoped-authz-via-grant-lookup
- **recipe-composition** (3) — business-domain-must-declare-applied-recipe, prefer-recipe-composition-over-l4-cross-import, recipe-invariants-must-resolve
- **reconciliation** (4) — denormalized-counter-reconcilable, in-doubt-outbound-call-holding-state, storage-reclaim-must-be-reconciled, storage-reconciliation-sweeps
- **records** (2) — lang-records-for-dtos, messaging-payload-record
- **recursive-cte** (2) — provenance-dag-append-only-acyclic-rollup, provenance-dag-traversal-is-bounded-and-cycle-safe
- **redis** (1) — cacheable-requires-explicit-ttl
- **refactor-discipline** (1) — promote-on-third-use
- **referential-integrity** (3) — destructive-remove-checks-inbound-references, recipe-invariants-must-resolve, self-referential-tree-reparent-rejects-cycle
- **referral** (1) — referral-disclosure-and-fraud-controls
- **regression-test** (1) — dogfood-finding-real-bug-must-reference-test-coverage
- **rejection** (1) — domain-rejection-uses-rfc9457-problem-detail
- **reliability** (4) — async-job-queue-at-least-once-dlq, http-explicit-timeouts, saga-compensating-transactions, transactional-outbox-no-dual-write
- **render-correctness** (1) — hooks-before-conditional-return
- **reparent** (1) — self-referential-tree-reparent-rejects-cycle
- **replay-resistance** (1) — password-reset-success-invalidates-token-family
- **reproducibility** (2) — build-no-snapshot-dependencies, derived-value-pins-its-time-varying-input
- **reservation** (1) — balance-reservation-is-two-phase-and-conserving
- **resilience** (1) — resilience-circuit-breaker-retry-bulkhead
- **resource-consumption** (1) — quota-atomic-tenant-claim
- **rest-assured** (1) — testing-restassured-blackbox
- **rest-client** (1) — http-restclient-over-resttemplate
- **resumable** (1) — resumable-upload-tus-offset
- **retention** (3) — e-government-approval-gpki-approval-line, electronic-tax-invoice-vat-pki-retention, retention-delete-on-high-volume-table-must-be-bounded
- **retry** (1) — resilience-circuit-breaker-retry-bulkhead
- **retry-safety** (5) — api-idempotency-key-required, http-delete-idempotency-rfc9110, idempotency-key-on-mutations, in-doubt-outbound-call-holding-state, per-subject-marker-entity-idempotent
- **reversal** (1) — immutable-record-corrected-by-reversal-not-edit
- **revocation** (1) — document-signing-pki-timestamp-revocation
- **rfc-7807** (3) — error-rfc7807-problem-detail, traceid-in-error-response, validation-error-envelope
- **rfc-9110** (1) — http-content-negotiation-rfc9110
- **rfc-9457** (2) — bulk-operation-partial-success-207, domain-rejection-uses-rfc9457-problem-detail
- **role-hierarchy** (1) — role-hierarchy-subsumes-lower-tiers
- **rollback** (1) — transaction-rollback-on-checked
- **rollup** (1) — provenance-dag-append-only-acyclic-rollup
- **row-lock** (1) — waitlist-promotion-is-atomic-fifo
- **rrn** (2) — no-rrn-collection-without-legal-basis, no-rrn-logging
- **rule-of-three** (1) — promote-on-third-use
- **rules-of-hooks** (1) — hooks-before-conditional-return
- **safety** (1) — negative-copresence-gate-is-set-evaluated-graded-failclosed
- **saga** (1) — saga-compensating-transactions
- **same-origin-policy** (1) — cors-allowlist-and-preflight
- **scheduled** (1) — async-scheduled-fixed-delay-vs-fixed-rate
- **scheduled-task** (1) — storage-reconciliation-sweeps
- **scheduling** (3) — completion-reset-recurring-interval, deadline-obligation-grounded-multi-axis-ladder-closed-loop, timeout-sweep-is-a-concurrent-mutator
- **scope-deferral** (1) — dogfood-finding-must-have-expiry-trigger
- **scope-discipline** (1) — spec-domain-mode-gates-frontend-trio
- **screen-share-leak** (1) — stored-server-error-sanitize-at-render-layer
- **sealed** (1) — lang-sealed-result-hierarchies
- **sealed-period** (2) — period-close-reject-late-write, sealed-period-watermark-monotonic-close
- **secret** (1) — secret-shown-once-uses-beforeunload-guard
- **secrets** (2) — config-no-secret-in-yaml, config-validation-fail-fast-typed
- **securities** (1) — security-token-transfer-compliance-gate
- **security** (19) — actuator-restrict-exposure, cache-not-on-controllers, cacheable-requires-explicit-ttl, config-no-secret-in-yaml, cors-allowlist-and-preflight, error-no-stacktrace-leak, externally-verifiable-artifact-uses-asymmetric-signature, facet-count-scope-parity-and-allowlist, observability-no-pii-in-logs, presigned-url-signature-required, query-field-allowlist-sort-filter-bound, security-csrf-scoped-disable, security-default-headers, security-stateless-session-policy, sensitive-read-audit-record-before-return-mask-and-purpose, time-gated-decisions-read-injected-clock, two-factor-auth-totp-webauthn, validation-mass-assignment-guard, webhook-hmac-required
- **self-referential** (1) — self-referential-tree-reparent-rejects-cycle
- **server-skip** (1) — mutation-skipped-outcome-surfaces-reason
- **session** (1) — security-stateless-session-policy
- **settlement** (2) — multilateral-netting-conserves-per-node-and-set-wide, payment-split-tender-coverage-sums-to-total-and-capture-bounded-by-auth
- **sha-256** (1) — stored-blob-carries-content-digest-verified-on-read
- **shared-utility** (1) — promote-on-third-use
- **signing** (1) — externally-verifiable-artifact-uses-asymmetric-signature
- **skeleton** (1) — multi-tenant-aop-guard-skeleton
- **slo** (1) — observability-slo-error-budget-convention
- **soft-delete** (4) — destructive-remove-checks-inbound-references, erasure-and-purge-consult-legal-hold-gate, soft-delete-audit-trail, soft-delete-only-on-base-entity
- **spec-discipline** (1) — spec-domain-mode-gates-frontend-trio
- **spec-trio** (1) — recipe-invariants-must-resolve
- **spoliation** (1) — erasure-and-purge-consult-legal-hold-gate
- **spring** (2) — async-completablefuture-return-type, http-restclient-over-resttemplate
- **spring-cache** (1) — cache-explicit-name-key-sync
- **spring-mvc** (2) — error-controller-advice, web-rest-controller-annotation
- **spring-proxy** (1) — transaction-no-self-invocation
- **spring-security** (1) — role-hierarchy-subsumes-lower-tiers
- **sre** (2) — incident-dashboard-background-poll-plus-refresh, observability-slo-error-budget-convention
- **sse** (1) — realtime-single-protocol-declared
- **startup** (1) — config-validation-fail-fast-typed
- **state-machine** (30) — authorization-parity-executed-matches-authorized-four-eyes-positive-gates, balance-reservation-is-two-phase-and-conserving, business-day-deadline-arithmetic-calendar-vs-business-roll-and-versioned-holidays, catalog-variant-resolves-unique-active-sku-and-purchasability-gated, completion-reset-recurring-interval, computed-decision-versioned-basis-and-four-eyes-override, cumulative-register-is-value-monotone-with-governed-reset, deadline-obligation-grounded-multi-axis-ladder-closed-loop, dimensional-uom-conversion-compatibility-bridge-and-basis, dunning-collections-one-way-ladder-aging-and-cure, external-reconciliation-classify-dispose-resolve-and-idempotent-rerun, limit-crossing-drives-irreversible-terminal-and-blocks-derived-use, mandate-fanout-conserved-recall-check-battery-and-deemed-election, negative-copresence-gate-is-set-evaluated-graded-failclosed, net-meter-signed-net-is-derived-from-two-monotone-direction-registers, order-cart-spine-price-snapshot-immutable-after-submit-merge-and-fulfillment-conserves, persistence-state-machine-atomic, quorum-weighted-tally-with-quorum-gate-and-frozen-policy, record-linkage-banded-verdict-and-survivorship-merge, remeasurement-supersession-versioned-recompute-and-trueup, security-token-transfer-compliance-gate, sensitive-read-audit-record-before-return-mask-and-purpose, settlement-finality-dvp-novation-and-fail-ladder, state-conditional-mutation-authority-is-a-declared-monotone-table, subscription-state-machine-explicit, timed-offer-exclusive-assignment-and-reoffer-ladder, two-axis-inventory-reservation-reserve-commit-release-hold, valuation-run-projection-as-of-snapshot-conserving-fan-out-and-rebase, variance-tolerance-band-asymmetric-gate-and-disposition, waitlist-promotion-is-atomic-fifo
- **state-projection** (1) — monotonic-ingest-reject-stale-event
- **storage** (4) — server-side-stored-error-sanitize, storage-reclaim-must-be-reconciled, storage-reconciliation-sweeps, stored-blob-carries-content-digest-verified-on-read
- **structured-logs** (1) — observability-structured-logging
- **subscription** (1) — subscription-state-machine-explicit
- **tamper-evidence** (1) — tamper-evident-log-hashchain
- **tamper-evident-log** (1) — tamper-evident-log-external-anchor
- **tanstack-query** (3) — background-poll-must-show-refresh-state, incident-dashboard-background-poll-plus-refresh, optimistic-update-snapshot-rollback
- **tax** (4) — derived-value-pins-its-time-varying-input, korean-vat-10-percent-calculation, order-tax-application-skips-exempt-scope-and-recompute-converges-to-one-record, pricing-pipeline-orders-discount-before-tax-and-total-conserves
- **technical-debt** (1) — dogfood-finding-must-have-expiry-trigger
- **temporal** (3) — completion-reset-recurring-interval, temporal-as-of-point-in-time-query, temporal-validity-record-non-overlap
- **test-coverage** (1) — dogfood-finding-real-bug-must-reference-test-coverage
- **testing** (4) — testing-archunit-layer-boundary, testing-archunit-no-cyclic-packages, testing-archunit-repository-shape, testing-restassured-blackbox
- **thread-safety** (1) — core-singleton-no-mutable-state
- **time** (2) — time-gated-decisions-read-injected-clock, time-proportional-accrual-prorates-partial-period
- **time-bound** (1) — time-bounded-access-grant-rebac-window-and-eligibility-gate
- **timeout** (2) — http-explicit-timeouts, resilience-circuit-breaker-retry-bulkhead
- **timestamp** (2) — client-must-not-fabricate-audit-timestamps, document-signing-pki-timestamp-revocation
- **toctou** (2) — quota-atomic-tenant-claim, temporal-validity-record-non-overlap
- **token-invalidation** (1) — password-reset-success-invalidates-token-family
- **toolchain** (1) — build-java-toolchain-explicit
- **totp** (1) — two-factor-auth-totp-webauthn
- **tracing** (2) — observability-mdc-trace-propagation, traceid-in-error-response
- **transaction** (5) — chunked-import-required-when-rowcount-gt-1000, transaction-no-self-invocation, transaction-propagation-requires-new, transaction-readonly-queries, transaction-rollback-on-checked
- **transactional** (1) — value-transfer-must-be-balanced
- **transactional-outbox** (1) — transactional-outbox-no-dual-write
- **tree** (1) — self-referential-tree-reparent-rejects-cycle
- **trust** (1) — admin-cannot-rewrite-user-content
- **ttl** (2) — cache-caffeine-expiration, cacheable-requires-explicit-ttl
- **tus** (1) — resumable-upload-tus-offset
- **twelve-factor** (1) — config-validation-fail-fast-typed
- **unique-constraint** (1) — per-subject-marker-entity-idempotent
- **unit-of-measure** (1) — unit-of-measure-conversion-is-exact-and-pinned
- **upload** (1) — resumable-upload-tus-offset
- **utility-class** (1) — quality-utility-class-shape
- **validation** (10) — dimensional-uom-conversion-compatibility-bridge-and-basis, korean-brn-format, material-divisibility-reject-not-round-integer-vs-fractional, order-multiple-quantization-non-conserving-ceiling-to-moq, payment-iso-4217-currency, validation-custom-constraint, validation-error-envelope, validation-jakarta-bean-constraints, validation-mass-assignment-guard, variance-tolerance-band-asymmetric-gate-and-disposition
- **value-conservation** (2) — immutable-record-corrected-by-reversal-not-edit, period-close-reject-late-write
- **value-integrity** (1) — value-transfer-must-be-balanced
- **vat** (2) — electronic-tax-invoice-vat-pki-retention, korean-vat-10-percent-calculation
- **versioning** (2) — api-versioning-uri-prefix, computed-decision-versioned-basis-and-four-eyes-override
- **visibility-timeout** (1) — async-job-queue-at-least-once-dlq
- **voting** (1) — quorum-weighted-tally-with-quorum-gate-and-frozen-policy
- **w3c-trace-context** (1) — distributed-tracing-w3c-context-propagation
- **waitlist** (1) — waitlist-promotion-is-atomic-fifo
- **watermark** (3) — monotonic-ingest-reject-stale-event, period-close-reject-late-write, sealed-period-watermark-monotonic-close
- **wcag** (1) — background-poll-must-show-refresh-state
- **web** (3) — web-explicit-produces, web-rest-controller-annotation, web-specific-mapping-methods
- **webauthn** (1) — two-factor-auth-totp-webauthn
- **webhook** (3) — billing-event-idempotent, secret-shown-once-uses-beforeunload-guard, webhook-hmac-required
- **websocket** (1) — realtime-single-protocol-declared
- **x509** (1) — document-signing-pki-timestamp-revocation
- **개인정보보호법** (1) — audit-log-pii-hash-required

## Rules
| id | impact | verification | title |
|---|---|---|---|
| accumulator-consume-is-atomic-non-rejecting | HIGH | review | A consume against a drawdown threshold (deductible / copay / budget / data-cap) must be ONE atomic non-rejecting partial draw — applied = min(delta, headroom), advance by applied, return the residual — never a read-then-write, never a total refusal |
| actuator-build-info | MEDIUM | gradle:testPractices | Enable Spring Boot buildInfo() and surface it via /actuator/info |
| actuator-kubernetes-probes | HIGH | gradle:testPractices | Expose /actuator/health/liveness + /actuator/health/readiness |
| actuator-restrict-exposure | HIGH | gradle:testPractices | management.endpoints.web.exposure.include must be an explicit allow-list |
| admin-cannot-rewrite-user-content | HIGH | gradle:testCommentThread | ROLE_ADMIN may MODERATE (delete) but MUST NOT rewrite user-authored content |
| api-idempotency-key-required | HIGH | gradle:testPayment | POST endpoints with non-idempotent side effects must require an Idempotency-Key header |
| api-no-entity-leak | HIGH | gradle:testPractices | Return DTO records from controllers, never JPA entities |
| api-pagination-pageable | HIGH | gradle:testPractices | List endpoints must use Pageable and clamp size |
| api-versioning-uri-prefix | MEDIUM | gradle:testPractices | Include a /v{N}/ segment in every public API URI |
| async-completablefuture-return-type | HIGH | gradle:testPractices | @Async methods must return CompletableFuture, never void |
| async-job-queue-at-least-once-dlq | HIGH | review | An async job queue MUST be at-least-once with explicit ack + visibility timeout, bounded jittered retry, and a dead-letter queue — idempotent enqueue and workers |
| async-scheduled-fixed-delay-vs-fixed-rate | MEDIUM | gradle:testPractices | Use fixedDelay for variable-duration tasks; reserve fixedRate for instant heartbeats |
| async-virtual-thread-executor | HIGH | gradle:testPractices | Use JDK 21 virtual threads for blocking-IO workloads |
| attested-governed-edit-carries-reason-and-preimage | HIGH | review | An edit to governed data must be an attested change — atomically recording who / when (injected clock) / old → new / a mandatory non-blank reason at the sole mutator, appended to a per-field history that never obscures a prior value |
| audit-log-pii-hash-required | HIGH | review | AUDIT log lines MUST hash PII identifiers — never write raw email / phone / RRN to log aggregators |
| authorization-parity-executed-matches-authorized-four-eyes-positive-gates | HIGH | review | An authorized action must bind its EXECUTION to the approved envelope by canonical parity hash (executed-matches-authorized), require TWO distinct human signoffs separated from the requester on the high-value path (four-eyes / NIST two-person rule), and refuse execution until every declared mandatory companion gate is recorded present (positive-gates) — all under the action's row lock so it executes exactly once |
| background-poll-must-show-refresh-state | HIGH | guard:background_poll_refresh_state_guard.sh | Background-polled pages MUST expose dataUpdatedAt + aria-busy on mutations |
| balance-reservation-is-two-phase-and-conserving | HIGH | review | A claim against a fungible pooled balance must be TWO-PHASE — an over-reserve-safe RESERVE that places a reversible hold (reserved term, available = funded − committed − reserved), then a SETTLE that commits actual ≤ reserved AND returns the unused remainder in one transaction — never a single-phase commit, never a settle that can exceed its hold |
| banded-pricing-segments-qty-per-band-and-conserves | HIGH | review | Tiered / marginal / time-of-use pricing must segment the quantity across half-open bands and charge each portion at its own band's rate (charge = Σ qty-in-band × rate) — the bands tile [0,∞) with no gap/overlap, every unit is charged exactly once, and the total is rounded ONCE — never a single blended rate, never a sum of independently-rounded per-band charges |
| bfla-privileged-endpoint-authz-presence | HIGH | guard:admin_preauthorize_guard.sh | Every privileged/admin mapped endpoint MUST carry a class-level or method-level authorization annotation |
| billing-event-idempotent | CRITICAL | review | All BillingEvent writes must carry a unique idempotencyKey; duplicate provider events must be rejected without creating a second row |
| build-java-toolchain-explicit | MEDIUM | gradle:testPractices | Declare an explicit Java toolchain in build.gradle.kts |
| build-no-snapshot-dependencies | HIGH | gradle:testPractices | Production builds must not depend on -SNAPSHOT artifacts |
| build-spring-boot-bom | HIGH | gradle:testPractices | Apply the Spring Boot dependency-management plugin (BOM pinning) |
| bulk-operation-partial-success-207 | HIGH | review | A bulk endpoint MUST report per-item partial success (207-style), declare its atomicity mode, cap batch size, and pre-validate before mutating |
| business-day-deadline-arithmetic-calendar-vs-business-roll-and-versioned-holidays | HIGH | review | A statutory/regulatory deadline computed by CALENDAR-vs-BUSINESS-day arithmetic must RECORD its full reconstructible basis (start date, N, mode, holiday-calendar id + version, raw date, roll convention, adjusted date — never a bare stored date), skip weekends + the configured holiday set in BUSINESS mode (CALENDAR counts every day), apply and RECORD a roll convention off a non-business day, RECOMPUTE 'overdue' on read (never a stored boolean), and pin the holiday calendar as a VERSIONED input so a later edit does not silently move an already-computed deadline |
| business-domain-must-declare-applied-recipe | HIGH | review | Every L4 domain README that participates in a Business Pattern Recipe composition must declare applied_recipe: <pattern-name> in its frontmatter metadata block |
| cache-caffeine-expiration | HIGH | gradle:testPractices | Caffeine cache must declare explicit expireAfterWrite and maximumSize |
| cache-explicit-name-key-sync | HIGH | gradle:testPractices | @Cacheable must declare value, key, and sync=true explicitly |
| cache-not-on-controllers | HIGH | gradle:testPractices | @Cacheable / @CachePut / @CacheEvict are forbidden on @RestController classes |
| cacheable-requires-explicit-ttl | HIGH | review | @Cacheable caches must have explicit TTL configured on the CacheManager |
| caller-authentication-only-no-userid-param | HIGH | gradle:testFavorites | Caller identity derives from Authentication only — never accept userId via path or query |
| catalog-variant-resolves-unique-active-sku-and-purchasability-gated | HIGH | review | A variant-product catalog must give every product exactly one default SKU, resolve a chosen option-value set to EXACTLY ONE active SKU (a duplicate sku-generating signature unrepresentable via UNIQUE(product_id, option_signature) — never an arbitrary iterator().next() pick), gate purchasability on the active-date window AND archival at the cart path (not only at display), and require a sellable SKU to resolve a non-null price at the catalog boundary |
| chat-message-delivery-receipts-presence | MEDIUM | review | A chat system MUST carry a typed message envelope with delivery/read receipts, room lifecycle, presence, paginated offline-catchup history, and moderation |
| chunked-import-required-when-rowcount-gt-1000 | HIGH | gradle:testIntegration | CSV and Excel imports with potentially >1000 rows must use chunked streaming with per-chunk transactions |
| client-must-not-fabricate-audit-timestamps | HIGH | review | Client must NOT fabricate audit timestamps — server is the source of truth |
| completion-reset-recurring-interval | HIGH | review | A recurring obligation whose interval RESETS ON COMPLETION must advance its window FROM the completion instant (not on a fixed calendar grid), carry at most one append-only occurrence per window (exactly-once 409), recompute due/overdue from the clock rather than a stored boolean, let a @Lazy-self @Scheduled sweep record only a non-authoritative overdue flag (never auto-complete), and serialize concurrent completes on the row lock so exactly one advances |
| composite-bundle-price-is-conserving-rollup-of-children | MEDIUM | review | A composite item (bundle / kit) priced as a CONSERVING roll-up of its children — in ITEM_SUM mode its price is Σ over children of (child.unitPrice × child.quantity) + Σ(bundle fees), for retail / sale / taxable (sale falling back to a child's retail when the child has no sale price); in BUNDLE mode its price is a FIXED base price NOT summed from children — with taxability DERIVED from the children and NO independently-settable rolled-up total column, so a non-conserving composite total is unrepresentable (the COMPOSITION direction, the dual of banded/promotion decomposition) |
| computed-decision-versioned-basis-and-four-eyes-override | HIGH | review | A computed decision (quote / rate / score / eligibility) must snapshot its appraisal-sufficient basis immutably, re-determine only by appending a reasoned NEW version (never overwrite), and gate a manual override behind a justification plus a four-eyes approver distinct from the requester — DB-backstopped via @Check (approved_by <> decided_by) |
| config-no-secret-in-yaml | HIGH | gradle:testPractices | Never hardcode secrets in application.yml; use ${ENV[:default]} |
| config-profile-isolation | MEDIUM | gradle:testPractices | Move profile-specific config out of application.yml into application-{profile}.yml |
| config-typed-properties | HIGH | gradle:testPractices | Bind config through @ConfigurationProperties records, not @Value |
| config-validation-fail-fast-typed | HIGH | review | Configuration MUST be typed, validated, and fail-fast at startup — separated from code, immutable after boot |
| consent-explicit-optin-withdrawable-recorded | HIGH | review | Consent MUST be an explicit affirmative opt-in, withdrawable as easily as given, recorded with proof, and purpose-scoped |
| consume-path-consults-shared-fail-closed-blocking-gate | HIGH | review | Consuming a referenced entity MUST consult one shared fail-closed blocking-status gate, re-read in-transaction |
| containment-scope-authz-tree-derived-downward-cascade | HIGH | review | Hierarchical containment-scope authorization must model org units as a TREE with a materialized ancestor path, derive the DOWNWARD-ONLY containment cascade from that path at decision time (a grant at a node authorizes that node and its whole subtree — never its siblings or ancestors, never a leaf grant cascading upward), return 403 OUT_OF_SCOPE when no satisfying grant is held at the target node or any ancestor, and keep grants immutable + idempotent with concurrent same-key grants serialized on the node row |
| core-aop-proxy-no-final | HIGH | gradle:testPractices | Do not mark proxied beans (or their public methods) as final |
| core-constructor-injection | HIGH | gradle:testPractices | Use constructor injection with final fields |
| core-singleton-no-mutable-state | HIGH | gradle:testPractices | Singleton beans must not carry unsynchronized mutable state |
| cors-allowlist-and-preflight | HIGH | review | CORS MUST be an explicit origin allowlist with correct preflight + credentials policy — never a reflected-origin wildcard |
| cumulative-register-is-value-monotone-with-governed-reset | HIGH | review | A cumulative register (meter / odometer / counter) must be VALUE-monotone — an appended read is ≥ the anchor and consumption is delta = curr − prior computed under the row lock — and a decrease is rejected (422) unless it is a governed ROLLOVER (wrapped-delta) or EXCHANGE (baseline reset); never a silent negative delta |
| currency-amount-precision-explicit | CRITICAL | gradle:testBilling | All monetary amounts in billing domain must be stored as long integer minor units; float, double, and BigDecimal representations are prohibited |
| customs-e-trade-hs-edi-aeo | HIGH | review | Customs e-trade MUST classify by WCO HS code, exchange UN/EDIFACT messages over authenticated certs, honor AEO status, and retain declarations for the statutory period |
| deadline-obligation-grounded-multi-axis-ladder-closed-loop | HIGH | review | A governed deadline obligation must derive its deadline from a recorded anchor+rule (never a free-typed date), take the EARLIEST candidate when multiple axes govern it, fire ordered escalation rungs exactly once as appended additive events, and reach its terminal ONLY through an explicit who/when acknowledgment — the sweep never auto-expires it |
| default-member-singleton-exactly-one-default-clear-then-set | MEDIUM | review | A parent-scoped child collection that elects one preferred member MUST keep AT MOST ONE default — setting a member default atomically clears every other member's default in the same transaction (clear-all-then-set-one), the empty→first-member transition auto-defaults the sole member, and a partial unique index backstops the invariant so a torn or concurrent write can never leave two defaults |
| denormalized-counter-reconcilable | MEDIUM | review | A denormalized usage counter MUST be reconcilable against its source rows — recompute, detect drift, repair, and decrement on release |
| derived-aggregate-consistency-recompute-eligibility-empty | MEDIUM | review | A denormalized derived aggregate (an average / count / sum cached on a parent from its child rows) MUST be recomputed from its CURRENT source rows on every change (never hand-edited, never drifting), computed over a DECLARED eligibility predicate (so an unapproved / soft-deleted row cannot silently move it), with a DEFINED empty-set sentinel (never a divide-by-zero) — a denormalized MEAN is a quotient, distinct from the catalog's SUM-conservation family |
| derived-key-idempotent-statement-generation | HIGH | review | A generated statement's identity MUST be a deterministic content hash of (subject, period, basis) — never a client-supplied idempotency header — so an identical regeneration returns the SAME row and a changed basis appends a new version, and the statement's columns stay immutable once written |
| derived-value-pins-its-time-varying-input | HIGH | review | A value derived from a time-varying input MUST pin that input for re-derivability |
| destructive-action-confirm-with-side-effects | HIGH | review | Destructive admin actions MUST confirm with explicit side-effect enumeration |
| destructive-remove-checks-inbound-references | HIGH | review | Destructive remove of a structural entity MUST count live inbound references first — never silently orphan dependents |
| dimensional-uom-conversion-compatibility-bridge-and-basis | HIGH | review | A cross-dimension unit conversion must enforce a DIMENSIONAL-COMPATIBILITY precondition (same-dimension ⇒ pure ratio; cross-dimension ⇒ a recorded versioned bridging material property, else 422 INCOMPATIBLE_DIMENSIONS — never a silent wrong number), record its full reconstructible basis (from-quantity/unit, to-unit, dimension verdict, factor, material version, result), and be deterministic BigDecimal arithmetic at a recorded scale |
| distributed-tracing-w3c-context-propagation | MEDIUM | review | Distributed tracing MUST propagate W3C Trace Context across services — traceparent in, span out, trace_id in logs |
| document-signing-pki-timestamp-revocation | HIGH | review | A signed document MUST use a declared standard signature format over a verified PKI chain, with a trusted timestamp, revocation-checked verification, and long-term retention |
| dogfood-finding-must-have-expiry-trigger | MEDIUM | guard:dogfood_finding_expiry_trigger_guard.sh | Dogfood-ledger scope_deferral findings MUST include an explicit expiry trigger |
| dogfood-finding-real-bug-must-reference-closure-commit | MEDIUM | guard:dogfood_finding_real_bug_closure_commit_guard.sh | Dogfood-ledger real_bug findings MUST reference closure_commit_sha |
| dogfood-finding-real-bug-must-reference-test-coverage | MEDIUM | guard:dogfood_finding_real_bug_test_coverage_guard.sh | Dogfood-ledger real_bug findings MUST reference regression-test coverage |
| domain-metrics-bounded-cardinality | HIGH | review | A domain operation's metrics MUST use bounded-cardinality labels — fixed enums only, never ids / PII / unbounded values |
| domain-rejection-uses-rfc9457-problem-detail | MEDIUM | review | A refused domain operation MUST return its declared RFC 9457 problem type with the correct status, no partial side effect, and no misleading Retry-After |
| dunning-collections-one-way-ladder-aging-and-cure | HIGH | review | An overdue-receivable collections lifecycle must walk a ONE-WAY dunning ladder with EXACTLY-ONCE stage transitions (a uq(case,stage) DB backstop, never skip or reverse), compute its aging bucket DETERMINISTICALLY from days-overdue at a RECORDED as-of instant (never a bare label), open a cure window on payment that resets to CURRENT and HALTS the ladder on full cure / resumes it on lapse, and serialize concurrent advances on the case row so exactly one wins |
| e-government-approval-gpki-approval-line | HIGH | review | Electronic government approval MUST be GPKI-signed with a sequential approval line, post-approval and security-grade escalation, and long-term tamper-evident retention |
| electronic-tax-invoice-vat-pki-retention | HIGH | review | An electronic tax invoice MUST use the standard format, separate identifier PII, balance VAT, transmit to the authority, be PKI-signed and timestamped, and retain for the statutory period |
| erasure-and-purge-consult-legal-hold-gate | HIGH | review | Erasure AND soft-delete purge MUST consult a fail-closed legal-hold gate before deleting |
| error-controller-advice | HIGH | gradle:testPractices | Translate exceptions through a centralised @RestControllerAdvice |
| error-message-not-in-native-title-attribute | MEDIUM | review | Mutation error messages MUST NOT render in the native `title` tooltip |
| error-no-stacktrace-leak | HIGH | gradle:testPractices | Error responses must not leak stack-trace or exception class names |
| error-rfc7807-problem-detail | HIGH | gradle:testPractices | Error bodies must follow RFC 7807 application/problem+json |
| external-reconciliation-classify-dispose-resolve-and-idempotent-rerun | HIGH | review | An external-feed reconciliation must CLASSIFY each internal/external pair EXACTLY ONCE with its recorded basis (internal value, external value, delta — never a bare aggregate count), require EXPLICIT human DISPOSITION of every BREAK (who/when/reason) before the run can be RESOLVED (an undisposed break is 422), be IDEMPOTENT on the feed snapshot hash (same feed → same run, changed feed → new run, prior retained), and serialize concurrent disposes on one break so exactly one wins |
| externally-verifiable-artifact-uses-asymmetric-signature | HIGH | review | Artifacts a third party must verify MUST use a detached asymmetric signature — never HMAC |
| facet-count-scope-parity-and-allowlist | HIGH | review | A facet-count aggregation MUST be computed over the IDENTICAL authorization/filter scope as the list query it accompanies, and the field a caller may facet on MUST be a compile-time allowlist — a non-allowlisted field is rejected by NAME with 422, fail-closed, before any aggregation query runs |
| field-level-projection-authz-omits-not-nulls | HIGH | review | Field-level projection MUST be server-decided per caller and OMIT unauthorized fields — never load-then-null |
| health-probes-liveness-readiness-startup | HIGH | review | A service MUST expose distinct liveness, readiness, and startup health endpoints — and fail readiness before shutdown drain |
| hooks-before-conditional-return | HIGH | review | React hooks MUST be called before any conditional early return — Rules of Hooks |
| http-content-negotiation-rfc9110 | MEDIUM | review | An API serving multiple representations MUST do proactive content negotiation — rank Accept, validate Content-Type with 415, answer 406 when nothing matches |
| http-delete-idempotency-rfc9110 | MEDIUM | gradle:testFavorites | DELETE endpoints MUST be idempotent — second call on absent target returns 204, not 404 |
| http-explicit-timeouts | HIGH | gradle:testPractices | Every HTTP client must declare finite connect + read timeouts |
| http-restclient-over-resttemplate | MEDIUM | gradle:testPractices | Use RestClient for outbound HTTP, not RestTemplate |
| http-shared-client-singleton | HIGH | gradle:testPractices | Declare HTTP clients as @Bean singletons, never per-call |
| i18n-default-and-supported-locales-declared | MEDIUM | review | A multilingual recipe MUST declare its default and supported locales as BCP 47 tags, with every message bundle covering the supported set |
| idempotency-key-on-mutations | CRITICAL | review | Payment, notification, and email-outbox POST mutations must enforce a required Idempotency-Key request header, deduplicated via IdempotencyKeyStore |
| identity-claim-on-auth-atomic-idempotent-guarded | HIGH | review | When an anonymous principal authenticates for the first time, the records it accreted while anonymous (cart / order / draft / wishlist) MUST be CLAIMED by the now-authenticated identity atomically and idempotently — transferred exactly once across all N records, a replayed or concurrent claim a no-op — and the claim MUST refuse any record already owned by a different registered principal via a structural compare-and-set (owner IS NULL), never a check-then-act pre-check that races (CWE-367) |
| immutable-record-corrected-by-reversal-not-edit | HIGH | review | A posted immutable record is corrected by APPENDING a reversing entry — never by editing or deleting the original |
| in-doubt-outbound-call-holding-state | HIGH | review | A non-idempotent outbound call whose response is lost MUST enter a holding state — never silently retry, never assume failure |
| incident-dashboard-background-poll-plus-refresh | MEDIUM | review | Incident dashboards MUST poll in background AND expose a manual Refresh control with "last updated" timestamp |
| korean-brn-format | HIGH | review | Backend endpoints accepting a Korean Business Registration Number (사업자등록번호) must validate the input against the 10-digit NNN-NN-NNNNN format before persistence or logging |
| korean-vat-10-percent-calculation | HIGH | review | Backend services computing Korean VAT must use BigDecimal with rate 0.10 and HALF_UP rounding; float, double, and inline rate literals (0.10d / 0.10f) are prohibited |
| lang-bigdecimal-for-measured-decimals | HIGH | review | Measured / aggregated non-money decimals must use scaled BigDecimal — never float, double, or money minor-units |
| lang-bigdecimal-for-money | HIGH | gradle:testPayment | Monetary amounts must use BigDecimal — never float or double |
| lang-no-public-mutable-fields | MEDIUM | gradle:testPractices | No public, non-static, non-final instance fields outside records |
| lang-records-for-dtos | MEDIUM | gradle:testPractices | Transport DTOs (*Request / *Response) must be Java records |
| lang-sealed-result-hierarchies | MEDIUM | gradle:testPractices | Model closed result hierarchies with sealed interface + record permits |
| limit-crossing-drives-irreversible-terminal-and-blocks-derived-use | HIGH | review | A cumulative register with a mandatory limit (life-limit / usage ceiling) must convert the crossing accrual into an IRREVERSIBLE terminal state in the SAME transaction — zero outgoing edges, late accrual rejected (409), and the DERIVED capability (install / dispatch / use) fail-closed on the same locked row; never a live row whose anchor ≥ limit |
| machine-computed-value-tracks-override-provenance | HIGH | review | A machine-computed but human-overridable field MUST track override provenance — and recompute MUST skip human overrides |
| mandate-fanout-conserved-recall-check-battery-and-deemed-election | HIGH | review | A one-directive fan-out must create EXACTLY N child tasks atomically and report completion as a DERIVED conserved recall (Σ terminal == N, never a stored flag), gate the mandate behind a pass-ALL check battery (every declared check recorded passed, else 422), auto-resolve a child's silence past its deadline to a recorded DEEMED default election EXACTLY ONCE via a @Scheduled poller driving a proxied @Transactional worker, and serialize the explicit child-complete against the deemed sweep on the task row so each child reaches a terminal state exactly once |
| material-divisibility-reject-not-round-integer-vs-fractional | HIGH | review | A per-material divisibility constraint must REJECT — never silently round — a quantity its policy forbids — an INTEGER_ONLY material rejects any quantity with a non-zero fractional part (422 NON_INTEGRAL_QUANTITY, naming the material) and a FRACTIONAL material rejects a quantity whose decimal scale exceeds its recorded maximum (422 EXCESS_PRECISION); integrality and scale are tested with EXACT BigDecimal.stripTrailingZeros (so 5 == 5.0 == 5.00 are integral, format-independent), the policy is a recorded versioned per-material property, and every check is recorded with the policy version in force — the deliberate opposite of the round-UP-to-a-lot-multiple quantizer, which CHANGES the number |
| messaging-payload-record | MEDIUM | gradle:testPractices | Message and event payloads must be Java records (immutable by construction) |
| messaging-publisher-interface | HIGH | gradle:testPractices | Service-layer publishers must depend on an abstract MessagePublisher interface |
| messaging-topic-name-constant | MEDIUM | gradle:testPractices | Topic / routing-key names must be public-static-final constants, not inline string literals |
| migration-forward-only | HIGH | gradle:testPractices | Migration versions are unique and monotonic — never renumber an applied migration |
| migration-no-baseline-on-migrate | HIGH | gradle:testPractices | spring.flyway.baseline-on-migrate must not be enabled in base config |
| migration-versioned-naming | HIGH | gradle:testPractices | SQL migrations must follow Flyway V{version}__{description}.sql naming |
| monetary-arithmetic-fails-closed-across-currencies-absent-explicit-recorded-conversion | HIGH | review | Monetary arithmetic must be currency-TAGGED and FAIL-CLOSED across currencies — adding or subtracting two amounts whose ISO-4217 currency codes differ, absent an explicit recorded conversion, MUST THROW (never silently coerce, never assume a shared currency, never use one operand's currency for the other); same-currency arithmetic returns a new exact-integer amount in that same currency, and the ONLY sanctioned cross-currency path is an explicit, RECORDED conversion that brings one operand into the other's currency (the exchange RATE itself is out of scope — the converted amount is supplied) |
| monotonic-ingest-reject-stale-event | HIGH | review | A late / out-of-order external event MUST NOT clobber a fresher current-state row — reject at-or-behind the watermark |
| multi-tenant-aop-guard-skeleton | HIGH | guard:practices/evals/multi_tenant_aop_guard_skeleton_guard.sh | Recipes declaring tenant_model: multi must adopt the canonical multi-tenant skeleton — cross-cutting <root>.multitenancy package, TenantOwned marker on every tenant-scoped @Entity, globally-ordered MultiTenantProblemDetailAdvice, and explicit ThreadPoolTaskExecutor with TenantContextAwareTaskDecorator |
| multilateral-netting-conserves-per-node-and-set-wide | HIGH | review | Multilateral netting must conserve BOTH per-node and set-wide — each member's net = Σ owed-to − Σ owed-by (one currency), the sum of ALL members' nets == EXACTLY 0 per currency, computed by a sole-mutator single transaction with a DB-backstopped rollup — never a per-operation conservation (this is the SET-WIDE dual of balanced-posting) |
| mutation-in-flight-uses-aria-busy | MEDIUM | review | In-flight mutations MUST use aria-busy + aria-disabled, not native `disabled` |
| mutation-skipped-outcome-surfaces-reason | MEDIUM | review | Mutations that may NO-OP (skipped by server invariant) MUST surface the skipped outcome with the server's reason |
| negative-copresence-gate-is-set-evaluated-graded-failclosed | HIGH | review | A contraindication / conflict gate must evaluate the candidate against the SET of the subject's other active members (set-intersection on a normalized concept), grade each finding ABSOLUTE vs RELATIVE, FAIL CLOSED on an unassessable candidate, and re-read the set in the same transaction — never a single-subject one-flag check, never a silent allow on an unknown concept |
| net-meter-signed-net-is-derived-from-two-monotone-direction-registers | HIGH | review | A net meter's SIGNED net must be DERIVED from two independently value-monotone direction registers (IMPORT +, EXPORT −) — net = cumulativeImport − cumulativeExport, recorded as a basis and CROSS-CHECKED against an independent recompute (never trusted by-construction) — with each direction reading taken under the meter row lock and a closed billing period frozen (a backdate is 409) |
| no-billing-cross-import-from-payment | CRITICAL | gradle:testBilling | billing and payment packages must not import each other; the boundary defined in §5.2.6 is enforced by ArchUnit |
| no-rrn-collection-without-legal-basis | CRITICAL | review | Backend services must not accept, store, or process raw RRN (주민등록번호) without an explicit @LegalBasis annotation |
| no-rrn-logging | CRITICAL | guard:no_rrn_in_log_guard.sh | RRN (주민등록번호) must never appear in any log statement at any level |
| observability-mdc-trace-propagation | MEDIUM | gradle:testPractices | Populate MDC trace_id for every request, clear on exit |
| observability-no-pii-in-logs | HIGH | gradle:testPractices | Redact PII (including PAN) before it enters a log statement |
| observability-slo-error-budget-convention | MEDIUM | review | Observability MUST be convention-driven — declared SLIs/SLOs, error-budget burn-rate alerting, RED/USE coverage, exemplar metric→trace links, dashboards-as-code |
| observability-structured-logging | MEDIUM | gradle:testPractices | Emit structured key-value pairs, not concatenated log strings |
| offer-eligibility-predicate-evaluated-fail-closed-from-declared-criteria | HIGH | review | An offer/discount's applicability must be decided by a single deterministic, fail-closed evaluator that reads only the offer's DECLARED criteria — a BOGO qualifier→target minimum-quantity gate AND a customer-xref/segment eligibility gate — so that unknown or missing criteria DENY BY DEFAULT (not-applied) and an ineligible offer can never reach the discount-application path; applicability (WHO/WHICH-ITEMS) is decided here, never the discount amount |
| optimistic-update-snapshot-rollback | MEDIUM | review | Optimistic update MUST snapshot-and-rollback — never invalidate-only |
| order-cart-spine-price-snapshot-immutable-after-submit-merge-and-fulfillment-conserves | HIGH | review | A cart→order spine must freeze each line's unit price + name at add-time (the price the customer saw, @Column(updatable=false), never re-derived from the live catalog), reject every add/update/remove on a SUBMITTED order (only an IN_PROCESS cart is editable), MERGE quantity when the same SKU is added again instead of duplicating a line, and partition order units into fulfillment groups conservingly (Σ group-item quantity per line == line quantity) |
| order-multiple-quantization-non-conserving-ceiling-to-moq | HIGH | review | A net requirement quantized to a procurement constraint must round UP deterministically to the supplier lot multiple at or above the MOQ (orderQuantity = max(MOQ, ceil(required / multiple) * multiple)), and because this is NON-CONSERVING by design — the placed order exceeds the requirement — the surplus overage = orderQuantity − required MUST be computed exactly and RECORDED (never hidden), the full basis persisted so it is reconstructible, and MOQ / multiple held positive — the deliberate opposite of the catalog's conserving rounded-split |
| order-tax-application-skips-exempt-scope-and-recompute-converges-to-one-record | HIGH | review | Order-level tax application must (1) SKIP every declared-exempt scope — a tax-exempt customer or a tax-exempt line contributes ZERO to the non-exempt taxable base, so a fully-exempt order has total tax 0 — and (2) recompute IDEMPOTENTLY by find-existing → update-or-create-or-remove so that re-pricing converges to exactly ONE combined tax record per order whose amount == round(taxableBase × injectedRate), never duplicated and never stranded; the tax is DERIVED each time from the declared input and the injected rate, never a client-asserted amount |
| ordered-siblings-reorder-atomic | HIGH | review | Ordered sibling collections MUST persist an explicit position and renumber atomically under serialization |
| owner-deprovision-reassigns-not-orphans | HIGH | review | Deprovisioning a record owner MUST reassign their rows to a named successor — never orphan them |
| ownership-transfer-authz-audit-atomic | HIGH | review | An ownership reassignment MUST be initiator-authorized, written atomically all-or-nothing, and recorded in exactly one audit entry |
| password-reset-success-invalidates-token-family | HIGH | review | A successful password reset MUST invalidate the user's ENTIRE family of outstanding unused reset tokens — not just the consumed one |
| payment-iso-4217-currency | HIGH | gradle:testPayment | Currency codes must be ISO 4217 alpha-3 and the amount scale must match the currency's minor-unit count |
| payment-split-tender-coverage-sums-to-total-and-capture-bounded-by-auth | HIGH | review | An order paid by multiple tenders MUST conserve coverage — the sum of the active, successfully-authorized payment amounts sharing one order id MUST cover the order's frozen total before the order is confirmable (an under-covered order is rejected, never shipped unpaid) — and each capture MUST be bounded by its authorization (the cumulative captured amount never exceeds the authorized amount, the auth-side dual of the refund cap) |
| per-subject-marker-entity-idempotent | HIGH | review | A per-(subject, target) marker / junction entity MUST be idempotent on its natural key — UNIQUE constraint + no-op duplicate-create + no-op absent-delete |
| period-close-reject-late-write | HIGH | review | A write into a sealed/closed aggregation period MUST be rejected or rerouted — never silently mutate a finalized period |
| persistence-batch-inserts | HIGH | gradle:testPractices | Configure hibernate.jdbc.batch_size + order_inserts for bulk persists |
| persistence-entity-graph | MEDIUM | gradle:testPractices | Prefer @EntityGraph for annotation-driven fetch shape |
| persistence-no-n-plus-1 | HIGH | gradle:testPractices | Prevent N+1 queries with explicit fetch shape |
| persistence-optimistic-locking | HIGH | gradle:testPractices | Add @Version to entities updated under concurrent traffic |
| persistence-state-machine-atomic | HIGH | gradle:testPayment | State machine transitions must be atomic — @Version + transactional boundary + explicit transition method |
| pii-masked-at-dto-boundary | HIGH | gradle:testSessionManagement | Raw PII (IP, User-Agent, credentials) stored on entity for forensics but masked at DTO boundary |
| polymorphic-entity-ref-path-segment-guard | MEDIUM | review | Polymorphic (entityType, entityId) refs MUST be path-segment guarded client-side |
| prefer-recipe-composition-over-l4-cross-import | HIGH | review | When a business domain matches a Business Pattern Recipe, cross-L4 wiring must follow the Recipe composition contract; ad-hoc multi-L4 cross-imports without applied_recipe declaration are prohibited |
| presigned-url-signature-required | HIGH | review | File-storage presigned URLs must include an HMAC server signature before returning to callers |
| pricing-pipeline-orders-discount-before-tax-and-total-conserves | HIGH | review | A pricing pipeline must run phases in a fixed deterministic order — discount BEFORE tax so each item's taxable base is its amount MINUS its prorated order discount (tax on the NET price, never the gross) — and must close the order total as a conserving sum of its disclosed components (total = subTotal − orderAdjustments + shipping + tax + fees) with no penny invented or lost |
| promote-on-third-use | MEDIUM | review | Catalog utilities MUST be promoted to a shared package on the third adoption — or carry an explicit deferral with expiry |
| promotion-offer-engine-conserves-determinism-atomic-cap | HIGH | review | A discount/offer application engine must conserve a prorated order-level discount to the cent (floor-then-distribute-remainder so Σ item shares == order discount exactly), apply offers in a DETERMINISTIC total order (priority then potential-savings, stable tie-break — never collection order), gate co-application on TWO orthogonal flags (stackable ≠ combinable), clamp every discount to the line price (never negative), and enforce max-uses ATOMICALLY via UNIQUE(offer_id, order_ref) plus a pessimistic offer-row lock — never check-then-insert (the Broadleaf max-uses TOCTOU this absorbs and strengthens) |
| provenance-dag-append-only-acyclic-rollup | HIGH | review | A provenance DAG MUST store edges append-only and immutable, stay acyclic as a rollup precondition, and roll up by product-down-path summed-across-paths |
| provenance-dag-traversal-is-bounded-and-cycle-safe | HIGH | review | Provenance / lineage / dependency-DAG traversal MUST be cycle-safe, depth-bounded and result-size-bounded |
| public-lookup-token-is-unguessable-and-enumeration-resistant | HIGH | review | A public possession-of-token read MUST use an unguessable, PK-distinct token and deny bad tokens as an indistinguishable 404 — the token IS the authorization |
| published-edition-immutable-copy-on-write | HIGH | review | Published content editions are immutable copy-on-write snapshots — never edit a live edition in place |
| quality-no-system-streams | MEDIUM | gradle:testPractices | Production code must not write to System.out / System.err |
| quality-optional-only-as-return | MEDIUM | gradle:testPractices | Optional is a return type — never a field, never a parameter |
| quality-utility-class-shape | LOW | gradle:testPractices | Utility classes must be final + private no-arg constructor |
| query-field-allowlist-sort-filter-bound | HIGH | review | A list/search endpoint that accepts client-supplied SORT and FILTER field names MUST bound them with a per-resource ALLOWLIST (the exact fields it permits, mapping each PUBLIC name to an internal entity property, restricting direction to asc/desc and operator to a closed safe set), rejecting any non-allowlisted field by NAME with a 422 — never forwarding the raw string into a Sort/Specification, never silently ignoring it |
| quorum-weighted-tally-with-quorum-gate-and-frozen-policy | HIGH | review | A collective weighted decision must collect IMMUTABLE one-per-voter ballots, freeze the resolution policy (threshold + quorum fraction + abstention mode + tie-break) at motion-open, measure QUORUM against ELIGIBLE weight (not cast weight) so quorum-not-met yields NO_DECISION distinct from REJECTED, compare the threshold with EXACT integer/BigDecimal arithmetic, break ties by the frozen deterministic order — and resolve as a PURE reproducible function of the immutable ballots so re-resolving returns the identical record |
| quota-atomic-tenant-claim | HIGH | review | Per-tenant accumulating quota MUST be claimed atomically — never check-then-increment |
| rbac-stub-default-fail-closed | HIGH | review | RBAC role stub MUST default to least-privilege role — never 'admin' in dev |
| realtime-single-protocol-declared | MEDIUM | review | A recipe MUST declare a single realtime protocol — SSE OR WebSocket — never both in one recipe |
| recipe-invariants-must-resolve | CRITICAL | guard:recipe_governance_guard.sh | Every business_invariants entry in a recipe spec YAML must carry spec_ref: or rule_ref: pointing to an existing artifact; unresolvable references are prohibited |
| record-linkage-banded-verdict-and-survivorship-merge | HIGH | review | Record linkage must band its verdicts Fellegi-Sunter-style with the score, per-field feature breakdown, and thresholds RECORDED on the proposal; the REVIEW band decides only by an explicit human confirm/reject; and a merge records per-field survivorship while TOMBSTONING the loser with a forward pointer — never deleting it |
| referral-disclosure-and-fraud-controls | HIGH | review | A referral program MUST disclose the material connection, attribute trackably, and guard against self-referral / multi-account / refund-reversal fraud |
| relationship-scoped-authz-via-grant-lookup | HIGH | review | Non-owner access to another subject's resource MUST be a grant-table lookup — not owner-equality, not a static role |
| remeasurement-supersession-versioned-recompute-and-trueup | HIGH | review | Remeasured values must supersede append-only (new row + pointer, no ACTUAL→ESTIMATED downgrade), settlement runs must be versioned with their input basis recorded and recompute idempotently, and a CLOSED period is corrected only by posting the NET delta forward into an open period — the run-of-record is never rewritten |
| reproducible-procedure-recorded-seed-replay-and-blinding | HIGH | review | An auditable deterministic procedure must record its SEED (a draw), pin its classifier VERSION (a classification), and BLIND its sensitive result fields — so a draw is replayable from the recorded seed, the same input under the same version is byte-identical, and a non-privileged caller never sees the raw blinded value |
| resilience-circuit-breaker-retry-bulkhead | HIGH | review | Every outbound dependency call MUST be wrapped in resilience controls — timeout, circuit breaker, bounded retry with jittered backoff, bulkhead, fallback |
| resumable-upload-tus-offset | MEDIUM | review | Large file uploads MUST be resumable by byte offset (tus-style) with per-chunk integrity, size/type allowlist, and session expiry cleanup |
| retention-delete-on-high-volume-table-must-be-bounded | HIGH | review | Scheduled retention/purge on a high-volume table MUST drop partitions or batch the DELETE — never one unbounded DELETE ... WHERE created_at < cutoff |
| role-hierarchy-subsumes-lower-tiers | HIGH | review | When >2 roles exist and higher tiers subsume lower tiers, declare a RoleHierarchy @Bean — never enumerate hasAnyRole(...) |
| rounded-split-conserves-total-largest-remainder | HIGH | review | A rounded total split across N buckets MUST be allocated so the parts sum back to the total exactly — never round each part independently |
| saga-compensating-transactions | HIGH | review | A multi-service business transaction MUST be a saga — ordered local transactions with reverse-order compensation, never a distributed 2PC |
| saturating-clamped-running-balance | HIGH | review | A saturating balance clamps AT its ceiling on accrual and AT zero on debit — it never errors and never stores an out-of-range value — while every operation records BOTH the requested and applied (post-clamp) amount, append-only, and concurrent accrual near the ceiling converges to EXACTLY the cap |
| sealed-period-watermark-monotonic-close | HIGH | review | A period-seal watermark MUST be one-way monotonic — close only advances it, re-close is idempotent, reopen is privileged and audited |
| secret-shown-once-uses-beforeunload-guard | HIGH | review | One-time-revealed plaintext secrets MUST wire beforeunload guard for the duration of the reveal panel |
| security-csrf-scoped-disable | HIGH | gradle:testPractices | Disable CSRF only for bearer-token paths, never globally |
| security-default-headers | HIGH | gradle:testPractices | Keep Spring Security's default response headers enabled |
| security-stateless-session-policy | HIGH | gradle:testPractices | SessionCreationPolicy.STATELESS for JWT / bearer-token APIs |
| security-token-transfer-compliance-gate | HIGH | review | A tokenized-security unit transfer MUST pass every compliance gate (recipient eligibility, lock-up, per-investor holding limit, sender balance) atomically before the append-only register is mutated; any gate failure rejects with no ledger change (fail-closed) |
| self-referential-tree-reparent-rejects-cycle | HIGH | review | Reparenting a node in a mutable self-referential tree MUST reject a move that creates a cycle — the parent relation stays a DAG |
| self-reported-input-plausibility-range-rate-and-unverified-provenance | HIGH | review | A self-reported, server-unverifiable value (claimed location, self-meter-read, odometer entry, attestation) must NOT be trusted as authoritative — it must pass a RANGE bound and a RATE-OF-CHANGE limit vs the prior accepted reading, be persisted as SELF_REPORTED_UNVERIFIED with its plausibility basis (never CONFIRMED), and an implausible submission must be rejected (422) AND recorded as an auditable attempt — never silently dropped |
| sensitive-read-audit-record-before-return-mask-and-purpose | HIGH | review | Reading a governed sensitive field is itself an AUDITED event — every service read that returns the raw @SensitiveField value MUST append an immutable access-log row (who / when / what / why) in the SAME transaction and BEFORE the value is returned; the default projection masks the value and the raw value is reached ONLY via the audited, purpose-stated reveal path, whose append-only trail is admin-queryable |
| server-side-stored-error-sanitize | HIGH | review | Stored error columns MUST be PII-sanitized at storage time — render-layer scrub alone is insufficient |
| settlement-finality-dvp-novation-and-fail-ladder | HIGH | review | Post-trade settlement must commit its two legs ATOMICALLY (delivery occurs if and only if payment occurs), reach an IRREVOCABLE final state after which novation/cancel/amend are all refused, conserve the obligation across any pre-finality counterparty novation (recorded append-only), and walk the fail ladder with exactly-once transitions under a row lock that lets exactly one settle finalize |
| shared-counter-claim-must-be-atomic | CRITICAL | review | A claim against a bounded shared counter MUST be a single atomic statement — never read-then-insert |
| soft-delete-audit-trail | HIGH | gradle:testCommentThread | Soft-delete via status flip preserves audit trail — hard-delete forbidden when audit matters |
| soft-delete-only-on-base-entity | HIGH | gradle:testPractices | Soft-delete must be implemented via @SQLDelete on BaseEntity subclasses, never via application-level flag fields |
| spec-domain-mode-gates-frontend-trio | HIGH | guard:l4_frontend_domain_mode_guard.sh | Frontend full-trio MUST be gated by the spec's `domain_mode` declaration |
| state-conditional-mutation-authority-is-a-declared-monotone-table | HIGH | review | Which fields of an aggregate are mutable must be a function of its CURRENT STATE — a DECLARED per-(state,field) authority table (never a blanket "editable while not terminal", never an if-scatter), monotonically tightened by forward transitions with widening only through a RECORDED governed re-open, and re-checked under the row's PESSIMISTIC_WRITE lock so a concurrent state advance cannot let a stale-state edit through |
| storage-reclaim-must-be-reconciled | HIGH | review | Blob reclaim on row delete MUST be reconciled — never a fire-and-forget inline storage delete |
| storage-reconciliation-sweeps | MEDIUM | review | Object store and DB MUST be reconciled by scheduled idempotent sweeps — reverse-purge orphan blobs, forward-quarantine dangling rows, never a raw NoSuchKey 500 |
| stored-blob-carries-content-digest-verified-on-read | HIGH | review | A stored blob MUST carry a write-time content digest (SHA-256) that the read path re-verifies — a mismatch is a fail-closed error, never a silent serve |
| stored-server-error-sanitize-at-render-layer | HIGH | review | Server-supplied stored error strings MUST pass a PII / secret deny-list at the render layer |
| subscription-state-machine-explicit | CRITICAL | gradle:testBilling | Subscription.status must only be mutated through SubscriptionStateMachine; direct setStatus() calls outside the state machine are prohibited |
| tamper-evident-log-external-anchor | HIGH | review | A tamper-evident log MUST anchor its chain head to a sink outside the log owner's unilateral write control |
| tamper-evident-log-hashchain | HIGH | review | Tamper-EVIDENT logs MUST hash-chain each entry to its predecessor — app-immutability alone is not tamper-evidence |
| temporal-as-of-point-in-time-query | HIGH | review | A temporally-versioned record MUST answer an as-of query with exactly the one row in force at the instant, and hold at most one open-ended current row per scope |
| temporal-validity-record-non-overlap | HIGH | review | Effective-dated records MUST forbid overlapping validity windows with a DB range-exclusion constraint — never a pre-insert overlap SELECT |
| testing-archunit-layer-boundary | MEDIUM | gradle:testPractices | Enforce controller → service → repository layering with ArchUnit |
| testing-archunit-no-cyclic-packages | MEDIUM | gradle:testPractices | Forbid cyclic package dependencies with ArchUnit slicing |
| testing-archunit-repository-shape | MEDIUM | gradle:testPractices | Classes named *Repository must extend Spring Data's JpaRepository |
| testing-restassured-blackbox | MEDIUM | gradle:testPractices | Use RestAssured + @LocalServerPort for practice tests, not MockMvc |
| time-bounded-access-grant-rebac-window-and-eligibility-gate | HIGH | review | A time-bounded relationship grant (ReBAC) must decide access by RECOMPUTING the window predicate over the injected Clock (now ∈ [validFrom, validUntil) AND ACTIVE) — never a stored 'expired' flag — be append-only + revocable (who/when recorded, no delete, fail-closed when revoked), and a multi-credential eligibility gate must pass ONLY when EVERY required credential class is held and non-expired at now (a single missing/expired class fails closed naming the class) |
| time-gated-decisions-read-injected-clock | HIGH | review | Time-gated decisions must read an injected Clock and compare a server-stored instant — never a client timestamp |
| time-proportional-accrual-prorates-partial-period | HIGH | review | A quantity earned over time must be accrued as rate × elapsed-fraction-of-period and a partial period MUST be prorated on a declared day-count basis — never granted or charged a full period |
| timed-offer-exclusive-assignment-and-reoffer-ladder | HIGH | review | A timed-assignment workflow must extend an offer to a candidate with a DEADLINE (OPEN until accept/decline/deadline; a @Scheduled sweep expires past-deadline OPEN offers EXACTLY ONCE, recorded SYSTEM/when), hold EXCLUSIVITY so at most ONE offer per subject is accepted (the loser of a competing accept gets 409 via a uq(subject_id) backstop under the subject row lock), and re-offer a declined/expired offer to the next candidate as a NEW row in an ordered append-only ladder |
| timeout-sweep-is-a-concurrent-mutator | HIGH | review | A scheduled timeout sweep is a concurrent mutator — it must re-check in its own transaction, carry @Version so it LOSES the race against a live action, run REQUIRES_NEW per row, and be idempotent |
| traceid-in-error-response | HIGH | review | Every ProblemDetail error response must include a traceId property |
| transaction-no-self-invocation | HIGH | gradle:testPractices | Do not self-invoke @Transactional methods |
| transaction-propagation-requires-new | MEDIUM | gradle:testPractices | Use Propagation.REQUIRES_NEW for writes that must persist independently |
| transaction-readonly-queries | MEDIUM | gradle:testPractices | Mark read-only queries with @Transactional(readOnly = true) |
| transaction-rollback-on-checked | HIGH | gradle:testPractices | Declare rollbackFor when the method throws a checked exception |
| transactional-outbox-no-dual-write | HIGH | review | A producer that mutates state AND emits a message MUST use a transactional outbox — never a dual write |
| transformation-conserves-with-classified-residual | HIGH | review | A material transformation must conserve to an ACCOUNTED residual — Σ(input) == Σ(good output) + Σ(classified residual), per base unit, with every residual unit tagged to a governed disposition code — never net-zero, never an unexplained shrinkage |
| two-axis-inventory-reservation-reserve-commit-release-hold | HIGH | review | A two-axis available/reserved inventory must derive AVAILABLE = onHand − reserved (never store it), reserve a HELD hold only when derived available ≥ q (422 else, reserved += q, onHand untouched), COMMIT a held reservation by decrementing BOTH onHand and reserved (the goods leave), RELEASE it by decrementing reserved alone (the hold frees), move HELD → (COMMITTED\|RELEASED) EXACTLY once (409 otherwise), keep reserved == Σ(HELD quantities) with 0 ≤ reserved ≤ onHand, and serialize concurrent reserves on the item row so exactly available/q win |
| two-factor-auth-totp-webauthn | HIGH | review | Second-factor auth MUST use a real distinct factor (TOTP/WebAuthn) with secure enrollment, per-session verification, recovery codes, attempt limits, and session binding |
| unit-of-measure-conversion-is-exact-and-pinned | HIGH | review | Cross-unit quantity conversion MUST use an exact, pinned, re-derivable factor — never an ad-hoc float multiply |
| uploaded-image-metadata-stripped-on-ingest | HIGH | review | Strip embedded metadata from accepted raster images by re-encoding on ingest |
| validation-custom-constraint | MEDIUM | gradle:testPractices | Encode domain-specific shape in @Constraint + ConstraintValidator |
| validation-error-envelope | HIGH | gradle:testPractices | Return validation failures as RFC 7807 with a structured errors[] array |
| validation-jakarta-bean-constraints | HIGH | gradle:testPractices | Annotate DTOs with Jakarta Bean Validation + @Valid on the handler |
| validation-mass-assignment-guard | HIGH | gradle:testPractices | Bind HTTP payloads to a whitelist DTO, never directly to an entity |
| valuation-run-projection-as-of-snapshot-conserving-fan-out-and-rebase | HIGH | review | A versioned valuation run must be pinned to an AS-OF instant + recorded basis and IMMUTABLE once computed (an as-of read returns the GREATEST as-of ≤ T, never a later run), fan out to N per-position outputs whose values SUM EXACTLY to the run total (a DB @Check AND an INDEPENDENT repo-SUM cross-check, never a by-construction tautology), and rebase by creating a NEW baseline run that RETAINS every prior run verbatim via a forward pointer — all serialized on the subject row so concurrent recompute/rebase create exactly one new version |
| value-transfer-must-be-balanced | CRITICAL | review | A value-moving operation MUST post balanced legs that net to exactly zero — conserve, never mint or destroy |
| variance-tolerance-band-asymmetric-gate-and-disposition | HIGH | review | A standard-vs-actual appraisal must DERIVE the variance (actual − standard, never an entered field), PIN the asymmetric tolerance band that governed THIS verdict on the row, render the verdict by a two-sided gate (WITHIN_TOLERANCE iff variance ∈ [−lowerTolerance, +upperTolerance], else OUT_OF_TOLERANCE), BLOCK any dependent operation on a breach (422 naming the variance + band) until an explicit who/when/reason DISPOSITION is recorded, and serialize concurrent dispositions on the appraisal row so exactly one wins |
| waitlist-promotion-is-atomic-fifo | HIGH | review | Waitlist promotion MUST be one atomic FIFO transaction — never read-then-promote |
| web-explicit-produces | MEDIUM | gradle:testPractices | Controllers must declare produces = application/json explicitly |
| web-rest-controller-annotation | MEDIUM | gradle:testPractices | JSON-API controllers must carry @RestController, never bare @Controller |
| web-specific-mapping-methods | MEDIUM | gradle:testPractices | Use @GetMapping / @PostMapping shortcuts, never bare @RequestMapping |
| webhook-hmac-required | HIGH | gradle:testIntegration | Inbound webhook endpoints must verify HMAC-SHA256 signatures before processing |
