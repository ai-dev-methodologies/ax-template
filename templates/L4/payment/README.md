# L4/payment — Payment Reference Workload

**Tenant model**: `single` — per [`specs/multi-tenant-l0.yaml#MULTI-TENANT-ISOLATION-DEFAULT-001`](../../../specs/multi-tenant-l0.yaml). This L4 reference workload ships as **single-tenant**. Recipes composing this domain into a multi-tenant SaaS (e.g. `b2b-admin` with `tenant_model: multi`) MUST adopt one of `MULTI-TENANT-ISOLATION-001` (Hibernate filter row-level) / `-002` (schema-per-tenant) / `-003` (AOP guard) plus `MULTI-TENANT-PROPAGATION-001` (request-scoped TenantContext) + `-002` (async propagation) before production. fork-receivers MUST NOT assume cross-tenant data isolation in this L4 as-shipped.

`templates/L4/payment/` is the **payment domain** reference workload for
**ax-template**. It demonstrates how to compose L1 shadcn primitives, L2
payment feature blocks, and L3 page templates into a payment vertical backed
by the `contracts/payment-openapi.yaml` Spring Boot API.

**Status (per [`docs/IMPLEMENTATION-STATUS.md`](../../../docs/IMPLEMENTATION-STATUS.md))**:
**impl** — backend Java reference workload ready at `backend/src/main/java/com/ax/template/authblueprint/payment/` (20+ files: state machine, ledger, refund, slow-provider decorator). Frontend layer here is a stub. Single-tenant by default. Korean PG (Toss/Kakao/Naver) adapter implementations are deferred to R17+ — current PaymentProvider interface is generic.

---

## What's included

| File | Purpose |
|---|---|
| `app/layout.tsx` | Root HTML shell + Providers (QueryClient, MSW) |
| `app/page.tsx` | Redirect → `/checkout` |
| `app/providers.tsx` | TanStack Query + MSW dev provider |
| `app/(payment)/layout.tsx` | Authenticated layout: AppShell + Sidebar + AppHeader |
| `app/(payment)/page.tsx` | Redirect → `/checkout` |
| `app/(payment)/checkout/page.tsx` | **CHECKOUT** — PaymentMethodPicker + PaymentCheckoutForm + IdempotencyKeyHandler + SlowProviderWarning |
| `app/(payment)/success/[orderId]/page.tsx` | **SUCCESS** — Receipt view; idempotent (safe for webhook redirects) |
| `app/(payment)/failure/[orderId]/page.tsx` | **FAILURE** — Error state + retry link to checkout |
| `app/(payment)/methods/page.tsx` | **LIST** — Payment history (DataTable + FilterBar + Pagination + EmptyState) |
| `app/(payment)/methods/new/page.tsx` | **ADD METHOD** — PaymentMethodPicker selects method type → /checkout |
| `app/(payment)/methods/[id]/page.tsx` | **DETAIL** — Full payment details with refund/void actions |
| `app/(payment)/refund/[orderId]/page.tsx` | **REFUND** — Request full or partial refund (owner-only) |

---

## Key UX policies

### Idempotency (PAYMENT-IDEMP-001)

`IdempotencyKeyHandler` manages the `Idempotency-Key` header lifecycle:

```tsx
<IdempotencyKeyHandler>
  {(idempotencyKey, regenerate) => (
    <PaymentCheckoutForm
      onSubmit={(values) => {
        await payAction({ ...values, idempotencyKey })
        regenerate() // fresh key for next submit
      }}
    />
  )}
</IdempotencyKeyHandler>
```

- Key is regenerated after each successful payment
- If the server returns `{ replayed: true }`, the checkout page shows
  an "already processed" banner — no new charge

### Slow provider warning (PAYMENT-PROVIDER-007)

`SlowProviderWarning` appears after 3 000 ms with `aria-live="polite"`:

```tsx
<SlowProviderWarning
  isLoading={mutation.isPending}
  thresholdMs={3000}
/>
```

### Webhook-redirect idempotency

Both `/success/[orderId]` and `/failure/[orderId]` are idempotent:
the same `orderId` always renders the same state. Safe when a payment
provider redirects the user back via GET after charging.

### IDOR protection (PAYMENT-AUTHZ-003)

`GET /api/payments/{id}` returns **404** (not 403) for cross-user access
to prevent enumeration of existing payment IDs.

---

## How to fork

### 1. Copy the directory into your project

```bash
cp -r templates/L4/payment/app <your-nextjs-project>/app
```

