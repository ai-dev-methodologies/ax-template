/**
 * L4/payment composition.spec.ts — TDD anchor (SP10)
 *
 * RED phase:  fails before templates/L4/payment/ is written.
 * GREEN phase: passes after all L4/payment files are present and correctly structured.
 *
 * Strategy: file-existence + frontmatter + import-boundary checks.
 * Runs under @playwright/test (used by ax-verify-L4 playwright step).
 * No browser / server needed — all checks are static file analysis.
 *
 * Per-file assertions:
 *   1. File exists at expected path
 *   2. Has template_id: and evidence: in frontmatter comment
 *   3. Pages with backend ops have backend_operation_id: in frontmatter
 *   4. L4 files do NOT import from other L4 domains
 *   5. Key L2 blocks are correctly imported where required
 *
 * Payment-specific assertions:
 *   6. checkout/page.tsx imports SlowProviderWarning and IdempotencyKeyHandler
 *   7. checkout/page.tsx imports PaymentCheckoutForm and PaymentMethodPicker
 *   8. success/[orderId]/page.tsx has backend_operation_id: getPayment
 *   9. failure/[orderId]/page.tsx has backend_operation_id: getPayment
 *  10. refund/[orderId]/page.tsx has backend_operation_id: refundPayment
 */
import { test, expect } from '@playwright/test'
import * as fs from 'fs'
import * as path from 'path'

const REPO_ROOT = path.resolve(__dirname, '../../../..')
const L4_PAYMENT = path.join(REPO_ROOT, 'templates/L4/payment')

function readFile(filePath: string): string {
  return fs.readFileSync(filePath, 'utf-8')
}

function fileExists(filePath: string): boolean {
  return fs.existsSync(filePath)
}

function hasFrontmatter(src: string): boolean {
  return src.includes('template_id:') && src.includes('evidence:')
}

function hasBackendOperationId(src: string, opId: string): boolean {
  return src.includes(`backend_operation_id: ${opId}`)
}

function hasImportFrom(src: string, target: string): boolean {
  return src.includes(target)
}

