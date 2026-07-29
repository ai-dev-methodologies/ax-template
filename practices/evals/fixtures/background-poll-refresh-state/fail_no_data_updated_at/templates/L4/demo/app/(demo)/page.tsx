'use client'
// R82b [41] fixture page — polls in the background but never surfaces freshness.
// The mutation trigger DOES carry a real aria-busy attribute, so the only failing
// leg is the original R82 requirement: dataUpdatedAt must be visible.
import { useQuery, useMutation } from '@tanstack/react-query'

export default function DemoPage() {
  const { data } = useQuery({
    queryKey: ['demo'],
    queryFn: () => fetch('/api/demo').then((r) => r.json()),
    refetchInterval: 5000,
  })
  const retry = useMutation({ mutationFn: () => fetch('/api/demo/retry', { method: 'POST' }) })

  return (
    <div>
      <span>{(data ?? []).length} rows</span>
      <button
        type="button"
        aria-busy={retry.isPending || undefined}
        onClick={() => retry.mutate()}
      >
        Retry
      </button>
    </div>
  )
}
