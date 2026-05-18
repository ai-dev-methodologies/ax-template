/**
 * tests/recipes/b2b-admin-compose.spec.ts — TDD anchor (SP39)
 *
 * RED phase:  fails before recipes/b2b-admin/ is created.
 * GREEN phase: passes after SP39 atomic commit creates b2b-admin recipe files.
 *
 * Assertion contract (per PRD §4.3 TDD anchor):
 *   1. recipes/b2b-admin/ directory exists
 *   2. recipes/b2b-admin/RECIPE.md exists and has correct frontmatter
 *   3. RECIPE.md enabled_l4_domains list matches [audit-log, auth, crud, feature-flags, search] (alphabetical)
 *   4. specs/recipes/b2b-admin-recipe-l0.yaml exists
 *   5. recipes/b2b-admin/L4-composition.md exists
 *   6. recipes/b2b-admin/L2-block-recipe.md exists
 *   7. recipes/b2b-admin/spec-trio-template.yaml exists
 *
 * Simulates: /ax-scaffold business b2b-admin test-ba --dry-run exits 0
 */
import { test, expect } from '@playwright/test'
import * as fs from 'fs'
import * as path from 'path'

const REPO_ROOT = path.resolve(__dirname, '../../..')
const RECIPE_DIR = path.join(REPO_ROOT, 'recipes/b2b-admin')
const RECIPE_MD = path.join(RECIPE_DIR, 'RECIPE.md')
const SPEC_YAML = path.join(REPO_ROOT, 'specs/recipes/b2b-admin-recipe-l0.yaml')

const EXPECTED_L4_DOMAINS = ['audit-log', 'auth', 'crud', 'feature-flags', 'search']

function readFile(filePath: string): string {
  return fs.readFileSync(filePath, 'utf-8')
}

function fileExists(filePath: string): boolean {
  return fs.existsSync(filePath)
}

function extractFrontmatterList(content: string, key: string): string[] {
  const lines = content.split('\n')
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

test.describe('b2b-admin recipe compose — SP39 TDD anchor', () => {
  test('recipes/b2b-admin/ directory exists', () => {
    expect(fileExists(RECIPE_DIR)).toBe(true)
  })

  test('recipes/b2b-admin/RECIPE.md exists', () => {
    expect(fileExists(RECIPE_MD)).toBe(true)
  })

  test('RECIPE.md has pattern: b2b-admin in frontmatter', () => {
    const content = readFile(RECIPE_MD)
    expect(content).toContain('pattern: b2b-admin')
  })

  test('RECIPE.md enabled_l4_domains matches expected alphabetical list', () => {
    const content = readFile(RECIPE_MD)
    const domains = extractFrontmatterList(content, 'enabled_l4_domains')
    expect(domains.sort()).toEqual(EXPECTED_L4_DOMAINS)
  })

  test('specs/recipes/b2b-admin-recipe-l0.yaml exists', () => {
    expect(fileExists(SPEC_YAML)).toBe(true)
  })

  test('recipes/b2b-admin/L4-composition.md exists', () => {
    expect(fileExists(path.join(RECIPE_DIR, 'L4-composition.md'))).toBe(true)
  })

  test('recipes/b2b-admin/L2-block-recipe.md exists', () => {
    expect(fileExists(path.join(RECIPE_DIR, 'L2-block-recipe.md'))).toBe(true)
  })

  test('recipes/b2b-admin/spec-trio-template.yaml exists', () => {
    expect(fileExists(path.join(RECIPE_DIR, 'spec-trio-template.yaml'))).toBe(true)
  })

  test('b2b-admin spec-trio-template.yaml has applied_recipes: b2b-admin', () => {
    const content = readFile(path.join(RECIPE_DIR, 'spec-trio-template.yaml'))
    expect(content).toContain('- b2b-admin')
  })
})