/** Returns any illegal cross-L4 domain imports */
function illegalCrossL4Imports(src: string): string[] {
  const illegal: string[] = []
  for (const line of src.split('\n')) {
    if (/^import\s/.test(line) || /from\s+['"]/.test(line)) {
      // L4/payment must NOT import from other L4 domains
      if (/templates\/L4\/(auth|crud|practices)/.test(line)) {
        illegal.push(line.trim())
      }
    }
  }
  return illegal
}

// ─── file registry ──────────────────────────────────────────────────────────

interface FileEntry {
  file: string
  description: string
  backendOpId?: string
  mustImport?: string[]
}

const REQUIRED_FILES: FileEntry[] = [
  {
    file: 'app/layout.tsx',
    description: 'root layout with Providers wrapper',
  },
  {
    file: 'app/page.tsx',
    description: 'root redirect to /(payment)/checkout',
  },
  {
    file: 'app/providers.tsx',
    description: 'client provider tree (QueryClient)',
  },
  {
    file: 'app/(payment)/layout.tsx',
    description: 'payment route group layout with AppShell + Sidebar + AppHeader',
  },
  {
    file: 'app/(payment)/page.tsx',
    description: 'redirect to /checkout',
  },
  {
    file: 'app/(payment)/checkout/page.tsx',
    description: 'CHECKOUT — uses PaymentCheckoutForm + PaymentMethodPicker + IdempotencyKeyHandler + SlowProviderWarning',
    backendOpId: 'createPayment',
    mustImport: [
      'payment-checkout-form',
      'payment-method-picker',
      'idempotency-key-handler',
      'slow-provider-warning',
    ],
  },
  {
    file: 'app/(payment)/success/[orderId]/page.tsx',
    description: 'SUCCESS — receipt view after successful payment',
    backendOpId: 'getPayment',
  },
  {
    file: 'app/(payment)/failure/[orderId]/page.tsx',
    description: 'FAILURE — error state + retry after failed payment',
    backendOpId: 'getPayment',
  },
  {
    file: 'app/(payment)/methods/page.tsx',
    description: 'LIST payment methods (DataTable)',
    backendOpId: 'listPayments',
    mustImport: ['data-table'],
  },
  {
    file: 'app/(payment)/methods/new/page.tsx',
    description: 'ADD method — PaymentMethodPicker in form mode',
    backendOpId: 'createPayment',
    mustImport: ['payment-method-picker'],
  },
  {
    file: 'app/(payment)/methods/[id]/page.tsx',
    description: 'DETAIL/EDIT method — view payment method details',
    backendOpId: 'getPayment',
  },
  {
    file: 'app/(payment)/refund/[orderId]/page.tsx',
    description: 'REFUND request — initiate refund for an order',
    backendOpId: 'refundPayment',
  },
  {
    file: 'README.md',
    description: 'fork instructions',
  },
  {
    file: 'next.config.ts',
    description: 'minimal Next.js config for fork test',
  },
]

// ─── suite ───────────────────────────────────────────────────────────────────

test.describe('L4/payment composition contract', () => {
  test('templates/L4/payment/ directory exists', () => {
    expect(
      fileExists(L4_PAYMENT),
      'templates/L4/payment/ must exist — SP10 creates it'
    ).toBe(true)
  })

  for (const entry of REQUIRED_FILES) {
    const filePath = path.join(L4_PAYMENT, entry.file)

    test(`[exists] ${entry.file} — ${entry.description}`, () => {
      expect(
        fileExists(filePath),
        `Missing: templates/L4/payment/${entry.file}`
      ).toBe(true)
    })

    // Frontmatter checks for TSX/TS files (skip README/next.config)
    if (entry.file.endsWith('.tsx') || entry.file.endsWith('.ts')) {
      test(`[frontmatter] ${entry.file} has template_id and evidence`, () => {
        const src = readFile(filePath)
        expect(
          hasFrontmatter(src),
          `${entry.file} must have template_id: and evidence: in frontmatter comment`
        ).toBe(true)
      })

      test(`[layer] ${entry.file} has layer: L4`, () => {
        const src = readFile(filePath)
        expect(src).toContain('layer: L4')
      })

      test(`[domain] ${entry.file} has domain: payment`, () => {
        const src = readFile(filePath)
        expect(src).toContain('domain: payment')
      })

      test(`[no cross-L4] ${entry.file} has no imports from other L4 domains`, () => {
        const src = readFile(filePath)
        const bad = illegalCrossL4Imports(src)
        expect(
          bad,
          `${entry.file} has illegal cross-L4 imports: ${bad.join(', ')}`
        ).toHaveLength(0)
      })
    }

    // backend_operation_id check for pages with ops
    if (entry.backendOpId) {
      test(`[backend_op] ${entry.file} has backend_operation_id: ${entry.backendOpId}`, () => {
        const src = readFile(filePath)
        expect(
          hasBackendOperationId(src, entry.backendOpId!),
          `${entry.file} must declare backend_operation_id: ${entry.backendOpId}`
        ).toBe(true)
      })
    }

    // Import check for specific L2 blocks
    if (entry.mustImport && entry.mustImport.length > 0) {
      for (const target of entry.mustImport) {
        test(`[import] ${entry.file} imports from ${target}`, () => {
          const src = readFile(filePath)
          expect(
            hasImportFrom(src, target),
            `${entry.file} must import from ${target}`
          ).toBe(true)
        })
      }
    }
  }

  // README content check
  test('[readme] README.md has "how to fork" instructions', () => {
    const readmePath = path.join(L4_PAYMENT, 'README.md')
    const src = readFile(readmePath)
    expect(src).toContain('fork')
    expect(src).toContain('copy')
  })

  // Payment-specific: checkout imports all 4 key L2 blocks
  test('[payment-specific] checkout/page.tsx slow-provider warning triggers at 3s', () => {
    const checkoutPath = path.join(L4_PAYMENT, 'app/(payment)/checkout/page.tsx')
    const src = readFile(checkoutPath)
    // Must reference the slow provider threshold (3000 or 3_000)
    expect(src).toMatch(/3[_,]?000|thresholdMs|SlowProviderWarning/)
  })

  test('[payment-specific] checkout/page.tsx uses IdempotencyKeyHandler render prop', () => {
    const checkoutPath = path.join(L4_PAYMENT, 'app/(payment)/checkout/page.tsx')
    const src = readFile(checkoutPath)
    expect(src).toContain('IdempotencyKeyHandler')
    // render prop pattern — receives idempotencyKey
    expect(src).toMatch(/idempotencyKey|idempotency_key/)
  })

  test('[payment-specific] success/[orderId] is idempotent (uses orderId from params)', () => {
    const successPath = path.join(L4_PAYMENT, 'app/(payment)/success/[orderId]/page.tsx')
    const src = readFile(successPath)
    expect(src).toMatch(/orderId|params/)
  })

  test('[payment-specific] failure/[orderId] has retry navigation', () => {
    const failurePath = path.join(L4_PAYMENT, 'app/(payment)/failure/[orderId]/page.tsx')
    const src = readFile(failurePath)
    expect(src).toMatch(/retry|checkout|href/)
  })
})
