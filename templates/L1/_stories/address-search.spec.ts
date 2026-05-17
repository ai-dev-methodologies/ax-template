/**
 * Story spec — SP14 address-search.spec.ts
 *
 * Smoke tests for the AddressSearch component (Kakao Postcode Widget v2 wrapper).
 *
 * Because Kakao widget is CDN-loaded and opens a modal, the story page MUST
 * accept an `injector` prop for DI. Tests use a mock injector that immediately
 * calls `oncomplete` with synthetic data — no real CDN request needed.
 *
 * Acceptance gate (from SP14 spec):
 *   Simulate opening widget → selecting "강남대로 396" → verify controlled
 *   value updates with { zonecode, roadAddress }.
 *
 * @story address-search
 * @layer L1
 * @component AddressSearch
 */

import { test, expect } from '@playwright/test'

const STORY_URL = 'http://localhost:3000/stories/address-search'

test.describe('AddressSearch', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto(STORY_URL)
    await page.waitForLoadState('networkidle')
  })

  test('renders address search trigger button', async ({ page }) => {
    const trigger = page.locator('[data-testid="address-search-trigger"]')
    await expect(trigger).toBeVisible()
  })

  test('renders read-only address display fields', async ({ page }) => {
    const roadInput = page.locator('[data-testid="address-road"]')
    const zoneInput = page.locator('[data-testid="address-zone"]')
    await expect(roadInput).toBeVisible()
    await expect(zoneInput).toBeVisible()
  })

  test('clicking trigger invokes injector and resolves address (mock)', async ({ page }) => {
    // The story page must expose a mock injector that calls oncomplete with:
    // { zonecode: "06290", roadAddress: "강남대로 396", jibunAddress: "역삼동 736-2" }
    // when the widget would open.

    const trigger = page.locator('[data-testid="address-search-trigger"]')
    await trigger.click()

    // After mock injector fires, controlled state should update
    const roadInput = page.locator('[data-testid="address-road"]')
    await expect(roadInput).toHaveValue('강남대로 396', { timeout: 3000 })

    const zoneInput = page.locator('[data-testid="address-zone"]')
    await expect(zoneInput).toHaveValue('06290', { timeout: 3000 })
  })

  test('address values are exposed as window.__addressResult after selection', async ({ page }) => {
    const trigger = page.locator('[data-testid="address-search-trigger"]')
    await trigger.click()

    const result = await page.evaluate(
      () => (window as unknown as { __addressResult?: { zonecode: string; roadAddress: string } }).__addressResult
    )
    expect(result?.roadAddress).toBe('강남대로 396')
    expect(result?.zonecode).toBe('06290')
  })

  test('focus returns to trigger button after address selection (WCAG 2.4.3)', async ({ page }) => {
    const trigger = page.locator('[data-testid="address-search-trigger"]')
    await trigger.click()

    // After selection, focus should return to the trigger (focus management)
    const focusedElement = await page.evaluate(() => {
      const el = document.activeElement as HTMLElement
      return el?.getAttribute('data-testid')
    })
    expect(focusedElement).toBe('address-search-trigger')
  })

  test('trigger button is keyboard accessible (Enter activates)', async ({ page }) => {
    const trigger = page.locator('[data-testid="address-search-trigger"]')
    await trigger.focus()
    await page.keyboard.press('Enter')

    // After keyboard activation, mock injector fires and address is populated
    const roadInput = page.locator('[data-testid="address-road"]')
    await expect(roadInput).toHaveValue('강남대로 396', { timeout: 3000 })
  })
})
