/*
---
template_id: L3/pages/import-csv
layer: L3
provenance_class: internal_design
evidence:
  - source_type: external
    citation: "Next.js 16 App Router file conventions — page.tsx"
    url: "https://nextjs.org/docs/app/building-your-application/routing/pages"
  - source_type: external
    citation: "Web Accessibility Initiative — File Upload guidance"
    url: "https://www.w3.org/WAI/WCAG21/Techniques/html/H37"
  - source_type: internal_design
    rationale: "Generic CSV import skeleton. Accepts dropzoneSlot (L1 file-dropzone), previewSlot (optional preview table), and an onImport callback. Three-phase flow: drop → preview → import. L4 owns file parsing and API upload."
imports_from: [L1, L2]
imports_forbidden: [L4]
---
*/
'use client'

import * as React from 'react'

/**
 * ImportCsvPage — generic CSV import flow skeleton.
 *
 * Slot props:
 *   - dropzoneSlot  (required) file drop zone (L4 passes L1 FileDropzone from SP14)
 *   - previewSlot   (optional) CSV preview / column-mapping table
 *   - onImport      (optional) called when user clicks "Import"
 *   - importLabel   (optional) Import button label (default: "Import")
 *   - cancelHref    (optional) Cancel link href
 *   - title         (optional) page heading (default: "Import CSV")
 *   - description   (optional) page subtitle
 *
 * Flow:
 *   1. User drops file → L4 parses CSV → passes previewSlot
 *   2. User reviews mapping → clicks "Import" → onImport() awaited
 *   3. Success / error feedback handled by L4 (toast, redirect, etc.)
 *
 * L4 usage:
 *   import ImportCsvPage from 'templates/L3/pages/import-csv/page'
 *   import { FileDropzone } from 'templates/L1/components/file-dropzone'
 *   export default function ProductImportRoute() {
 *     const [preview, setPreview] = React.useState<ReactNode>(null)
 *     async function handleDrop(files: File[]) {
 *       const rows = await parseCsv(files[0])
 *       setPreview(<PreviewTable rows={rows} />)
 *     }
 *     async function handleImport() {
 *       await api.post('/products/import', { ... })
 *       router.push('/products')
 *     }
 *     return (
 *       <ImportCsvPage
 *         dropzoneSlot={<FileDropzone onDrop={handleDrop} accept=".csv" />}
 *         previewSlot={preview}
 *         onImport={handleImport}
 *         cancelHref="/products"
 *       />
 *     )
 *   }
 */
export interface ImportCsvPageProps {
  /** File drop zone — L4 passes L1 FileDropzone with onDrop handler */
  dropzoneSlot: React.ReactNode
  /** CSV preview / column mapping table (optional, shown when file is dropped) */
  previewSlot?: React.ReactNode
  /** Called when the user clicks the Import button */
  onImport?: () => void | Promise<void>
  /** Import button label (default: "Import") */
  importLabel?: string
  /** Cancel link href */
  cancelHref?: string
  /** Page heading (default: "Import CSV") */
  title?: string
  /** Optional subtitle */
  description?: string
}

export default function ImportCsvPage({
  dropzoneSlot,
  previewSlot,
  onImport,
  importLabel = 'Import',
  cancelHref,
  title = 'Import CSV',
  description = 'Drop a CSV file below, review the preview, then click Import.',
}: ImportCsvPageProps) {
  const [isPending, setIsPending] = React.useState(false)

  async function handleImport() {
    setIsPending(true)
    try {
      await onImport?.()
    } finally {
      setIsPending(false)
    }
  }

  return (
    <main className="container mx-auto px-4 py-8 max-w-3xl space-y-6">
      {/* Header */}
      <div className="flex items-start justify-between gap-4">
        <div className="space-y-1">
          <h1 className="text-2xl font-semibold tracking-tight">{title}</h1>
          {description && (
            <p className="text-sm text-muted-foreground">{description}</p>
          )}
        </div>
        {cancelHref && (
          <a
            href={cancelHref}
            className="text-sm text-muted-foreground hover:text-foreground transition-colors"
          >
            Cancel
          </a>
        )}
      </div>

      {/* Step 1: Dropzone */}
      <div className="space-y-2">
        <h2 className="text-sm font-medium">1. Select file</h2>
        <div className="w-full">{dropzoneSlot}</div>
      </div>

      {/* Step 2: Preview (rendered when L4 passes a previewSlot) */}
      {previewSlot && (
        <div className="space-y-2">
          <h2 className="text-sm font-medium">2. Preview &amp; mapping</h2>
          <div className="rounded-lg border overflow-hidden">{previewSlot}</div>
        </div>
      )}

      {/* Step 3: Import action */}
      <div className="flex items-center gap-3 border-t pt-4">
        <button
          type="button"
          onClick={handleImport}
          disabled={isPending}
          className="inline-flex items-center rounded-md bg-primary px-6 py-2 text-sm font-medium text-primary-foreground hover:opacity-90 transition-opacity disabled:pointer-events-none disabled:opacity-50"
        >
          {isPending ? 'Importing…' : importLabel}
        </button>
        {cancelHref && (
          <a
            href={cancelHref}
            className="text-sm text-muted-foreground hover:text-foreground transition-colors"
          >
            Cancel
          </a>
        )}
      </div>
    </main>
  )
}
