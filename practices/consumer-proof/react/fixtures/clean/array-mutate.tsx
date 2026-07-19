// CLEAN — the correct rewrite of array-mutate.tsx.
// Rebuild the array immutably and hand the setter a new reference.
'use client'
import { useState } from 'react'

export default function TodoList() {
  const [items, setItems] = useState<string[]>([])

  function add(next: string) {
    setItems((prev) => [...prev, next]) // immutable — no mutation of state
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
