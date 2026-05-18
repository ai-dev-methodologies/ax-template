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
