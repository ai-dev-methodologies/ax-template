/**
 * tests/recipes/community-compose.spec.ts — R7 SP41b TDD anchor
 *
 * RED phase:  fails before recipes/community/ is created (directory does not exist).
 * GREEN phase: passes after SP41b atomic commit creates community recipe files.
 *
 * Assertion contract (per PRD §4.2 TDD anchor):
 *   1. recipes/community/ directory exists
 *   2. recipes/community/RECIPE.md exists and has correct frontmatter
 *   3. RECIPE.md enabled_l4_domains list equals [audit-log, auth, crud, notification, search]
 *   4. specs/recipes/community-recipe-l0.yaml exists with 5 invariants
 *   5. recipes/community/L4-composition.md exists
 *   6. recipes/community/L2-block-recipe.md exists
 *   7. recipes/community/spec-trio-template.yaml exists
 *
 * Excluded from vitest run; executed via playwright per R6 recipe pattern.
 */
import { test, expect } from '@playwright/test'
import * as fs from 'fs'
import * as path from 'path'

const REPO_ROOT = path.resolve(__dirname, '../../..')
const RECIPE_DIR = path.join(REPO_ROOT, 'recipes/community')
const RECIPE_MD = path.join(RECIPE_DIR, 'RECIPE.md')
const SPEC_YAML = path.join(REPO_ROOT, 'specs/recipes/community-recipe-l0.yaml')

const EXPECTED_L4_DOMAINS = ['audit-log', 'auth', 'crud', 'notification', 'search']

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

test.describe('community recipe compose — R7 SP41b TDD anchor', () => {
  test('recipes/community/ directory exists', () => {
    expect(fileExists(RECIPE_DIR)).toBe(true)
  })

  test('recipes/community/RECIPE.md exists', () => {
    expect(fileExists(RECIPE_MD)).toBe(true)
  })

  test('RECIPE.md has pattern: community in frontmatter', () => {
    const content = readFile(RECIPE_MD)
    expect(content).toContain('pattern: community')
  })

  test('RECIPE.md enabled_l4_domains matches expected alphabetical list', () => {
    const content = readFile(RECIPE_MD)
    const domains = extractFrontmatterList(content, 'enabled_l4_domains')
    expect(domains.sort()).toEqual(EXPECTED_L4_DOMAINS)
  })

  test('specs/recipes/community-recipe-l0.yaml exists', () => {
    expect(fileExists(SPEC_YAML)).toBe(true)
  })

  test('community recipe spec carries all 5 invariants (INV-001 .. INV-005)', () => {
    const content = readFile(SPEC_YAML)
    for (const id of ['COMMUNITY-INV-001', 'COMMUNITY-INV-002', 'COMMUNITY-INV-003', 'COMMUNITY-INV-004', 'COMMUNITY-INV-005']) {
      expect(content).toContain(id)
    }
  })

  test('recipes/community/L4-composition.md exists', () => {
    expect(fileExists(path.join(RECIPE_DIR, 'L4-composition.md'))).toBe(true)
  })

  test('recipes/community/L2-block-recipe.md exists', () => {
    expect(fileExists(path.join(RECIPE_DIR, 'L2-block-recipe.md'))).toBe(true)
  })

  test('recipes/community/spec-trio-template.yaml exists', () => {
    expect(fileExists(path.join(RECIPE_DIR, 'spec-trio-template.yaml'))).toBe(true)
  })

  test('community spec-trio-template.yaml has applied_recipes: community', () => {
    const content = readFile(path.join(RECIPE_DIR, 'spec-trio-template.yaml'))
    expect(content).toContain('- community')
  })

  test('RECIPE.md cites 2 external evidence anchors (Discourse + Reddit-archive)', () => {
    const content = readFile(RECIPE_MD)
    expect(content).toContain('meta.discourse.org')
    expect(content).toContain('github.com/reddit-archive')
  })
})
