'use client'
/*
---
template_id: L3/pages/error-page/error
layer: L3
provenance_class: internal_design
evidence:
  - source_type: external
    citation: "Next.js 15 App Router error.tsx — error boundary with reset"
    url: "https://nextjs.org/docs/app/building-your-application/routing/error-handling"
  - source_type: internal
    rationale: "Generic error boundary for ax-template L4. Must be 'use client' per Next.js convention. Exposes error details in development only."
dependencies: []
---
*/
import { useEffect } from 'react'

/**
 * Error — Next.js App Router error.tsx convention.
 *
 * Must be a Client Component ('use client') as it receives error and reset
 * as props from the Next.js error boundary. Automatically wraps the
 * route segment's children.
 *
 * Props (injected by Next.js):
 *   - error   Error object (message is safe to display; only expose
 *             stack/digest in development environments)
 *   - reset   Callback that re-renders the error boundary's children
 *
 * L4 usage: Copy this file to your L4 route segment directory. Customize
 * the message and add error reporting (Sentry, Datadog, etc.) in useEffect.
 */
interface ErrorProps {
  error: Error & { digest?: string }
  reset: () => void
}

export default function ErrorPage({ error, reset }: ErrorProps) {
  useEffect(() => {
    // TODO (L4): Report to your error tracking service here
    // e.g. captureException(error)
    console.error('[ErrorBoundary]', error)
  }, [error])

  return (
    <main
      className="flex min-h-svh flex-col items-center justify-center px-4 text-center space-y-6"
      role="alert"
      aria-live="assertive"
    >
      {/* Icon */}
      <div className="flex h-16 w-16 items-center justify-center rounded-full bg-destructive/10 text-destructive text-2xl">
        ⚠
      </div>

      <div className="space-y-2">
        <h1 className="text-2xl font-semibold tracking-tight">
          Something went wrong
        </h1>
        <p className="text-sm text-muted-foreground max-w-sm">
          An unexpected error occurred. You can try again or go back to the
          previous page.
        </p>

        {/* Show digest in production for support reference; show message in dev */}
        {process.env.NODE_ENV === 'development' ? (
          <p className="mt-2 font-mono text-xs text-muted-foreground bg-muted rounded px-3 py-2 max-w-sm text-left break-all">
            {error.message}
          </p>
        ) : error.digest ? (
          <p className="text-xs text-muted-foreground">
            Error reference: <code>{error.digest}</code>
          </p>
        ) : null}
      </div>

      <div className="flex gap-3">
        <button
          onClick={reset}
          className="inline-flex items-center justify-center rounded-md bg-primary px-5 py-2 text-sm font-medium text-primary-foreground hover:opacity-90 transition-opacity"
        >
          Try again
        </button>
        <a
          href="/"
          className="inline-flex items-center justify-center rounded-md border px-5 py-2 text-sm font-medium hover:bg-muted transition-colors"
        >
          Go home
        </a>
      </div>
    </main>
  )
}
