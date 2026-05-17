# list-page — L3 Generic List View Template

Generic page skeleton for list/table views. Parameterized entirely via slot props.
No domain-specific data fetching or routing logic.

## Slot contract

| Slot | Type | Required | Description |
|---|---|---|---|
| `title` | `string` | ✅ | Page heading (h1) |
| `description` | `string` | — | Subtitle below heading |
| `listSlot` | `ReactNode` | ✅ | Main list or table content |
| `filterSlot` | `ReactNode` | — | Filter bar / search input area (rendered above list) |
| `paginationSlot` | `ReactNode` | — | Pagination controls (rendered below list) |
| `createHref` | `string` | — | If provided, renders a "Create" anchor button in the header |
| `createLabel` | `string` | — | Label for create anchor (default: `"Create"`) |

## Usage (L4 example)

```tsx
import ListPage from 'templates/L3/pages/list-page/page'

export default function ProductsPage() {
  return (
    <ListPage
      title="Products"
      description="Manage your product catalog"
      filterSlot={<ProductFilter />}
      listSlot={<ProductTable />}
      paginationSlot={<Pagination total={100} page={1} />}
      createHref="/products/new"
    />
  )
}
```

## Layer dependencies

- **L1**: No direct imports (styling uses Tailwind utility classes)
- **L2**: Receives L2 blocks via `filterSlot`, `listSlot`, `paginationSlot` props
- **L4**: Provides concrete data, column definitions, and action handlers
