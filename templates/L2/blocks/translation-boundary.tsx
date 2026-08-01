/*
---
template_id: L2/blocks/translation-boundary
layer: L2
provenance_class: internal_design
evidence:
  # Both anchors re-anchored 2026-08-01 (BACKLOG P2-73). The next-intl quote was an authored
  # positioning claim ("the canonical i18n library") that no next-intl page makes about
  # itself; the react.dev quote no longer exists on the rewritten Component page. Both are
  # PROTECTED LEDGER IDENTITIES — re-anchored, not deleted. Quotes below are copied verbatim
  # from the 2026-08-01 extractor output appended to each snapshot.
  - source_type: upstream_id
    upstream_id: next-intl-2026-05
    section: "Overview"
    quote: "next-intl provides the essential foundation for internationalization in Next.js apps."
  - source_type: upstream_id
    upstream_id: react-19-error-boundary
    section: "Required lifecycle methods"
    quote: "An Error Boundary is a special component that lets you display some fallback UI instead of the part that crashed"
  - source_type: external
    citation: "next-intl docs — error handling when translations are missing"
    url: "https://next-intl.dev/docs/environments/error-files"
    quoted_at: "2026-05-18"
dependencies: ["next-intl"]
imports_from: [L1]
imports_forbidden: [L4, app/, lib/auth/]
---
*/
'use client'

import * as React from 'react'

export interface TranslationBoundaryProps {
  /**
   * Content requiring translations.
   * If translations fail to load, `fallback` is rendered instead.
   */
  children: React.ReactNode
  /**
   * Fallback UI on translation load failure.
   * Defaults to a minimal indicator to avoid blank screens.
   */
  fallback?: React.ReactNode
  /**
   * Namespace (next-intl message namespace) rendered by this boundary.
   * Used for diagnostic labelling only.
   */
  namespace?: string
}

interface TranslationBoundaryState {
  hasError: boolean
  error: Error | null
}

/**
 * TranslationBoundary — L2 block.
 *
 * Error boundary specifically for i18n loading failures.
 * Wraps a subtree that depends on translations and renders `fallback`
 * when next-intl throws (e.g., missing message namespace).
 *
 * Usage:
 * ```tsx
 * <TranslationBoundary namespace="Payment" fallback={<Skeleton />}>
 *   <PaymentForm />
 * </TranslationBoundary>
 * ```
 *
 * Note: every user-facing string inside the boundary must use `t()`.
 * Hardcoded literals (e.g., <span>결제하기</span>) violate the
 * `no-hardcoded-user-facing-string-in-l4` rule for post-2026-05-18 paths.
 */
export class TranslationBoundary extends React.Component<
  TranslationBoundaryProps,
  TranslationBoundaryState
> {
  constructor(props: TranslationBoundaryProps) {
    super(props)
    this.state = { hasError: false, error: null }
  }

  static getDerivedStateFromError(error: Error): TranslationBoundaryState {
    return { hasError: true, error }
  }

  componentDidCatch(error: Error, info: React.ErrorInfo) {
    if (process.env.NODE_ENV !== 'production') {
      console.error(
        `[TranslationBoundary${this.props.namespace ? ` ns="${this.props.namespace}"` : ''}] translation error:`,
        error,
        info,
      )
    }
  }

  render() {
    if (this.state.hasError) {
      return (
        this.props.fallback ?? (
          <span role="status" aria-live="polite" style={{ opacity: 0.5 }}>
            ···
          </span>
        )
      )
    }
    return this.props.children
  }
}
