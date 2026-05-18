# reset-password — L3 Reset Password Page Template

Generic reset-password skeleton. Accepts `token` as a prop (extracted by the L4 caller
from Next.js dynamic route params `[token]`). Renders new-password + confirm-password
inputs, validates they match client-side, then calls `onSubmit` with the new password.

## Slot contract

| Prop | Type | Required | Description |
|---|---|---|---|
| `token` | `string` | ✅ | Reset token (L4 extracts from route params) |
| `onSubmit` | `(password: string) => void \| Promise<void>` | — | Called with new password on submit |
| `loginHref` | `string` | ✅ | Back-to-sign-in link href |
| `successSlot` | `ReactNode` | — | Overrides built-in success confirmation state |

## Behaviour

1. User types new password + confirm password → clicks "Update password"
2. Client-side match check; if mismatch, shows error without calling `onSubmit`
3. `onSubmit(password)` is awaited; submit button shows "Updating…" while pending
4. On resolve: switches to success state ("Password updated")

## Usage (L4 example)

```tsx
import ResetPasswordPage from 'templates/L3/pages/reset-password/[token]/page'

export default async function ResetPasswordRoute({
  params,
}: {
  params: { token: string }
}) {
  async function doReset(password: string) {
    await fetch('/api/auth/reset-password', {
      method: 'POST',
      body: JSON.stringify({ token: params.token, password }),
    })
  }
  return <ResetPasswordPage token={params.token} onSubmit={doReset} loginHref="/login" />
}
```

## Layer dependencies

- **L1**: No direct imports (uses Tailwind utility classes)
- **L2**: No L2 blocks imported directly
- **L4**: Extracts token from route params; provides `onSubmit` API call
