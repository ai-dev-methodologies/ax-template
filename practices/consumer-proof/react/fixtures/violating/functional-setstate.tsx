// VIOLATING — ax/prefer-functional-setstate
// The setter argument directly references the state variable `count`, risking a
// stale closure. The rule wants the functional form.
'use client'
import { useState } from 'react'

export default function Counter() {
  const [count, setCount] = useState(0)

  return (
    <button onClick={() => setCount(count + 1)}>
      {count}
    </button>
  )
}
