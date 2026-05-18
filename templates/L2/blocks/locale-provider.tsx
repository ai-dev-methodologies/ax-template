/*
---
template_id: L2/blocks/locale-provider
layer: L2
provenance_class: internal_design
evidence:
  - source_type: upstream_id
    upstream_id: next-intl-2026-05
    section: "LocaleProvider / NextIntlClientProvider"
    quote: "Messages must be provided to the client via NextIntlClientProvider or a custom LocaleProvider wrapper"
  - source_type: external
    citation: "next-intl docs — NextIntlClientProvider wraps client subtree with locale and messages"
    url: "https://next-intl.dev/docs/getting-started/app-router"
    quoted_at: "2026-05-18"
dependencies: ["next-intl"]
imports_from: [L1]
imports_forbidden: [L4, app/, lib/auth/]
---
*/
import * as React from 'react'
import { NextIntlClientProvider } from 'next-intl'

export interface LocaleProviderProps {
  /**
   * BCP 47 locale string, e.g. "ko" or "en".
   * Passed to NextIntlClientProvider as `locale`.
   */
  locale: string
  /**
   * Flat message dictionary keyed by namespace then message key.
   * Load from `getMessages()` on the server side in the layout.
   */
  messages: Record<string, Record<string, string>>
  /** Client component tree */
  children: React.ReactNode
  /**
   * Optional timezone string, e.g. "Asia/Seoul".
   * Defaults to the server timezone when omitted.
   */
  timeZone?: string
}

/**
 * LocaleProvider — L2 block.
 *
 * Wraps the client-side subtree with next-intl's `NextIntlClientProvider`.
 * Must be placed in `app/[locale]/layout.tsx` (or equivalent) where the
 * `locale` segment and translated messages are available.
 *
 * Usage (in `app/[locale]/layout.tsx`):
 * ```tsx
 * import { getMessages } from 'next-intl/server';
 *
 * export default async function LocaleLayout({ children, params }) {
 *   const messages = await getMessages();
 *   return (
 *     <LocaleProvider locale={params.locale} messages={messages} timeZone="Asia/Seoul">
 *       {children}
 *     </LocaleProvider>
 *   );
 * }
 * ```
 */
export function LocaleProvider({
  locale,
  messages,
  children,
  timeZone,
}: LocaleProviderProps) {
  return (
    <NextIntlClientProvider locale={locale} messages={messages} timeZone={timeZone}>
      {children}
    </NextIntlClientProvider>
  )
}
