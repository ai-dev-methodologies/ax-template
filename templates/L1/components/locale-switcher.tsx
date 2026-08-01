/*
---
template_id: L1/components/locale-switcher
layer: L1
provenance_class: internal_design
evidence:
  # Re-anchored 2026-08-01 (BACKLOG P2-73). The previous quote was an ADAPTED config
  # fragment ('ko'/'en' are this template's locales, not the page's), so it was never
  # verbatim page text. PROTECTED LEDGER IDENTITY — re-anchored, not deleted. Quote copied
  # verbatim from the 2026-08-01 extractor output appended to the snapshot.
  - source_type: upstream_id
    upstream_id: next-intl-2026-05
    section: "Middleware (locale detection)"
    quote: "If your app doesn't require unique pathnames per locale, you can provide a locale to next-intl based on user preferences or other application logic."
  - source_type: external
    citation: "next-intl docs — useLocale, useRouter for locale switching"
    url: "https://next-intl.dev/docs/routing/navigation"
    quoted_at: "2026-05-18"
a11y_criteria:
  - "WCAG 2.2 SC 4.1.2 Name/Role/Value — select has aria-label"
  - "WCAG 2.2 SC 2.4.6 Headings and Labels — label identifies language"
dependencies: ["next-intl"]
imports_from: []
imports_forbidden: [L2, L3, L4, app/, lib/auth/]
---
*/
'use client'

import { useLocale, useTranslations } from 'next-intl'
import { useRouter, usePathname } from 'next/navigation'
import { useTransition } from 'react'

export type SupportedLocale = 'ko' | 'en'

export interface LocaleSwitcherProps {
  /** Available locales to switch between. Defaults to ['ko', 'en']. */
  locales?: SupportedLocale[]
  /** Optional CSS class override */
  className?: string
}

const LOCALE_LABELS: Record<SupportedLocale, string> = {
  ko: '한국어',
  en: 'English',
}

/**
 * LocaleSwitcher — L1 primitive.
 *
 * Renders a `<select>` allowing the user to switch between available locales.
 * On change, replaces the current route's locale segment and navigates.
 *
 * Usage:
 * ```tsx
 * <LocaleSwitcher locales={['ko', 'en']} />
 * ```
 */
export function LocaleSwitcher({
  locales = ['ko', 'en'],
  className,
}: LocaleSwitcherProps) {
  const locale = useLocale() as SupportedLocale
  const router = useRouter()
  const pathname = usePathname()
  const [isPending, startTransition] = useTransition()

  function handleChange(next: SupportedLocale) {
    startTransition(() => {
      // next-intl router.replace preserves the current pathname but changes locale.
      // eslint-disable-next-line @typescript-eslint/no-explicit-any
      router.replace(pathname, { locale: next } as any)
    })
  }

  return (
    <select
      value={locale}
      onChange={(e) => handleChange(e.target.value as SupportedLocale)}
      disabled={isPending}
      aria-label="언어 선택 / Select language"
      className={className}
    >
      {locales.map((l) => (
        <option key={l} value={l}>
          {LOCALE_LABELS[l]}
        </option>
      ))}
    </select>
  )
}
