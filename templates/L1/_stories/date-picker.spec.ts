/**
 * Story spec — SP14 date-picker.spec.ts
 *
 * Smoke tests for the DatePicker component (Calendar + Popover).
 * Verifies: trigger renders, popover opens/closes, selecting a date
 * updates the displayed label, and keyboard dismiss works.
 *
 * @story date-picker
 * @layer L1
 * @component DatePicker
 */

import { test, expect } from '@playwright/test'

const STORY_URL = 'http://localhost:3000/stories/date-picker'

test.describe('DatePicker', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto(STORY_URL)
    await page.waitForLoadState('networkidle')
  })

  test('renders trigger button with placeholder text', async ({ page }) => {
    const trigger = page.locator('[data-testid="date-picker-trigger"]')
    await expect(trigger).toBeVisible()
    const text = await trigger.textContent()
    expect(text).toBeTruthy()
  })

  test('opens calendar popover on trigger click', async ({ page }) => {
    const trigger = page.locator('[data-testid="date-picker-trigger"]')
    await trigger.click()

    const grid = page.locator('[role="grid"]')
    await expect(grid).toBeVisible()
  })

  test('closes calendar when a date is selected', async ({ page }) => {
    const trigger = page.locator('[data-testid="date-picker-trigger"]')
    await trigger.click()

    const dayBtn = page.locator('[role="gridcell"] button:not([disabled])').first()
    await dayBtn.click()

    // Popover should close after selection
    const grid = page.locator('[role="grid"]')
    await expect(grid).not.toBeVisible({ timeout: 2000 })
  })

  test('trigger label updates to selected date after selection', async ({ page }) => {
    const trigger = page.locator('[data-testid="date-picker-trigger"]')
    const initialText = await trigger.textContent()

    await trigger.click()
    const dayBtn = page.locator('[role="gridcell"] button:not([disabled])').first()
    await dayBtn.click()

    const updatedText = await trigger.textContent()
    expect(updatedText).not.toBe(initialText)
  })

  test('pressing Escape closes the calendar popover', async ({ page }) => {
    const trigger = page.locator('[data-testid="date-picker-trigger"]')
    await trigger.click()

    await expect(page.locator('[role="grid"]')).toBeVisible()

    await page.keyboard.press('Escape')
    await expect(page.locator('[role="grid"]')).not.toBeVisible({ timeout: 2000 })
  })
})
