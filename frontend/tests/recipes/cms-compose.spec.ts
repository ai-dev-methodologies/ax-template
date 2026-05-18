/**
 * tests/recipes/cms-compose.spec.ts — R8 SP43 TDD anchor
 *
 * RED phase:  fails before recipes/cms/ is created (directory does not exist).
 * GREEN phase: passes after SP43 atomic commit creates cms recipe files.
 *
 * Assertion contract (per PRD §4.2 TDD anchor):
 *   1. recipes/cms/ directory exists
 *   2. recipes/cms/RECIPE.md exists with pattern: cms
 *   3. RECIPE.md enabled_l4_domains list equals
 *      [audit-log, crud, notification, scheduled-task] (alphabetical;
 *      auth + search optional via override_allowed)
 *   4. specs/recipes/cms-recipe-l0.yaml exists with all 5 invariants
 *   5. recipes/cms/L4-composition.md exists
 *   6. recipes/cms/L2-block-recipe.md exists
 *   7. recipes/cms/spec-trio-template.yaml exists
 *   8. l2_blocks_used contains only files present at templates/L2/blocks/*.tsx
 *
 * Excluded from vitest run; executed via playwright per R6/R7 recipe pattern.
 */
import { test, expect } from '@playwright/test'
import * as fs from 'fs'
import * as path from 'path'

const REPO_ROOT = path.resolve(__dirname, '../../..')
const RECIPE_DIR = path.join(REPO_ROOT, 'recipes/cms')
const RECIPE_MD = path.join(RECIPE_DIR, 'RECIPE.md')
const SPEC_YAML = path.join(REPO_ROOT, 'specs/recipes/cms-recipe-l0.yaml')

const EXPECTED_L4_DOMAINS = ['audit-log', 'crud', 'notification', 'scheduled-task']

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

test.describe('cms recipe compose — R8 SP43 TDD anchor', () => {
  test('recipes/cms/ directory exists', () => {
    expect(fileExists(RECIPE_DIR)).toBe(true)
  })

  test('recipes/cms/RECIPE.md exists', () => {
    expect(fileExists(RECIPE_MD)).toBe(true)
  })

  test('RECIPE.md has pattern: cms in frontmatter', () => {
    const content = readFile(RECIPE_MD)
    expect(content).toContain('pattern: cms')
  })

  test('RECIPE.md enabled_l4_domains matches expected alphabetical list', () => {
    const content = readFile(RECIPE_MD)
    const domains = extractFrontmatterList(content, 'enabled_l4_domains')
    expect(domains.sort()).toEqual(EXPECTED_L4_DOMAINS)
  })

  test('specs/recipes/cms-recipe-l0.yaml exists', () => {
    expect(fileExists(SPEC_YAML)).toBe(true)
  })

  test('cms recipe spec carries all 5 invariants (INV-001 .. INV-005)', () => {
    const content = readFile(SPEC_YAML)
    for (const id of ['CMS-INV-001', 'CMS-INV-002', 'CMS-INV-003', 'CMS-INV-004', 'CMS-INV-005']) {
      expect(content).toContain(id)
    }
  })

  test('recipes/cms/L4-composition.md exists', () => {
    expect(fileExists(path.join(RECIPE_DIR, 'L4-composition.md'))).toBe(true)
  })

  test('recipes/cms/L2-block-recipe.md exists', () => {
    expect(fileExists(path.join(RECIPE_DIR, 'L2-block-recipe.md'))).toBe(true)
  })

  test('recipes/cms/spec-trio-template.yaml exists', () => {
    expect(fileExists(path.join(RECIPE_DIR, 'spec-trio-template.yaml'))).toBe(true)
  })

  test('cms spec-trio-template.yaml has applied_recipes: cms', () => {
    const content = readFile(path.join(RECIPE_DIR, 'spec-trio-template.yaml'))
    expect(content).toContain('- cms')
  })

  test('l2_blocks_used contains only files present at templates/L2/blocks/*.tsx', () => {
    const content = readFile(SPEC_YAML)
    const blocks = extractFrontmatterList(content, 'l2_blocks_used')
    expect(blocks.length).toBeGreaterThan(0)
    for (const block of blocks) {
      const blockPath = path.join(REPO_ROOT, 'templates/L2/blocks', `${block}.tsx`)
      expect(fileExists(blockPath)).toBe(true)
    }
  })

  test('RECIPE.md cites Sanity + Contentful + Strapi + Brunch evidence anchors', () => {
    const content = readFile(RECIPE_MD)
    expect(content).toContain('sanity.io')
    expect(content).toContain('contentful.com')
    expect(content).toContain('docs.strapi.io')
    expect(content).toContain('brunch.co.kr')
  })

  test('RECIPE.md cites Sanity scheduled-publishing topic-relevant verbatim', () => {
    const content = readFile(RECIPE_MD)
    expect(content).toContain('scheduled-publishing')
  })
})
