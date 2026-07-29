// R82b [41] fixture view — the props-only presentational half of the pair.
export default function DemoView({
  rows,
  onRetry,
  retryPending,
}: {
  rows: unknown[]
  onRetry: () => void
  retryPending: boolean
}) {
  return (
    <ul>
      {rows.map((_, i) => (
        <li key={i}>
          row {i}
        </li>
      ))}
      <button
        type="button"
        data-pending={retryPending || undefined}
        onClick={onRetry}
      >
        Retry
      </button>
    </ul>
  )
}
