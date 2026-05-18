# L2 Block Selection — marketplace

> Which existing L2 blocks to use and in what composition order.

## Block Inventory

All blocks listed here already exist at `templates/L2/blocks/`. No new L2 blocks are introduced by this recipe.

| Block | File | Usage in recipe | L3 page |
|---|---|---|---|
| `crud-list-adapter` | `crud-list-adapter.tsx` | Listing list with pagination | `list-page` (listings) |
| `crud-create-form` | `crud-create-form.tsx` | New listing form | `create-page` |
| `crud-edit-form` | `crud-edit-form.tsx` | Edit listing (price, description) | `edit-page` |
| `data-table` | `data-table.tsx` | Order management table with status | `list-page` (orders) |
| `filter-bar` | `filter-bar.tsx` | Keyword + price range input | `search-results-page`, `list-page` |
| `faceted-filter` | `faceted-filter.tsx` | Category / condition / location facets | `search-results-page` |
| `search-input` | `search-input.tsx` | Top-bar listing search | global nav |
| `search-palette` | `search-palette.tsx` | Quick-search overlay with recent / suggested | global nav |
| `payment-checkout-form` | `payment-checkout-form.tsx` | Buyer payment checkout (escrow capture) | `detail-page` (listing) |
| `payment-method-picker` | `payment-method-picker.tsx` | Card / bank transfer for escrow | `detail-page` |
| `notification-list` | `notification-list.tsx` | Bid alerts, order status updates | `dashboard-page` |
| `notification-bell` | `notification-bell.tsx` | Unread notification count badge | global nav |
| `confirm-dialog` | `confirm-dialog.tsx` | Confirm purchase / release escrow / open dispute | `detail-page` |
| `feature-flag-toggle` | `feature-flag-toggle.tsx` | Admin: KYC threshold flag toggle | `settings-overview` (admin) |
| `feature-gate` | `feature-gate.tsx` | Client-side KYC gate on high-value listing creation | `create-page` |
| `kpi-card` | `kpi-card.tsx` | GMV, active listings, dispute rate | `dashboard-page` |

## Composition Order

```
search-results-page
  ├── search-input          ← keyword entry
  ├── filter-bar            ← price range + condition filter
  └── faceted-filter        ← category / location checkboxes

list-page (listings)
  ├── filter-bar            ← text + date listed filter
  └── crud-list-adapter     ← paginated listing cards

list-page (orders)
  ├── filter-bar            ← status / date filter
  └── data-table            ← order rows with escrow status badge

create-page (listing)
  ├── feature-gate          ← KYC check if above threshold
  └── crud-create-form      ← title, price, category, images

edit-page (listing)
  └── crud-edit-form        ← update price / description

detail-page (listing)
  ├── payment-method-picker ← buyer selects payment method
  ├── payment-checkout-form ← escrow capture (POST /api/orders)
  └── confirm-dialog        ← confirm purchase / release / dispute

dashboard-page
  ├── kpi-card × 3          ← GMV / active listings / dispute rate
  ├── notification-list     ← recent bid / order events
  └── notification-bell     ← unread count (global nav)
```

## Notes

- `feature-gate` wraps `crud-create-form`; blocks high-value listing creation until IDV check passes.
- `search-palette` triggers on keyboard shortcut (⌘K); shares search state with `search-input`.
- `confirm-dialog` is reused for three actions: buy-now purchase, escrow release, dispute open.
- `payment-checkout-form` sends to platform's Stripe Connect capture endpoint; seller payout handled server-side.
