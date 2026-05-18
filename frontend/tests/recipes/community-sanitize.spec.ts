/**
 * tests/recipes/community-sanitize.spec.ts — R7 SP41b co-shipped invariant test
 *
 * Anchors COMMUNITY-INV-005 (community-html-sanitization) which is a
 * recipe-level invariant authored inline in specs/recipes/community-recipe-l0.yaml
 * — NOT a new practices/rules/*.md file (PRD §1.8 + §10).
 *
 * Assertion contract: server-side sanitize behavior is documented in the
 * recipe Spec Trio. This test verifies the contract is present on disk and
 * the recipe correctly references it (catalog-level integrity — the
 * runtime fork-receiver implements the actual sanitizer in their Spring
 * Boot service per the spec hint).
 *
 * RED phase:  fails before recipes/community/ is created (spec absent).
 * GREEN phase: passes after SP41b atomic commit lands community Spec Trio.
 *
 * This test is excluded from frontend/vitest.config.ts the same way other
 * recipe playwright specs are excluded (R6 codex review fix-cycle pattern).
 */
import { test, expect } from '@playwright/test'
import * as fs from 'fs'
import * as path from 'path'

const REPO_ROOT = path.resolve(__dirname, '../../..')
const RECIPE_DIR = path.join(REPO_ROOT, 'recipes/community')
const RECIPE_MD = path.join(RECIPE_DIR, 'RECIPE.md')
const SPEC_YAML = path.join(REPO_ROOT, 'specs/recipes/community-recipe-l0.yaml')

function readFile(filePath: string): string {
  return fs.readFileSync(filePath, 'utf-8')
}

function fileExists(filePath: string): boolean {
  return fs.existsSync(filePath)
}

test.describe('community recipe sanitize — R7 SP41b co-shipped invariant', () => {
  test('community recipe directory exists', () => {
    expect(fileExists(RECIPE_DIR)).toBe(true)
  })

  test('community recipe spec yaml exists', () => {
    expect(fileExists(SPEC_YAML)).toBe(true)
  })

  test('INV-005 declares co-shipped-rule: community-html-sanitization', () => {
    const content = readFile(SPEC_YAML)
    expect(content).toContain('COMMUNITY-INV-005')
    expect(content).toMatch(/co-shipped-rule:\s*"?community-html-sanitization/)
  })

  test('INV-005 points invariant_test back to this file (referential integrity)', () => {
    const content = readFile(SPEC_YAML)
    expect(content).toContain('frontend/tests/recipes/community-sanitize.spec.ts')
  })

  test('INV-005 documents disallowed elements in the spec notes', () => {
    const content = readFile(SPEC_YAML)
    // Server-side sanitize disallowlist per OWASP guidance
    expect(content).toMatch(/<script>/)
    expect(content).toMatch(/<iframe>/)
    expect(content).toMatch(/javascript:/)
  })

  test('RECIPE.md INV-005 row cites co-shipped-rule + invariant_test', () => {
    const content = readFile(RECIPE_MD)
    expect(content).toContain('COMMUNITY-INV-005')
    expect(content).toContain('co-shipped-rule: community-html-sanitization')
    expect(content).toContain('frontend/tests/recipes/community-sanitize.spec.ts')
  })

  test('No new practices/rules/community-html-sanitization.md file added (PRD §1.8 + §10)', () => {
    const rulesFile = path.join(REPO_ROOT, 'practices/rules/community-html-sanitization.md')
    expect(fileExists(rulesFile)).toBe(false)
  })
})
