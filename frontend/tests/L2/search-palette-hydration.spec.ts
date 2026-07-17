/**
 * P1-67 audit closure — SearchPalette SSR/hydration mismatch from
 * `navigator.platform` read directly in the render body.
 *
 * TEST-INFRA NOTE (read before extending this file):
 *   search-palette.tsx statically imports the external npm package `cmdk`.
 *   Vite/vitest resolve bare specifiers by walking up the directory tree
 *   from the IMPORTING file. Because templates/L2/blocks/ lives OUTSIDE the
 *   frontend/ project root (where `cmdk` is actually installed, as a sibling
 *   directory rather than an ancestor), that walk never reaches
 *   frontend/node_modules/cmdk — so any attempt to import or render
 *   search-palette.tsx from a vitest test fails at Vite's import-analysis
 *   transform ("Failed to resolve import 'cmdk'"), before vi.mock('cmdk', …)
 *   interception even has a chance to run (verified empirically: both a
 *   hoisted vi.mock + static import and a vi.doMock + dynamic import()
 *   reproduce the identical resolve failure). This is the SAME class of gap
 *   already documented in frontend/tests/_stubs/next-navigation.ts, whose
 *   fix is a `resolve.alias` entry in the shared frontend/vitest.config.ts.
 *   That file is out of scope for this change (shared config; not a
 *   templates/L2/blocks/ file) — see the audit report for the reasoning
 *   proof that substitutes for a behavioral/render test here.
 *
 *   optimistic-update.tsx (P0-29, tested in optimistic-update-concurrency.
 *   vitest.tsx) has zero external npm dependencies, so it does not hit this
 *   gap and IS behaviorally render-tested.
 *
 * This file provides what IS reachable without that alias: structural
 * assertions on the source that fail on the pre-fix code and pass on the
 * fixed code (RED-on-revert), matching the existing precedent in
 * frontend/tests/L2/contract.spec.ts and templates/L2/_fixtures/dirty-guard.
 * spec.ts (both use file-content assertions rather than full DOM rendering).
 */
import { describe, it, expect } from 'vitest'
import * as fs from 'fs'
import * as path from 'path'

const REPO_ROOT = path.resolve(__dirname, '../../..')
const SOURCE_PATH = path.join(REPO_ROOT, 'templates/L2/blocks/search-palette.tsx')
const source = fs.readFileSync(SOURCE_PATH, 'utf-8')

describe('search-palette.tsx — SSR/hydration-safe shortcut symbol (P1-67, structural)', () => {
  it('gates the platform-dependent symbol behind a mount-state flag (hasMounted pattern)', () => {
    expect(source).toMatch(/const \[hasMounted, setHasMounted\] = React\.useState\(false\)/)
    expect(source).toMatch(/React\.useEffect\(\(\) => \{\s*setHasMounted\(true\)\s*\}, \[\]\)/)
  })

  it('the resolved shortcut symbol depends on hasMounted, defaulting SSR-safe to Ctrl', () => {
    const match = source.match(/const shortcutSymbol =\s*([\s\S]*?)\n\n/)
    expect(match, 'shortcutSymbol derivation not found').toBeTruthy()
    const derivation = match![1]
    expect(derivation).toContain('hasMounted')
    // Ctrl must be the fallback in the ternary (SSR / pre-mount default).
    expect(derivation.trim().endsWith("'Ctrl'")).toBe(true)
  })

  it('no remaining unconditional navigator.platform read in the render body', () => {
    // Pre-fix, `navigator?.platform?.includes('Mac')` appeared twice, inline
    // in JSX (aria-label + <kbd>), ungated by any mount check. Post-fix, the
    // ONLY *code* reference to navigator.platform must be inside the gated
    // `shortcutSymbol` derivation (already asserted above); JSX must consume
    // the derived variable, not re-read navigator directly. (Excludes the
    // doc-comment prose above the derivation, which also mentions the
    // property name in English.)
    const codeOnly = source.replace(/\/\*[\s\S]*?\*\//g, '').replace(/\/\/.*$/gm, '')
    const navigatorReads = codeOnly.match(/navigator\??\.platform/g) ?? []
    expect(navigatorReads.length).toBe(1)

    const jsxSymbolReads = source.match(/\{shortcutSymbol\}/g) ?? []
    // Used in both the aria-label template literal and the visible <kbd>.
    expect(jsxSymbolReads.length).toBeGreaterThanOrEqual(1)
    expect(source).not.toMatch(/aria-label=\{`검색 열기 \(\$\{navigator/)
    expect(source).not.toMatch(/>\s*\{navigator\?\.\s*platform/)
  })
})
