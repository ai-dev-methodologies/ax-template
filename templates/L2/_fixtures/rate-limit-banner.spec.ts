/**
 * TDD Anchor — R56 rate-limit-banner.spec.ts
 *
 * Purpose: Static assertions on the L2 rate-limit-banner primitive
 * introduced in R56. Mirrors the auto-save-indicator.spec.ts pattern —
 * file existence + structural invariants that a future refactor must
 * not silently break.
 *
 * Pre-R56: rate-limit-banner.tsx ENOENT → fixture fails.
 * Post-R56: component ships; all static checks pass.
 *
 * @fixture rate-limit-banner
 * @layer L2
 * @component RateLimitBanner / RateLimitBannerProvider
 */

import { test, expect } from '@playwright/test'
import * as fs from 'node:fs'
import * as path from 'node:path'

const REPO_ROOT = path.resolve(__dirname, '../../../..')
const COMPONENT = path.join(
  REPO_ROOT,
  'templates/L2/blocks/rate-limit-banner.tsx',
)

// ─── Static checks ────────────────────────────────────────────────────────────

test.describe('rate-limit-banner.tsx — static assertions', () => {
  test('exists in templates/L2/blocks/', () => {
    expect(fs.existsSync(COMPONENT), 'RATE_LIMIT_BANNER_MISSING').toBe(true)
  })

  test('exports the four public surface symbols', () => {
    const content = fs.readFileSync(COMPONENT, 'utf-8')
    const exports = [
      'parseRetryAfter',
      'extractRetryAfterFrom429',
      'useRateLimitBanner',
      'RateLimitBannerProvider',
    ]
    for (const sym of exports) {
      expect(content, `Missing export: ${sym}`).toMatch(new RegExp(`export[^\\n]+\\b${sym}\\b`))
    }
  })

  test('parseRetryAfter handles both RFC 9110 forms (delta-seconds + HTTP-date)', () => {
    const content = fs.readFileSync(COMPONENT, 'utf-8')
    // delta-seconds: must accept positive integers
    expect(content).toMatch(/\^\\d\+\$/)
    // HTTP-date: must invoke Date.parse
    expect(content).toContain('Date.parse')
  })

  test('extractRetryAfterFrom429 returns null on non-429', () => {
    const content = fs.readFileSync(COMPONENT, 'utf-8')
    // Must short-circuit on non-429 to let callers chain without an outer if.
    expect(content).toMatch(/status\s*!==\s*429/)
    expect(content).toMatch(/return\s+null/)
  })

  test('uses role=status + aria-live=polite (WCAG 2.2 SC 4.1.3)', () => {
    const content = fs.readFileSync(COMPONENT, 'utf-8')
    // polite, not assertive — rate-limit is a wait surface, not an interrupt.
    expect(content).toContain('role="status"')
    expect(content).toContain('aria-live="polite"')
  })

  test('frontmatter cites RFC 6585 §4 + RFC 9110 §10.2.3 + WCAG SC 4.1.3', () => {
    const content = fs.readFileSync(COMPONENT, 'utf-8')
    expect(content, 'missing RFC 6585 evidence').toContain('RFC 6585')
    expect(content, 'missing RFC 9110 Retry-After evidence').toContain('RFC 9110')
    expect(content, 'missing WCAG 2.2 SC 4.1.3 evidence').toContain('Status Messages')
  })

  test('provider exposes notify429 / dismiss / state hook surface', () => {
    const content = fs.readFileSync(COMPONENT, 'utf-8')
    expect(content).toContain('notify429')
    expect(content).toContain('dismiss')
  })

  test('countdown ticker uses single setInterval scoped to active state', () => {
    const content = fs.readFileSync(COMPONENT, 'utf-8')
    expect(content).toContain('setInterval')
    expect(content).toContain('clearInterval')
  })

  test('auto-dismiss when secondsRemaining reaches 0', () => {
    const content = fs.readFileSync(COMPONENT, 'utf-8')
    // The component must clear state at the end of the countdown, not hold
    // the banner forever after retry-after expires.
    expect(content).toMatch(/setState\(null\)|setTimeout.*setState\(null\)/s)
  })

  test('formatMessage prop allows fork-receiver copy override', () => {
    const content = fs.readFileSync(COMPONENT, 'utf-8')
    expect(content).toContain('formatMessage')
  })
})
