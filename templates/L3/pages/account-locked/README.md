# account-locked — L3 Account Locked Page Template

Generic account-locked informational skeleton. Shows the lock reason and unlock
instructions (admin-contact or time-based). Purely display — no interactive form.
L4 provides context-specific lock information via props.

## Slot contract

| Prop | Type | Required | Description |
|---|---|---|---|
| `reason` | `string` | — | Short lock reason (default: "Too many failed sign-in attempts.") |
| `unlockAt` | `string` | — | ISO-8601 timestamp for time-based unlock |
| `adminContact` | `string` | — | Admin email or URL for manual unlock |
| `loginHref` | `string` | — | Sign-in link href |
| `customSlot` | `ReactNode` | — | Overrides built-in reason/instructions block |

## Behaviour

- Shows a lock icon with reason, optional unlock time, and admin contact
- `unlockAt` is formatted via `toLocaleString` in the browser locale
- `adminContact` renders as `mailto:` link when it's an email, `<a href>` when it's a URL
- `customSlot` fully replaces the built-in info block when provided

## Usage (L4 example)

```tsx
import AccountLockedPage from 'templates/L3/pages/account-locked/page'

export default async function LockedRoute({ searchParams }) {
  const lock = await getLockInfo(session.userId)
  return (
    <AccountLockedPage
      reason={lock.reason}
      unlockAt={lock.unlockAt?.toISOString()}
      adminContact="support@example.com"
      loginHref="/login"
    />
  )
}
```

## Layer dependencies

- **L1**: No direct imports (uses Tailwind utility classes)
- **L2**: No L2 blocks imported directly
- **L4**: Provides lock context (reason, unlockAt, adminContact) from backend
