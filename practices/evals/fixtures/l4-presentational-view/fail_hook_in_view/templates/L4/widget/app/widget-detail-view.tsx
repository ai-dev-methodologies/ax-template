import * as React from 'react'
import { useQuery } from '@tanstack/react-query'

export interface Widget {
  id: string
  name: string
}

// VIOLATION — a "view" that still owns its own data-fetching hook is not
// props-only; it fails for the exact same vitest-resolution reason the
// convention exists to fix.
export default function WidgetDetailView({ widget }: { widget: Widget }) {
  useQuery({ queryKey: ['noop'], queryFn: async () => null })
  return <div>{widget.name}</div>
}
