/*
---
template_id: L2/blocks/loading-boundary
layer: L2
provenance_class: internal_design
evidence:
  - source_type: external
    citation: "React 19 — Suspense with a fallback UI"
    url: "https://react.dev/reference/react/Suspense"
  - source_type: internal
    rationale: "L2 common block — Suspense + ErrorBoundary wrapper with a customisable fallback slot."
dependencies: []
imports_from: [L1]
imports_forbidden: [L4, app/, lib/]
---
*/
import * as React from 'react'

// ─── minimal error boundary ───────────────────────────────────────────────────

interface ErrorBoundaryState {
  hasError: boolean
  error: Error | null
}

interface ErrorBoundaryProps {
  fallback?: React.ReactNode
  onError?: (error: Error, info: React.ErrorInfo) => void
  children: React.ReactNode
}

class ErrorBoundary extends React.Component<ErrorBoundaryProps, ErrorBoundaryState> {
  constructor(props: ErrorBoundaryProps) {
    super(props)
    this.state = { hasError: false, error: null }
  }

  static getDerivedStateFromError(error: Error): ErrorBoundaryState {
    return { hasError: true, error }
  }

  componentDidCatch(error: Error, info: React.ErrorInfo) {
    this.props.onError?.(error, info)
  }

  render() {
    if (this.state.hasError) {
      return this.props.fallback ?? (
        <div role="alert" className="rounded-md border border-destructive/30 bg-destructive/10 p-4 text-sm text-destructive">
          Something went wrong. Please try again.
        </div>
      )
    }
    return this.props.children
  }
}

// ─── loading boundary ─────────────────────────────────────────────────────────

export interface LoadingBoundaryProps {
  children: React.ReactNode
  /** Suspense fallback (default: spinner) */
  fallback?: React.ReactNode
  /** Error boundary fallback (default: error message) */
  errorFallback?: React.ReactNode
  /** Called when an error is caught */
  onError?: (error: Error, info: React.ErrorInfo) => void
}

/**
 * LoadingBoundary — L2 common block.
 *
 * Combines React.Suspense and a minimal ErrorBoundary.
 * Renders a spinner while async children are loading,
 * and an error fallback if they throw.
 *
 * ## Slot contract
 * | Slot           | Required | Description                     |
 * |---------------|----------|---------------------------------|
 * | children       | yes      | Async content (may suspend)     |
 * | fallback       | no       | Loading skeleton / spinner      |
 * | errorFallback  | no       | Error state UI                  |
 */
export default function LoadingBoundary({
  children,
  fallback,
  errorFallback,
  onError,
}: LoadingBoundaryProps) {
  const loadingFallback = fallback ?? (
    <div
      role="status"
      aria-label="Loading"
      className="flex items-center justify-center py-12"
    >
      <span
        aria-hidden="true"
        className="h-8 w-8 animate-spin rounded-full border-4 border-muted border-t-primary"
      />
    </div>
  )

  return (
    <ErrorBoundary fallback={errorFallback} onError={onError}>
      <React.Suspense fallback={loadingFallback}>{children}</React.Suspense>
    </ErrorBoundary>
  )
}
