# admin-overview-page — L3 Admin Overview Page Template

Flat-tab admin dashboard with KPI summary row and four named tab panels:
Overview / Users / System / Activity. Uses flat tab/header navigation
(no breadcrumb dependency — SP34 §5.7 architecture note). Banner slot at the top
accepts `ImpersonationBanner`, `MaintenanceNotice`, or `NetworkStatusPill`.

## Slot contract

| Prop | Type | Required | Description |
|---|---|---|---|
| `title` | `string` | — | Page heading (default: `"Administration"`) |
| `activeTab` | `AdminTab` | — | Controlled active tab (`overview` \| `users` \| `system` \| `activity`) |
| `onTabChange` | `(tab) => void` | — | Tab change callback (push to URL search params) |
| `kpiCards` | `AdminKpiCard[]` | — | KPI summary row (shown in overview tab) |
| `overviewSlot` | `ReactNode` | — | Dashboard summary content |
| `usersSlot` | `ReactNode` | — | User management table + invite flow |
| `systemSlot` | `ReactNode` | — | System health, background jobs, feature-flags |
| `activitySlot` | `ReactNode` | — | Audit event feed, impersonation log |
| `bannerSlot` | `ReactNode` | — | Top banner (ImpersonationBanner, MaintenanceNotice) |
| `headerAction` | `ReactNode` | — | Header action button (e.g. "Invite user") |

## AdminKpiCard shape

```ts
interface AdminKpiCard {
  label: string
  value: string        // formatted string: "12,345"
  delta?: string       // "+3%"
  deltaPositive?: boolean
  icon?: string        // emoji or URL
}
```

## Usage (L4 example)

```tsx
// app/admin/page.tsx
import AdminOverviewPage, { AdminTab } from 'templates/L3/pages/admin-overview-page/page'
import ImpersonationBanner from 'templates/L2/blocks/impersonation-banner'

export default async function AdminRoute({ searchParams }) {
  const session = await getAdminSession()
  const tab = (searchParams.tab ?? 'overview') as AdminTab

  return (
    <AdminOverviewPage
      activeTab={tab}
      bannerSlot={
        <ImpersonationBanner session={session} onEndImpersonation={endImpersonation} />
      }
      kpiCards={[
        { label: 'Total users', value: '12,345', delta: '+3%', deltaPositive: true, icon: '👥' },
        { label: 'Active sessions', value: '892', icon: '🔐' },
      ]}
      overviewSlot={<DashboardSummary />}
      usersSlot={<UserManagementTable />}
      systemSlot={<SystemHealthPanel />}
      activitySlot={<ActivityFeed events={auditEvents} />}
    />
  )
}
```

## Layer dependencies

- **L1**: No direct imports
- **L2**: `impersonation-banner.tsx`, `activity-feed.tsx`, `maintenance-notice.tsx` (via slot props)
- **L4**: Provides domain data, session, and admin actions
