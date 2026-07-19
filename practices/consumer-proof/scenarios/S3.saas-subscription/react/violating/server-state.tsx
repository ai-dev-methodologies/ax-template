// VIOLATING — ax/no-server-state-in-local-state
// Seeds useState directly from the current-plan query result's `.data`,
// mirroring server state into local state and defeating revalidation when
// the plan changes elsewhere (e.g. the upgrade CTA in PricingTable).
'use client'
import { useState } from 'react'
import useSWR from 'swr'

const fetcher = (url: string) => fetch(url).then((r) => r.json())

export default function CurrentPlanBadge() {
  const [plan, setPlan] = useState(useSWR('/api/subscription/plan', fetcher).data)

  return <span onClick={() => setPlan(null)}>{plan?.name}</span>
}
