// VIOLATING — ax/no-server-state-in-local-state
// Seeds useState directly from a query result's `.data`, mirroring server state
// into local state and defeating revalidation.
'use client'
import { useState } from 'react'
import useSWR from 'swr'

const fetcher = (url: string) => fetch(url).then((r) => r.json())

export default function Profile() {
  const [user, setUser] = useState(useSWR('/api/user', fetcher).data)

  return <div onClick={() => setUser(null)}>{user?.name}</div>
}
