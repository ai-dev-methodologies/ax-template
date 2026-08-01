# next-intl-2026-05 — upstream snapshot (2026-08-01 refresh, append-only)

**Source URL(s):** https://next-intl.dev/docs/getting-started/app-router (re-fetched 2026-08-01; every pre-existing section below the divider is preserved verbatim)
**HTTP status:** 200
**Fetched at:** 2026-08-01T01:47:00Z
**Extractor invocation:** `practices/scripts/snapshot-extract.sh https://next-intl.dev/docs/getting-started/app-router`
**Fetch receipt:** `practices/upstream/_FETCH-RECEIPTS.yaml` id `r100`
**Body SHA-256 (below the `---` divider, header excluded):** df8a728b09c679c375e8cb4e29e6a0c67fecc0fa5b124b603a62293da0b02490

---

# next-intl — Upstream Snapshot (2026-05)

**Source:** https://next-intl.dev/docs/getting-started/app-router
**Fetched:** 2026-05-18
**Via:** WebFetch
**Snapshot ID:** next-intl-2026-05

---

## Overview

`next-intl` is the canonical i18n library for Next.js App Router. It provides:
- `useTranslations()` hook — typed `t()` function for client and server components
- `getTranslations()` — async variant for server-only components
- `LocaleProvider` pattern — sets `locale` in context so `useTranslations` knows which messages to load
- Pluralization, rich text, and ICU message format support

## LocaleProvider / NextIntlClientProvider

Messages must be provided to the client via `NextIntlClientProvider` or a custom `LocaleProvider` wrapper:

```tsx
// app/[locale]/layout.tsx
import { NextIntlClientProvider } from 'next-intl';
import { getMessages } from 'next-intl/server';

export default async function LocaleLayout({ children, params: { locale } }) {
  const messages = await getMessages();
  return (
    <NextIntlClientProvider messages={messages}>
      {children}
    </NextIntlClientProvider>
  );
}
```

## useTranslations

```tsx
'use client';
import { useTranslations } from 'next-intl';

export function PayButton() {
  const t = useTranslations('Payment');
  return <button>{t('submit')}</button>; // ✅ — not a hardcoded literal
}
```

**Anti-pattern (hardcoded literal):**
```tsx
export function PayButton() {
  return <button>결제하기</button>; // ❌ — hardcoded Korean; breaks i18n
}
```

## KRW Currency Format

Per ISO 4217, Korean Won (KRW) has 0 decimal places. Correct format:
```ts
new Intl.NumberFormat('ko-KR', { style: 'currency', currency: 'KRW' }).format(1234);
// → '₩1,234'   (₩ prefix, comma-separated, no decimals)
```

Incorrect:
```ts
new Intl.NumberFormat('en-US', { style: 'currency', currency: 'KRW' }).format(1234);
// → 'KRW 1,234.00' — wrong symbol, wrong decimals
```

## Fallback Chain

Recommended fallback order for Korean enterprise:
```
ko-KR → en-US → key
```

If a translation is missing in `ko-KR`, `next-intl` falls back to `en-US`, then renders the message key itself.

## Middleware (locale detection)

```ts
// middleware.ts
import createMiddleware from 'next-intl/middleware';

export default createMiddleware({
  locales: ['ko', 'en'],
  defaultLocale: 'ko',
  localeDetection: true,
});
```

## Relative Time

`next-intl` provides `useFormatter()` for relative time:
```tsx
const format = useFormatter();
format.relativeTime(new Date('2024-01-01')); // → '1년 전'
```

## References

- next-intl docs: https://next-intl.dev/docs/getting-started/app-router
- ICU message format: https://unicode-org.github.io/icu/userguide/format_parse/messages/
- ISO 4217 KRW: https://www.iso.org/iso-4217-currency-codes.html
- Intl.NumberFormat MDN: https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Intl/NumberFormat

---

## Upstream refresh 2026-08-01 (verbatim extractor output)

Source: https://next-intl.dev/docs/getting-started/app-router
HTTP status: 200 · extracted bytes: 6086 · sha256: 335cd6a510ee2f7d1fa1cf480f370cdda762b4505cc1ab3a0b77759e1d89f1fe
Extractor: `practices/scripts/snapshot-extract.sh` (curl -> deterministic HTML->text; no model in the loop)
Fetch receipt: `practices/upstream/_FETCH-RECEIPTS.yaml` id `r100`

Everything above this divider is the previous snapshot, preserved byte-for-byte (append-only:
history is recorded, never rewritten). The block below is the UNMODIFIED extractor output for
the 2026-08-01 re-fetch of the same URL — it is the current upstream text, and any citation that
claims to quote this source verbatim must match it.

