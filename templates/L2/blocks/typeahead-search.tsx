/*
---
template_id: L2/blocks/typeahead-search
layer: L2
provenance_class: internal_design
evidence:
  - source_type: external
    citation: "MDN Web Docs — CompositionEvent: compositionstart/compositionend lifecycle for IME (Input Method Editor). Korean (한글) syllable assembly fires compositionstart when a syllable block begins and compositionend when it is finalised. Debounced search MUST NOT fire during active composition."
    url: "https://developer.mozilla.org/en-US/docs/Web/API/CompositionEvent"
  - source_type: external
    citation: "React Documentation — useRef: Stable ref object persists across renders without causing re-renders. Use isComposingRef to track IME state without triggering re-render on every keystroke."
    url: "https://react.dev/reference/react/useRef"
imports_from: [L1, L2]
imports_forbidden: [L4, app/, lib/auth/]
---
*/
'use client'

import * as React from 'react'

// ─── types ────────────────────────────────────────────────────────────────────

export interface TypeaheadSearchProps {
  /**
   * Called with the debounced query string.
   * NOT called during active Korean IME composition.
   */
  onQuery: (query: string) => void
  /** Debounce delay in ms. @default 300 */
  debounceMs?: number
  /** Placeholder text. */
  placeholder?: string
  /** Minimum query length before triggering onQuery. @default 1 */
  minLength?: number
  /** Show spinner while loading. @default false */
  isLoading?: boolean
  /** Controlled value. Leave undefined for uncontrolled. */
  value?: string
  /** Called on every keystroke (uncontrolled mode notifier). */
  onChange?: (value: string) => void
  className?: string
  inputClassName?: string
  id?: string
}

// ─── component ───────────────────────────────────────────────────────────────

/**
 * TypeaheadSearch — debounced search input with Korean IME (한글) guard.
 *
 * IME guard:
 *   Korean syllables are assembled by the OS input method (e.g. ㄱ+ㅏ+ㅇ → 강).
 *   During assembly, `compositionstart` fires; `compositionend` fires when the
 *   syllable is committed. This component suppresses debounce triggers while
 *   `isComposingRef.current === true` so that partial syllables never reach the
 *   backend. The final composed value is flushed via `compositionend`.
 *
 * Fork instructions:
 *   1. Wire `onQuery` to your search API call (e.g. TanStack Query `setQueryKey`).
 *   2. Adjust `debounceMs` for your latency budget (300ms is a sensible default).
 *   3. Pass `isLoading` from your query state to show the spinner.
 *   4. Wrap with a `<div>` + dropdown `<ul>` to render suggestions below the input.
 *
 * @example
 * ```tsx
 * const [query, setQuery] = React.useState('')
 * const { data, isFetching } = useSearchQuery(query)
 *
 * <TypeaheadSearch
 *   placeholder="검색어 입력…"
 *   onQuery={setQuery}
 *   isLoading={isFetching}
 * />
 * ```
 */
export default function TypeaheadSearch({
  onQuery,
  debounceMs = 300,
  placeholder = '검색어 입력…',
  minLength = 1,
  isLoading = false,
  value: controlledValue,
  onChange,
  className,
  inputClassName,
  id,
}: TypeaheadSearchProps) {
  const isControlled = controlledValue !== undefined
  const [internalValue, setInternalValue] = React.useState('')
  const displayValue = isControlled ? controlledValue : internalValue

  const isComposingRef = React.useRef(false)
  const timerRef = React.useRef<ReturnType<typeof setTimeout> | null>(null)

  // ── Debounce helper ───────────────────────────────────────────────────────
  function scheduleQuery(v: string) {
    if (timerRef.current) clearTimeout(timerRef.current)
    timerRef.current = setTimeout(() => {
      if (!isComposingRef.current && v.length >= minLength) {
        onQuery(v)
      }
    }, debounceMs)
  }

  function clearTimer() {
    if (timerRef.current) {
      clearTimeout(timerRef.current)
      timerRef.current = null
    }
  }

  React.useEffect(() => () => clearTimer(), [])

  // ── Input change ──────────────────────────────────────────────────────────
  function handleChange(e: React.ChangeEvent<HTMLInputElement>) {
    const v = e.target.value
    if (!isControlled) setInternalValue(v)
    onChange?.(v)

    // Suppress debounce during IME composition
    if (!isComposingRef.current) {
      scheduleQuery(v)
    }
  }

  // ── IME lifecycle ─────────────────────────────────────────────────────────
  function handleCompositionStart() {
    isComposingRef.current = true
    clearTimer()
  }

  function handleCompositionEnd(e: React.CompositionEvent<HTMLInputElement>) {
    isComposingRef.current = false
    const v = e.currentTarget.value
    if (!isControlled) setInternalValue(v)
    onChange?.(v)
    scheduleQuery(v)
  }

  // ── Clear button ──────────────────────────────────────────────────────────
  function handleClear() {
    clearTimer()
    if (!isControlled) setInternalValue('')
    onChange?.('')
    onQuery('')
  }

  const showClear = displayValue.length > 0 && !isLoading

  return (
    <div className={['relative flex items-center', className ?? ''].join(' ')}>
      {/* Search icon */}
      <svg
        xmlns="http://www.w3.org/2000/svg"
        width={16}
        height={16}
        viewBox="0 0 24 24"
        fill="none"
        stroke="currentColor"
        strokeWidth={2}
        className="absolute left-3 text-muted-foreground pointer-events-none"
        aria-hidden="true"
      >
        <circle cx={11} cy={11} r={8} />
        <path d="m21 21-4.35-4.35" />
      </svg>

      <input
        id={id}
        type="search"
        autoComplete="off"
        autoCorrect="off"
        autoCapitalize="off"
        spellCheck={false}
        value={displayValue}
        placeholder={placeholder}
        onChange={handleChange}
        onCompositionStart={handleCompositionStart}
        onCompositionEnd={handleCompositionEnd}
        className={[
          'w-full pl-9 pr-8 py-2 text-sm rounded-lg border bg-background',
          'placeholder:text-muted-foreground focus:outline-none focus:ring-2 focus:ring-ring',
          inputClassName ?? '',
        ].join(' ')}
        aria-label={placeholder}
        role="searchbox"
        aria-busy={isLoading}
      />

      {/* Loading spinner */}
      {isLoading && (
        <span
          className="absolute right-3 animate-spin text-muted-foreground"
          aria-label="검색 중"
        >
          <svg
            xmlns="http://www.w3.org/2000/svg"
            width={14}
            height={14}
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            strokeWidth={2}
            aria-hidden="true"
          >
            <path d="M21 12a9 9 0 1 1-6.219-8.56" />
          </svg>
        </span>
      )}

      {/* Clear button */}
      {showClear && (
        <button
          type="button"
          onClick={handleClear}
          className="absolute right-3 text-muted-foreground hover:text-foreground"
          aria-label="검색어 지우기"
        >
          <svg
            xmlns="http://www.w3.org/2000/svg"
            width={14}
            height={14}
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            strokeWidth={2}
            aria-hidden="true"
          >
            <path d="M18 6 6 18M6 6l12 12" />
          </svg>
        </button>
      )}
    </div>
  )
}
