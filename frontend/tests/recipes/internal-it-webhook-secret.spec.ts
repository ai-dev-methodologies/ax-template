/**
 * tests/recipes/internal-it-webhook-secret.spec.ts — R9 SP45b co-shipped invariant test
 *
 * Anchors INTERNAL-IT-INV-005 (webhook-secret-encryption) which is a
 * recipe-level invariant authored inline in
 * specs/recipes/internal-it-recipe-l0.yaml — NOT a new practices/rules/*.md
 * file (PRD §1.8 + §10 — TD-2026-05-22-025 Follow-ups: promotion deferred
 * indefinitely per M5 framing).
 *
 * Assertion contract: per-endpoint webhook signing-secret encryption-at-rest
 * behavior is documented in the recipe Spec Trio. This test verifies the
 * catalog-level contract is present on disk and the recipe correctly
 * references it (the runtime fork-receiver implements the actual
 * KmsAttributeConverter in their Spring Boot service per the spec hint).
 *
 * RED phase:  fails before recipes/internal-it/ is created (spec absent).
 * GREEN phase: passes after SP45b atomic commit lands internal-it Spec Trio.
 *
 * This test is excluded from frontend/vitest.config.ts the same way other
 * recipe playwright specs are excluded (R6 codex review fix-cycle pattern;
 * R7 community-sanitize precedent).
 */
import { test, expect } from '@playwright/test'
import * as fs from 'fs'
import * as path from 'path'

const REPO_ROOT = path.resolve(__dirname, '../../..')
const RECIPE_DIR = path.join(REPO_ROOT, 'recipes/internal-it')
const RECIPE_MD = path.join(RECIPE_DIR, 'RECIPE.md')
const SPEC_YAML = path.join(REPO_ROOT, 'specs/recipes/internal-it-recipe-l0.yaml')

function readFile(filePath: string): string {
  return fs.readFileSync(filePath, 'utf-8')
}

function fileExists(filePath: string): boolean {
  return fs.existsSync(filePath)
}

test.describe('internal-it recipe webhook-secret — R9 SP45b co-shipped invariant', () => {
  test('internal-it recipe directory exists', () => {
    expect(fileExists(RECIPE_DIR)).toBe(true)
  })

  test('internal-it recipe spec yaml exists', () => {
    expect(fileExists(SPEC_YAML)).toBe(true)
  })

  test('INV-005 declares co-shipped-rule: webhook-secret-encryption', () => {
    const content = readFile(SPEC_YAML)
    expect(content).toContain('INTERNAL-IT-INV-005')
    expect(content).toMatch(/co-shipped-rule:\s*"?webhook-secret-encryption/)
  })

  test('INV-005 points invariant_test back to this file (referential integrity)', () => {
    const content = readFile(SPEC_YAML)
    expect(content).toContain(
      'frontend/tests/recipes/internal-it-webhook-secret.spec.ts'
    )
  })

  test('INV-005 documents KMS-managed AES-256 envelope encryption in spec notes', () => {
    const content = readFile(SPEC_YAML)
    expect(content).toMatch(/KMS-managed/i)
    expect(content).toMatch(/AES-256/i)
    expect(content).toMatch(/envelope encryption/i)
  })

  test('INV-005 names a plaintext-logging prohibition + ciphertext-at-rest test', () => {
    const content = readFile(SPEC_YAML)
    expect(content).toMatch(/plaintext/i)
    // Test must check ciphertext-at-rest behavior
    expect(content).toMatch(/encrypted ciphertext/i)
  })

  test('RECIPE.md INV-005 row cites co-shipped-rule + invariant_test', () => {
    const content = readFile(RECIPE_MD)
    expect(content).toContain('INTERNAL-IT-INV-005')
    expect(content).toContain('co-shipped-rule: webhook-secret-encryption')
    expect(content).toContain(
      'frontend/tests/recipes/internal-it-webhook-secret.spec.ts'
    )
  })

  test('No new practices/rules/webhook-secret-encryption.md file added (TD-025 Follow-ups M5)', () => {
    const rulesFile = path.join(
      REPO_ROOT,
      'practices/rules/webhook-secret-encryption.md'
    )
    expect(fileExists(rulesFile)).toBe(false)
  })

  test('No new practices/rules/security-secret-encryption-at-rest.md file added (TD-025 Follow-ups M5)', () => {
    // Deferred-indefinitely promotion candidate — must not pre-emptively ship
    const rulesFile = path.join(
      REPO_ROOT,
      'practices/rules/security-secret-encryption-at-rest.md'
    )
    expect(fileExists(rulesFile)).toBe(false)
  })

  test('RECIPE.md cross-references TD-2026-05-22-025 Follow-ups (M5 framing)', () => {
    const content = readFile(RECIPE_MD)
    expect(content).toContain('TD-2026-05-22-025')
  })
})
