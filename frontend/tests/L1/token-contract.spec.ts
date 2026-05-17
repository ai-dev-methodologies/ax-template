/**
 * L1 TDD anchor: token-contract.spec.ts
 * RED phase: fails before blueprints/ui-tokens-manifest.yaml exists.
 * GREEN phase: passes after manifest is written with all 8 token categories.
 *
 * PRD ref: §SP5 "token-contract.spec.ts — TDD anchor"
 * Schema ref: blueprints/templates/ui-manifest.schema.yaml (SP2)
 */
import { describe, it, expect } from 'vitest'
import * as fs from 'fs'
import * as path from 'path'

// __dirname = frontend/tests/L1 → 3 levels up = repo root (ax-template/)
const REPO_ROOT = path.resolve(__dirname, '../../..')
const MANIFEST_PATH = path.join(REPO_ROOT, 'blueprints/ui-tokens-manifest.yaml')
const SCHEMA_PATH = path.join(REPO_ROOT, 'blueprints/templates/ui-manifest.schema.yaml')

describe('token-contract — blueprints/ui-tokens-manifest.yaml', () => {
  it('schema reference file exists (SP2 prerequisite)', () => {
    expect(fs.existsSync(SCHEMA_PATH), `Expected schema at ${SCHEMA_PATH}`).toBe(true)
  })

  it('manifest file exists', () => {
    expect(
      fs.existsSync(MANIFEST_PATH),
      `Missing: blueprints/ui-tokens-manifest.yaml — create it in SP5`
    ).toBe(true)
  })

  it('manifest is non-empty text', () => {
    const raw = fs.readFileSync(MANIFEST_PATH, 'utf-8')
    expect(raw.length).toBeGreaterThan(100)
  })

  it('has schema_version field', () => {
    const raw = fs.readFileSync(MANIFEST_PATH, 'utf-8')
    expect(raw).toMatch(/^schema_version\s*:/m)
  })

  it('has all 8 required top-level token categories', () => {
    const raw = fs.readFileSync(MANIFEST_PATH, 'utf-8')
    const required = [
      'color',
      'typography',
      'spacing',
      'motion',
      'radius',
      'shadow',
      'z_index',
      'wcag_contrast_attestation',
    ]
    for (const cat of required) {
      expect(raw, `Missing category: ${cat}`).toMatch(new RegExp(`^${cat}\\s*:`, 'm'))
    }
  })

  it('color category has surface, text, accent, status, border sub-groups', () => {
    const raw = fs.readFileSync(MANIFEST_PATH, 'utf-8')
    for (const sub of ['surface', 'text', 'accent', 'status', 'border']) {
      expect(raw, `Missing color.${sub}`).toContain(`  ${sub}:`)
    }
  })

  it('typography category has base, sm, lg, xl, hero fluid sizes', () => {
    const raw = fs.readFileSync(MANIFEST_PATH, 'utf-8')
    for (const key of ['base', 'sm', 'lg', 'xl', 'hero']) {
      expect(raw, `Missing typography.${key}`).toContain(`  ${key}:`)
    }
  })

  it('motion category has duration, easing, and reduced_motion_override', () => {
    const raw = fs.readFileSync(MANIFEST_PATH, 'utf-8')
    expect(raw).toContain('  duration:')
    expect(raw).toContain('  easing:')
    expect(raw).toContain('  reduced_motion_override:')
  })

  it('wcag_contrast_attestation is a list with at least 4 entries', () => {
    const raw = fs.readFileSync(MANIFEST_PATH, 'utf-8')
    // Count lines that look like list items (    - pair:) under the key
    const attestationItems = (raw.match(/^\s+-\s+pair:/gm) || []).length
    expect(attestationItems, 'Expected at least 4 WCAG contrast attestation pairs').toBeGreaterThanOrEqual(4)
  })

  it('color tokens use oklch color space (perceptually uniform)', () => {
    const raw = fs.readFileSync(MANIFEST_PATH, 'utf-8')
    expect(raw).toContain('oklch(')
  })

  it('typography tokens use clamp() for fluid scaling', () => {
    const raw = fs.readFileSync(MANIFEST_PATH, 'utf-8')
    expect(raw).toContain('clamp(')
  })
})
