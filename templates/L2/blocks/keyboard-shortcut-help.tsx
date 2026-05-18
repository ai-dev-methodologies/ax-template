/*
---
template_id: L2/blocks/keyboard-shortcut-help
layer: L2
provenance_class: internal_design
evidence:
  - source_type: external
    citation: "WCAG 2.2 SC 1.3.3 — Sensory Characteristics: Instructions for understanding and operating content do not rely solely on visual presentation; provide text labels for keyboard shortcuts."
    url: "https://www.w3.org/WAI/WCAG22/Understanding/sensory-characteristics.html"
    quoted_at: "2026-05-18"
  - source_type: external
    citation: "WCAG 2.2 SC 4.1.3 — Status Messages: dialog opened via keyboard shortcut ('?') must be programmatically announced."
    url: "https://www.w3.org/WAI/WCAG22/Understanding/status-messages.html"
    quoted_at: "2026-05-18"
  - source_type: internal
    rationale: "Keyboard shortcut help dialog/panel typically opened with '?' key. Shows an organized table of shortcuts grouped by category. Overlay pattern — caller manages open state."
dependencies: []
imports_from: [L1]
imports_forbidden: [L4, app/, lib/auth/, lib/payment/]
---
*/
'use client'

import * as React from 'react'

export interface ShortcutEntry {
  /** Key combination shown in the <kbd> element (e.g. "⌘K", "Ctrl+K"). */
  keys: string
  /** Human-readable description of what the shortcut does. */
  description: string
}

export interface ShortcutGroup {
  /** Group heading (e.g. "Navigation", "Actions"). */
  label: string
  shortcuts: ShortcutEntry[]
}

export interface KeyboardShortcutHelpProps {
  /** Whether the help panel is visible. */
  open: boolean
  /** Called when the user closes the panel (Escape, backdrop click, close button). */
  onClose: () => void
  /** Groups of shortcuts to display. */
  groups: ShortcutGroup[]
  /** Dialog title (default: "Keyboard shortcuts"). */
  title?: string
}

/**
 * KeyboardShortcutHelp — modal dialog displaying grouped keyboard shortcuts.
 *
 * Typically opened with the `?` key. Caller manages open state.
 *
 * ```tsx
 * import KeyboardShortcutHelp from 'templates/L2/blocks/keyboard-shortcut-help'
 *
 * const SHORTCUTS: ShortcutGroup[] = [
 *   {
 *     label: 'Navigation',
 *     shortcuts: [
 *       { keys: 'G then D', description: 'Go to Dashboard' },
 *       { keys: 'G then S', description: 'Go to Settings' },
 *     ],
 *   },
 *   {
 *     label: 'Search',
 *     shortcuts: [{ keys: '⌘K', description: 'Open command palette' }],
 *   },
 * ]
 *
 * export function App() {
 *   const [open, setOpen] = React.useState(false)
 *
 *   React.useEffect(() => {
 *     const handler = (e: KeyboardEvent) => {
 *       if (e.key === '?' && !e.ctrlKey && !e.metaKey) setOpen(v => !v)
 *     }
 *     document.addEventListener('keydown', handler)
 *     return () => document.removeEventListener('keydown', handler)
 *   }, [])
 *
 *   return <KeyboardShortcutHelp open={open} onClose={() => setOpen(false)} groups={SHORTCUTS} />
 * }
 * ```
 */
export default function KeyboardShortcutHelp({
  open,
  onClose,
  groups,
  title = 'Keyboard shortcuts',
}: KeyboardShortcutHelpProps) {
  const dialogRef = React.useRef<HTMLDialogElement>(null)

  // Sync native <dialog> open state
  React.useEffect(() => {
    const dialog = dialogRef.current
    if (!dialog) return
    if (open && !dialog.open) {
      dialog.showModal()
    } else if (!open && dialog.open) {
      dialog.close()
    }
  }, [open])

  // Close on Escape (native <dialog> already handles this; sync state)
  React.useEffect(() => {
    const dialog = dialogRef.current
    if (!dialog) return
    const handler = () => onClose()
    dialog.addEventListener('close', handler)
    return () => dialog.removeEventListener('close', handler)
  }, [onClose])

  return (
    <dialog
      ref={dialogRef}
      aria-label={title}
      data-testid="keyboard-shortcut-help"
      className="w-full max-w-lg rounded-xl shadow-xl backdrop:bg-black/40 p-0 border-0 bg-background text-foreground"
      onClick={(e) => {
        // Close when clicking outside the dialog content
        if (e.target === e.currentTarget) onClose()
      }}
    >
      {/* Header */}
      <div className="flex items-center justify-between px-6 pt-5 pb-4 border-b">
        <h2 className="text-base font-semibold">{title}</h2>
        <button
          type="button"
          onClick={onClose}
          aria-label="Close keyboard shortcut help"
          className="rounded p-1 text-muted-foreground hover:text-foreground focus:outline-none focus-visible:ring-2 focus-visible:ring-ring"
        >
          ✕
        </button>
      </div>

      {/* Shortcut groups */}
      <div className="px-6 py-4 space-y-5 max-h-[70vh] overflow-y-auto">
        {groups.map((group) => (
          <section key={group.label} aria-labelledby={`shortcuts-group-${group.label}`}>
            <h3
              id={`shortcuts-group-${group.label}`}
              className="text-xs font-semibold uppercase tracking-widest text-muted-foreground mb-2"
            >
              {group.label}
            </h3>
            <dl className="space-y-1.5">
              {group.shortcuts.map((s) => (
                <div
                  key={s.keys}
                  className="flex items-center justify-between text-sm gap-4"
                >
                  <dt className="text-foreground">{s.description}</dt>
                  <dd>
                    <kbd className="rounded border bg-muted px-2 py-0.5 text-xs font-mono font-medium text-muted-foreground shadow-sm">
                      {s.keys}
                    </kbd>
                  </dd>
                </div>
              ))}
            </dl>
          </section>
        ))}
      </div>
    </dialog>
  )
}
