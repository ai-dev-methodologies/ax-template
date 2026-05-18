/*
---
template_id: L2/_fixtures/typeahead-search-ime.spec.ts
layer: L2
provenance_class: internal_design
evidence:
  - source_type: external
    citation: "MDN Web Docs — CompositionEvent: compositionstart / compositionend lifecycle for IME. Playwright's page.keyboard and insertText APIs allow simulation of IME-composed input for testing Korean syllable assembly."
    url: "https://developer.mozilla.org/en-US/docs/Web/API/CompositionEvent"
  - source_type: external
    citation: "Playwright — page.dispatchEvent: Dispatch arbitrary DOM events including compositionstart/compositionend to test IME lifecycle guards in search components."
    url: "https://playwright.dev/docs/api/class-page#page-dispatch-event"
---
*/
import { test, expect, type Page } from '@playwright/test'

// ─── helpers ──────────────────────────────────────────────────────────────────

/**
 * Simulates Korean IME composition in a Playwright test.
 *
 * Steps:
 *   1. Fire compositionstart (IME session begins)
 *   2. Insert partial syllable text via insertText (no DOM event — simulates in-progress composition)
 *   3. Fire compositionend with the finalised syllable block
 *
 * This mirrors what the OS input method does when assembling 강남:
 *   ㄱ → compositionstart
 *   강 (assembled) → compositionend { data: '강' }
 *   ㄴ → new compositionstart
 *   남 (assembled) → compositionend { data: '남' }
 */
async function typeKoreanIME(page: Page, selector: string, text: string): Promise<void> {
  const input = page.locator(selector)
  await input.click()

  // Simulate syllable-by-syllable composition
  const syllables = Array.from(text) // Split Korean string into Unicode code points
  for (const syllable of syllables) {
    await input.dispatchEvent('compositionstart')
    await input.dispatchEvent('input', { inputType: 'insertCompositionText', data: syllable })
    await input.dispatchEvent('compositionend', { data: syllable })
    // Allow React state update
    await page.waitForTimeout(10)
  }
}

// ─── tests ────────────────────────────────────────────────────────────────────

test.describe('TypeaheadSearch — Korean IME guard', () => {
  test.beforeEach(async ({ page }) => {
    // Navigate to a page that renders TypeaheadSearch.
    // In a fork, replace with your actual search page URL.
    await page.goto('/search')
  })

  test('SEARCH-FE-001: search input is present and focusable', async ({ page }) => {
    const input = page.getByRole('searchbox')
    await expect(input).toBeVisible()
    await expect(input).toBeFocusable()
  })

  test('SEARCH-FE-002: query is NOT submitted during active IME composition', async ({ page }) => {
    const input = page.getByRole('searchbox')
    const capturedQueries: string[] = []

    // Intercept search API to capture queries
    await page.route('**/api/v1/search', async (route, request) => {
      const body = JSON.parse(request.postData() ?? '{}') as { query?: string }
      if (body.query) capturedQueries.push(body.query)
      await route.fulfill({ status: 200, body: JSON.stringify({ hits: [], totalHits: 0, page: 0, size: 20, processingTimeMs: 0 }) })
    })

    // Fire compositionstart — IME session begins
    await input.dispatchEvent('compositionstart')

    // Simulate partial syllable 'ㄱ' being assembled
    await input.dispatchEvent('input', { inputType: 'insertCompositionText', data: 'ㄱ' })
    await page.waitForTimeout(400) // Wait past debounce window

    // No query should have been fired during composition
    expect(capturedQueries).toHaveLength(0)
  })

  test('SEARCH-FE-002: query IS submitted after compositionend', async ({ page }) => {
    const input = page.getByRole('searchbox')
    const capturedQueries: string[] = []

    await page.route('**/api/v1/search', async (route, request) => {
      const body = JSON.parse(request.postData() ?? '{}') as { query?: string }
      if (body.query) capturedQueries.push(body.query)
      await route.fulfill({ status: 200, body: JSON.stringify({ hits: [], totalHits: 0, page: 0, size: 20, processingTimeMs: 0 }) })
    })

    // Complete Korean IME sequence for '강남'
    await typeKoreanIME(page, '[role="searchbox"]', '강남')
    await page.waitForTimeout(400) // Wait past debounce window

    // After compositionend, the debounced query should fire
    expect(capturedQueries.length).toBeGreaterThan(0)
    expect(capturedQueries[capturedQueries.length - 1]).toContain('강남')
  })

  test('SEARCH-FE-001: Cmd+K opens search palette', async ({ page }) => {
    // Only applies if SearchPalette is rendered on the page
    const palette = page.getByRole('dialog', { name: '검색' })
    await expect(palette).not.toBeVisible()

    await page.keyboard.press('Meta+k')

    await expect(palette).toBeVisible()
  })

  test('SEARCH-FE-001: Escape closes search palette', async ({ page }) => {
    await page.keyboard.press('Meta+k')
    const palette = page.getByRole('dialog', { name: '검색' })
    await expect(palette).toBeVisible()

    await page.keyboard.press('Escape')
    await expect(palette).not.toBeVisible()
  })

  test('SEARCH-FE-003: result highlights match query term', async ({ page }) => {
    await page.route('**/api/v1/search', async (route) => {
      await route.fulfill({
        status: 200,
        body: JSON.stringify({
          hits: [{ id: '1', title: '강남역 맛집 추천', snippet: '강남역 주변 인기 레스토랑', score: 1.0 }],
          totalHits: 1, page: 0, size: 20, processingTimeMs: 5,
        }),
      })
    })

    await page.goto('/search/results?q=강남')
    await expect(page.locator('mark').first()).toBeVisible()
    await expect(page.locator('mark').first()).toContainText('강남')
  })
})
