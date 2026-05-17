/**
 * Story spec — SP14 otp-input.spec.ts
 *
 * Smoke tests for the OtpInput component (input-otp@^1 wrapper).
 * Verifies: 6 slots render, typing fills slots left-to-right,
 * backspace clears slots right-to-left, only digits are accepted (digitsOnly),
 * and onChange fires with the complete OTP string after all 6 digits.
 *
 * @story otp-input
 * @layer L1
 * @component OtpInput
 */

import { test, expect } from '@playwright/test'

const STORY_URL = 'http://localhost:3000/stories/otp-input'

test.describe('OtpInput', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto(STORY_URL)
    await page.waitForLoadState('networkidle')
  })

  test('renders 6 OTP slots', async ({ page }) => {
    const slots = page.locator('[data-testid="otp-slot"]')
    await expect(slots).toHaveCount(6)
  })

  test('separator is visible between slot groups', async ({ page }) => {
    const sep = page.locator('[data-testid="otp-separator"]')
    await expect(sep).toBeVisible()
  })

  test('typing digits fills slots sequentially', async ({ page }) => {
    const container = page.locator('[data-testid="otp-input"]')
    await container.click()

    await page.keyboard.type('123456')

    // After typing 6 digits, the onChange callback fires
    const otpValue = await page.evaluate(
      () => (window as unknown as { __otpValue?: string }).__otpValue
    )
    expect(otpValue).toBe('123456')
  })

  test('non-digit characters are rejected', async ({ page }) => {
    const container = page.locator('[data-testid="otp-input"]')
    await container.click()

    await page.keyboard.type('abc')

    const otpValue = await page.evaluate(
      () => (window as unknown as { __otpValue?: string }).__otpValue ?? ''
    )
    // No digits typed — value should remain empty or previous state
    expect(otpValue).not.toMatch(/[a-z]/)
  })

  test('backspace clears the last filled slot', async ({ page }) => {
    const container = page.locator('[data-testid="otp-input"]')
    await container.click()

    await page.keyboard.type('123')
    await page.keyboard.press('Backspace')

    // After backspace, last slot should be empty
    const slots = page.locator('[data-testid="otp-slot"]')
    const thirdSlotText = await slots.nth(2).textContent()
    expect(thirdSlotText?.trim()).toBe('')
  })

  test('onChange fires with complete 6-digit value', async ({ page }) => {
    const container = page.locator('[data-testid="otp-input"]')
    await container.click()

    await page.keyboard.type('654321')

    const otpValue = await page.evaluate(
      () => (window as unknown as { __otpValue?: string }).__otpValue
    )
    expect(otpValue).toBe('654321')
  })
})
