# create-page — L3 Generic Create Form Template

Generic page skeleton for resource creation forms. Constraint: the form slot
handles all validation, submission, and success redirect — this template
provides only layout and navigation structure.

## Slot contract

| Slot | Type | Required | Description |
|---|---|---|---|
| `title` | `string` | ✅ | Page heading (h1) |
| `description` | `string` | — | Subtitle below heading |
| `formSlot` | `ReactNode` | ✅ | Form content (L2 form block, react-hook-form, etc.) |
| `cancelHref` | `string` | — | If provided, renders a Cancel link in the header |
| `cancelLabel` | `string` | — | Cancel link label (default: `"Cancel"`) |

## Usage (L4 example)

```tsx
import CreatePage from 'templates/L3/pages/create-page/page'

export default function NewProductPage() {
  return (
    <CreatePage
      title="New Product"
      description="Add a product to your catalog"
      cancelHref="/products"
      formSlot={<ProductCreateForm />}
    />
  )
}
```

## Layer dependencies

- **L1**: No direct imports
- **L2**: Receives L2 form blocks via `formSlot`
- **L4**: Provides domain schema, submit handler, and success redirect
