/**
 * TDD Anchor — SP14 combobox-ime.spec.ts
 *
 * Purpose: Asserts that the Combobox component respects Korean (Hangul) IME
 * composition events. During composition (compositionstart → compositionupdate →
 * compositionend), the onChange callback MUST NOT fire. Only after compositionend
 * should onChange fire once with the fully-composed text.
 *
 * This is the first green test for SP21's `combobox-respects-hangul-ime-composition` rule.
 *
 * Pre-SP14: component does not exist → test fails ENOENT.
 * Post-SP14: component ships → test passes GREEN.
 *
 * @story combobox-ime
 * @layer L1
 * @component Combobox
 */

import { test, expect } from '@playwright/test'

// The story page renders a controlled Combobox that records
// onChange calls and composition events to window.__events.
const STORY_URL = 'http://localhost:3000/stories/combobox-ime'

test.describe('Combobox — Korean IME composition', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto(STORY_URL)
    await page.waitForLoadState('networkidle')
  })

  test('onChange does NOT fire during IME composition (compositionstart fired)', async ({ page }) => {
    const input = page.locator('[data-testid="combobox-input"]')
    await input.click()

    // Simulate IME composition: compositionstart
    await page.evaluate(() => {
      const el = document.querySelector('[data-testid="combobox-input"]') as HTMLInputElement
      el.dispatchEvent(new CompositionEvent('compositionstart', { bubbles: true, data: '' }))
    })

    // Type individual Hangul jamo characters (mid-composition — not yet composed)
    await page.evaluate(() => {
      const el = document.querySelector('[data-testid="combobox-input"]') as HTMLInputElement
      el.dispatchEvent(new CompositionEvent('compositionupdate', { bubbles: true, data: '강' }))
    })

    // onChange call count should still be 0 during composition
    const onChangeDuringComposition = await page.evaluate(
      () => (window as unknown as { __onChangeCallCount: number }).__onChangeCallCount ?? 0
    )
    expect(onChangeDuringComposition).toBe(0)
  })

  test('onChange fires ONCE after compositionend with complete composed text', async ({ page }) => {
    const input = page.locator('[data-testid="combobox-input"]')
    await input.click()

    // Full IME sequence for "강남"
    await page.evaluate(() => {
      const el = document.querySelector('[data-testid="combobox-input"]') as HTMLInputElement
      el.dispatchEvent(new CompositionEvent('compositionstart', { bubbles: true, data: '' }))
      el.dispatchEvent(new CompositionEvent('compositionupdate', { bubbles: true, data: '강' }))
      el.dispatchEvent(new CompositionEvent('compositionupdate', { bubbles: true, data: '강남' }))
      el.dispatchEvent(new CompositionEvent('compositionend', { bubbles: true, data: '강남' }))
      // Synthetic input event fires after compositionend
      el.value = '강남'
      el.dispatchEvent(new Event('input', { bubbles: true }))
    })

    // onChange fires exactly once after composition ends
    const onChangeCallCount = await page.evaluate(
      () => (window as unknown as { __onChangeCallCount: number }).__onChangeCallCount ?? 0
    )
    expect(onChangeCallCount).toBe(1)

    // The value passed to onChange is the complete composed text
    const lastOnChangeValue = await page.evaluate(
      () => (window as unknown as { __lastOnChangeValue: string }).__lastOnChangeValue ?? ''
    )
    expect(lastOnChangeValue).toBe('강남')
  })

  test('backspace during composition does not corrupt text', async ({ page }) => {
    const input = page.locator('[data-testid="combobox-input"]')
    await input.click()

    // Start composition
    await page.evaluate(() => {
      const el = document.querySelector('[data-testid="combobox-input"]') as HTMLInputElement
      el.dispatchEvent(new CompositionEvent('compositionstart', { bubbles: true, data: '' }))
      el.dispatchEvent(new CompositionEvent('compositionupdate', { bubbles: true, data: '강남' }))
    })

    // Press backspace during composition (browser manages the buffer internally)
    await page.keyboard.press('Backspace')

    // Update composition state after backspace
    await page.evaluate(() => {
      const el = document.querySelector('[data-testid="combobox-input"]') as HTMLInputElement
      el.dispatchEvent(new CompositionEvent('compositionupdate', { bubbles: true, data: '강' }))
    })

    // End composition with reduced text
    await page.evaluate(() => {
      const el = document.querySelector('[data-testid="combobox-input"]') as HTMLInputElement
      el.dispatchEvent(new CompositionEvent('compositionend', { bubbles: true, data: '강' }))
      el.value = '강'
      el.dispatchEvent(new Event('input', { bubbles: true }))
    })

    // Input value should be the composed text without corruption
    const inputValue = await input.inputValue()
    expect(inputValue).toBe('강')

    // onChange fires with uncorrupted value
    const lastValue = await page.evaluate(
      () => (window as unknown as { __lastOnChangeValue: string }).__lastOnChangeValue ?? ''
    )
    expect(lastValue).toBe('강')
  })
})
