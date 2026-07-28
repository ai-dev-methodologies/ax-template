import * as React from 'react'
import { useQuery } from '@tanstack/react-query'

interface Widget {
  id: string
  name: string
}

export default function WidgetPage() {
  const { data } = useQuery<Widget>({ queryKey: ['widget'], queryFn: async () => ({ id: '1', name: 'x' }) })
  if (!data) return null
  // Re-inlined — the view file still exists on disk but is no longer imported.
  return <div>{data.name}</div>
}
