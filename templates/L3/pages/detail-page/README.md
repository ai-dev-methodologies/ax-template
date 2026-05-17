# detail-page — L3 Generic Detail View Template

Generic page skeleton for single-resource detail views. The template lives at
`[id]/page.tsx` to follow Next.js dynamic-route conventions. L4 wraps it and
resolves the `id` param before passing content via slots.

## Slot contract

| Slot | Type | Required | Description |
|---|---|---|---|
| `title` | `string` | ✅ | Resource name — rendered as `<h1>` |
| `sectionsSlot` | `ReactNode` | ✅ | Main content: field groups, cards, tabs |
| `actionsSlot` | `ReactNode` | — | Action buttons (edit, delete, export…) in header |
| `backHref` | `string` | — | If provided, renders a `← Back` link |
| `backLabel` | `string` | — | Label for back link (default: `"Back"`) |

## Usage (L4 example)

```tsx
import DetailPage from 'templates/L3/pages/detail-page/[id]/page'

interface PageProps { params: { id: string } }

export default async function ProductDetailPage({ params }: PageProps) {
  const product = await getProduct(params.id)
  return (
    <DetailPage
      title={product.name}
      backHref="/products"
      actionsSlot={<ProductActions id={params.id} />}
      sectionsSlot={<ProductSections product={product} />}
    />
  )
}
```

## Layer dependencies

- **L1**: No direct imports
- **L2**: Receives L2 field-group / card blocks via `sectionsSlot` and `actionsSlot`
- **L4**: Resolves `params.id`, fetches data, composes L2 blocks
