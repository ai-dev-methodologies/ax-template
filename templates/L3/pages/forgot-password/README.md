# forgot-password — L3 Forgot Password Page Template

Generic forgot-password skeleton. Renders an email input form with a submit button.
On success, displays a "check your email" confirmation state. No domain-specific
API calls — the L4 layer wires the submission logic via `onSubmit`.

Integrates with the email-outbox backend template (SP19).

## Slot contract

| Prop | Type | Required | Description |
|---|---|---|---|
| `onSubmit` | `(email: string) => void \| Promise<void>` | — | Called with the email address on submit |
| `loginHref` | `string` | ✅ | Back-to-sign-in link href |
| `successSlot` | `ReactNode` | — | Overrides built-in success confirmation state |
| `description` | `string` | — | Subtitle below the heading (default: "Enter your email…") |

## Behaviour

1. User types email → clicks "Send reset link"
2. `onSubmit(email)` is awaited; submit button shows "Sending…" while pending
3. On resolve: switches to confirmation state ("Check your email")
4. `successSlot` overrides the built-in confirmation if provided

## Usage (L4 example)

```tsx
import ForgotPasswordPage from 'templates/L3/pages/forgot-password/page'

export default function ForgotPasswordRoute() {
  async function requestReset(email: string) {
    await fetch('/api/auth/forgot-password', {
      method: 'POST',
      body: JSON.stringify({ email }),
    })
  }
  return <ForgotPasswordPage onSubmit={requestReset} loginHref="/login" />
}
```

## Layer dependencies

- **L1**: No direct imports (uses Tailwind utility classes)
- **L2**: No L2 blocks imported directly; L4 may pass L2 blocks via `successSlot`
- **L4**: Provides `onSubmit` API call; wires email-outbox backend
