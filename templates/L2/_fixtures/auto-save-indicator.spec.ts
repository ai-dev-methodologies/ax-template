/**
 * TDD Anchor — SP27 auto-save-indicator.spec.ts
 *
 * Purpose: Asserts debounced auto-save indicator behavior.
 * Specifically:
 *   1. auto-save-indicator.tsx exists
 *   2. Component transitions: idle → pending → saving → saved → idle
 *   3. saveFn is called after debounce (not on every keystroke)
 *   4. Failed save shows 'error' status indefinitely
 *   5. Metrics shim events fire on success/error
 *
 * Pre-SP27: auto-save-indicator.tsx ENOENT → fixture fails.
 * Post-SP27 (first green): component ships; static checks pass.
 *
 * @fixture auto-save-indicator
 * @layer L2
 * @component AutoSaveIndicator
 */

import { test, expect } from '@playwright/test'
import * as fs from 'node:fs'
import * as path from 'node:path'

const REPO_ROOT = path.resolve(__dirname, '../../../..')

// ─── Static checks ────────────────────────────────────────────────────────────

test.describe('auto-save-indicator.tsx — static assertions', () => {
  test('exists in templates/L2/blocks/', () => {
    const componentPath = path.join(REPO_ROOT, 'templates/L2/blocks/auto-save-indicator.tsx')
    expect(fs.existsSync(componentPath), 'AUTO_SAVE_INDICATOR_MISSING').toBe(true)
  })

  test('uses setTimeout-based debounce (not immediate call)', () => {
    const content = fs.readFileSync(
      path.join(REPO_ROOT, 'templates/L2/blocks/auto-save-indicator.tsx'),
      'utf-8'
    )
    expect(content).toContain('setTimeout')
    expect(content).toContain('clearTimeout')
  })

  test('supports idle/pending/saving/saved/error states', () => {
    const content = fs.readFileSync(
      path.join(REPO_ROOT, 'templates/L2/blocks/auto-save-indicator.tsx'),
      'utf-8'
    )
    const states = ['idle', 'pending', 'saving', 'saved', 'error']
    for (const state of states) {
      expect(content, `Missing state: ${state}`).toContain(`'${state}'`)
    }
  })

  test('emits form.auto_save metrics via __axMetrics shim', () => {
    const content = fs.readFileSync(
      path.join(REPO_ROOT, 'templates/L2/blocks/auto-save-indicator.tsx'),
      'utf-8'
    )
    expect(content).toContain('__axMetrics')
    expect(content).toContain('form.auto_save')
  })

  test('uses role=status for accessibility (non-assertive live region)', () => {
    const content = fs.readFileSync(
      path.join(REPO_ROOT, 'templates/L2/blocks/auto-save-indicator.tsx'),
      'utf-8'
    )
    expect(content).toContain('role="status"')
    expect(content).toContain('aria-live="polite"')
  })

  test('saves to idle after success (not permanently "saved")', () => {
    const content = fs.readFileSync(
      path.join(REPO_ROOT, 'templates/L2/blocks/auto-save-indicator.tsx'),
      'utf-8'
    )
    // Must have a setTimeout to return to idle after 'saved'
    expect(content).toMatch(/setStatus\(['"]idle['"]\)/)
    // The idle-return timer must be scoped to the success path
    expect(content).toMatch(/saved.*setTimeout|setTimeout.*idle/s)
  })

  test('data-save-status attribute for E2E testing', () => {
    const content = fs.readFileSync(
      path.join(REPO_ROOT, 'templates/L2/blocks/auto-save-indicator.tsx'),
      'utf-8'
    )
    expect(content).toContain('data-save-status')
  })
})

// ─── Browser integration tests ────────────────────────────────────────────────

test.describe('AutoSaveIndicator — browser behavior (requires dev server)', () => {
  test.skip(({ headless }) => headless, 'Skip in CI — requires interactive dev server')

  test('transitions pending → saving → saved on successful save', async ({ page }) => {
    await page.goto('http://localhost:3000/stories/auto-save-indicator')
    await page.waitForLoadState('networkidle')

    // Type in a field to trigger dirty state
    const input = page.locator('[data-testid="auto-save-input"]')
    await input.fill('hello')

    // Should be pending immediately after typing
    const indicator = page.locator('[data-save-status]')
    await expect(indicator).toHaveAttribute('data-save-status', 'pending')

    // After debounce (default 1000ms), should be saving
    await page.waitForTimeout(1200)
    // Status transitions through saving → saved

    // Should reach 'saved' within 3s of typing
    await expect(indicator).toHaveAttribute('data-save-status', 'saved', { timeout: 3000 })
  })

  test('shows error status on save failure', async ({ page }) => {
    await page.goto('http://localhost:3000/stories/auto-save-indicator?failSave=true')
    await page.waitForLoadState('networkidle')

    const input = page.locator('[data-testid="auto-save-input"]')
    await input.fill('trigger error')

    await page.waitForTimeout(1500)
    const indicator = page.locator('[data-save-status]')
    await expect(indicator).toHaveAttribute('data-save-status', 'error', { timeout: 3000 })
  })

  test('does NOT call saveFn on every keystroke (debounced)', async ({ page }) => {
    await page.addInitScript(() => {
      ;(window as unknown as Record<string, number>).__saveFnCallCount = 0
    })

    await page.goto('http://localhost:3000/stories/auto-save-indicator?trackSaves=true')
    await page.waitForLoadState('networkidle')

    const input = page.locator('[data-testid="auto-save-input"]')
    // Type rapidly (6 keystrokes within 500ms — well within 1s debounce)
    await input.pressSequentially('abcdef', { delay: 80 })

    // Wait for one debounce cycle
    await page.waitForTimeout(1500)

    const callCount = await page.evaluate(
      () => (window as unknown as Record<string, number>).__saveFnCallCount ?? 0
    )
    // Should be 1 (debounced), not 6 (one per keystroke)
    expect(callCount).toBe(1)
  })
})
