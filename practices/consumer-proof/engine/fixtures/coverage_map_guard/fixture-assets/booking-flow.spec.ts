/**
 * practices/consumer-proof/engine/fixtures/coverage_map_guard/fixture-assets/booking-flow.spec.ts
 *
 * P3-58 fixture asset — NOT a real test suite. Reproduces the exact rename-bypass this
 * guard's check 7 content heuristic exists to close: this file is BYTE-IDENTICAL in
 * assertion shape to frontend/tests/recipes/booking-compose.spec.ts (every assertion
 * derives from fs.existsSync/readFileSync — no runtime/HTTP/browser interaction) but is
 * named `booking-flow.spec.ts`, NOT `*-compose.spec.ts` — so the filename-only exclusion
 * (`_BARE_COMPOSE_SPEC_RE`) does not match it, and before the P3-58 content heuristic it
 * would have silently qualified as a "live, re-executable" S3 nonvacuity path purely
 * because it matches `*.spec.*`.
 */
import { test, expect } from '@playwright/test'
import * as fs from 'fs'
import * as path from 'path'

const REPO_ROOT = path.resolve(__dirname, '../../../../../..')
const RECIPE_DIR = path.join(REPO_ROOT, 'recipes/booking')
const RECIPE_MD = path.join(RECIPE_DIR, 'RECIPE.md')

function readFile(filePath: string): string {
  return fs.readFileSync(filePath, 'utf-8')
}

function fileExists(filePath: string): boolean {
  return fs.existsSync(filePath)
}

test.describe('booking recipe compose — rename-bypass fixture (P3-58)', () => {
  test('recipes/booking/ directory exists', () => {
    expect(fileExists(RECIPE_DIR)).toBe(true)
  })

  test('recipes/booking/RECIPE.md exists', () => {
    expect(fileExists(RECIPE_MD)).toBe(true)
  })

  test('RECIPE.md has pattern: booking in frontmatter', () => {
    const content = readFile(RECIPE_MD)
    expect(content).toContain('pattern: booking')
  })
})
