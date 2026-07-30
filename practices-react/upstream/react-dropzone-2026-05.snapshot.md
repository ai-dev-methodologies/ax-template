# react-dropzone API — Frozen Snapshot + useDropzone Refresh

**Source URL(s):** https://react-dropzone.js.org/ (original 2026-05 fetch, preserved below); https://react-dropzone.js.org/guide/getting-started (2026-07-30 refresh)
**HTTP status:** 200 (both URLs)
**Fetched at:** 2026-07-30T00:51:30Z
**Extractor invocation:** `practices/scripts/snapshot-extract.sh https://react-dropzone.js.org/guide/getting-started`
**Body SHA-256 (below the `---` divider, header excluded):** 7a87a1520c1bfc16c18c6c85cc2c286099c094c10ce9dedd292d847f2c7c5d7b

---

# react-dropzone API — Frozen Snapshot 2026-05

Source: https://react-dropzone.js.org/  
Package: `react-dropzone@^14`  
Fetched: 2026-05-18  
Purpose: Evidence anchor for `templates/L1/components/file-dropzone.tsx`

## Installation

```bash
npm install react-dropzone
```

## useDropzone Hook

Primary API for file drag-and-drop. Returns props spreaders and state flags.

```typescript
import { useDropzone } from 'react-dropzone'

const {
  getRootProps,    // props for the drop-zone container element
  getInputProps,   // props for the hidden <input type="file">
  isDragActive,    // boolean: a file is currently being dragged over
  isDragAccept,    // boolean: all dragged files are accepted
  isDragReject,    // boolean: some dragged files would be rejected
  acceptedFiles,   // File[]: files accepted so far
  fileRejections,  // FileRejection[]: files rejected with reasons
  open,            // () => void: programmatically open the file picker
} = useDropzone({
  onDrop: (acceptedFiles: File[]) => { /* ... */ },
  accept: { 'image/*': ['.jpeg', '.png', '.gif', '.webp'] },
  maxSize: 5 * 1024 * 1024,  // 5 MB
  maxFiles: 10,
  disabled: false,
  multiple: true,
  noClick: false,
  noDrag: false,
})
```

## accept Prop (MIME-type map)

```typescript
// ✅ Correct: MIME type → extensions[]
accept: {
  'image/*': ['.png', '.jpg', '.jpeg', '.gif'],
  'application/pdf': ['.pdf'],
}
// ❌ Wrong: flat string array (v11+ changed to MIME map)
```

## FileRejection Shape

```typescript
interface FileRejection {
  file: File
  errors: Array<{
    code: 'file-too-large' | 'file-invalid-type' | 'too-many-files' | 'file-too-small'
    message: string
  }>
}
```

## Accessibility

- The dropzone container renders as a `<div>` with `role="presentation"` by default.
- The hidden `<input>` receives focus and keyboard events.
- `noClick` and `noKeyboard` are available to customize interaction surface.

## Basic Example

```tsx
function MyDropzone() {
  const { getRootProps, getInputProps, isDragActive } = useDropzone({
    onDrop: (acceptedFiles) => console.log(acceptedFiles),
    accept: { 'image/*': ['.jpeg', '.png'] },
    maxSize: 5_242_880,
  })

  return (
    <div {...getRootProps()}>
      <input {...getInputProps()} />
      {isDragActive ? <p>Drop here…</p> : <p>Drag and drop, or click to select</p>}
    </div>
  )
}
```

## useDropzone (2026-07 refresh)

Source: https://react-dropzone.js.org/guide/getting-started (curl+snapshot-extract.sh; the doc site moved from a single-page overview to
a multi-page "Guide" — the root `https://react-dropzone.js.org/` is now a landing shell whose
useDropzone reference content lives at this Guide URL)

react-dropzone is a set of React hooks and components for creating a drag 'n' drop zone for files. Use the `useDropzone` hook to bind the necessary handlers to any element — it returns
`getRootProps()`/`getInputProps()` prop-spreaders plus state flags such as `isDragActive`.
