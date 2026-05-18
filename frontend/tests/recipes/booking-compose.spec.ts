/**
 * tests/recipes/booking-compose.spec.ts — TDD anchor (SP39)
 *
 * RED phase:  fails before recipes/booking/ is created (directory does not exist).
 * GREEN phase: passes after SP39 atomic commit creates booking recipe files.
 *
 * Assertion contract (per PRD §4.1 TDD anchor):
 *   1. recipes/booking/ directory exists
 *   2. recipes/booking/RECIPE.md exists and has correct frontmatter
 *   3. RECIPE.md enabled_l4_domains list matches [audit-log, crud, feature-flags, notification, payment] (alphabetical)
 *   4. specs/recipes/booking-recipe-l0.yaml exists
 *   5. recipes/booking/L4-composition.md exists
 *   6. recipes/booking/L2-block-recipe.md exists
 *   7. recipes/booking/spec-trio-template.yaml exists
 *
 * Simulates: /ax-scaffold business booking test-bk --dry-run exits 0
 */
import { test, expect } from '@playwright/test'
import * as fs from 'fs'
import * as path from 'path'

const REPO_ROOT = path.resolve(__dirname, '../../..')
const RECIPE_DIR = path.join(REPO_ROOT, 'recipes/booking')
const RECIPE_MD = path.join(RECIPE_DIR, 'RECIPE.md')
const SPEC_YAML = path.join(REPO_ROOT, 'specs/recipes/booking-recipe-l0.yaml')

const EXPECTED_L4_DOMAINS = ['audit-log', 'crud', 'feature-flags', 'notification', 'payment']

function readFile(filePath: string): string {
  return fs.readFileSync(filePath, 'utf-8')
}

function fileExists(filePath: string): boolean {
  return fs.existsSync(filePath)
}

function extractFrontmatterList(content: string, key: string): string[] {
  const lines = content.split('\n')
  const result: string[] = []
  let inBlock = false
  for (const line of lines) {
    if (line.trim() === '---' && !inBlock) { inBlock = true; continue }
    if (line.trim() === '---' && inBlock) break
    if (inBlock && line.trim().startsWith(`${key}:`)) {
      // next lines are list items
      continue
    }
    if (inBlock && result.length === 0 && line.trim().startsWith('- ') &&
        lines[lines.indexOf(line) - 1]?.trim().startsWith(`${key}:`)) {
      result.push(line.trim().slice(2))
    }
  }
  // Re-parse: find key line then collect subsequent "  - " lines
  const keyIdx = lines.findIndex(l => l.trim().startsWith(`${key}:`))
  if (keyIdx === -1) return []
  const collected: string[] = []
  for (let i = keyIdx + 1; i < lines.length; i++) {
    const l = lines[i].trim()
    if (l.startsWith('- ')) {
      collected.push(l.slice(2).trim())
    } else if (l && !l.startsWith('#')) {
      break
    }
  }
  return collected
}

test.describe('booking recipe compose — SP39 TDD anchor', () => {
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

  test('RECIPE.md enabled_l4_domains matches expected alphabetical list', () => {
    const content = readFile(RECIPE_MD)
    const domains = extractFrontmatterList(content, 'enabled_l4_domains')
    expect(domains.sort()).toEqual(EXPECTED_L4_DOMAINS)
  })

  test('specs/recipes/booking-recipe-l0.yaml exists', () => {
    expect(fileExists(SPEC_YAML)).toBe(true)
  })

  test('recipes/booking/L4-composition.md exists', () => {
    expect(fileExists(path.join(RECIPE_DIR, 'L4-composition.md'))).toBe(true)
  })

  test('recipes/booking/L2-block-recipe.md exists', () => {
    expect(fileExists(path.join(RECIPE_DIR, 'L2-block-recipe.md'))).toBe(true)
  })

  test('recipes/booking/spec-trio-template.yaml exists', () => {
    expect(fileExists(path.join(RECIPE_DIR, 'spec-trio-template.yaml'))).toBe(true)
  })

  test('booking spec-trio-template.yaml has applied_recipes: booking', () => {
    const content = readFile(path.join(RECIPE_DIR, 'spec-trio-template.yaml'))
    expect(content).toContain('- booking')
  })
})
