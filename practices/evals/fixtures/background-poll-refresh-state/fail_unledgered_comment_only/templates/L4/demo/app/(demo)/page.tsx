'use client'
// R82b [41] fixture page — polls in the background and owns a mutation.
// The mutation's button markup (and therefore its aria-busy attribute) lives in
// this file, but the attribute was never actually written — only mentioned. An
// unconverted page has no ledgered view to hold it (BACKLOG P3-97).
import { useQuery, useMutation } from '@tanstack/react-query'

export default function DemoPage() {
  const { data, dataUpdatedAt } = useQuery({
    queryKey: ['demo'],
    queryFn: () => fetch('/api/demo').then((r) => r.json()),
    refetchInterval: 5000,
  })
  const retry = useMutation({ mutationFn: () => fetch('/api/demo/retry', { method: 'POST' }) })

  return (
    <div>
      <span aria-live="polite">
        {dataUpdatedAt ? `Updated ${new Date(dataUpdatedAt).toLocaleTimeString()}` : null}
      </span>
      <button type="button" data-pending={retry.isPending || undefined} onClick={() => retry.mutate()}>
        Retry
      </button>
    </div>
  )
}
