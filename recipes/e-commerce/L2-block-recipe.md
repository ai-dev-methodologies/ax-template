# L2 Block Selection — e-commerce

> Which existing L2 blocks to use and in what composition order.

## Block Inventory

All blocks listed here already exist at `templates/L2/blocks/`. No new L2 blocks are introduced by this recipe.

| Block | File | Usage in recipe | L3 page |
|---|---|---|---|
| `crud-list-adapter` | `crud-list-adapter.tsx` | Product list with pagination | `list-page` (products) |
| `crud-create-form` | `crud-create-form.tsx` | New product / new order form | `create-page` |
| `data-table` | `data-table.tsx` | Order management table with sort/filter | `list-page` (orders) |
| `filter-bar` | `filter-bar.tsx` | Top-level product/order filter inputs | `list-page`, `search-results-page` |
| `faceted-filter` | `faceted-filter.tsx` | Category / price / availability facets | `search-results-page` |
| `kpi-card` | `kpi-card.tsx` | Revenue, order count, conversion rate KPIs | `admin` or `dashboard` |
| `event-stream` | `event-stream.tsx` | Real-time order status updates | `detail-page` (order) |

## Composition Order

```
search-results-page
  ├── filter-bar          ← keyword + price range input
  └── faceted-filter      ← category / availability checkboxes

list-page (products)
  ├── filter-bar          ← text search + date filter
  └── crud-list-adapter   ← paginated product rows

list-page (orders)
  ├── filter-bar          ← status / date range
  └── data-table          ← sortable order rows with status badges

create-page (product)
  └── crud-create-form    ← product fields (title, price, inventory)

detail-page (order)
  └── event-stream        ← live status (PENDING → CONFIRMED → SHIPPED)

edit-page (product)
  └── crud-edit-form      ← (use existing crud-edit-form.tsx)
```

## Notes

- `event-stream` connects to `GET /api/orders/{id}/stream` (SSE endpoint from notification L4).
- `faceted-filter` works alongside `filter-bar`; they share a shared filter state object passed down.
- `kpi-card` is optional in base scaffold; instantiate when an analytics/admin panel is added.
