/*
---
template_id: L1/lib/utils
layer: L1
provenance_class: external_canonical
evidence:
  - source_type: upstream_id
    upstream_id: shadcn-ui-2026-05
    section: "What it is"
    quote: "This is not a component library. It is how you build your component library."
---
*/

import { clsx, type ClassValue } from 'clsx'
import { twMerge } from 'tailwind-merge'

/**
 * Merge class names with Tailwind conflict resolution.
 * All L1 components use this utility for className composition.
 */
export function cn(...inputs: ClassValue[]): string {
  return twMerge(clsx(inputs))
}
