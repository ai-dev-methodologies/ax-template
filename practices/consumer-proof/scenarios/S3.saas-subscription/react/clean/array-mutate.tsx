// CLEAN — the correct rewrite of array-mutate.tsx.
// Rebuild the array immutably and hand the setter a new reference.
'use client'
import { useState } from 'react'

export default function TrackedFeaturesList() {
  const [trackedFeatures, setTrackedFeatures] = useState<string[]>([])

  function track(feature: string) {
    setTrackedFeatures((prev) => [...prev, feature]) // immutable — no mutation of state
  }

  return (
    <ul>
      <button onClick={() => track('api-calls')}>Track</button>
      {trackedFeatures.map((f) => (
        <li key={f}>{f}</li>
      ))}
    </ul>
  )
}
