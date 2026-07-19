// VIOLATING — ax/no-array-mutate-on-state
// Typical AI output: pushes directly into the useState array, then passes the
// SAME reference to the setter. In-place mutation on useState-derived state.
'use client'
import { useState } from 'react'

export default function TodoList() {
  const [items, setItems] = useState<string[]>([])

  function add(next: string) {
    items.push(next) // mutateMethodOnState: `.push` on useState-derived array
    setItems(items)
  }

  return (
    <ul>
      <button onClick={() => add('new')}>Add</button>
      {items.map((i) => (
        <li key={i}>{i}</li>
      ))}
    </ul>
  )
}
