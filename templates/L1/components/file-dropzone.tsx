/*
---
template_id: L1/components/file-dropzone
layer: L1
provenance_class: external_canonical
evidence:
  - source_type: upstream_id
    upstream_id: react-dropzone-2026-05
    section: useDropzone
    quote: "The primary API for integrating drag-and-drop functionality into React components."
  - source_type: external
    citation: "WCAG 2.2 SC 2.1.1 Keyboard (Level A) — full normative text, W3C Recommendation 2023-10-05"
    url: "https://www.w3.org/TR/WCAG22/#keyboard"
    quote: "All functionality of the content is operable through a keyboard interface without requiring specific timings for individual keystrokes, except where the underlying function requires input that depends on the path of the user's movement and not just the endpoints."
    quoted_at: "2026-07-29"
a11y_criteria:
  - "WCAG 2.2 SC 2.1.1 — keyboard accessible; space/enter opens file picker"
  - "WCAG 2.2 SC 4.1.2 — role='button' with aria-label on drop zone"
  - "WCAG 2.2 SC 1.4.1 — drag-active state uses --color-accent-subtle border, not color alone"
dependencies: ["react-dropzone@^14"]
drift_snapshot_ref: "practices-react/upstream/shadcn-registry-2026-05.snapshot.md#file-dropzone"
---
*/
import * as React from 'react'
import { useDropzone, type FileRejection, type Accept } from 'react-dropzone'
import { UploadCloud, X, FileText } from 'lucide-react'
import { cn } from '../lib/utils'

export interface FileDropzoneProps {
  /** MIME type map for accepted files — e.g. { 'image/*': ['.png', '.jpg'] } */
  accept?: Accept
  /** Maximum file size in bytes (default: 10 MB) */
  maxSize?: number
  /** Maximum number of files (default: unlimited) */
  maxFiles?: number
  /** Allow multiple file selection (default: true) */
  multiple?: boolean
  /** Currently uploaded files (controlled) */
  value?: File[]
  /** Called when accepted files change */
  onChange?: (files: File[]) => void
  /** Called when files are rejected (validation failures) */
  onReject?: (rejections: FileRejection[]) => void
  /** Disables the dropzone */
  disabled?: boolean
  /** Additional className for the dropzone container */
  className?: string
  /** Hint text displayed inside dropzone (below icon) */
  hint?: string
}

function formatBytes(bytes: number): string {
  if (bytes < 1024) return `${bytes} B`
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`
}

/**
 * FileDropzone — react-dropzone wrapper with shadcn/design-token styling.
 *
 * Features:
 * - Drag-and-drop + click-to-upload
 * - Preview list of accepted files with remove button
 * - File size + type validation via react-dropzone
 * - Rejection feedback shown inline
 */
export function FileDropzone({
  accept,
  maxSize = 10 * 1024 * 1024,
  maxFiles,
  multiple = true,
  value = [],
  onChange,
  onReject,
  disabled = false,
  className,
  hint,
}: FileDropzoneProps) {
  const handleDrop = React.useCallback(
    (acceptedFiles: File[], rejections: FileRejection[]) => {
      if (acceptedFiles.length > 0) {
        onChange?.(multiple ? [...value, ...acceptedFiles] : acceptedFiles)
      }
      if (rejections.length > 0) {
        onReject?.(rejections)
      }
    },
    [multiple, onChange, onReject, value]
  )

  const { getRootProps, getInputProps, isDragActive, isDragAccept, isDragReject } = useDropzone({
    accept,
    maxSize,
    maxFiles,
    multiple,
    disabled,
    onDrop: handleDrop,
  })

  const removeFile = React.useCallback(
    (index: number) => {
      onChange?.(value.filter((_, i) => i !== index))
    },
    [onChange, value]
  )

  const defaultHint = React.useMemo(() => {
    const sizePart = `최대 ${formatBytes(maxSize)}`
    if (accept) {
      const exts = Object.values(accept).flat().join(', ')
      return `${exts} · ${sizePart}`
    }
    return sizePart
  }, [accept, maxSize])

  return (
    <div className="space-y-[--space-2]">
      <div
        {...getRootProps()}
        className={cn(
          'flex flex-col items-center justify-center rounded-[--radius-lg]',
          'border-2 border-dashed border-[--color-border]',
          'p-[--space-8] text-center',
          'transition-colors duration-[--duration-fast]',
          'cursor-pointer hover:border-[--color-accent] hover:bg-[--color-surface-subtle]',
          'focus-visible:outline-none focus-visible:ring-2',
          'focus-visible:ring-[--color-focus-ring] focus-visible:ring-offset-2',
          isDragActive && !isDragReject && 'border-[--color-accent] bg-[--color-surface-subtle]',
          isDragAccept && 'border-[--color-success]',
          isDragReject && 'border-[--color-error] bg-[--color-error-subtle]',
          disabled && 'cursor-not-allowed opacity-50 hover:border-[--color-border] hover:bg-transparent',
          className
        )}
        role="button"
        aria-label="파일을 여기에 드래그하거나 클릭하여 업로드"
      >
        <input {...getInputProps()} />
        <UploadCloud
          className={cn(
            'mb-[--space-3] h-10 w-10',
            isDragReject ? 'text-[--color-error]' : 'text-[--color-text-muted]'
          )}
          aria-hidden="true"
        />
        <p className="text-[length:--text-sm] font-[number:--weight-medium]">
          {isDragActive
            ? isDragReject
              ? '지원하지 않는 파일 형식입니다'
              : '여기에 놓으세요'
            : '파일을 드래그하거나 클릭하여 업로드'}
        </p>
        <p className="mt-[--space-1] text-[length:--text-xs] text-[--color-text-muted]">
          {hint ?? defaultHint}
        </p>
      </div>

      {value.length > 0 && (
        <ul className="space-y-[--space-1]" aria-label="업로드된 파일 목록">
          {value.map((file, index) => (
            <li
              key={`${file.name}-${index}`}
              className={cn(
                'flex items-center gap-[--space-2] rounded-[--radius-md]',
                'border border-[--color-border] bg-[--color-surface]',
                'px-[--space-3] py-[--space-2]',
                'text-[length:--text-sm]'
              )}
            >
              <FileText className="h-4 w-4 shrink-0 text-[--color-text-muted]" aria-hidden="true" />
              <span className="flex-1 truncate">{file.name}</span>
              <span className="text-[--color-text-muted]">{formatBytes(file.size)}</span>
              <button
                type="button"
                onClick={() => removeFile(index)}
                className={cn(
                  'ml-[--space-1] rounded-full p-[--space-1]',
                  'text-[--color-text-muted] hover:text-[--color-text]',
                  'hover:bg-[--color-surface-subtle]',
                  'focus-visible:outline-none focus-visible:ring-2',
                  'focus-visible:ring-[--color-focus-ring]'
                )}
                aria-label={`${file.name} 삭제`}
              >
                <X className="h-3 w-3" />
              </button>
            </li>
          ))}
        </ul>
      )}
    </div>
  )
}
