import * as React from 'react'
import { useQuery } from '@tanstack/react-query'
import WidgetDetailView, { type Widget } from './widget-detail-view'

export default function WidgetPage() {
  const { data } = useQuery<Widget>({ queryKey: ['widget'], queryFn: async () => ({ id: '1', name: 'x' }) })
  if (!data) return null
  return <WidgetDetailView widget={data} />
}
