# import-csv — L3 CSV Import Page Template

Generic CSV import flow skeleton. Three-phase flow: drop file → review preview →
click Import. Accepts the file drop zone and preview table as ReactNode slots from L4.
No CSV parsing or API upload logic — L4 handles those via `onImport`.

Uses L1 `FileDropzone` component (SP14) passed as a slot.

## Slot contract

| Prop | Type | Required | Description |
|---|---|---|---|
| `dropzoneSlot` | `ReactNode` | ✅ | File drop zone — L4 passes L1 `<FileDropzone />` with `onDrop` handler |
| `previewSlot` | `ReactNode` | — | CSV preview / column-mapping table (shown once a file is dropped) |
| `onImport` | `() => void \| Promise<void>` | — | Called when user clicks the Import button |
| `importLabel` | `string` | — | Import button label (default: `"Import"`) |
| `cancelHref` | `string` | — | Cancel link href |
| `title` | `string` | — | Page heading (default: `"Import CSV"`) |
| `description` | `string` | — | Optional subtitle |

## Flow

1. User drops a `.csv` file onto `dropzoneSlot`
2. L4 parses the CSV and updates `previewSlot` with a preview table
3. User reviews the mapping, then clicks **Import**
4. `onImport()` is awaited; button shows "Importing…" while pending
5. L4 handles success/error feedback (toast, redirect, etc.)

## Usage (L4 example)

```tsx
import ImportCsvPage from 'templates/L3/pages/import-csv/page'
import { FileDropzone } from 'templates/L1/components/file-dropzone'

export default function ProductImportRoute() {
  const [preview, setPreview] = React.useState<React.ReactNode>(null)

  async function handleDrop(files: File[]) {
    const rows = await parseCsv(files[0])
    setPreview(<PreviewTable rows={rows} />)
  }

  async function handleImport() {
    await api.post('/products/import', { ... })
    router.push('/products')
  }

  return (
    <ImportCsvPage
      dropzoneSlot={<FileDropzone onDrop={handleDrop} accept={{ 'text/csv': ['.csv'] }} />}
      previewSlot={preview}
      onImport={handleImport}
      cancelHref="/products"
    />
  )
}
```

## Layer dependencies

- **L1**: Accepts L1 `FileDropzone` via `dropzoneSlot` (SP14)
- **L2**: Receives preview table via `previewSlot`
- **L4**: Owns file parsing, column mapping state, and API upload
