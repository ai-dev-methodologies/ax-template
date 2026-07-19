// CLEAN — the correct rewrite of server-state.tsx.
// Read straight from the query cache; do not copy into useState.
'use client'
import useSWR from 'swr'

const fetcher = (url: string) => fetch(url).then((r) => r.json())

export default function Profile() {
  const { data: user } = useSWR('/api/user', fetcher)

  return <div>{user?.name}</div>
}
