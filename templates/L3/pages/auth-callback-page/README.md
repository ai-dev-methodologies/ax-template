# auth-callback-page — L3 OAuth / Email-Verify Callback Skeleton

Generic page template for OAuth provider callbacks and email-verification landing
pages. Renders loading, success, or failure UI based on a `status` prop. All
token exchange and verification logic lives in L4.

## Slot contract

| Slot | Type | Required | Description |
|---|---|---|---|
| `provider` | `string` | ✅ | Identity provider label ("Google", "Kakao", "Naver", "Email") |
| `status` | `'loading' \| 'success' \| 'failure'` | ✅ | Current callback state |
| `successHref` | `string` | ✅ | Href for the "Continue" link (success state) |
| `failureHref` | `string` | ✅ | Href for the "Try again" link (failure state) |
| `statusSlot` | `ReactNode` | — | Overrides all built-in status rendering |
| `successLabel` | `string` | — | Success CTA label (default: `"Continue"`) |
| `failureLabel` | `string` | — | Failure CTA label (default: `"Try again"`) |

## Status state machine

```
initial → loading → success → (redirect via successHref)
                 ↘ failure → (link to failureHref)
```

The L4 caller drives state transitions. This template is purely presentational.

## Usage (L4 example)

```tsx
// L4: app/(auth)/callback/google/page.tsx
import AuthCallbackPage, { type CallbackStatus } from 'templates/L3/pages/auth-callback-page/page'
import { exchangeGoogleCode } from '@/lib/auth/google'

interface PageProps { searchParams: { code?: string; error?: string } }

export default async function GoogleCallbackPage({ searchParams }: PageProps) {
  let status: CallbackStatus = 'loading'
  if (searchParams.error) status = 'failure'
  else if (searchParams.code) {
    const ok = await exchangeGoogleCode(searchParams.code)
    status = ok ? 'success' : 'failure'
  }
  return (
    <AuthCallbackPage
      provider="Google"
      status={status}
      successHref="/dashboard"
      failureHref="/login"
    />
  )
}
```

## Accessibility

- Loading state: `aria-live="polite"` + `aria-busy="true"` on the status region
- Failure state: `role="alert"` + `aria-live="assertive"` for screen reader announcement

## Layer dependencies

- **L1**: No direct imports
- **L2**: No L2 blocks (callback page is single-focus, no feature blocks needed)
- **L4**: Resolves OAuth code, sets `status` prop, provides provider label
