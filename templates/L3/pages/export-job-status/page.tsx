/*
---
template_id: L3/pages/export-job-status
layer: L3
provenance_class: internal_design
evidence:
  - source_type: external
    citation: "Next.js 16 App Router file conventions — page.tsx"
    url: "https://nextjs.org/docs/app/building-your-application/routing/pages"
  - source_type: external
    citation: "WAI-ARIA Authoring Practices — Progress indicator pattern"
    url: "https://www.w3.org/WAI/ARIA/apg/patterns/meter/"
  - source_type: internal_design
    rationale: "Generic long-running export job status skeleton. Accepts job status and progress as props. Polling logic lives in L4 (via SWR/React Query or server polling). Shows status badge, optional progress bar, and download link when complete."
imports_from: [L1, L2]
imports_forbidden: [L4]
---
*/
import * as React from 'react'

/**
 * ExportJobStatusPage — generic export job status skeleton.
 *
 * Slot props:
 *   - status        (required) current job status
 *   - jobName       (optional) human-readable job name
 *   - progress      (optional) 0–100 completion percentage
 *   - downloadHref  (optional) download URL shown when status is "completed"
 *   - downloadLabel (optional) download CTA label (default: "Download")
 *   - retryHref     (optional) retry link shown when status is "failed"
 *   - description   (optional) subtitle or instructions
 *   - pollingSlot   (optional) custom polling indicator (overrides built-in spinner)
 *
 * Polling:
 *   L4 polls the backend (SWR/React Query or server-action refresh) and re-renders
 *   this page with updated `status` + `progress` props. This template is stateless.
 *
 * L4 usage:
 *   import ExportJobStatusPage from 'templates/L3/pages/export-job-status/page'
 *   export default async function ExportStatusRoute({ params }) {
 *     const job = await getExportJob(params.jobId)
 *     return (
 *       <ExportJobStatusPage
 *         jobName="Product catalog export"
 *         status={job.status}
 *         progress={job.progress}
 *         downloadHref={job.downloadUrl}
 *         retryHref={`/exports/new`}
 *       />
 *     )
 *   }
 */
export type JobStatus = 'pending' | 'running' | 'completed' | 'failed'

export interface ExportJobStatusPageProps {
  /** Current job execution status */
  status: JobStatus
  /** Human-readable job name */
  jobName?: string
  /** Completion percentage 0–100 */
  progress?: number
  /** Download URL (shown when status is "completed") */
  downloadHref?: string
  /** Download CTA label (default: "Download") */
  downloadLabel?: string
  /** Retry link (shown when status is "failed") */
  retryHref?: string
  /** Optional subtitle */
  description?: string
  /** Custom polling indicator (overrides built-in animated spinner) */
  pollingSlot?: React.ReactNode
}

const STATUS_CONFIG: Record<
  JobStatus,
  { label: string; badge: string; icon: string }
> = {
  pending: {
    label: 'Waiting',
    badge: 'bg-muted text-muted-foreground',
    icon: '⏳',
  },
  running: {
    label: 'Processing',
    badge: 'bg-blue-100 text-blue-700',
    icon: '⚙️',
  },
  completed: {
    label: 'Complete',
    badge: 'bg-green-100 text-green-700',
    icon: '✓',
  },
  failed: {
    label: 'Failed',
    badge: 'bg-red-100 text-red-700',
    icon: '✕',
  },
}

export default function ExportJobStatusPage({
  status,
  jobName = 'Export job',
  progress,
  downloadHref,
  downloadLabel = 'Download',
  retryHref,
  description,
  pollingSlot,
}: ExportJobStatusPageProps) {
  const config = STATUS_CONFIG[status]
  const showProgress =
    progress !== undefined && (status === 'running' || status === 'pending')

  return (
    <main className="flex min-h-svh items-center justify-center px-4">
      <div className="w-full max-w-sm space-y-6 text-center">
        {/* Status icon */}
        <div
          className={[
            'mx-auto flex h-14 w-14 items-center justify-center rounded-full text-2xl',
            status === 'running' || status === 'pending'
              ? 'bg-blue-50'
              : status === 'completed'
              ? 'bg-green-100'
              : 'bg-red-100',
          ].join(' ')}
          role="img"
          aria-label={config.label}
        >
          {status === 'running'
            ? pollingSlot ?? (
                <span
                  className="h-8 w-8 animate-spin rounded-full border-4 border-primary border-t-transparent block"
                  aria-hidden="true"
                />
              )
            : config.icon}
        </div>

        {/* Heading + status badge */}
        <div className="space-y-2">
          <h1 className="text-xl font-semibold tracking-tight">{jobName}</h1>
          <span
            className={[
              'inline-flex items-center rounded-full px-3 py-1 text-xs font-medium',
              config.badge,
            ].join(' ')}
          >
            {config.label}
          </span>
        </div>

        {/* Description */}
        {description && (
          <p className="text-sm text-muted-foreground">{description}</p>
        )}

        {/* Progress bar */}
        {showProgress && (
          <div
            className="h-2 w-full rounded-full bg-muted overflow-hidden"
            role="progressbar"
            aria-valuenow={progress}
            aria-valuemin={0}
            aria-valuemax={100}
            aria-label={`${progress}% complete`}
          >
            <div
              className="h-full bg-primary transition-all duration-500"
              style={{ width: `${progress}%` }}
            />
          </div>
        )}

        {/* Download CTA */}
        {status === 'completed' && downloadHref && (
          <a
            href={downloadHref}
            download
            className="inline-flex items-center justify-center rounded-md bg-primary px-6 py-2 text-sm font-medium text-primary-foreground hover:opacity-90 transition-opacity"
          >
            {downloadLabel}
          </a>
        )}

        {/* Retry link */}
        {status === 'failed' && retryHref && (
          <a
            href={retryHref}
            className="inline-flex items-center justify-center rounded-md border px-6 py-2 text-sm font-medium hover:bg-muted transition-colors"
          >
            Try again
          </a>
        )}
      </div>
    </main>
  )
}
