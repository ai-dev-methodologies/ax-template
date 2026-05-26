/*
---
template_id: L2/blocks/confirm-dialog
layer: L2
provenance_class: internal_design
evidence:
  - source_type: external
    citation: "WAI-ARIA Authoring Practices — Alert Dialog Pattern"
    url: "https://www.w3.org/WAI/ARIA/apg/patterns/alertdialog/"
  - source_type: internal
    rationale: "L2 common block — generic confirm/cancel dialog; onConfirm/onCancel callbacks injected by L4."
dependencies: [alert-dialog, button]
imports_from: [L1]
imports_forbidden: [L4, app/, lib/]
---
*/
import * as React from 'react'

export interface ConfirmDialogProps {
  open: boolean
  title: string
  description?: string
  confirmLabel?: string
  cancelLabel?: string
  /** Highlight confirm as destructive */
  destructive?: boolean
  onConfirm: () => void
  onCancel: () => void
  isLoading?: boolean
}

export default function ConfirmDialog({
  open,
  title,
  description,
  confirmLabel = 'Confirm',
  cancelLabel = 'Cancel',
  destructive = false,
  onConfirm,
  onCancel,
  isLoading = false,
}: ConfirmDialogProps) {
  if (!open) return null

  return (
    <div
      role="alertdialog"
      aria-modal="true"
      aria-labelledby="cd-title"
      aria-describedby={description ? 'cd-desc' : undefined}
      className="fixed inset-0 z-50 flex items-center justify-center p-4"
    >
      {/* Backdrop */}
      <div
        aria-hidden="true"
        onClick={onCancel}
        className="absolute inset-0 bg-background/80 backdrop-blur-sm"
      />

      {/* Dialog */}
      <div className="relative z-10 w-full max-w-sm rounded-lg border border-border bg-background p-6 shadow-lg space-y-4">
        <h2 id="cd-title" className="text-lg font-semibold">
          {title}
        </h2>

        {description && (
          <p id="cd-desc" className="text-sm text-muted-foreground">
            {description}
          </p>
        )}

        <div className="flex justify-end gap-3">
          <button
            type="button"
            onClick={onCancel}
            disabled={isLoading}
            className="inline-flex items-center rounded-md border border-input bg-background px-4 py-2 text-sm font-medium shadow-sm hover:bg-accent hover:text-accent-foreground focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring disabled:pointer-events-none disabled:opacity-50"
          >
            {cancelLabel}
          </button>

          <button
            type="button"
            onClick={onConfirm}
            disabled={isLoading}
            /* R82 — aria-busy reflects the wrapped mutation lifecycle so
               screen readers track the in-flight state of any
               confirm-gated mutation (WCAG SC 4.1.3 Status Messages).
               The L2 block sits in front of destructive admin actions
               on background-polled pages (file-storage delete,
               email-outbox retry/delete, approval revoke) and is the
               canonical surface where the aria-busy signal must land. */
            aria-busy={isLoading || undefined}
            className={[
              'inline-flex items-center rounded-md px-4 py-2 text-sm font-medium shadow focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring disabled:pointer-events-none disabled:opacity-50 aria-busy:opacity-60',
              destructive
                ? 'bg-destructive text-destructive-foreground hover:bg-destructive/90'
                : 'bg-primary text-primary-foreground hover:bg-primary/90',
            ].join(' ')}
          >
            {isLoading ? 'Loading…' : confirmLabel}
          </button>
        </div>
      </div>
    </div>
  )
}
