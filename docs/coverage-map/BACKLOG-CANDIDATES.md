# Backlog Candidates — Engine W1 Iter1

Enforcement gaps discovered via consumer-proof harness (cell S3.e-commerce).

## Candidates

- **P1-candidate: Ship FE locale-aware date/time formatting rule (Intl.DateTimeFormat/Intl.NumberFormat enforcement)** — 출처: engine w1 iter1, cell S3.e-commerce (G2, block severity)
- **P2-candidate: Add standalone --root shell guard for unbounded-repository-read invariant (findAll without Pageable)** — 출처: engine w1 iter1, cell S3.e-commerce (G1, warn severity)
- **P2-candidate: Ship ax/no-raw-billing-amount ESLint rule OR add executable guard to practices/evals/** — 출처: engine w1 iter1, cell S3.e-commerce (G3, warn severity)
- **P2-candidate: Harden money_boundary_seam_guard.sh to strip comments before pattern matching** — 출처: engine w1 iter1, cell S3.e-commerce (G4, warn severity)

## Candidates — Engine W1 Iter2

Enforcement gaps discovered via consumer-proof harness (cell S3.b2b-admin).

- **P1-candidate: Ship catalog guard enforcing PRESENCE of method-level authorization on admin endpoints (decouple from *AdminController naming)** — 출처: engine w1 iter2, cell S3.b2b-admin (G1, block severity)
- **P1-candidate: Add evidence-anchored rule for BFLA invariant (every privileged endpoint MUST carry @PreAuthorize/@PostAuthorize, anchor: OWASP API5:2023)** — 출처: engine w1 iter2, cell S3.b2b-admin (G2, block severity)
- **P1-candidate: Ship ESLint rule or shell guard enforcing non-PII-only analytics/telemetry payloads (use allowlist, not denylist)** — 출처: engine w1 iter2, cell S3.b2b-admin (G3, block severity)
- **P2-candidate: Add spec item or rule capturing client-side data-minimization invariant for telemetry (anchor: GDPR Article 5(1)(e) / 개인정보보호법)** — 출처: engine w1 iter2, cell S3.b2b-admin (G4, warn severity)
- **P2-candidate: Add canonical client-side analytics/telemetry wrapper template (L0 or L2) with configurable allowlist** — 출처: engine w1 iter2, cell S3.b2b-admin (G5, warn severity)

## Candidates — Engine W1 Iter3

Enforcement gaps discovered via consumer-proof harness (cell S3.saas-subscription).

- **P1-candidate: Ship catalog guard enforcing FE pagination parser parity with BE PageEnvelope contract (assert dereferences of data, pagination.{page,pageSize,totalElements,totalPages,hasMore})** — 출처: engine w1 iter3, cell S3.saas-subscription (G1, block severity)
- **P2-candidate: Add L0 or L2 template providing canonical PageEnvelope parser (composable export, TypeScript-ready)** — 출처: engine w1 iter3, cell S3.saas-subscription (G2, warn severity)
- **P2-candidate: Enrich L2 pagination.tsx props to include totalPages and hasMore; provide adapter/factory mapping BE PageEnvelope → PaginationProps** — 출처: engine w1 iter3, cell S3.saas-subscription (G3, warn severity)
- **P2-candidate: Patch run-consumer-proof.sh eslint invocation to use explicit --config flag or re-root lint; add fixture validating violating rule produces non-0 exit from out-of-tree scenario** — 출처: engine w1 iter3, cell S3.saas-subscription (G4, warn severity)

## Candidates — Engine W1 Iter5

Enforcement gaps discovered via consumer-proof harness (cell S2.AUDIT-PII.FE + S2.MULTI-TENANT.BE).

- **P1-candidate: Ship catalog enforcement asset (ESLint ax/* rule or practices/evals shell guard) that blocks FE error-render call-sites from hand-reading Response.detail/message and rendering unvetted; require parseError()/sanitizeStoredError() seam** — 출처: engine w1 iter5, cell S2.AUDIT-PII.FE (G1, block severity)
- **P1-candidate: Ship catalog guard (practices/evals shell guard or ArchUnit rule) verifying multi-tenant repository call-sites use tenant-scoped methods (findByIdAndTenantId/findAllByTenantId, not bare findById/findAll)** — 출처: engine w1 iter5, cell S2.MULTI-TENANT.BE (G2, block severity)
- **P2-candidate: Ship common/TenantContext.java runtime primitive (ThreadLocal-backed, with static current()/set/clear) and wire blueprints/multi-tenant-manifest.yaml reference** — 출처: engine w1 iter5, cell S2.MULTI-TENANT.BE (G3, warn severity)
- **P1-candidate: Add vitest cases for templates/L0/fork-receiver-kit/parse-error.ts covering JSON RFC 9457 branch (body.detail/body.message) + sanitizeStoredError seam; integrate to R25 test-coverage gate** — 출처: engine w1 iter5, cell S2.AUDIT-PII.FE (G4, block severity, closed this session)

## Candidates — Engine W1 Iter4

Enforcement gaps discovered via consumer-proof harness (cell S2.AUTHZ.FE). Backfilled 2026-07-20 — this iteration ran during wave-1 but was never logged (gap-logging hole surfaced by fable5 wave-exit verification).

- **P1-candidate: Ship ax/* ESLint rule or practices/evals shell guard enforcing that every ax:admin-action marker in a .tsx file is preceded by a useCallerRole import + role comparison (component-granularity BFLA)** — 출처: engine w1 iter4, cell S2.AUTHZ.FE (G1, block severity)
- **P1-candidate: Add SSRF use-time allowlist re-validation — spec item (ssrf-prevention-l0.yaml or webhook-l0 addition), common/UrlAllowlistValidator SPI, and practices/evals shell guard requiring restTemplate fetch call-sites to be preceded by an allowlist check** — 출처: engine w1 iter4, cell S2.AUTHZ.FE (G2, block severity)
- **P2-candidate: Add vitest unit tests for templates/L0/fork-receiver-kit/use-caller-id.ts (role resolution + caller-role-absent fallback); wire into R25 frontend test-coverage gate** — 출처: engine w1 iter4, cell S2.AUTHZ.FE (G3, warn severity)

## Candidates — Engine W1 Iter6

Enforcement gaps discovered via consumer-proof harness (cell S2.AUDIT-PII.XB). Backfilled 2026-07-20 — this iteration ran during wave-1 but was never logged (gap-logging hole surfaced by fable5 wave-exit verification).

- **P1-candidate: Ship practices/evals shell guard (or ArchUnit rule) scanning *Response.from()/*Dto.from() DTO factory methods for PII-shaped getters not routed through AuditLogPiiRedactor/AuditPiiHelper#piiHash; close remaining metadataJson/userAgent residual on AuditLogDto paths** — 출처: engine w1 iter6, cell S2.AUDIT-PII.XB (G1, block severity)
- **P2-candidate: Add practices-react rule or ax/* ESLint rule requiring a webhook-delivery status component to render a distinct visual state (not the success pill) when signatureStatus indicates verification failure** — 출처: engine w1 iter6, cell S2.AUDIT-PII.XB (G2, warn severity)
