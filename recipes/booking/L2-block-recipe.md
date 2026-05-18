# L2 Block Selection — booking

> Which existing L2 blocks to use and in what composition order.

## Block Inventory

All blocks listed here already exist at `templates/L2/blocks/`. No new L2 blocks are introduced by this recipe.

| Block | File | Usage in recipe | L3 page |
|---|---|---|---|
| `calendar` | `calendar.tsx` (L1) | Resource availability view; date selection | `detail-page` (resource) |
| `date-range-picker` | `date-range-picker.tsx` (L1) | Check-in / check-out date selection | `create-page` (reservation) |
| `crud-create-form` | `crud-create-form.tsx` | New reservation form | `create-page` |
| `crud-edit-form` | `crud-edit-form.tsx` | Edit reservation (modify dates, guest count) | `edit-page` |
| `crud-list-adapter` | `crud-list-adapter.tsx` | Reservation list with pagination | `list-page` |
| `data-table` | `data-table.tsx` | Resource management table | `list-page` (admin) |
| `confirm-dialog` | `confirm-dialog.tsx` | Cancellation confirmation with fee warning | `detail-page` |
| `notification-list` | `notification-list.tsx` | Booking confirmation / cancellation alerts | `list-page`, `dashboard-page` |
| `payment-checkout-form` | `payment-checkout-form.tsx` | Deposit payment at reservation creation | `create-page` |
| `payment-method-picker` | `payment-method-picker.tsx` | Card / bank transfer selection for deposit | `create-page` |
| `kpi-card` | `kpi-card.tsx` | Occupancy rate, revenue, cancellation rate | `dashboard-page` |
| `relative-time` | `relative-time.tsx` (L1) | "Cancels in 2h" free-window countdown | `detail-page` |

## Composition Order

```
list-page (reservations)
  └── crud-list-adapter    ← paginated reservation rows with status badge

list-page (resources — admin)
  └── data-table           ← resource rows with availability toggle

create-page (reservation)
  ├── date-range-picker    ← check-in / check-out selection
  ├── crud-create-form     ← guest count, special requests
  └── payment-checkout-form ← deposit capture

edit-page (reservation)
  └── crud-edit-form       ← modify dates / guest count (pre-confirmation only)

detail-page (reservation)
  ├── calendar             ← resource availability overlay
  ├── confirm-dialog       ← cancellation with free-window fee warning
  └── relative-time        ← free-cancellation countdown

dashboard-page
  ├── kpi-card × 3         ← occupancy rate / revenue / cancellation rate
  └── notification-list    ← recent booking events
```

## Notes

- `date-range-picker` and `calendar` share a date-state object; pass via shared context.
- `confirm-dialog` shows fee amount from feature-flag `booking.cancellation_policy.variant`.
- `relative-time` reads `reservation.free_cancellation_until` from the API.
- `payment-checkout-form` calls `POST /api/reservations` with embedded payment intent; atomicity in backend.
