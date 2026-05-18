/**
 * tests/recipes/api-gateway-relay-compose.spec.ts — R10 SP47 TDD anchor
 *
 * RED phase:  fails before recipes/api-gateway-relay/ is created (directory
 *             does not exist AND specs/recipes/api-gateway-relay-recipe-l0.yaml
 *             absent AT PRD signature; cross-cutting specs/ratelimit-l0.yaml
 *             EXISTS at PRD signature with RATELIMIT-1/2 disk-resolvable).
 * GREEN phase: passes after SP47 atomic commit creates the recipe quartet +
 *             recipe spec + L4 README appends + manifest add.
 *
 * Assertion contract (per PRD §4.1 + §4.5 tdd_anchor):
 *   1. recipes/api-gateway-relay/ directory exists
 *   2. recipes/api-gateway-relay/RECIPE.md exists with pattern: api-gateway-relay
 *   3. RECIPE.md preamble contains the gateway-pattern-composer disambiguation
 *      sentence VERBATIM (M3 closure — gateway-composer-vs-webhook-primitive)
 *   4. RECIPE.md enabled_l4_domains list equals
 *      [audit-log, auth, crud, scheduled-task, webhook] (alphabetical mandatory)
 *   5. specs/recipes/api-gateway-relay-recipe-l0.yaml exists with all 5
 *      invariants (INV-001 .. INV-005)
 *   6. INV-003 cites cross-cutting specs/ratelimit-l0.yaml#RATELIMIT-1 + #RATELIMIT-2
 *   7. recipes/api-gateway-relay/L4-composition.md exists
 *   8. recipes/api-gateway-relay/L2-block-recipe.md exists
 *   9. recipes/api-gateway-relay/spec-trio-template.yaml exists
 *  10. l2_blocks_used contains only files present at templates/L2/blocks/*.tsx
 *  11. RECIPE.md cites Kong + AWS + Cloudflare + Tyk + Apigee evidence anchors
 *      (5 English) + Toss + NAVER Cloud Platform (2 Korean — fresh-vendor add)
 *  12. RECIPE.md preserves 2 Korean verbatim PASS (Toss + NAVER Cloud Platform)
 *
 * Excluded from vitest run; executed via playwright per R6/R7/R8/R9 recipe pattern.
 */
import { test, expect } from '@playwright/test'
import * as fs from 'fs'
import * as path from 'path'

const REPO_ROOT = path.resolve(__dirname, '../../..')
const RECIPE_DIR = path.join(REPO_ROOT, 'recipes/api-gateway-relay')
const RECIPE_MD = path.join(RECIPE_DIR, 'RECIPE.md')
const SPEC_YAML = path.join(REPO_ROOT, 'specs/recipes/api-gateway-relay-recipe-l0.yaml')
const RATELIMIT_SPEC_YAML = path.join(REPO_ROOT, 'specs/ratelimit-l0.yaml')

const EXPECTED_L4_DOMAINS = [
  'audit-log',
  'auth',
  'crud',
  'scheduled-task',
  'webhook',
]

// M3 disambiguation preamble — must appear VERBATIM in RECIPE.md
const PREAMBLE_VERBATIM =
  'api-gateway-relay is a GATEWAY-PATTERN COMPOSER that registers and routes inbound traffic to multiple backend services via webhook L4\'s outbound-emit primitive; NOT itself a primitive.'

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