### 2. Install dependencies

```bash
npm install @tanstack/react-query msw
# or
pnpm add @tanstack/react-query msw
```

### 3. Configure path aliases

Add to `tsconfig.json`:

```json
{
  "compilerOptions": {
    "paths": {
      "templates/*": ["../../ax-template/templates/*"]
    }
  }
}
```

Or copy the L1/L2/L3 blocks you use into `src/components/` and update
the import paths inside the copied files.

### 4. Wire authentication

The backend API requires a Bearer JWT. Before copying, inject the auth
token via a Next.js route handler (BFF proxy) or an Axios/fetch interceptor.

### 5. Configure MSW handlers (development)

Create `src/mocks/browser.ts` and `src/mocks/handlers.ts` with mock
responses for `/api/payments`. See the
[MSW docs](https://mswjs.io/docs/getting-started) for setup.

### 6. Replace placeholder order data

The template uses a `ORDER-PLACEHOLDER` orderId and a hardcoded `amount`.
In a real fork:

- Read `orderId` from URL params or your cart/order state
- Read `amount` and `currency` from your order total
- Replace `paymentMethodToken` with the provider-derived token (see Step 7)

> **Note for redirect-style PGs (KG이니시스 / NICE페이먼츠 / KCP / Toss V1):**
> These providers do NOT return a client-side token synchronously. Instead
> they open a popup or full-page redirect, and POST a
> `{authToken, TID, signature, mid, amt, ...}` payload back to a callback
> URL you control. In Step 7, treat `paymentMethodToken` as the
> **server-derived TID** issued after callback signature verification,
> NOT as a client SDK return value.

### 7. Configure payment provider

The checkout form sends a `paymentMethodToken` to the backend. How that
token is obtained depends on the PG integration style:

**Tokenization-style (Stripe / Toss V2):**

1. Call the provider's client SDK in the browser (e.g. `Stripe.js
   createToken`) with the card data — card data never touches your backend.
2. Send the returned token (`tok_xxx`) directly to `createPayment` as
   `paymentMethodToken`.

**Redirect-style (KG이니시스 / NICE페이먼츠 / KCP / Toss V1):**

1. Your backend signs the order metadata (`amount`, `orderId`, merchant
   key) and returns the signed payload to the browser.
2. Browser invokes the PG's redirect/popup SDK (`INIStdPay.pay()`,
   `goPay()`, etc.) with the signed payload.
3. PG redirects the user to its hosted page, takes card input there,
   then POSTs `{authToken, TID, signature, ...}` to **your callback URL**.
4. Your backend verifies the callback signature using the PG's secret
   (KG이니시스 `SignatureKey`, NICE페이먼츠 `MerchantKey`). On signature
   failure, abort. Mismatched signature = forged callback.
5. With the verified `TID`, issue the authorize/capture request to the
   PG's REST API. The PG returns the final `TID` + approval metadata.
6. Pass the verified final `TID` as `paymentMethodToken` to
   `createPayment`. Pass any additional PG-specific fields
   (`P_TID`/`MOID`/`transactionDate` etc.) via the request's `metadata`
   map so the audit ledger captures the full PG trace.

Never send raw card numbers to your own backend regardless of style. For
redirect-style PGs, the catalog's `PaymentProvider` interface treats the
verified server-side TID as the canonical token — see the upcoming
`PaymentProvider` interface extension (deferred R18+) for callback-verify
+ multi-step authorize support.

---

## Spec Trio binding

| Artifact | File |
|---|---|
| Page Compliance Spec | `specs/payment-frontend-l0.yaml` |
| UI Contract | `contracts/payment-ui.yaml` |
| UI Policy Manifest | `blueprints/payment-ui-manifest.yaml` |
| Backend OpenAPI | `contracts/payment-openapi.yaml` |
| Backend Security Spec | `specs/payment-l0.yaml` |

Run `bash practices/evals/trio_integrity_guard.sh --domain payment` to verify
the full Spec Trio is intact.

---

## Verification

```bash
# Verify L4 payment composition contract (static analysis)
cd frontend && npx playwright test tests/L4/payment/

# Verify full domain spec trio
bash skills/ax-verify-domain/scripts/run.sh payment

# Verify L4 layer (auth + crud + payment all green)
bash skills/ax-verify-L4/scripts/run.sh
```

## Recipe Composition

applied_recipe: e-commerce
applied_recipes:
  - booking
  - e-commerce
  - marketplace
