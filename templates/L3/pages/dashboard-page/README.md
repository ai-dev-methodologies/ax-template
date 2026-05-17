# dashboard-page — L3 Generic Dashboard Skeleton Template

Generic page skeleton for dashboards. Accepts an array of widget slots and
renders them in a responsive CSS grid. No hard-coded widget types — L4 passes
the concrete stat cards, charts, and tables.

## Slot contract

| Slot | Type | Required | Description |
|---|---|---|---|
| `title` | `string` | — | Page heading (default: `"Dashboard"`) |
| `description` | `string` | — | Subtitle below heading |
| `widgetSlots` | `ReactNode[]` | ✅ | Widget nodes — stat cards, charts, tables. Pass `[]` for empty state. |
| `headerSlot` | `ReactNode` | — | Header area slot for date pickers or global filters |

## Grid behaviour

- **mobile**: 1 column
- **tablet (`sm`)**: 2 columns
- **desktop (`lg`)**: 3 columns
- **wide (`xl`)**: 4 columns

Pass widgets in the order they should appear in reading order.

## Usage (L4 example)

```tsx
import DashboardPage from 'templates/L3/pages/dashboard-page/page'

export default function AdminDashboard() {
  return (
    <DashboardPage
      title="Admin Dashboard"
      description="Overview of key metrics"
      headerSlot={<DateRangePicker />}
      widgetSlots={[
        <RevenueCard key="revenue" />,
        <OrdersCard key="orders" />,
        <ActiveUsersCard key="users" />,
        <ConversionChart key="conversion" />,
      ]}
    />
  )
}
```

## Layer dependencies

- **L1**: No direct imports
- **L2**: Receives L2 stat-card and chart blocks via `widgetSlots`
- **L4**: Provides widget data, time range selection, and real slot composition
