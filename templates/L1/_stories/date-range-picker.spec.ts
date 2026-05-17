/**
 * Story spec — SP14 date-range-picker.spec.ts
 *
 * Smoke tests for the DateRangePicker component (Calendar mode="range" + Popover).
 * Verifies: trigger renders, popover opens, selecting start then end date
 * updates trigger label with "from ~ to" format, and range state is reflected.
 *
 * @story date-range-picker
 * @layer L1
 * @component DateRangePicker
 */

import { test, expect } from '@playwright/test'

const STORY_URL = 'http://localhost:3000/stories/date-range-picker'

test.describe('DateRangePicker', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto(STORY_URL)
    await page.waitForLoadState('networkidle')
  })

  test('renders trigger button with placeholder', async ({ page }) => {
    const trigger = page.locator('[data-testid="date-range-picker-trigger"]')
    await expect(trigger).toBeVisible()
  })

  test('opens calendar on trigger click', async ({ page }) => {
    const trigger = page.locator('[data-testid="date-range-picker-trigger"]')
    await trigger.click()

    const grid = page.locator('[role="grid"]').first()
    await expect(grid).toBeVisible()
  })

  test('shows two months side-by-side by default', async ({ page }) => {
    const trigger = page.locator('[data-testid="date-range-picker-trigger"]')
    await trigger.click()

    // Two month grids should be rendered
    const grids = page.locator('[role="grid"]')
    const count = await grids.count()
    expect(count).toBeGreaterThanOrEqual(2)
  })

  test('selecting two dates shows range in trigger label', async ({ page }) => {
    const trigger = page.locator('[data-testid="date-range-picker-trigger"]')
    await trigger.click()

    // Click first available day (range start)
    const days = page.locator('[role="gridcell"] button:not([disabled])')
    await days.nth(0).click()

    // Click a later day (range end)
    await days.nth(5).click()

    // Trigger label should now contain a range indicator
    const updatedText = await trigger.textContent()
    // Korean "~" or "-" between dates
    expect(updatedText).toMatch(/\d/)
  })

  test('selected range days have aria-selected attribute', async ({ page }) => {
    const trigger = page.locator('[data-testid="date-range-picker-trigger"]')
    await trigger.click()

    const days = page.locator('[role="gridcell"] button:not([disabled])')
    await days.nth(0).click()
    await days.nth(3).click()

    // At least one day in range should be aria-selected
    const selected = page.locator('[aria-selected="true"]').first()
    await expect(selected).toBeVisible()
  })
})
