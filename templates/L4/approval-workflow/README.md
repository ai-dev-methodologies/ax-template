# L4 / approval-workflow — Fork & Copy Guide

**Tenant model**: `single` — per [`specs/multi-tenant-l0.yaml#MULTI-TENANT-ISOLATION-DEFAULT-001`](../../../specs/multi-tenant-l0.yaml). This L4 reference workload ships as **single-tenant**. Recipes composing this domain into a multi-tenant SaaS (e.g. `b2b-admin` with `tenant_model: multi`) MUST adopt one of `MULTI-TENANT-ISOLATION-001` (Hibernate filter row-level) / `MULTI-TENANT-ISOLATION-002` (schema-per-tenant) / `MULTI-TENANT-ISOLATION-003` (AOP guard) plus `MULTI-TENANT-PROPAGATION-001` (request-scoped TenantContext) + `MULTI-TENANT-PROPAGATION-002` (async propagation) before production. fork-receivers MUST NOT assume cross-tenant data isolation in this L4 as-shipped.

**Status**: full-trio (R43 promoted, 2026-05-24, dogfood-iterated 7 rounds with two personas — sales-mgr requester + CFO mid-chain approver — until both reported GREEN). R39 shipped this domain as a backend-only stub; R43 added the Next.js admin tree: caller's inbox, my-filed requests, new request form, detail page with state-machine timeline.

## Domain summary

Sequential ordered multi-step approval (Korean enterprise 결재선 pattern). `ApprovalRequest` owns an ordered `List<ApprovalStep>` (JPA `OneToMany cascade`) with two state machines: request status (`DRAFT / SUBMITTED / APPROVED / REJECTED / CANCELLED`) and step status (`PENDING / APPROVED / REJECTED`). Both state transitions go through sole-mutator services; payload immutability is enforced via `JPA updatable=false` (R31 iter1 dogfood closure).

## Backend reference

- Java package: `backend/src/main/java/com/ax/template/authblueprint/approvalworkflow/`
- Spec: [`specs/approval-workflow-l0.yaml`](../../../specs/approval-workflow-l0.yaml) — 15 items / 5 families (LIFECYCLE × 4, AUTHZ × 3, STEP × 5, QUERY × 2, PAYLOAD × 1)
- Tests: `./gradlew testApprovalWorkflow` — GREEN (26/26 incl. iter1+2 dogfood violation proofs)
- Anchored generic rules (R38):
  - [`practices/rules/caller-authentication-only-no-userid-param.md`](../../../practices/rules/caller-authentication-only-no-userid-param.md) — duplicate approver + self-approve guards
  - [`practices/rules/admin-cannot-rewrite-user-content.md`](../../../practices/rules/admin-cannot-rewrite-user-content.md) — payload immutability across review

## Frontend (R43 full-trio)

| File | Purpose |
|------|---------|
| `app/layout.tsx` | Root Next.js layout with `Providers` |
| `app/page.tsx` | Redirect to `/approvals/inbox` |
| `app/providers.tsx` | `QueryClientProvider` (TanStack v5, staleTime 5s — approvals need fresh status) |
| `app/(approvals)/layout.tsx` | Route-group layout: AppShell + Sidebar (Pending / My filed / New) |
| `app/(approvals)/inbox/page.tsx` | Caller's pending steps — age-prioritized (24h / 72h coloring), explicit oldest-first sort, YOUR TURN badges |
| `app/(approvals)/my/page.tsx` | Caller's filed requests across all statuses |
| `app/(approvals)/new/page.tsx` | New draft form with client-side duplicate/self-approve/JSON guards, dirty-form cancel confirm |
| `app/(approvals)/[id]/page.tsx` | Detail: state-machine timeline + audience-matrix action panels + upstream-approver-comment callouts |
| `app/use-caller-id.ts` | Shared session hook + sameUser/normalizeUserId helpers; dev stub + production hard-stop |
| `app/parse-error.ts` | Shared RFC 9457 ProblemDetail unwrap with text/html fallback (Cloudflare/nginx/Tomcat) |
| `next.config.ts` | API proxy + security headers |

Audience matrix (mirrors the backend state machine):

| Viewer | DRAFT | SUBMITTED (waiting) | SUBMITTED (your turn) | APPROVED / REJECTED / CANCELLED |
|---|---|---|---|---|
| Requester | Submit + Discard + Create another | Cancel (with ripple confirm if approvals exist) | (same as approver if also approver) | Read-only + halted-panel if rejected |
| Approver (current step) | not visible | not visible | YOUR TURN panel: upstream comments + Approve (auto-advance) + Reject (confirm + mandatory comment) | not visible |
| Approver (later step) | not visible | "waiting on X" header indicator | n/a | Read-only with their own past decision |

UI-layer anchoring of R38 generic rules:

- **caller-authentication-only-no-userid-param**: `useCallerId()` is the single source of truth; sameUser-normalized comparisons across new-form duplicate-check, self-approval check, and detail action gating.
- **admin-cannot-rewrite-user-content**: payload becomes locked on submit; even admin role cannot rewrite the original document the requester filed (Korean enterprise audit posture).
- **http-delete-idempotency-rfc9110**: cancel + discard operations treat 204 on absent target as success.
- **pii-masked-at-dto-boundary**: no PII at the UI layer — caller-id is the only identity comparison key.

Multi-tenant integration anchor (b2b-admin recipe MULTI-TENANT-INTEGRATION-002): the action-gating logic NEVER accepts a `?userId=` query parameter. Caller-id is derived from the server session via `useCallerId()`.

## Dogfood closure

R43 followed the 2-persona dogfood protocol (sales-mgr requester P1 + CFO mid-chain approver P2) for 7 iter rounds. iter1 inventory found 32 issues across both personas. iter1–iter7 closed:

- the entire critical / high band (callerId stub safety, JSON race, reject-without-reason, error visibility, halted-state silence, upstream conditional approvals invisible to mid-chain approver, header↔timeline contradiction on halted/cancelled/draft, Rules-of-Hooks ordering)
- the medium throughput band (inbox prioritization signal, mutation auto-advance, refetch best-effort, persistent back-to-inbox, JPA jargon plain-language replacement)
- the low polish band (aria-label on disabled remove, sameUser symmetric usage, parseError extraction + text/html fallback, dead-code elimination)

10 findings remained deferred with explicit rationale (raw JSON payload → fork-receiver typed-form, approver autocomplete → user-directory backend, auto-advance-to-next-pending → DTO extension, IP/UA forensics → audit-log domain, admin override → backend RBAC, notification dispatch status → notification subsystem, addendum → audit policy decision, styled confirm dialog → design system, amount/department on inbox row → DTO extension, downstream cancel notification → notification subsystem). Final convergence verdict: GREEN.

## How to fork into your project

1. Copy the Java package `com.ax.template.authblueprint.approvalworkflow` to your project's `<base>.approvalworkflow`.
2. Copy the `app/` tree above into your Next.js project's `src/app/` (preserving the `(approvals)` route group).
3. Copy `specs/approval-workflow-l0.yaml` for the contract surface.
4. Replace `app/use-caller-id.ts` with your real session hook — the production hard-stop will throw if you forget.
5. Replace the raw-JSON payload textarea with typed forms per request `type` (the amber banner in `new/page.tsx` flags this obligation).
6. Wire approver inputs to your user-directory autocomplete (the README's freeform inputs are placeholders).
7. The duplicate-approver guard and self-approve guard are required structural invariants — do NOT relax them; the iter1+2 backend dogfood adds them as VIOLATION proof tests.
8. Adopt a `tenant_model: multi` isolation mode before production if your composition declares one.
