# L4 / email-outbox — Fork & Copy Guide

**Tenant model**: `single` — per [`specs/multi-tenant-l0.yaml#MULTI-TENANT-ISOLATION-DEFAULT-001`](../../../specs/multi-tenant-l0.yaml). This L4 reference workload ships as **single-tenant**. Recipes composing this domain into a multi-tenant SaaS (e.g. `b2b-admin` with `tenant_model: multi`) MUST adopt one of `MULTI-TENANT-ISOLATION-001` (Hibernate filter row-level) / `MULTI-TENANT-ISOLATION-002` (schema-per-tenant) / `MULTI-TENANT-ISOLATION-003` (AOP guard) plus `MULTI-TENANT-PROPAGATION-001` (request-scoped TenantContext) + `MULTI-TENANT-PROPAGATION-002` (async propagation) before production. fork-receivers MUST NOT assume cross-tenant data isolation in this L4 as-shipped.

**Status**: full-trio (R51 promoted, 2026-05-26). Promoted future_add → selectable. R50/R52 catalog rules preempted day-one (no iter1 high/critical findings expected per the dogfood protocol; this README documents the day-one preempt for auditability).

## Domain summary

Transactional email outbox pattern. `EmailOutboxService.enqueue()` persists a `PENDING` row (after rendering subject/body via `EmailTemplateService`). A scheduled `processQueue()` cycle reads due rows (`PENDING` or `RETRY` with `nextAttemptAt <= now`) and dispatches via the swappable `EmailSenderService` adapter (SMTP / SES / SendGrid). Failures increment `retryCount` and set exponential backoff (2^retryCount × 30s). After `MAX_RETRIES` (3) the row moves to `DLQ` for operator triage.

## Backend reference

- Java package: `backend/src/main/java/com/ax/template/authblueprint/emailoutbox/`
- Spec: [`specs/email-outbox-l0.yaml`](../../../specs/email-outbox-l0.yaml) — 8 items / 5 families (QUEUE × 2, SEND × 2, RETRY × 2, TEMPLATE × 1, ADMIN × 1)
- Tests: `./gradlew testEmailOutbox` — GREEN (8 compliance + 4 violation proof)
- Migration: `backend/src/main/resources/db/migration/V024__create_email_outbox.sql`

## Default sender (DEV ONLY)

`LoggingEmailSenderConfig` provides a `@ConditionalOnMissingBean` default that logs `(recipient, subject)` to stdout. **Fork-receivers MUST replace this with their real adapter (SMTP / SES / SendGrid / Mailgun) before production.** The default refuses to ship in `prod` / `production` profile unless `ax.email-outbox.allow-logging-sender-in-prod=true` — matches the `useCallerId` / `useCallerRole` production hard-stop pattern (R47 rbac-stub-default-fail-closed).

## Frontend (R51 full-trio)

| File | Purpose |
|------|---------|
| `app/layout.tsx` | Root Next.js layout with `Providers` |
| `app/page.tsx` | Redirect to `/admin/email-outbox` |
| `app/providers.tsx` | `QueryClientProvider` (TanStack v5, staleTime 15s) |
| `app/(admin)/layout.tsx` | Route-group layout: AppShell + Sidebar |
| `app/(admin)/email-outbox/page.tsx` | **Outbox monitor** — status filter, 10s background poll, per-row Retry / Delete |
| `app/use-caller-id.ts` | Shared session + `useCallerRole()` for the ROLE_ADMIN gate |
| `app/parse-error.ts` | Shared RFC 9457 ProblemDetail unwrap + text/html fallback + Korean PII deny-list + `sanitizeStoredError` helper |
| `next.config.ts` | API proxy + security headers |

UI-layer enforcement of the catalog rules (R47 + R50 + R52 lessons preempted from day one):

- **rbac-stub-default-fail-closed** (R47): `useCallerRole` defaults to `'user'`; admin path requires `NEXT_PUBLIC_DEV_AS_ADMIN=1` env opt-in.
- **destructive-action-confirm-with-side-effects** (R50): Retry and Delete confirm with verbatim consequence text — Retry resets retryCount and re-fires the send chain; Delete removes the row from audit.
- **stored-server-error-sanitize-at-render-layer** (R50): `lastError` on the outbox row is rendered via `sanitizeStoredError` (Korean PII + Bearer/JWT/PEM/internal hostname deny-list).
- **secret-shown-once-uses-beforeunload-guard** (R50): N/A for this domain — no plaintext secret revealed once.
- **incident-dashboard-background-poll-plus-refresh** (R50): the outbox monitor sets `refetchInterval: 10_000` + `refetchIntervalInBackground: true` + visible `Updated HH:MM:SS` + manual Refresh button.
- **mutation-skipped-outcome-surfaces-reason** (R50): Retry endpoint refuses SENT rows with `409 EMAIL_OUTBOX_INVALID_TRANSITION` — the UI surfaces the server's reason in an amber `role='alert'` span rather than a green-success toast.
- **mutation-in-flight-uses-aria-busy** (R47): all action buttons use `aria-busy` + `aria-disabled` + onClick `if (busy) return`; native `disabled` is not used for transient mutation state.
- **error-message-not-in-native-title-attribute** (R47): mutation errors render in `role='alert'` aria-live spans, never in button `title`.
- **hooks-before-conditional-return** (R47): every `useState` / `useEffect` / `useQuery` / `useMutation` is declared before the role-gate's conditional return.
- **client-must-not-fabricate-audit-timestamps** (R47): `createdAt` / `sentAt` / `nextAttemptAt` are rendered as received; pending state is a typed Set in component state, never written into the cache as a synthetic timestamp.

R52 catalog rules applied:
- **Cache-Control: no-store** on every `/api/admin/email-outbox` response (shared-workstation cache leak prevention).

## How to fork into your project

1. Copy the Java package `com.ax.template.authblueprint.emailoutbox` to your project's `<base>.emailoutbox`.
2. Copy the `app/` tree above into your Next.js project's `src/app/` (preserving the `(admin)` route group).
3. Copy `specs/email-outbox-l0.yaml` for the contract surface.
4. Replace `LoggingEmailSenderConfig` with your real SMTP / SES / SendGrid / Mailgun adapter. The default refuses to start in `prod` profile without an explicit opt-in flag — replacing it is mandatory.
5. Seed `email_templates` rows for your domain (welcome / order-confirmation / password-reset / etc.) before any `enqueue` call.
6. Wire a scheduled `processQueue()` invocation (every 30s — 2 min depending on email volume). The `scheduled-task` L4 (`testScheduledTask` GREEN, R49 full-trio) is the canonical pattern.
7. If your composition declares `tenant_model: multi`, adopt one of the `MULTI-TENANT-ISOLATION-00{1,2,3}` modes before production.
