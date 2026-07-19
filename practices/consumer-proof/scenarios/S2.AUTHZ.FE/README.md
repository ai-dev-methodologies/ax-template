# S2.AUTHZ.FE — consumer-proof dogfood cell

FE authz slice, composed on the L4 webhook vertical's admin surface
(`templates/L4/webhook/app/(admin)/webhooks/page.tsx`), plus an SSRF
additional requirement at a call site `canary-gaps.yaml` CANARY-005 does not
already cover.

## Cell status (per `practices/consumer-proof/engine/coverage-map.yaml`)

`S2.AUTHZ.FE` — `status: partial`. Rule docs
(`impersonation-banner-required-when-acting-as-other-user.md`,
`no-impersonation-bypass-via-helper-rename.md`,
`audit-log-frontend-viewer-rbac-virtualized.md`) and a real L0 primitive
(`templates/L0/fork-receiver-kit/use-caller-id.ts`) exist, but none of the 14
`ax/*` ESLint rules mechanically enforce RBAC-in-render — no automated
non-vacuity.

## Findings

1. **NAMED GAP — admin-only action not self-gated.** The webhook admin
   *page* already gates its whole surface with
   `if (role !== 'admin') return <EmptyState .../>`. A new admin-only action
   ("Send test delivery" — triggers a real outbound server-side fetch of the
   endpoint's target URL) added as a standalone component naturally inherits
   the assumption that the enclosing page's gate is enough, and ships with no
   gate of its own. If that component is ever reused outside the gated page
   (a shared toolbar, a future refactor), the privileged action fires for any
   caller. This is OWASP API5:2023 BFLA: authorization must be enforced at
   the function, not only wherever it happens to be mounted today.
   - `react/violating/TestWebhookDeliveryButton.tsx` — imports
     `useCallerRole` but never checks it before rendering the marked
     `ax:admin-action` region.
   - `react/clean/TestWebhookDeliveryButton.tsx` — checks
     `role !== 'admin'` and returns `null` before the marked region.
   - `scenario-guards/fe_admin_action_missing_role_gate_guard.sh` — HAND-
     ROLLED (no ax/* ESLint rule or shell guard covers this; capability-gap
     signal). Grep-based: every `ax:admin-action` marker in a `.tsx` file
     must be preceded, in source order, by a `useCallerRole` import AND a
     `role === 'admin'` / `role !== 'admin'` comparison.

2. **ADDITIONAL REQUIREMENT — SSRF URL-allowlist check before server-side
   fetch of a user-supplied URL.** `canary-gaps.yaml` CANARY-005 already
   names this gap at *registration* time (storing a webhook subscription
   target URL). This dogfood surfaces the SAME class of defect at a
   *different* call site the canary does not cover: *test-delivery* time — an
   admin clicking "Send test delivery" causes the backend to re-fetch the
   ALREADY-STORED target URL on demand, right now, with no re-validation.
   A URL that was benign at registration can be repointed at an internal
   host (`169.254.169.254`, `127.0.0.1`, RFC 1918) by the time this later
   fetch runs (DNS rebinding / a since-edited endpoint). OWASP API7:2023 SSRF.
   - `java/violating-root/.../webhooktest/WebhookTestDeliveryService.java` —
     `restTemplate.getForObject(targetUrl, ...)` with no preceding check.
   - `java/clean-root/.../webhooktest/WebhookTestDeliveryService.java` —
     calls `urlAllowlistValidator.assertAllowed(targetUrl)` immediately
     before the fetch.
   - `java/clean-root/.../webhooktest/UrlAllowlistValidator.java` — HAND-
     ROLLED (no catalog SPI/primitive exists for this; capability-gap
     signal). Minimal fail-closed check: scheme must be `https`, resolved
     host must not be loopback / link-local / site-local / any-local.
   - `scenario-guards/ssrf_missing_allowlist_check_guard.sh` — HAND-ROLLED.
     Grep-based: every `restTemplate.<fetch-method>(<variable>, ...)` call in
     a `*Service.java` file must be preceded, in the same file, by an
     `AllowlistValidator.assertAllowed(...)` / `.validate(...)` call.

## Catalog assets reused (not hand-rolled)

- `templates/L0/fork-receiver-kit/use-caller-id.ts` (`useCallerRole`) — both
  FE fixtures.
- `templates/L0/fork-receiver-kit/parse-error.ts` (`parseError`) — both FE
  fixtures' failed-fetch error path.
- `templates/L4/webhook/app/(admin)/webhooks/page.tsx` — the real admin
  surface + page-level gate pattern this scenario's action extends and whose
  convention it dogfoods at component granularity.
- Package convention `com.ax.template.authblueprint.<domain>` (existing
  `webhook` domain's `RestTemplate`-based `WebhookHttpClient` pattern) for
  the Java fixtures' `webhooktest` package.

## Running the proof

```bash
bash practices/consumer-proof/scenarios/S2.AUTHZ.FE/run-scenario-proof.sh
```

Exit 0 = both violating fixtures BLOCKED by their intended signature, both
clean fixtures scanned + PASS, cardinality gate satisfied (2/2 cases ran).

## Isolation

Everything lives under this scenario dir (`java/`, `react/`,
`scenario-guards/`). No file under `practices/rules/`,
`practices-react/eslint-plugin-ax/`, `backend/src/`, or `frontend/src/` is
read, written, or wired into `run-all-guards.sh` / R25 by this scenario.
