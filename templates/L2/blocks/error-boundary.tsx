/*
---
template_id: L2/blocks/error-boundary
layer: L2
provenance_class: external_canonical
evidence:
  - source_type: upstream_id
    upstream_id: react-19-error-boundary
    section: "Required lifecycle methods"
    quote: "Error boundaries are React class components that let you display some fallback UI instead of the component tree that crashed. They catch errors during rendering, in lifecycle methods, and in constructors of any components below them in the tree."
  - source_type: upstream_id
    upstream_id: react-19-error-boundary
    section: "Reset key pattern"
    quote: "A common pattern to reset an error boundary is to change a `key` prop. When `key` changes, React remounts the subtree from scratch, clearing the error state"
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