Next.js App Router internationalization (i18n) – Internationalization (i18n) for Next.js Skip to content Docs Learn Studio Beta Examples Blog Search GitHub GitHub Search Getting started App Router Pages Router Usage guide Translations Numbers Dates and times Lists Display names Request configuration useExtracted Next.js plugin Routing Setup Configuration Proxy / middleware Navigation Environments Server & Client Components Server Actions, Metadata & Route Handlers Error files (e.g. not-found) Testing Core library Runtime requirements Workflows & integrations TypeScript augmentation Localization management with Crowdin AI agents Linting messages Storybook Design principles Presented by Crowdin logo Light On This Page Getting started messages/en.json i18n/request.ts next.config.ts app/layout.tsx app/page.tsx Next steps Locale-based routing Provide a locale Internationalization isn’t just translating words Edit this page Docs Getting started App Router Next.js App Router internationalization (i18n) Prefer to watch a video? Set up next-intl Getting started If you haven’t done so already, create a Next.js app that uses the App Router and run: npm install next-intl Now, we’re going to create the following file structure: ├── messages │ ├── en.json │ └── ... ├── next.config.ts └── src ├── i18n │ └── request.ts └── app ├── layout.tsx └── page.tsx Let’s set up the files: messages/en.json Messages represent the translations that are available per language and can be provided either locally or loaded from a remote data source. The simplest option is to add JSON files in your local project folder: messages/en.json { "HomePage" : { "title" : " Hello world! " } } i18n/request.ts next-intl creates a request-scoped configuration object, which you can use to provide messages and other options based on the user’s locale to Server Components. src/i18n/request.ts import {getRequestConfig} from ' next-intl/server ' ; export default getRequestConfig ( async () => { // Static for now, we'll change this later const locale = ' en ' ; return { locale , messages : ( await import ( ` ../../messages/ ${ locale } .json ` )) . default }; }); Can I move this file somewhere else? This file is supported out-of-the-box as ./i18n/request.ts both in the src folder as well as in the project root with the extensions .ts , .tsx , .js and .jsx . If you prefer to move this file somewhere else, you can optionally provide a path to the plugin: next.config.ts const withNextIntl = createNextIntlPlugin ( // Specify a custom path here ' ./somewhere/else/request.ts ' ); next.config.ts Now, set up the plugin which links your i18n/request.ts file to next-intl . next.config.ts next.config.js next.config.ts import {NextConfig} from ' next ' ; import createNextIntlPlugin from ' next-intl/plugin ' ; const nextConfig : NextConfig = {} ; const withNextIntl = createNextIntlPlugin (); export default withNextIntl ( nextConfig ); next.config.js const createNextIntlPlugin = require ( ' next-intl/plugin ' ); const withNextIntl = createNextIntlPlugin (); /** @type { import('next').NextConfig } */ const nextConfig = {} ; module . exports = withNextIntl ( nextConfig ); app/layout.tsx To make your request configuration available to Client Components, you can wrap the children in your root layout with NextIntlClientProvider . app/layout.tsx import {NextIntlClientProvider} from ' next-intl ' ; type Props = { children : React . ReactNode ; }; export default async function RootLayout ( { children } : Props ) { return ( < html > < body > < NextIntlClientProvider > { children } </ NextIntlClientProvider > </ body > </ html > ); } app/page.tsx Use translations in your page components or anywhere else! app/page.tsx import {useTranslations} from ' next-intl ' ; export default function HomePage () { const t = useTranslations ( ' HomePage ' ); return < h1 > { t ( ' title ' ) } </ h1 > ; } In case of async components, you can use the awaitable getTranslations function instead: app/page.tsx import {getTranslations} from ' next-intl/server ' ; export default async function HomePage () { const t = await getTranslations ( ' HomePage ' ); return < h1 > { t ( ' title ' ) } </ h1 > ; } Next steps Locale-based routing If you’d like to use unique pathnames for every language that your app supports (e.g. /en/about or example.de/über-uns ), you can continue to set up a top-level [locale] segment for your app. Set up locale-based routing → Provide a locale If your app doesn’t require unique pathnames per locale, you can provide a locale to next-intl based on user preferences or other application logic. The simplest option is to use a cookie: src/i18n/request.ts import {cookies} from ' next/headers ' ; import {getRequestConfig} from ' next-intl/server ' ; export default getRequestConfig ( async () => { const store = await cookies (); const locale = store . get ( ' locale ' ) ?. value || ' en ' ; return { locale // ... }; }); Internationalization isn’t just translating words next-intl provides the essential foundation for internationalization in Next.js apps. It handles aspects like translations, date and number formatting, as well as internationalized routing. However, building for a global audience spans a wider range of topics: Choosing the right architecture and routing strategy for your app Integrating with backend services or a CMS Leveraging generative AI for content localization Streamlining your development workflow with TypeScript and IDE tooling Collaborating with your team using a translation management system Understanding all the pieces that contribute to a truly localized experience Mastering SEO for multilingual apps to reach global audiences Build international Next.js apps with confidence Learn how to build delightful, multilingual apps with Next.js—from the basics to advanced patterns, all through a real-world project. Get started → Getting started Pages Router Docs · Learn · Studio · Examples · Blog · v3 v4 X · Bluesky · GitHub · Hosted on Vercel
