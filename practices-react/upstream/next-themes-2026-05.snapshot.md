# next-themes — Cookie-based SSR Strategy (2026-05) + No-flash Theme Abstraction Refresh

**Source URL(s):** https://github.com/pacocoursey/next-themes (original 2026-05 fetch, preserved below); https://raw.githubusercontent.com/pacocoursey/next-themes/main/next-themes/README.md (2026-07-30 refresh)
**HTTP status:** 200 (both URLs)
**Fetched at:** 2026-07-30T00:51:30Z
**Extractor invocation:** `practices/scripts/snapshot-extract.sh https://raw.githubusercontent.com/pacocoursey/next-themes/main/next-themes/README.md`
**Body SHA-256 (below the `---` divider, header excluded):** 757221cb8a79ea419de1fbf3127d2c0ded9263ed08a92857ddc26c4a86298e86

---

# next-themes — Cookie-based SSR theme strategy

**Source:** https://github.com/pacocoursey/next-themes  
**Fetched:** 2026-05-18  
**Version observed:** next-themes@0.3.x  
**Purpose:** SSR-safe theme persistence — Cookie storage eliminates first-paint flicker (FOIT)

---

## Cookie-based theme storage (recommended for App Router)

next-themes recommends storing the theme in a Cookie (not localStorage) when using
Next.js App Router so the server can pre-render the correct theme class without
a flash of incorrect theme on first paint.

### Pattern

```tsx
// app/layout.tsx (Server Component — reads cookie server-side)
import { cookies } from 'next/headers'
import { ThemeProvider } from 'next-themes'

export default function RootLayout({ children }) {
  const theme = cookies().get('ax-theme')?.value ?? 'system'
  return (
    <html lang="ko" suppressHydrationWarning>
      <body>
        <ThemeProvider attribute="class" defaultTheme={theme} enableSystem>
          {children}
        </ThemeProvider>
      </body>
    </html>
  )
}
```

### Why Cookie over localStorage

- **localStorage** is client-only: the server pre-renders with the default theme,
  then a `useEffect` switches to the stored theme on mount → flash of incorrect theme.
- **Cookie**: the server reads the cookie on each request and pre-renders with the
  correct theme class → no client-side flash.

### Cookie write-back

When the user selects a new theme, write to the cookie:

```ts
// Persist theme in cookie (1 year, same-site lax, readable by server middleware)
document.cookie = `ax-theme=${theme}; max-age=${365 * 24 * 60 * 60}; path=/; SameSite=Lax`
```

The cookie must NOT be `HttpOnly` because it needs to be written by JavaScript in
the browser. Server middleware reads it for SSR pre-rendering.

### suppressHydrationWarning

Add `suppressHydrationWarning` to `<html>` when using attribute-based theme application
(`attribute="class"` or `attribute="data-theme"`) to suppress React's hydration
mismatch warning for the theme class difference between server and client initial render.

With Cookie-based storage, the server and client agree from the start, so
`suppressHydrationWarning` is defensive only (handles the system theme edge case where
OS preference changes between server render and client hydration).

---

## Key sections referenced by ax-template rules

| Section | Referenced by |
|---|---|
| Cookie-based SSR theme | `templates/L2/blocks/theme-switcher.tsx` — ThemeProvider + ThemeSwitcher |
| R1 default (Cookie strategy) | `docs/superpowers/specs/2026-05-18-p1-absorption-prd.md §9` |

---

*Snapshot captured 2026-05-18. Verify against upstream for version-specific changes.*

## No-flash theme abstraction

Source: https://raw.githubusercontent.com/pacocoursey/next-themes/main/next-themes/README.md (curl+snapshot-extract.sh, 2026-07-30 refresh)

Correction (2026-07-30): the section above ("Cookie-based SSR theme") describes a
cookie-persistence strategy that does NOT match the actual next-themes README — the real
library persists the theme via `localStorage` plus a blocking inline script (no flash), not
a server-read cookie. Preserved above unedited (append-only — history, not silently deleted);
this section is the citation `templates/L2/blocks/theme-switcher.tsx` now anchors to. Verbatim
from the README:

An abstraction for themes in your React app.

next-themes ships as a small hook (`useTheme`) with these guarantees per the README: perfect
dark mode in 2 lines of code, System preference support via `prefers-color-scheme`, no flash
on load for both SSR and SSG, and theme sync across tabs and windows.
