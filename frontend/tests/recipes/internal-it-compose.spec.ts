/**
 * tests/recipes/internal-it-compose.spec.ts — R9 SP45b TDD anchor
 *
 * RED phase:  fails before recipes/internal-it/ is created (directory does not exist
 *             AND specs/webhook-l0.yaml#WEBHOOK-SIGN-001/RETRY-001 anchors require
 *             SP45 to have landed first).
 * GREEN phase: passes after SP45 + SP45b atomic commits create the webhook L4
 *             Spec Trio and the internal-it recipe files.
 *
 * Assertion contract (per PRD §4.2 TDD anchor):
 *   1. recipes/internal-it/ directory exists
 *   2. recipes/internal-it/RECIPE.md exists with pattern: internal-it
 *   3. RECIPE.md enabled_l4_domains list equals
 *      [audit-log, auth, crud, notification, scheduled-task, webhook]
 *      (alphabetical; webhook is the 6th — first-consumer arrival)
 *   4. specs/recipes/internal-it-recipe-l0.yaml exists with all 5 invariants
 *   5. recipes/internal-it/L4-composition.md exists
 *   6. recipes/internal-it/L2-block-recipe.md exists
 *   7. recipes/internal-it/spec-trio-template.yaml exists
 *   8. l2_blocks_used contains only files present at templates/L2/blocks/*.tsx
 *   9. INV-003 cites SP45-shipped WEBHOOK-SIGN-001 + WEBHOOK-RETRY-001 anchors
 *  10. RECIPE.md cites Jira + PagerDuty + Toss + Naver Works evidence anchors
 *
 * Excluded from vitest run; executed via playwright per R6/R7/R8 recipe pattern.
 */
import { test, expect } from '@playwright/test'
import * as fs from 'fs'
import * as path from 'path'

const REPO_ROOT = path.resolve(__dirname, '../../..')
const RECIPE_DIR = path.join(REPO_ROOT, 'recipes/internal-it')
const RECIPE_MD = path.join(RECIPE_DIR, 'RECIPE.md')
const SPEC_YAML = path.join(REPO_ROOT, 'specs/recipes/internal-it-recipe-l0.yaml')
const WEBHOOK_SPEC_YAML = path.join(REPO_ROOT, 'specs/webhook-l0.yaml')

const EXPECTED_L4_DOMAINS = [
  'audit-log',
  'auth',
  'crud',
  'notification',
  'scheduled-task',
  'webhook',
]

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

test.describe('internal-it recipe compose — R9 SP45b TDD anchor', () => {
  test('recipes/internal-it/ directory exists', () => {
    expect(fileExists(RECIPE_DIR)).toBe(true)
  })

  test('recipes/internal-it/RECIPE.md exists', () => {
    expect(fileExists(RECIPE_MD)).toBe(true)
  })

  test('RECIPE.md has pattern: internal-it in frontmatter', () => {
    const content = readFile(RECIPE_MD)
    expect(content).toContain('pattern: internal-it')
  })

  test('RECIPE.md enabled_l4_domains matches expected alphabetical 6-domain list', () => {
    const content = readFile(RECIPE_MD)
    const domains = extractFrontmatterList(content, 'enabled_l4_domains')
    expect(domains.sort()).toEqual(EXPECTED_L4_DOMAINS)
  })

  test('specs/recipes/internal-it-recipe-l0.yaml exists', () => {
    expect(fileExists(SPEC_YAML)).toBe(true)
  })

  test('internal-it recipe spec carries all 5 invariants (INV-001 .. INV-005)', () => {
    const content = readFile(SPEC_YAML)
    for (const id of [
      'INTERNAL-IT-INV-001',
      'INTERNAL-IT-INV-002',
      'INTERNAL-IT-INV-003',
      'INTERNAL-IT-INV-004',
      'INTERNAL-IT-INV-005',
    ]) {
      expect(content).toContain(id)
    }
  })

  test('recipes/internal-it/L4-composition.md exists', () => {
    expect(fileExists(path.join(RECIPE_DIR, 'L4-composition.md'))).toBe(true)
  })

  test('recipes/internal-it/L2-block-recipe.md exists', () => {
    expect(fileExists(path.join(RECIPE_DIR, 'L2-block-recipe.md'))).toBe(true)
  })

  test('recipes/internal-it/spec-trio-template.yaml exists', () => {
    expect(fileExists(path.join(RECIPE_DIR, 'spec-trio-template.yaml'))).toBe(true)
  })

  test('internal-it spec-trio-template.yaml has applied_recipes: internal-it', () => {
    const content = readFile(path.join(RECIPE_DIR, 'spec-trio-template.yaml'))
    expect(content).toContain('- internal-it')
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

  test('INV-003 cites SP45-shipped WEBHOOK-SIGN-001 + WEBHOOK-RETRY-001 anchors', () => {
    const recipeContent = readFile(SPEC_YAML)
    expect(recipeContent).toContain('specs/webhook-l0.yaml#WEBHOOK-SIGN-001')
    // RETRY-001 referenced in INV-003 notes (anchored to two webhook spec items)
    expect(recipeContent).toMatch(/WEBHOOK-RETRY-001/)

    // And the upstream anchors must actually exist on disk in the SP45-shipped spec
    expect(fileExists(WEBHOOK_SPEC_YAML)).toBe(true)
    const webhookSpec = readFile(WEBHOOK_SPEC_YAML)
    expect(webhookSpec).toContain('WEBHOOK-SIGN-001')
    expect(webhookSpec).toContain('WEBHOOK-RETRY-001')
  })

  test('RECIPE.md cites Jira + PagerDuty + Toss + Naver Works evidence anchors', () => {
    const content = readFile(RECIPE_MD)
    expect(content).toContain('developer.atlassian.com')
    expect(content).toContain('support.pagerduty.com')
    expect(content).toContain('docs.tosspayments.com')
    expect(content).toContain('developers.worksmobile.com')
  })

  test('RECIPE.md preserves 2 Korean verbatim PASS (Toss + Naver Works)', () => {
    const content = readFile(RECIPE_MD)
    // Toss Korean verbatim
    expect(content).toContain('웹훅으로 실시간 업데이트')
    // Naver Works Korean verbatim
    expect(content).toContain('Bot API로 봇에서 메시지')
  })
})
