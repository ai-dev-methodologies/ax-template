import * as React from 'react'

export interface Widget {
  id: string
  name: string
}

export default function WidgetDetailView({ widget }: { widget: Widget }) {
  return <div>{widget.name}</div>
}
