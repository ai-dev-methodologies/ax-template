/*
---
template_id: L1/components/resizable
layer: L1
provenance_class: external_canonical
evidence:
  - source_type: upstream_id
    upstream_id: shadcn-ui-2026-05
    section: resizable
    quote: "Accessible resizable panel groups and layouts with keyboard support."
  - source_type: upstream_id
    upstream_id: wcag-2-2
    section: 2.5.1-pointer-gestures
    quote: "All functionality that uses multipoint or path-based gestures for operation can be operated with a single pointer."
a11y_criteria:
  - "WCAG 2.2 SC 2.5.1 Pointer Gestures — single-pointer alternative required for drag"
  - "WCAG 2.2 SC 2.5.8 Target Size — divider handle needs adequate hit area"
  - "Divider: role='separator' + aria-orientation + keyboard arrow-key resize"
dependencies: ["react-resizable-panels"]
drift_snapshot_ref: "practices-react/upstream/shadcn-registry-2026-05.snapshot.md#resizable"
---
*/
import * as React from 'react'
import * as ResizablePrimitive from 'react-resizable-panels'
import { cn } from '../lib/utils'

const ResizablePanelGroup = ({
  className,
  ...props
}: React.ComponentProps<typeof ResizablePrimitive.PanelGroup>) => (
  <ResizablePrimitive.PanelGroup
    className={cn('flex h-full w-full data-[panel-group-direction=vertical]:flex-col', className)}
    {...props}
  />
)

const ResizablePanel = ResizablePrimitive.Panel

const ResizableHandle = ({
  withHandle,
  className,
  ...props
}: React.ComponentProps<typeof ResizablePrimitive.PanelResizeHandle> & {
  withHandle?: boolean
}) => (
  <ResizablePrimitive.PanelResizeHandle
    className={cn(
      'relative flex w-px items-center justify-center',
      'bg-[--color-border] after:absolute after:inset-y-0',
      'after:left-1/2 after:w-1 after:-translate-x-1/2',
      'focus-visible:outline-none focus-visible:ring-2',
      'focus-visible:ring-[--color-focus-ring] focus-visible:ring-offset-1',
      'data-[panel-group-direction=vertical]:h-px',
      'data-[panel-group-direction=vertical]:w-full',
      'data-[panel-group-direction=vertical]:after:left-0',
      'data-[panel-group-direction=vertical]:after:h-1',
      'data-[panel-group-direction=vertical]:after:w-full',
      'data-[panel-group-direction=vertical]:after:-translate-y-1/2',
      'data-[panel-group-direction=vertical]:after:translate-x-0',
      '[&[data-resize-handle-active]]:bg-[--color-accent]',
      className
    )}
    {...props}
  >
    {withHandle && (
      <div className="z-10 flex h-4 w-3 items-center justify-center rounded-[--radius-sm] border border-[--color-border] bg-[--color-border]">
        <svg xmlns="http://www.w3.org/2000/svg" width="10" height="10" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true"><circle cx="9" cy="12" r="1"/><circle cx="9" cy="5" r="1"/><circle cx="9" cy="19" r="1"/><circle cx="15" cy="12" r="1"/><circle cx="15" cy="5" r="1"/><circle cx="15" cy="19" r="1"/></svg>
      </div>
    )}
  </ResizablePrimitive.PanelResizeHandle>
)

export { ResizablePanelGroup, ResizablePanel, ResizableHandle }
