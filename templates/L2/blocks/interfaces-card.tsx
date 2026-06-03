/*
---
template_id: L2/blocks/interfaces-card
layer: L2
provenance_class: internal_design
evidence:
  - source_type: internal
    rationale: "Codified from the community 21st.dev component jshguo/interfaces-card: hardcoded hex -> --ax-c-* design tokens; the shadcn cn util is inlined (clsx+tailwind-merge) and any UI-kit import removed so the block imports no @/ alias (ax block self-containment). Passes all 7 ax/* own-block rules. Governed by practices-react/rules/ux-block-uses-design-tokens-and-a11y.md."
dependencies: []
imports_from: []
imports_forbidden: [L4, app/, lib/]
---
*/
/**
 * @ax-codified-from 21st.dev/jshguo/interfaces-card
 * @ax-layer L2/blocks/card
 * Deterministic codify (codify.py): hex extracted to --ax-c-* tokens; provenance stamped.
 * REMAINING semantic pass (see .notes.md): a11y:add-role-aria, types:string-literal-variants
 */
import * as React from "react"

import { clsx, type ClassValue } from 'clsx'
import { twMerge } from 'tailwind-merge'
// cn inlined (ax blocks avoid the lib alias); behavior-identical to the shadcn util
function cn(...inputs: ClassValue[]) { return twMerge(clsx(inputs)) }

function Card({ className, ...props }: React.ComponentProps<"div">) {
  return (
    <div
      data-slot="card"
      className={cn(
        "bg-card text-card-foreground flex flex-col rounded-xl border",
        className
      )}
      {...props}
    />
  )
}

function CardHeader({ className, ...props }: React.ComponentProps<"div">) {
  return (
    <div
      data-slot="card-header"
      className={cn(
        "@container/card-header grid auto-rows-min grid-rows-[auto_auto] items-start gap-1 px-4 py-3 has-data-[slot=card-action]:grid-cols-[1fr_auto] border-b",
        className
      )}
      {...props}
    />
  )
}

function CardTitle({ className, ...props }: React.ComponentProps<"div">) {
  return (
    <div
      data-slot="card-title"
      className={cn("leading-none font-semibold text-lg", className)}
      {...props}
    />
  )
}

function CardDescription({ className, ...props }: React.ComponentProps<"div">) {
  return (
    <div
      data-slot="card-description"
      className={cn("text-muted-foreground text-sm", className)}
      {...props}
    />
  )
}

function CardAction({ className, ...props }: React.ComponentProps<"div">) {
  return (
    <div
      data-slot="card-action"
      className={cn(
        "col-start-2 row-span-2 row-start-1 self-start justify-self-end",
        className
      )}
      {...props}
    />
  )
}

function CardContent({ className, ...props }: React.ComponentProps<"div">) {
  return (
    <div
      data-slot="card-content"
      className={cn("p-4", className)}
      {...props}
    />
  )
}

function CardFooter({ className, ...props }: React.ComponentProps<"div">) {
  return (
    <div
      data-slot="card-footer"
      className={cn("flex items-center justify-stretch p-4 border-t", className)}
      {...props}
    />
  )
}

export {
  Card,
  CardHeader,
  CardFooter,
  CardTitle,
  CardAction,
  CardDescription,
  CardContent,
}
