/*
---
template_id: L1/components/avatar
layer: L1
provenance_class: external_canonical
evidence:
  - source_type: upstream_id
    upstream_id: shadcn-ui-2026-05
    section: avatar
    quote: "An image element with a fallback for representing the user."
  - source_type: upstream_id
    upstream_id: wcag-2-2
    section: 1.1.1-non-text-content
    quote: "All non-text content that is presented to the user has a text alternative that serves the equivalent purpose."
a11y_criteria:
  - "WCAG 2.2 SC 1.1.1 Non-text Content — AvatarImage needs alt prop"
  - "AvatarFallback text provides alt; decorative avatars get alt=''"
dependencies: ["@radix-ui/react-avatar"]
drift_snapshot_ref: "practices-react/upstream/shadcn-registry-2026-05.snapshot.md#avatar"
---
*/
import * as React from 'react'
import * as AvatarPrimitive from '@radix-ui/react-avatar'
import { cn } from '../lib/utils'

const Avatar = React.forwardRef<
  React.ElementRef<typeof AvatarPrimitive.Root>,
  React.ComponentPropsWithoutRef<typeof AvatarPrimitive.Root>
>(({ className, ...props }, ref) => (
  <AvatarPrimitive.Root
    ref={ref}
    className={cn(
      'relative flex h-8 w-8 shrink-0 overflow-hidden rounded-full',
      className
    )}
    {...props}
  />
))
Avatar.displayName = AvatarPrimitive.Root.displayName

const AvatarImage = React.forwardRef<
  React.ElementRef<typeof AvatarPrimitive.Image>,
  React.ComponentPropsWithoutRef<typeof AvatarPrimitive.Image>
>(({ className, ...props }, ref) => (
  <AvatarPrimitive.Image
    ref={ref}
    className={cn('aspect-square h-full w-full', className)}
    {...props}
  />
))
AvatarImage.displayName = AvatarPrimitive.Image.displayName

const AvatarFallback = React.forwardRef<
  React.ElementRef<typeof AvatarPrimitive.Fallback>,
  React.ComponentPropsWithoutRef<typeof AvatarPrimitive.Fallback>
>(({ className, ...props }, ref) => (
  <AvatarPrimitive.Fallback
    ref={ref}
    className={cn(
      'flex h-full w-full items-center justify-center rounded-full',
      'bg-[--color-surface-subtle] text-[--color-text-muted]',
      'text-[length:--text-sm] font-[number:--weight-medium]',
      className
    )}
    {...props}
  />
))
AvatarFallback.displayName = AvatarPrimitive.Fallback.displayName

export { Avatar, AvatarImage, AvatarFallback }
