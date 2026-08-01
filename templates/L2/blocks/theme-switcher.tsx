/*
---
template_id: L2/blocks/theme-switcher
layer: L2
provenance_class: external_canonical
evidence:
  - upstream_id: next-themes-2026-05
    section: "No-flash theme abstraction"
    quote: "An abstraction for themes in your React app."
  - source_type: external
    citation: "next-themes persists the selected theme in localStorage and prevents the first-paint flash with a blocking inline script — NOT with a server-read cookie. Corrected 2026-08-01 (BACKLOG P2-63): the entry that stood here attributed a 'Cookie strategy for SSR' to next-themes, which the library's README does not describe. The cookie storage below is an ax-template design choice (next evidence entry), not upstream guidance."
    url: "https://raw.githubusercontent.com/pacocoursey/next-themes/main/next-themes/README.md"
    quoted_at: "2026-08-01"
  - source_type: external
    citation: "PRD §9 R1 default: ThemeSwitcher must use Cookie-based storage so the server can pre-render the correct theme, eliminating SSR hydration mismatches and first-paint flicker. This is an ax-template decision that DIVERGES from next-themes (localStorage + blocking inline script); it is recorded here as internal provenance, not as an upstream claim."
    url: "internal:docs/superpowers/specs/2026-05-18-p1-absorption-prd.md#R1"
    quoted_at: "2026-05-18"
dependencies: []
imports_from: [L1]
imports_forbidden: [L4, app/, lib/auth/, lib/payment/]
---
*/
'use client'

import * as React from 'react'

export type Theme = 'light' | 'dark' | 'system'

// Cookie name must match the server-side reading convention.
const THEME_COOKIE = 'ax-theme'

/**
 * Read the current theme from the cookie (SSR-safe — runs only on client).
 */
function readThemeCookie(): Theme {
  if (typeof document === 'undefined') return 'system'
  const match = document.cookie.match(new RegExp(`(?:^|;\\s*)${THEME_COOKIE}=([^;]+)`))
  return (match?.[1] as Theme) ?? 'system'
}

/**
 * Persist the theme choice in a cookie (1 year, same-site lax, no httpOnly
 * because this must be readable by the Next.js server middleware).
 */
function writeThemeCookie(theme: Theme) {
  const maxAge = 365 * 24 * 60 * 60
  document.cookie = `${THEME_COOKIE}=${theme}; max-age=${maxAge}; path=/; SameSite=Lax`
}

/**
 * Apply the resolved theme to `<html data-theme="...">` and the
 * `prefers-color-scheme` media-query fallback.
 */
function applyTheme(theme: Theme) {
  const root = document.documentElement
  const resolved =
    theme === 'system'
      ? window.matchMedia('(prefers-color-scheme: dark)').matches
        ? 'dark'
        : 'light'
      : theme
  root.setAttribute('data-theme', resolved)
  root.classList.toggle('dark', resolved === 'dark')
}

// ─── context ──────────────────────────────────────────────────────────────────

interface ThemeContextValue {
  theme: Theme
  resolvedTheme: 'light' | 'dark'
  setTheme: (theme: Theme) => void
}

const ThemeContext = React.createContext<ThemeContextValue | null>(null)

export function useTheme(): ThemeContextValue {
  const ctx = React.useContext(ThemeContext)
  if (!ctx) throw new Error('useTheme must be used within <ThemeProvider>')
  return ctx
}

// ─── provider ─────────────────────────────────────────────────────────────────

export interface ThemeProviderProps {
  /**
   * Initial theme value, typically read from a cookie in the server component
   * and forwarded as a prop. Falls back to reading the cookie on the client.
   */
  initialTheme?: Theme
  children: React.ReactNode
}

/**
 * ThemeProvider — wraps the app and provides the theme context.
 *
 * ## Server component integration (recommended)
 *
 * ```tsx
 * // app/layout.tsx (Server Component)
 * import { cookies } from 'next/headers'
 * import { ThemeProvider } from 'templates/L2/blocks/theme-switcher'
 *
 * export default async function RootLayout({ children }) {
 *   const initialTheme = (cookies().get('ax-theme')?.value ?? 'system') as Theme
 *   return (
 *     <html lang="ko">
 *       <body>
 *         <ThemeProvider initialTheme={initialTheme}>
 *           {children}
 *         </ThemeProvider>
 *       </body>
 *     </html>
 *   )
 * }
 * ```
 *
 * This pattern eliminates the flash of incorrect theme (FOIT) by pre-rendering
 * the correct theme class server-side, avoiding the `useEffect` mount-gate
 * workaround that causes a visible flicker on first paint.
 *
 * next-themes solves the same problem differently (localStorage + a blocking
 * inline script). This template does not wrap or depend on next-themes —
 * `dependencies: []` — so the cookie above is ours to define; do not read the
 * code below as documentation of how next-themes behaves.
 */
