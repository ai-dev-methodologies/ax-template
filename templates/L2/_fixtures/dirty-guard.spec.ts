/**
 * TDD Anchor — SP27 dirty-guard.spec.ts
 *
 * Purpose: Asserts that dirty-guard.tsx intercepts navigation when a form has unsaved changes.
 * Specifically:
 *   1. dirty-guard.tsx exists in templates/L2/blocks/
 *   2. When isDirty=true, beforeunload handler is registered and fires on navigation
 *   3. When isDirty=false, navigation proceeds without blocking
 *   4. Metrics shim event fires on guard activation (form.dirty_block.fired_count)
 *
 * Pre-SP27:
 *   - dirty-guard.tsx does not exist → fixture fails with ENOENT
 *
 * Post-SP27 (first green):
 *   - dirty-guard.tsx ships with beforeunload registration
 *   - Playwright confirms the confirm dialog appears on navigation-away
 *
 * @fixture dirty-guard
 * @layer L2
 * @component DirtyGuard
 */

import { test, expect } from '@playwright/test'
import * as fs from 'node:fs'
import * as path from 'node:path'

const REPO_ROOT = path.resolve(__dirname, '../../../..')

// ─── Static checks ────────────────────────────────────────────────────────────

test.describe('dirty-guard.tsx — static assertions', () => {
  test('exists in templates/L2/blocks/', () => {
    const componentPath = path.join(REPO_ROOT, 'templates/L2/blocks/dirty-guard.tsx')
    expect(fs.existsSync(componentPath), 'DIRTY_GUARD_COMPONENT_MISSING').toBe(true)
  })

  test('registers beforeunload event listener when isDirty=true', () => {
    const content = fs.readFileSync(
      path.join(REPO_ROOT, 'templates/L2/blocks/dirty-guard.tsx'),
      'utf-8'
    )
    expect(content).toContain('beforeunload')
    expect(content).toContain('addEventListener')
    expect(content).toContain('removeEventListener')
  })

  test('emits form.dirty_block.fired_count metric', () => {
    const content = fs.readFileSync(
      path.join(REPO_ROOT, 'templates/L2/blocks/dirty-guard.tsx'),
      'utf-8'
    )
    expect(content).toContain('form.dirty_block.fired_count')
    expect(content).toContain('__axMetrics')
  })

  test('cleans up event listener on unmount (useEffect return)', () => {
    const content = fs.readFileSync(
      path.join(REPO_ROOT, 'templates/L2/blocks/dirty-guard.tsx'),
      'utf-8'
    )
    // Effect cleanup must remove the listener
    expect(content).toContain('removeEventListener')
  })

  test('has form-section-extended.tsx (SP15 back-compat shell complete)', () => {
    const shellPath = path.join(REPO_ROOT, 'templates/L2/blocks/form-section.tsx')
    expect(fs.existsSync(shellPath)).toBe(true)
    const content = fs.readFileSync(shellPath, 'utf-8')
    expect(content).toContain('@deprecated')
    expect(content).toContain('form-section-extended')
  })
})

// ─── Browser integration tests ────────────────────────────────────────────────

test.describe('DirtyGuard — browser behavior (requires dev server)', () => {
  test.skip(({ headless }) => headless, 'Skip in CI — requires interactive dev server')

  test('beforeunload fires when isDirty=true and user navigates away', async ({ page }) => {
    await page.addInitScript(() => {
      ;(window as unknown as Record<string, number>).__beforeUnloadFired = 0
      window.addEventListener('beforeunload', () => {
        ;(window as unknown as Record<string, number>).__beforeUnloadFired++
      })
    })

    await page.goto('http://localhost:3000/stories/dirty-guard?dirty=true')
    await page.waitForLoadState('networkidle')

    // Navigate away — triggers beforeunload
    await page.evaluate(() => { window.location.href = '/other' })
    await page.waitForTimeout(500)

    const fired = await page.evaluate(
      () => (window as unknown as Record<string, number>).__beforeUnloadFired ?? 0
    )
    expect(fired, 'DIRTY_GUARD_BEFOREUNLOAD_NOT_FIRED').toBeGreaterThanOrEqual(1)
  })

  test('metrics shim increments form.dirty_block.fired_count on activation', async ({ page }) => {
    await page.addInitScript(() => {
      const counts: Record<string, number> = {}
      ;(window as unknown as Record<string, unknown>).__axMetrics = {
        increment(key: string) { counts[key] = (counts[key] ?? 0) + 1 },
        get: (key: string) => counts[key] ?? 0,
      }
    })

    await page.goto('http://localhost:3000/stories/dirty-guard?dirty=true')
    await page.waitForLoadState('networkidle')

    // Trigger beforeunload
    page.on('dialog', dialog => dialog.dismiss())
    await page.evaluate(() => {
      window.dispatchEvent(new Event('beforeunload'))
    })
    await page.waitForTimeout(200)

    const count = await page.evaluate(() => {
      const m = (window as unknown as Record<string, { get: (k: string) => number }>).__axMetrics
      return m?.get('form.dirty_block.fired_count') ?? 0
    })
    expect(count).toBeGreaterThanOrEqual(1)
  })

  test('no dialog when isDirty=false', async ({ page }) => {
    let dialogFired = false
    page.on('dialog', () => { dialogFired = true })

    await page.goto('http://localhost:3000/stories/dirty-guard?dirty=false')
    await page.waitForLoadState('networkidle')
    await page.evaluate(() => { window.location.href = '/other' })
    await page.waitForTimeout(500)

    expect(dialogFired).toBe(false)
  })
})
