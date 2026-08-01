/*
---
template_id: L2/blocks/error-boundary
layer: L2
provenance_class: external_canonical
evidence:
  # Both anchors re-anchored 2026-08-01 (BACKLOG P2-73): react.dev/reference/react/Component
  # was rewritten and contains neither previous sentence. Quotes below are copied verbatim
  # from the 2026-08-01 extractor output appended to the snapshot.
  - source_type: upstream_id
    upstream_id: react-19-error-boundary
    section: "Required lifecycle methods"
    quote: "Typically, it is used together with static getDerivedStateFromError which lets you update state in response to an error and display an error message to the user. A component with these methods is called an Error Boundary"
  # NARROWER THAN THE CLAIM IT REPLACES, deliberately: the live page states the
  # change-the-key reset pattern as a general state-reset mechanism, and no longer states it
  # for error boundaries specifically. The quote is what the page says; the error-boundary
  # application of it is this template's own design decision, not an upstream claim.
  - source_type: upstream_id
    upstream_id: react-19-error-boundary
    section: "Reset key pattern"
    quote: "If you want to \"reset\" some state when a prop changes, consider either making a component fully controlled or fully uncontrolled with a key instead."
dependencies: []
imports_from: [L1]
imports_forbidden: [L4, app/, lib/auth/, lib/payment/]
---
*/
import * as React from 'react'

// ─── types ────────────────────────────────────────────────────────────────────

export interface ErrorFallbackProps {
  error: Error
  resetError: () => void
}

export interface ErrorBoundaryProps {
  children: React.ReactNode
  /** Static fallback node OR render prop receiving error + reset. */
  fallback?: React.ReactNode | ((props: ErrorFallbackProps) => React.ReactNode)
  /**
   * Change this value to reset the boundary (remounts the child subtree).
   * L4 usage: increment on "Try again" button click.
   */
  resetKey?: string | number
  /** Called on every caught error (use for telemetry / traceId logging). */
  onError?: (error: Error, info: React.ErrorInfo) => void
}

interface ErrorBoundaryState {
  hasError: boolean
  error: Error | null
}

// ─── class component (required by React error boundary API) ───────────────────

class ErrorBoundary extends React.Component<ErrorBoundaryProps, ErrorBoundaryState> {
  constructor(props: ErrorBoundaryProps) {
    super(props)
    this.state = { hasError: false, error: null }
    this.resetError = this.resetError.bind(this)
  }

  static getDerivedStateFromError(error: Error): ErrorBoundaryState {
    return { hasError: true, error }
  }

  componentDidCatch(error: Error, info: React.ErrorInfo): void {
    this.props.onError?.(error, info)
  }

  componentDidUpdate(prevProps: ErrorBoundaryProps): void {
    // Reset when resetKey changes so the parent can trigger recovery.
    if (
      this.state.hasError &&
      prevProps.resetKey !== this.props.resetKey
    ) {
      this.setState({ hasError: false, error: null })
    }
  }

  resetError(): void {
    this.setState({ hasError: false, error: null })
  }

  render(): React.ReactNode {
    if (!this.state.hasError) {
      return this.props.children
    }

    const { fallback } = this.props
    const error = this.state.error ?? new Error('Unknown render error')

    if (typeof fallback === 'function') {
      return fallback({ error, resetError: this.resetError })
    }

    if (fallback != null) {
      return fallback
    }

    // Built-in minimal fallback
    return (
      <div role="alert" style={{ padding: '1rem', border: '1px solid red', borderRadius: '4px' }}>
        <strong>Something went wrong.</strong>
        <p style={{ fontSize: '0.875rem', marginTop: '0.5rem', color: '#666' }}>
          {error.message}
        </p>
        <button
          onClick={this.resetError}
          style={{ marginTop: '0.75rem', padding: '0.25rem 0.75rem', cursor: 'pointer' }}
        >
          Try again
        </button>
      </div>
    )
  }
}

export default ErrorBoundary
