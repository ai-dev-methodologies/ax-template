# export-job-status — L3 Export Job Status Page Template

Generic long-running export job status skeleton. Stateless display component that
receives job `status` and optional `progress` as props. Polling logic lives entirely
in L4 (SWR, React Query, or Next.js Server Action revalidation). Shows a status
badge, optional progress bar, and download link when the job completes.

## Slot contract

| Prop | Type | Required | Description |
|---|---|---|---|
| `status` | `'pending' \| 'running' \| 'completed' \| 'failed'` | ✅ | Current job status |
| `jobName` | `string` | — | Human-readable job name (default: `"Export job"`) |
| `progress` | `number` | — | Completion percentage 0–100 (shown when pending or running) |
| `downloadHref` | `string` | — | Download URL (shown only when `status === "completed"`) |
| `downloadLabel` | `string` | — | Download button label (default: `"Download"`) |
| `retryHref` | `string` | — | Retry link (shown only when `status === "failed"`) |
| `description` | `string` | — | Optional subtitle |
| `pollingSlot` | `ReactNode` | — | Custom polling indicator (overrides built-in animated spinner) |

## Behaviour

- `pending`: shows ⏳ icon + "Waiting" badge
- `running`: shows animated spinner + "Processing" badge + optional progress bar
- `completed`: shows ✓ icon + "Complete" badge + download button
- `failed`: shows ✕ icon + "Failed" badge + retry link

## Usage (L4 example)

```tsx
import ExportJobStatusPage from 'templates/L3/pages/export-job-status/page'

export default async function ExportStatusRoute({
  params,
}: {
  params: { jobId: string }
}) {
  const job = await getExportJob(params.jobId)
  return (
    <ExportJobStatusPage
      jobName="Product catalog export"
      status={job.status}
      progress={job.progress}
      downloadHref={job.downloadUrl ?? undefined}
      retryHref="/exports/new"
    />
  )
}
```

For live polling, L4 wraps this in a client component that calls SWR/React Query
and passes updated `status`/`progress` props on each refetch interval.

## Layer dependencies

- **L1**: No direct imports (uses Tailwind utility classes)
- **L2**: No L2 blocks imported directly
- **L4**: Owns polling logic, job API calls, and download URL resolution
