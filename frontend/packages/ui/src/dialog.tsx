'use client';

import * as React from 'react';
import * as DialogPrimitive from '@radix-ui/react-dialog';
import { X } from 'lucide-react';
import { cn } from './utils';

/**
 * Token-driven dialog/drawer built on Radix Dialog. Added to the shared catalog
 * so per-persona apps (apps/**) can compose modal + slide-in surfaces without
 * forking their own overlay/focus-trap plumbing.
 *
 * - `DialogContent` is a centered modal by default.
 * - `DialogContent side="right"` renders a right-anchored drawer.
 * Motion is compositor-friendly (opacity/transform) and pauses under
 * prefers-reduced-motion. The overlay scrim + radius resolve against the active
 * theme tokens, so a persona radius override (e.g. [--radius:6px]) flows through.
 */

const Dialog = DialogPrimitive.Root;
const DialogTrigger = DialogPrimitive.Trigger;
const DialogClose = DialogPrimitive.Close;
const DialogPortal = DialogPrimitive.Portal;

const DialogOverlay = React.forwardRef<
  React.ElementRef<typeof DialogPrimitive.Overlay>,
  React.ComponentPropsWithoutRef<typeof DialogPrimitive.Overlay>
>(({ className, ...props }, ref) => (
  <DialogPrimitive.Overlay
    ref={ref}
    className={cn(
      'fixed inset-0 z-50 bg-foreground/30 backdrop-blur-[1px]',
      'data-[state=open]:animate-in data-[state=open]:fade-in',
      'data-[state=closed]:animate-out data-[state=closed]:fade-out',
      'motion-reduce:animate-none',
      className,
    )}
    {...props}
  />
));
DialogOverlay.displayName = DialogPrimitive.Overlay.displayName;

type DialogSide = 'center' | 'right';

interface DialogContentProps
  extends React.ComponentPropsWithoutRef<typeof DialogPrimitive.Content> {
  /** 'center' = modal (default), 'right' = slide-in drawer. */
  side?: DialogSide;
  /** Render a top-right close button. Defaults to true. */
  showClose?: boolean;
}

const SIDE_CLASS: Record<DialogSide, string> = {
  center:
    'left-1/2 top-1/2 w-full max-w-md -translate-x-1/2 -translate-y-1/2 rounded-[calc(var(--radius)+0.35rem)] border p-6 ' +
    'data-[state=open]:zoom-in-95 data-[state=open]:fade-in data-[state=closed]:zoom-out-95 data-[state=closed]:fade-out',
  right:
    'inset-y-0 right-0 flex w-full max-w-md flex-col border-l ' +
    'data-[state=open]:slide-in-from-right data-[state=closed]:slide-out-to-right',
};

const DialogContent = React.forwardRef<
  React.ElementRef<typeof DialogPrimitive.Content>,
  DialogContentProps
>(({ className, children, side = 'center', showClose = true, ...props }, ref) => (
  <DialogPortal>
    <DialogOverlay />
    <DialogPrimitive.Content
      ref={ref}
      className={cn(
        'fixed z-50 border-border bg-card text-card-foreground shadow-lg focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring',
        'data-[state=open]:animate-in data-[state=closed]:animate-out motion-reduce:animate-none',
        SIDE_CLASS[side],
        className,
      )}
      {...props}
    >
      {children}
      {showClose ? (
        <DialogPrimitive.Close
          className={cn(
            'absolute right-4 top-4 rounded-[var(--radius)] p-1 text-muted-foreground opacity-80 transition-opacity',
            'hover:opacity-100 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 focus-visible:ring-offset-background',
            'disabled:pointer-events-none',
          )}
        >
          <X aria-hidden="true" className="h-4 w-4" />
          <span className="sr-only">닫기</span>
        </DialogPrimitive.Close>
      ) : null}
    </DialogPrimitive.Content>
  </DialogPortal>
));
DialogContent.displayName = DialogPrimitive.Content.displayName;

function DialogHeader({ className, ...props }: React.HTMLAttributes<HTMLDivElement>) {
  return <div className={cn('flex flex-col space-y-1.5', className)} {...props} />;
}
DialogHeader.displayName = 'DialogHeader';

function DialogFooter({ className, ...props }: React.HTMLAttributes<HTMLDivElement>) {
  return (
    <div
      className={cn('flex flex-col-reverse gap-2 sm:flex-row sm:justify-end', className)}
      {...props}
    />
  );
}
DialogFooter.displayName = 'DialogFooter';

const DialogTitle = React.forwardRef<
  React.ElementRef<typeof DialogPrimitive.Title>,
  React.ComponentPropsWithoutRef<typeof DialogPrimitive.Title>
>(({ className, ...props }, ref) => (
  <DialogPrimitive.Title
    ref={ref}
    className={cn('text-base font-semibold leading-none tracking-tight text-foreground', className)}
    {...props}
  />
));
DialogTitle.displayName = DialogPrimitive.Title.displayName;

const DialogDescription = React.forwardRef<
  React.ElementRef<typeof DialogPrimitive.Description>,
  React.ComponentPropsWithoutRef<typeof DialogPrimitive.Description>
>(({ className, ...props }, ref) => (
  <DialogPrimitive.Description
    ref={ref}
    className={cn('text-sm text-muted-foreground', className)}
    {...props}
  />
));
DialogDescription.displayName = DialogPrimitive.Description.displayName;

export {
  Dialog,
  DialogTrigger,
  DialogClose,
  DialogPortal,
  DialogOverlay,
  DialogContent,
  DialogHeader,
  DialogFooter,
  DialogTitle,
  DialogDescription,
};
export type { DialogContentProps };
