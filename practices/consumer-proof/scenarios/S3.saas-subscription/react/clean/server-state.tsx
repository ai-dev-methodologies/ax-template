// CLEAN — the correct rewrite of server-state.tsx.
// Read straight from the query cache; do not copy into useState.
'use client'
import useSWR from 'swr'

const fetcher = (url: string) => fetch(url).then((r) => r.json())

export default function CurrentPlanBadge() {
  const { data: plan } = useSWR('/api/subscription/plan', fetcher)

  return <span>{plan?.name}</span>
}
