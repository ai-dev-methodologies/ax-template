---
snapshot_id: react-dropzone-2026-05
source: "https://react-dropzone.js.org/"
fetched_at: "2026-05-18T00:00:00Z"
via: WebFetch
bytes: 1620
sha: "b2c3d4e5f6a7b8c9d0e1f2a3b4c5d6e7f8a9b0c1d2e3f4a5b6c7d8e9f0a1b2c3"
tier: 3
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
