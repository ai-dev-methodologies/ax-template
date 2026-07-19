// VIOLATING — ax/no-array-mutate-on-state
// Typical AI output when asked to add a plan-feature to the usage widget's
// tracked-features list: pushes directly into the useState array, then
// passes the SAME reference to the setter. In-place mutation on
// useState-derived state.
'use client'
import { useState } from 'react'

export default function TrackedFeaturesList() {
  const [trackedFeatures, setTrackedFeatures] = useState<string[]>([])

  function track(feature: string) {
    trackedFeatures.push(feature) // mutateMethodOnState: `.push` on useState-derived array
    setTrackedFeatures(trackedFeatures)
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
