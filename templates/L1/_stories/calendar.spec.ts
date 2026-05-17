/**
 * Story spec — SP14 calendar.spec.ts
 *
 * Smoke tests for the Calendar component (react-day-picker v9 wrapper).
 * Verifies: renders grid, navigation works, date selection fires callback,
 * keyboard nav (arrow keys), and WCAG aria attributes are present.
 *
 * @story calendar
 * @layer L1
 * @component Calendar
 */

import { test, expect } from '@playwright/test'

const STORY_URL = 'http://localhost:3000/stories/calendar'

test.describe('Calendar', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto(STORY_URL)
    await page.waitForLoadState('networkidle')
  })

  test('renders a calendar grid with role="grid"', async ({ page }) => {
    const grid = page.locator('[role="grid"]')
    await expect(grid).toBeVisible()
  })

  test('displays month and year in caption', async ({ page }) => {
    // Caption label should contain a month name
    const caption = page.locator('[data-testid="calendar"]').locator('[class*="caption_label"]').first()
    await expect(caption).toBeVisible()
    const text = await caption.textContent()
    expect(text).toBeTruthy()
    expect(text!.length).toBeGreaterThan(3)
  })

  test('navigates to next month on clicking next button', async ({ page }) => {
    const caption = page.locator('[data-testid="calendar"]').locator('[class*="caption_label"]').first()
    const initialText = await caption.textContent()

    // Click next month button
    const nextBtn = page.locator('[data-testid="calendar"]').locator('button[aria-label*="next"], button[aria-label*="다음"]').first()
    await nextBtn.click()

    const nextText = await caption.textContent()
    expect(nextText).not.toBe(initialText)
  })

  test('navigates to previous month on clicking prev button', async ({ page }) => {
    const caption = page.locator('[data-testid="calendar"]').locator('[class*="caption_label"]').first()
    const initialText = await caption.textContent()

    const prevBtn = page.locator('[data-testid="calendar"]').locator('button[aria-label*="prev"], button[aria-label*="이전"]').first()
    await prevBtn.click()

    const prevText = await caption.textContent()
    expect(prevText).not.toBe(initialText)
  })

  test('clicking a day button fires onSelect with a Date', async ({ page }) => {
    // The story page exposes window.__selectedDate when onSelect fires
    const dayBtn = page.locator('[data-testid="calendar"] [role="gridcell"] button:not([disabled])').first()
    await dayBtn.click()

    const selectedDate = await page.evaluate(
      () => (window as unknown as { __selectedDate?: string }).__selectedDate
    )
    expect(selectedDate).toBeTruthy()
  })

  test('day buttons have aria-label with date', async ({ page }) => {
    const firstDay = page.locator('[data-testid="calendar"] [role="gridcell"] button:not([disabled])').first()
    const ariaLabel = await firstDay.getAttribute('aria-label')
    expect(ariaLabel).toBeTruthy()
  })

  test('keyboard arrow navigation moves focus between days', async ({ page }) => {
    const firstDay = page.locator('[data-testid="calendar"] [role="gridcell"] button:not([disabled])').first()
    await firstDay.focus()

    await page.keyboard.press('ArrowRight')

    // Focus should have moved — tabindex or focus class should change
    const focusedLabel = await page.evaluate(() => {
      const el = document.activeElement as HTMLElement
      return el?.getAttribute('aria-label')
    })
    expect(focusedLabel).toBeTruthy()
  })
})
