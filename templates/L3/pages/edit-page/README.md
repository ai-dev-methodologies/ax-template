# edit-page — L3 Generic Edit Form Template

Generic page skeleton for resource edit forms. Lives at `[id]/page.tsx` to
follow Next.js dynamic-route conventions. L4 resolves `params.id`, fetches
the resource, and passes initial values to the form slot.

## Slot contract

| Slot | Type | Required | Description |
|---|---|---|---|
| `title` | `string` | ✅ | Page heading (h1) |
| `description` | `string` | — | Subtitle below heading |
| `formSlot` | `ReactNode` | ✅ | Pre-populated form — L2 form block with initial values |
| `deleteSlot` | `ReactNode` | — | Destructive action area (rendered in a red "Danger Zone" box) |
| `cancelHref` | `string` | — | If provided, renders a Cancel link |
| `cancelLabel` | `string` | — | Cancel link label (default: `"Cancel"`) |

## Usage (L4 example)

```tsx
import EditPage from 'templates/L3/pages/edit-page/[id]/page'

interface PageProps { params: { id: string } }

export default async function EditProductPage({ params }: PageProps) {
  const product = await getProduct(params.id)
  return (
    <EditPage
      title={`Edit ${product.name}`}
      cancelHref={`/products/${params.id}`}
      formSlot={<ProductEditForm product={product} />}
      deleteSlot={<DeleteProductButton id={params.id} />}
    />
  )
}
```

## Layer dependencies

- **L1**: No direct imports
- **L2**: Receives L2 form blocks via `formSlot`; delete confirmation via `deleteSlot`
- **L4**: Resolves `params.id`, fetches resource, passes initial values to form