export function ThemeProvider({ initialTheme, children }: ThemeProviderProps) {
  const [theme, setThemeState] = React.useState<Theme>(
    () => initialTheme ?? readThemeCookie()
  )

  const resolvedTheme: 'light' | 'dark' = React.useMemo(() => {
    if (theme !== 'system') return theme
    if (typeof window === 'undefined') return 'light'
    return window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light'
  }, [theme])

  const setTheme = React.useCallback((next: Theme) => {
    setThemeState(next)
    writeThemeCookie(next)
    applyTheme(next)
  }, [])

  // Sync system preference changes while 'system' is active
  React.useEffect(() => {
    if (theme !== 'system') return
    const mq = window.matchMedia('(prefers-color-scheme: dark)')
    const handler = () => applyTheme('system')
    mq.addEventListener('change', handler)
    return () => mq.removeEventListener('change', handler)
  }, [theme])

  // Apply theme on mount (handles the initial server-rendered state)
  React.useEffect(() => {
    applyTheme(theme)
  }, []) // eslint-disable-line react-hooks/exhaustive-deps

  return (
    <ThemeContext.Provider value={{ theme, resolvedTheme, setTheme }}>
      {children}
    </ThemeContext.Provider>
  )
}

// ─── ThemeSwitcher UI ─────────────────────────────────────────────────────────

const THEME_OPTIONS: { value: Theme; label: string; icon: string }[] = [
  { value: 'light', label: 'Light', icon: '☀️' },
  { value: 'dark', label: 'Dark', icon: '🌙' },
  { value: 'system', label: 'System', icon: '💻' },
]

export interface ThemeSwitcherProps {
  /** Custom class name for the container. */
  className?: string
  /** Show labels alongside icons (default: true). */
  showLabels?: boolean
}

/**
 * ThemeSwitcher — segmented button that lets users pick light / dark / system.
 *
 * Must be rendered inside `<ThemeProvider>`.
 *
 * ```tsx
 * import { ThemeProvider, ThemeSwitcher } from 'templates/L2/blocks/theme-switcher'
 * // inside your settings page:
 * <ThemeSwitcher />
 * ```
 *
 * ## SSR hydration safety
 *
 * Theme is stored in a Cookie so the server can read it in a Server Component and
 * pre-render the correct theme without a first-paint flicker. No
 * `suppressHydrationWarning` hack needed.
 *
 * This is an ax-template design choice, NOT what next-themes does: next-themes
 * persists the theme in `localStorage` and suppresses the flash with a blocking
 * inline script (see `practices-react/upstream/next-themes-2026-05.snapshot.md`,
 * "No-flash theme abstraction"). A cookie is used here because it is the only
 * store the Next.js server can read during rendering; the trade-off is that the
 * value is sent on every request and is not shared with a next-themes install.
 */
export default function ThemeSwitcher({ className, showLabels = true }: ThemeSwitcherProps) {
  const { theme, setTheme } = useTheme()

  return (
    <div
      role="group"
      aria-label="Color theme"
      className={[
        'inline-flex rounded-md border border-border divide-x divide-border',
        className ?? '',
      ]
        .filter(Boolean)
        .join(' ')}
    >
      {THEME_OPTIONS.map(({ value, label, icon }) => (
        <button
          key={value}
          type="button"
          role="radio"
          aria-checked={theme === value}
          onClick={() => setTheme(value)}
          className={[
            'flex items-center gap-1.5 px-3 py-1.5 text-sm transition-colors',
            'first:rounded-l-md last:rounded-r-md',
            'focus:outline-none focus-visible:ring-2 focus-visible:ring-ring',
            theme === value
              ? 'bg-primary text-primary-foreground font-medium'
              : 'bg-background text-foreground hover:bg-accent',
          ].join(' ')}
          data-testid={`theme-option-${value}`}
        >
          <span aria-hidden="true">{icon}</span>
          {showLabels && <span>{label}</span>}
        </button>
      ))}
    </div>
  )
}