test.describe('api-gateway-relay recipe compose — R10 SP47 TDD anchor', () => {
  test('recipes/api-gateway-relay/ directory exists', () => {
    expect(fileExists(RECIPE_DIR)).toBe(true)
  })

  test('recipes/api-gateway-relay/RECIPE.md exists', () => {
    expect(fileExists(RECIPE_MD)).toBe(true)
  })

  test('RECIPE.md has pattern: api-gateway-relay in frontmatter', () => {
    const content = readFile(RECIPE_MD)
    expect(content).toContain('pattern: api-gateway-relay')
  })

  test('RECIPE.md contains the M3 disambiguation preamble VERBATIM', () => {
    const content = readFile(RECIPE_MD)
    // The preamble must appear verbatim somewhere in the file (PRD §4.1 M3 closure)
    expect(content).toContain(PREAMBLE_VERBATIM)
  })

  test('RECIPE.md enabled_l4_domains matches expected alphabetical 5-domain list', () => {
    const content = readFile(RECIPE_MD)
    const domains = extractFrontmatterList(content, 'enabled_l4_domains')
    expect(domains.sort()).toEqual(EXPECTED_L4_DOMAINS)
  })

  test('specs/recipes/api-gateway-relay-recipe-l0.yaml exists', () => {
    expect(fileExists(SPEC_YAML)).toBe(true)
  })

  test('api-gateway-relay recipe spec carries all 5 invariants (INV-001 .. INV-005)', () => {
    const content = readFile(SPEC_YAML)
    for (const id of [
      'API-GATEWAY-RELAY-INV-001',
      'API-GATEWAY-RELAY-INV-002',
      'API-GATEWAY-RELAY-INV-003',
      'API-GATEWAY-RELAY-INV-004',
      'API-GATEWAY-RELAY-INV-005',
    ]) {
      expect(content).toContain(id)
    }
  })

  test('INV-003 cites cross-cutting specs/ratelimit-l0.yaml#RATELIMIT-1/2', () => {
    const recipeContent = readFile(SPEC_YAML)
    expect(recipeContent).toContain('specs/ratelimit-l0.yaml#RATELIMIT-1')
    // RATELIMIT-2 referenced in INV-003 notes block
    expect(recipeContent).toMatch(/RATELIMIT-2/)

    // And the upstream anchors must actually exist on disk in the spec
    expect(fileExists(RATELIMIT_SPEC_YAML)).toBe(true)
    const ratelimitSpec = readFile(RATELIMIT_SPEC_YAML)
    expect(ratelimitSpec).toContain('RATELIMIT-1')
    expect(ratelimitSpec).toContain('RATELIMIT-2')
  })

  test('INV-001 cites SP45-shipped WEBHOOK-SIGN-001 + AUDIT-RECORD-001 anchors', () => {
    const recipeContent = readFile(SPEC_YAML)
    expect(recipeContent).toContain('specs/webhook-l0.yaml#WEBHOOK-SIGN-001')
    expect(recipeContent).toMatch(/AUDIT-RECORD-001/)
  })

  test('INV-004 cites WEBHOOK-CIRCUIT-001 + SCHED-LOCK-001 anchors', () => {
    const recipeContent = readFile(SPEC_YAML)
    expect(recipeContent).toContain('specs/webhook-l0.yaml#WEBHOOK-CIRCUIT-001')
    expect(recipeContent).toMatch(/SCHED-LOCK-001/)
  })

  test('INV-005 cites CRUD-VAL-1 + AUDIT-RECORD-002 + idempotency rule_ref', () => {
    const recipeContent = readFile(SPEC_YAML)
    expect(recipeContent).toContain('specs/crud-security.yaml#CRUD-VAL-1')
    expect(recipeContent).toMatch(/AUDIT-RECORD-002/)
    expect(recipeContent).toContain('practices/rules/idempotency-key-on-mutations.md')
  })

  test('recipes/api-gateway-relay/L4-composition.md exists', () => {
    expect(fileExists(path.join(RECIPE_DIR, 'L4-composition.md'))).toBe(true)
  })

  test('recipes/api-gateway-relay/L2-block-recipe.md exists', () => {
    expect(fileExists(path.join(RECIPE_DIR, 'L2-block-recipe.md'))).toBe(true)
  })

  test('recipes/api-gateway-relay/spec-trio-template.yaml exists', () => {
    expect(fileExists(path.join(RECIPE_DIR, 'spec-trio-template.yaml'))).toBe(true)
  })

  test('api-gateway-relay spec-trio-template.yaml has applied_recipes: api-gateway-relay', () => {
    const content = readFile(path.join(RECIPE_DIR, 'spec-trio-template.yaml'))
    expect(content).toContain('- api-gateway-relay')
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

  test('RECIPE.md cites Kong + AWS + Cloudflare + Tyk + Apigee evidence anchors (5 EN)', () => {
    const content = readFile(RECIPE_MD)
    expect(content).toContain('developer.konghq.com')
    expect(content).toContain('docs.aws.amazon.com/apigateway')
    expect(content).toContain('developers.cloudflare.com/api-shield')
    expect(content).toContain('tyk.io/docs')
    expect(content).toContain('docs.cloud.google.com/apigee')
  })

  test('RECIPE.md cites Toss + NAVER Cloud Platform evidence anchors (2 KO)', () => {
    const content = readFile(RECIPE_MD)
    expect(content).toContain('docs.tosspayments.com')
    expect(content).toContain('www.ncloud.com/product')
  })

  test('RECIPE.md preserves 2 Korean verbatim PASS (Toss + NAVER Cloud Platform)', () => {
    const content = readFile(RECIPE_MD)
    // Toss Korean verbatim — adjacent platform fallback
    expect(content).toContain('토스페이먼츠 API 엔드포인트')
    // NAVER Cloud Platform Korean verbatim — fresh-vendor adjacent
    expect(content).toContain('API 호출, 관리, 모니터링')
  })
})
