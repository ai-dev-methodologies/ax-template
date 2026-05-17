/*
---
template_id: L1/components/sheet
layer: L1
provenance_class: external_canonical
evidence:
  - source_type: upstream_id
    upstream_id: shadcn-ui-2026-05
    section: sheet
    quote: "Extends the Dialog component to display content that complements the main content of the screen."
  - source_type: upstream_id
    upstream_id: wcag-2-2
    section: 4.1.2-name-role-value
    quote: "For all user interface components, the name and role can be programmatically determined."
a11y_criteria:
  - "WCAG 2.2 SC 4.1.2 — role='dialog' with side-panel semantics via Radix"
  - "WCAG 2.2 SC 2.1.2 Focus trap — Radix handles; Escape closes"
  - "WCAG 2.2 SC 1.3.4 Orientation"
  - "SheetTitle always required (even sr-only)"
dependencies: ["@radix-ui/react-dialog"]
drift_snapshot_ref: "practices-react/upstream/shadcn-registry-2026-05.snapshot.md#sheet"
---
*/
import * as React from 'react'
import * as SheetPrimitive from '@radix-ui/react-dialog'
import { cva, type VariantProps } from 'class-variance-authority'
import { cn } from '../lib/utils'

const Sheet = SheetPrimitive.Root
const SheetTrigger = SheetPrimitive.Trigger
const SheetClose = SheetPrimitive.Close
const SheetPortal = SheetPrimitive.Portal

const SheetOverlay = React.forwardRef<
  React.ElementRef<typeof SheetPrimitive.Overlay>,
  React.ComponentPropsWithoutRef<typeof SheetPrimitive.Overlay>
>(({ className, ...props }, ref) => (
  <SheetPrimitive.Overlay
    className={cn(
      'fixed inset-0 z-[--z-overlay] bg-black/80',
      'data-[state=open]:animate-in data-[state=closed]:animate-out',
      'data-[state=closed]:fade-out-0 data-[state=open]:fade-in-0',
      className
    )}
    {...props}
    ref={ref}
  />
))
SheetOverlay.displayName = SheetPrimitive.Overlay.displayName

const sheetVariants = cva(
  [
    'fixed z-[--z-modal] gap-[--space-4]',
    'bg-[--color-surface] p-[--space-6]',
    'shadow-[--shadow-lg]',
    'transition ease-in-out',
    'data-[state=open]:duration-[--duration-normal]',
    'data-[state=closed]:duration-[--duration-fast]',
    'data-[state=open]:animate-in data-[state=closed]:animate-out',
  ],
  {
    variants: {
      side: {
        top: 'inset-x-0 top-0 border-b border-[--color-border] data-[state=closed]:slide-out-to-top data-[state=open]:slide-in-from-top',
        bottom: 'inset-x-0 bottom-0 border-t border-[--color-border] data-[state=closed]:slide-out-to-bottom data-[state=open]:slide-in-from-bottom',
        left: 'inset-y-0 left-0 h-full w-3/4 border-r border-[--color-border] data-[state=closed]:slide-out-to-left data-[state=open]:slide-in-from-left sm:max-w-sm',
        right: 'inset-y-0 right-0 h-full w-3/4 border-l border-[--color-border] data-[state=closed]:slide-out-to-right data-[state=open]:slide-in-from-right sm:max-w-sm',
      },
    },
    defaultVariants: { side: 'right' },
  }
)

interface SheetContentProps
  extends React.ComponentPropsWithoutRef<typeof SheetPrimitive.Content>,
    VariantProps<typeof sheetVariants> {}

const SheetContent = React.forwardRef<
  React.ElementRef<typeof SheetPrimitive.Content>,
  SheetContentProps
>(({ side = 'right', className, children, ...props }, ref) => (
  <SheetPortal>
    <SheetOverlay />
    <SheetPrimitive.Content
      ref={ref}
      className={cn(sheetVariants({ side }), className)}
      {...props}
    >
      <SheetPrimitive.Close
        className={cn(
          'absolute right-[--space-4] top-[--space-4] rounded-[--radius-sm] opacity-70',
          'ring-offset-[--color-surface] transition-opacity duration-[--duration-fast]',
          'hover:opacity-100',
          'focus:outline-none focus:ring-2 focus:ring-[--color-focus-ring] focus:ring-offset-2',
          'disabled:pointer-events-none',
          'data-[state=open]:bg-[--color-surface-subtle]'
        )}
      >
        <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" className="h-4 w-4" aria-hidden="true"><path d="M18 6 6 18"/><path d="m6 6 12 12"/></svg>
        <span className="sr-only">Close</span>
      </SheetPrimitive.Close>
      {children}
    </SheetPrimitive.Content>
  </SheetPortal>
))
SheetContent.displayName = SheetPrimitive.Content.displayName

const SheetHeader = ({ className, ...props }: React.HTMLAttributes<HTMLDivElement>) => (
  <div className={cn('flex flex-col space-y-[--space-2] text-center sm:text-left', className)} {...props} />
)
SheetHeader.displayName = 'SheetHeader'

const SheetFooter = ({ className, ...props }: React.HTMLAttributes<HTMLDivElement>) => (
  <div className={cn('flex flex-col-reverse sm:flex-row sm:justify-end sm:space-x-[--space-2]', className)} {...props} />
)
SheetFooter.displayName = 'SheetFooter'

const SheetTitle = React.forwardRef<
  React.ElementRef<typeof SheetPrimitive.Title>,
  React.ComponentPropsWithoutRef<typeof SheetPrimitive.Title>
>(({ className, ...props }, ref) => (
  <SheetPrimitive.Title
    ref={ref}
    className={cn('text-[length:--text-lg] font-[number:--weight-semibold] text-[--color-text]', className)}
    {...props}
  />
))
SheetTitle.displayName = SheetPrimitive.Title.displayName

const SheetDescription = React.forwardRef<
  React.ElementRef<typeof SheetPrimitive.Description>,
  React.ComponentPropsWithoutRef<typeof SheetPrimitive.Description>
>(({ className, ...props }, ref) => (
  <SheetPrimitive.Description
    ref={ref}
    className={cn('text-[length:--text-sm] text-[--color-text-muted]', className)}
    {...props}
  />
))
SheetDescription.displayName = SheetPrimitive.Description.displayName

export {
  Sheet,
  SheetPortal,
  SheetOverlay,
  SheetTrigger,
  SheetClose,
  SheetContent,
  SheetHeader,
  SheetFooter,
  SheetTitle,
  SheetDescription,
}
