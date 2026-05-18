# audit-log-page — L3 Audit Log Page Template

Flat-tab page skeleton for audit-log surfaces. Three tabs: Events / Exports / Settings.
Uses header navigation tabs (not breadcrumbs — see SP34 §5.7 architecture note).
L4 supplies typed event data and filter configuration via named slot props.

## Slot contract

| Prop | Type | Required | Description |
|---|---|---|---|
| `title` | `string` | — | Page heading (default: `"Audit Log"`) |
| `description` | `string` | — | Page subtitle |
| `activeTab` | `AuditLogTab` | — | Controlled active tab (`events` \| `exports` \| `settings`). Typically driven by `searchParams.tab`. |
| `onTabChange` | `(tab) => void` | — | Tab change callback (push to URL search params in L4) |
| `eventsSlot` | `ReactNode` | — | Events tab: audit event table + filter bar |
| `exportsSlot` | `ReactNode` | — | Exports tab: export-job status list + download links |
| `settingsSlot` | `ReactNode` | — | Settings tab: retention policies, archival rules |
| `headerAction` | `ReactNode` | — | Header action (e.g. "Export" button) |

## Usage (L4 example)

```tsx
// app/admin/audit-log/page.tsx
import AuditLogPage, { AuditLogTab } from 'templates/L3/pages/audit-log-page/page'

export default function AuditLogRoute({ searchParams }) {
  const tab = (searchParams.tab ?? 'events') as AuditLogTab
  return (
    <AuditLogPage
      activeTab={tab}
      eventsSlot={<AuditEventTable />}
      exportsSlot={<ExportJobList />}
      settingsSlot={<RetentionSettings />}
      headerAction={<ExportButton />}
    />
  )
}
```

## Navigation architecture

Uses flat tab navigation (not breadcrumb hierarchy). Tabs are rendered as
`role="tablist"` + `role="tab"` + `role="tabpanel"` for WCAG 4.1.2 compliance.
Active tab state is caller-controlled to support URL-driven navigation.

## Layer dependencies

- **L1**: No direct imports
- **L2**: `filter-bar.tsx`, `data-table.tsx` (injected via `eventsSlot`)
- **L4**: Provides typed `AuditEvent[]`, filter config, and export job management
