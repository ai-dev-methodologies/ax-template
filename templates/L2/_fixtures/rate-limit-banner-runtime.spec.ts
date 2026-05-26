/**
 * TDD Anchor — R85 rate-limit-banner-runtime.spec.ts
 *
 * Purpose: Runtime fixture for the R56 rate-limit-banner L2 primitive.
 * R74 shipped static assertions on file structure + exports + a11y
 * attributes; R85 adds Playwright runtime checks against an in-memory
 * harness page that imports the provider and asserts:
 *   1. notify429(retryAfter) renders the sticky banner with role=status
 *   2. countdown text decrements over time (poll text content)
 *   3. auto-dismiss when secondsRemaining → 0 (banner removed from DOM)
 *   4. Dismiss button removes banner immediately
 *   5. Multiple notify429 calls update same banner (single ticker, no leak)
 *
 * Pre-R85: no runtime check existed — refactor that broke notify429
 * propagation could pass R74 static checks. R85 closes the gap.
 *
 * Harness shape: this fixture targets a fork-receiver project context
 * (real React + DOM). In the catalog source tree, Playwright runs the
 * suite with a small inline harness page that imports the provider; in
 * fork-receiver context, the same suite runs against the fork's app
 * shell. The catalog refuses to ship its own runtime harness because
 * Next.js + bundler choices are fork-receiver-owned.
 *
 * @fixture rate-limit-banner-runtime
 * @layer L2
 * @component RateLimitBannerProvider / useRateLimitBanner
 */

import { test, expect, type Page } from '@playwright/test'
import * as fs from 'node:fs'
import * as path from 'node:path'

const REPO_ROOT = path.resolve(__dirname, '../../../..')
const COMPONENT = path.join(
  REPO_ROOT,
  'templates/L2/blocks/rate-limit-banner.tsx',
)

// ─── precondition: the component must exist (R74 static check) ──────────────

test.beforeAll(() => {
  if (!fs.existsSync(COMPONENT)) {
    throw new Error(
      'RATE_LIMIT_BANNER_MISSING — runtime fixture requires the L2 primitive ' +
        'at templates/L2/blocks/rate-limit-banner.tsx (R56). Run R74 static ' +
        'check first.',
    )
  }
})

/**
 * Harness assumption: the fork-receiver project exposes a route that
 * mounts <RateLimitBannerProvider unknownDurationSec={N}> with a button
 * that calls notify429(retryAfter, source) on click. The route is at
 * /__test__/rate-limit-banner for catalog harness and at any path the
 * fork-receiver wires for their own harness. The data-testid attributes
 * on the harness button + display elements are the contract.
 *
 * Expected harness DOM:
 *   <RateLimitBannerProvider unknownDurationSec={30}>
 *     <button data-testid="notify-3s" onClick={() => notify429(3, 'test')}>
 *       Notify 3s
 *     </button>
 *     <button data-testid="notify-null" onClick={() => notify429(null, 'no-retry-after')}>
 *       Notify (no retry-after)
 *     </button>
 *   </RateLimitBannerProvider>
 *
 * The banner itself comes from the provider's auto-mount with
 * data-testid="rate-limit-banner".
 */

const HARNESS_PATH = process.env.RATE_LIMIT_HARNESS_PATH ?? '/__test__/rate-limit-banner'

test.describe('rate-limit-banner runtime behavior', () => {
  test('notify429(3) shows banner with role=status', async ({ page }: { page: Page }) => {
    await page.goto(HARNESS_PATH)
    await page.click('[data-testid="notify-3s"]')

    const banner = page.locator('[data-testid="rate-limit-banner"]')
    await expect(banner).toBeVisible()
    await expect(banner).toHaveAttribute('role', 'status')
    await expect(banner).toHaveAttribute('aria-live', 'polite')
  })

  test('countdown decrements over time', async ({ page }: { page: Page }) => {
    await page.goto(HARNESS_PATH)
    await page.click('[data-testid="notify-3s"]')

    const banner = page.locator('[data-testid="rate-limit-banner"]')
    // initial render: "retry in 3s"
    await expect(banner).toContainText('retry in 3s', { timeout: 1500 })
    // after ~1s: "retry in 2s"
    await expect(banner).toContainText('retry in 2s', { timeout: 1500 })
    // after ~2s: "retry in 1s"
    await expect(banner).toContainText('retry in 1s', { timeout: 1500 })
  })

  test('auto-dismiss when secondsRemaining → 0', async ({ page }: { page: Page }) => {
    await page.goto(HARNESS_PATH)
    await page.click('[data-testid="notify-3s"]')

    const banner = page.locator('[data-testid="rate-limit-banner"]')
    await expect(banner).toBeVisible()
    // after countdown completes (3s + ~1.5s final-state buffer) the
    // provider sets state to null and the banner unmounts.
    await page.waitForFunction(
      () => !document.querySelector('[data-testid="rate-limit-banner"]'),
      { timeout: 6000 },
    )
  })

  test('Dismiss button removes banner immediately', async ({ page }: { page: Page }) => {
    await page.goto(HARNESS_PATH)
    await page.click('[data-testid="notify-3s"]')

    const banner = page.locator('[data-testid="rate-limit-banner"]')
    await expect(banner).toBeVisible()
    await banner.getByRole('button', { name: 'Dismiss' }).click()
    await expect(banner).toBeHidden()
  })

  test('multiple notify429 calls update same banner (no ticker leak)', async ({
    page,
  }: { page: Page }) => {
    await page.goto(HARNESS_PATH)
    await page.click('[data-testid="notify-3s"]')
    // Fire a second notify429 before the first countdown completes.
    await page.waitForTimeout(500)
    await page.click('[data-testid="notify-3s"]')

    // Only ONE banner element exists at any time.
    const banners = page.locator('[data-testid="rate-limit-banner"]')
    await expect(banners).toHaveCount(1)
  })

  test('notify429(null) uses unknownDurationSec fallback', async ({
    page,
  }: { page: Page }) => {
    await page.goto(HARNESS_PATH)
    await page.click('[data-testid="notify-null"]')

    const banner = page.locator('[data-testid="rate-limit-banner"]')
    // Harness sets unknownDurationSec={30}; banner shows "retry in 30s"
    // for the first tick.
    await expect(banner).toContainText('retry in 30s', { timeout: 1500 })
  })
})
