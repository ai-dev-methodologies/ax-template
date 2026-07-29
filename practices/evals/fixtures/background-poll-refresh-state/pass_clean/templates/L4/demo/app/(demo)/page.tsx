'use client'
// R82b [41] fixture page — polls in the background and owns a mutation.
// The mutation's button markup (and therefore its aria-busy attribute) lives in
// the co-located presentational view per the P2-42 convention; this comment says
// so truthfully, and MUST NOT be what satisfies the guard (BACKLOG P3-97).
import { useQuery, useMutation } from '@tanstack/react-query'
import DemoView from './demo-view'

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
      <DemoView rows={data ?? []} onRetry={() => retry.mutate()} retryPending={retry.isPending} />
    </div>
  )
}
