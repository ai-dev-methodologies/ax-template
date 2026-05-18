# settings-overview — L3 Settings Overview Page Template

Generic settings overview skeleton. Renders a page with four named section slots
(account / security / notifications / billing) plus an optional extra-sections array.
Each visible slot is wrapped in a card with a title and border. Pure display — no
interaction logic. L4 fills in the section content.

## Slot contract

| Prop | Type | Required | Description |
|---|---|---|---|
| `title` | `string` | — | Page heading (default: `"Settings"`) |
| `description` | `string` | — | Page subtitle |
| `accountSlot` | `ReactNode` | — | Account / profile settings content |
| `securitySlot` | `ReactNode` | — | Security settings (password, MFA, sessions) |
| `notificationsSlot` | `ReactNode` | — | Notification preferences |
| `billingSlot` | `ReactNode` | — | Billing / subscription content |
| `sections` | `SettingsSection[]` | — | Extra sections beyond the four named ones |

`SettingsSection` shape: `{ id: string; title: string; description?: string; content: ReactNode }`

Named sections that receive a `null` / `undefined` slot are omitted from the render.

## Usage (L4 example)

```tsx
import SettingsOverviewPage from 'templates/L3/pages/settings-overview/page'

export default function SettingsRoute() {
  return (
    <SettingsOverviewPage
      accountSlot={<AccountSection />}
      securitySlot={<SecuritySection />}
      notificationsSlot={<NotificationsSection />}
      billingSlot={<BillingSection />}
    />
  )
}
```

## Layer dependencies

- **L1**: No direct imports (uses Tailwind utility classes)
- **L2**: Receives L2 blocks via named section slots
- **L4**: Provides concrete section components; fetches settings data server-side
