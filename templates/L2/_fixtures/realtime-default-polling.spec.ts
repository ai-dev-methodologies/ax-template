/**
 * TDD Anchor — SP27 realtime-default-polling.spec.ts
 *
 * Purpose: Asserts that the default realtime transport is POLLING (not SSE).
 * Specifically:
 *   1. blueprints/realtime-policy-manifest.yaml exists with default_transport: polling
 *   2. EventStream with no transport prop (or transport="polling") does NOT instantiate EventSource
 *   3. TanStack Query interval fetch fires every 5s in polling mode
 *
 * Pre-SP27:
 *   - blueprints/realtime-policy-manifest.yaml does not exist → MANIFEST_NOT_FOUND
 *   - event-stream.tsx does not exist → fixture fails
 *
 * Post-SP27 (first green):
 *   - Manifest exists with default_transport: polling
 *   - No EventSource constructor call in default mode
 *
 * @fixture realtime-default-polling
 * @layer L2
 * @component EventStream
 */

import { test, expect } from '@playwright/test'
import * as fs from 'node:fs'
import * as path from 'node:path'

const REPO_ROOT = path.resolve(__dirname, '../../../..')

// ─── Static checks (no browser needed) ───────────────────────────────────────

test.describe('realtime-policy-manifest.yaml', () => {
  test('exists at blueprints/realtime-policy-manifest.yaml', () => {
    const manifestPath = path.join(REPO_ROOT, 'blueprints/realtime-policy-manifest.yaml')
    expect(fs.existsSync(manifestPath), 'MANIFEST_NOT_FOUND').toBe(true)
  })

  test('declares default_transport: polling', () => {
    const manifestPath = path.join(REPO_ROOT, 'blueprints/realtime-policy-manifest.yaml')
    const content = fs.readFileSync(manifestPath, 'utf-8')
    expect(content, 'REALTIME_DEFAULT_NOT_POLLING').toContain('default_transport: polling')
  })

  test('declares SSE as opt-in only (not default)', () => {
    const manifestPath = path.join(REPO_ROOT, 'blueprints/realtime-policy-manifest.yaml')
    const content = fs.readFileSync(manifestPath, 'utf-8')
    // SSE must be under opt_in_transports, not as default_transport
    expect(content).toContain('opt_in_transports')
    expect(content).toContain('name: sse')
    expect(content).not.toMatch(/default_transport:\s*sse/)
    expect(content).not.toMatch(/default_transport:\s*websocket/)
  })

  test('declares serverless_safe: false for SSE opt-in', () => {
    const manifestPath = path.join(REPO_ROOT, 'blueprints/realtime-policy-manifest.yaml')
    const content = fs.readFileSync(manifestPath, 'utf-8')
    expect(content).toContain('serverless_safe: false')
  })
})

test.describe('EventStream component — polling default', () => {
  test('event-stream.tsx exists in templates/L2/blocks/', () => {
    const componentPath = path.join(REPO_ROOT, 'templates/L2/blocks/event-stream.tsx')
    expect(fs.existsSync(componentPath), 'EVENT_STREAM_COMPONENT_MISSING').toBe(true)
  })

  test('event-stream.tsx default transport is polling (no EventSource in default path)', () => {
    const componentPath = path.join(REPO_ROOT, 'templates/L2/blocks/event-stream.tsx')
    const content = fs.readFileSync(componentPath, 'utf-8')

    // Transport prop defaults to 'polling' — must be explicit in the signature
    expect(content).toMatch(/transport\s*=\s*['"]polling['"]/,
      'Component must default transport to "polling"')

    // EventSource must be gated behind transport === 'sse' check
    // Ensure EventSource is not called at module top level or in polling useEffect
    const pollingEffectMatch = content.match(
      /if \(transport !== 'polling'\) return[\s\S]*?EventSource/
    )
    expect(pollingEffectMatch || content.includes("if (transport !== 'sse') return"),
      'EventSource must only appear in SSE opt-in branch, not in polling branch'
    ).toBeTruthy()
  })

  test('live-presence.tsx exists in templates/L2/blocks/', () => {
    const componentPath = path.join(REPO_ROOT, 'templates/L2/blocks/live-presence.tsx')
    expect(fs.existsSync(componentPath), 'LIVE_PRESENCE_COMPONENT_MISSING').toBe(true)
  })

  test('live-presence.tsx uses fetch (polling) not EventSource by default', () => {
    const componentPath = path.join(REPO_ROOT, 'templates/L2/blocks/live-presence.tsx')
    const content = fs.readFileSync(componentPath, 'utf-8')
    expect(content).toContain('fetch(presenceUrl')
    expect(content).not.toMatch(/new EventSource\s*\(/)
  })
})

// ─── Browser integration test (requires dev server at localhost:3000) ─────────
// Skipped in static CI; run manually with `npx playwright test --headed`

test.describe('EventStream browser — polling mode (requires dev server)', () => {
  test.skip(({ headless }) => headless, 'Skip in CI — requires interactive dev server')

  test('does NOT instantiate EventSource with default config', async ({ page }) => {
    const eventSourceCalls: string[] = []

    await page.addInitScript(() => {
      const OriginalEventSource = window.EventSource
      ;(window as unknown as Record<string, unknown>).__EventSourceCallCount = 0
      window.EventSource = new Proxy(OriginalEventSource, {
        construct(target, args) {
          ;(window as unknown as Record<string, number>).__EventSourceCallCount++
          return new target(args[0], args[1])
        }
      })
    })

    await page.goto('http://localhost:3000/stories/event-stream-polling')
    await page.waitForLoadState('networkidle')
    await page.waitForTimeout(6000)   // one polling interval (5s)

    const callCount = await page.evaluate(
      () => (window as unknown as Record<string, number>).__EventSourceCallCount ?? 0
    )
    expect(callCount, 'REALTIME_DEFAULT_NOT_POLLING: EventSource was constructed in polling mode').toBe(0)
  })

  test('TanStack Query refetch fires within polling interval', async ({ page }) => {
    await page.addInitScript(() => {
      ;(window as unknown as Record<string, number>).__fetchCallCount = 0
      const origFetch = window.fetch
      window.fetch = async (...args) => {
        const url = typeof args[0] === 'string' ? args[0] : ''
        if (url.includes('stream') || url.includes('events')) {
          ;(window as unknown as Record<string, number>).__fetchCallCount++
        }
        return origFetch.apply(window, args)
      }
    })

    await page.goto('http://localhost:3000/stories/event-stream-polling')
    await page.waitForLoadState('networkidle')
    await page.waitForTimeout(11000)   // two polling intervals

    const callCount = await page.evaluate(
      () => (window as unknown as Record<string, number>).__fetchCallCount ?? 0
    )
    // At least 2 polls in 11s (first immediate + one at 5s + one at 10s = 3, allow 2 min)
    expect(callCount).toBeGreaterThanOrEqual(2)
  })
})
