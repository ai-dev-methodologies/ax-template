/**
 * L2 contract.spec.ts — TDD anchor (SP7)
 *
 * RED phase:  fails before templates/L2/blocks/ are written.
 * GREEN phase: passes after all 26 L2 feature blocks are written.
 *
 * Strategy: file-existence + export-shape + import-boundary checks.
 * No DOM renderer required — works without Next.js server context.
 *
 * Per-block assertions:
 *   1. File exists at expected path
 *   2. Has `export default` (component export)
 *   3. Has `template_id:` and `evidence:` in frontmatter comment
 *   4. Does NOT import from templates/L3/, templates/L4/, or app/
 */
import { describe, it, expect } from 'vitest'
import * as fs from 'fs'
import * as path from 'path'

const REPO_ROOT = path.resolve(__dirname, '../../..')
const L2_BLOCKS = path.join(REPO_ROOT, 'templates/L2/blocks')

function readFile(filePath: string): string {
  return fs.readFileSync(filePath, 'utf-8')
}

function fileExists(filePath: string): boolean {
  return fs.existsSync(filePath)
}

function hasDefaultExport(src: string): boolean {
  return /export\s+default\s+/.test(src)
}

function hasFrontmatter(src: string): boolean {
  return src.includes('template_id:') && src.includes('evidence:')
}

/** Returns any illegal import targets found in file source */
function illegalImports(src: string): string[] {
  const illegal: string[] = []
  for (const line of src.split('\n')) {
    if (/^import\s/.test(line) || /from\s+['"]/.test(line)) {
      if (/templates\/L[34]\//.test(line)) illegal.push(line.trim())
      if (/from\s+['"]app\//.test(line)) illegal.push(line.trim())
    }
  }
  return illegal
}

// ─── block registry ──────────────────────────────────────────────────────────

interface BlockEntry {
  file: string
  family: string
  description: string
}

const BLOCKS: BlockEntry[] = [
  // Auth (5)
  { file: 'login-form.tsx', family: 'auth', description: 'login form with email/password' },
  { file: 'signup-form.tsx', family: 'auth', description: 'signup form with email/password/name' },
  { file: 'oauth-callback-panel.tsx', family: 'auth', description: 'OAuth callback status panel' },
  { file: 'email-verify-panel.tsx', family: 'auth', description: 'email verification status panel' },
  { file: 'protected-route.tsx', family: 'auth', description: 'auth guard wrapper component' },

  // Layout (3)
  { file: 'app-header.tsx', family: 'layout', description: 'app header with nav + action slots' },
  { file: 'app-shell.tsx', family: 'layout', description: 'app shell with header + sidebar + main slots' },
  { file: 'sidebar.tsx', family: 'layout', description: 'sidebar with nav items and active state' },

  // Data (7)
  { file: 'data-table.tsx', family: 'data', description: 'server-side sort/filter data table' },
  { file: 'filter-bar.tsx', family: 'data', description: 'filter bar for data views' },
  { file: 'pagination.tsx', family: 'data', description: 'pagination controls' },
  { file: 'empty-state.tsx', family: 'data', description: 'empty state illustration + cta' },
  { file: 'search-input.tsx', family: 'data', description: 'search input with clear action' },
  { file: 'bulk-actions-bar.tsx', family: 'data', description: 'bulk actions bar for selection state' },
  { file: 'column-picker.tsx', family: 'data', description: 'column visibility picker' },

  // CRUD (4)
  { file: 'crud-create-form.tsx', family: 'crud', description: 'generic create form with field schema' },
  { file: 'crud-edit-form.tsx', family: 'crud', description: 'generic edit form with initial values' },
  { file: 'crud-delete-confirm.tsx', family: 'crud', description: 'delete confirmation dialog' },
  { file: 'crud-list-adapter.tsx', family: 'crud', description: 'generic list adapter / renderer' },

  // Payment (4)
  { file: 'payment-checkout-form.tsx', family: 'payment', description: 'checkout form (accepts onSubmit)' },
  { file: 'payment-method-picker.tsx', family: 'payment', description: 'payment method selection' },
  { file: 'idempotency-key-handler.tsx', family: 'payment', description: 'idempotency key render-prop' },
  { file: 'slow-provider-warning.tsx', family: 'payment', description: '3s threshold slow-provider warning' },

  // Common (3)
  { file: 'confirm-dialog.tsx', family: 'common', description: 'generic confirm/cancel dialog' },
  { file: 'toast.tsx', family: 'common', description: 'sonner toast wrapper' },
  { file: 'loading-boundary.tsx', family: 'common', description: 'Suspense + fallback boundary' },
]

// ─── suite ───────────────────────────────────────────────────────────────────

describe('L2 feature block contract', () => {
  it('templates/L2/blocks/ directory exists', () => {
    expect(
      fileExists(L2_BLOCKS),
      'templates/L2/blocks/ must exist — create it in SP7'
    ).toBe(true)
  })

  for (const block of BLOCKS) {
    const filePath = path.join(L2_BLOCKS, block.file)

    describe(`[${block.family}] ${block.file}`, () => {
      it(`file exists (${block.description})`, () => {
        expect(
          fileExists(filePath),
          `Missing: templates/L2/blocks/${block.file}`
        ).toBe(true)
      })

      it('has a default export', () => {
        const src = readFile(filePath)
        expect(
          hasDefaultExport(src),
          `${block.file} must export a default React component`
        ).toBe(true)
      })

      it('has template_id and evidence in frontmatter comment', () => {
        const src = readFile(filePath)
        expect(
          hasFrontmatter(src),
          `${block.file} must have template_id: and evidence: in frontmatter comment`
        ).toBe(true)
      })

      it('does NOT import from L3, L4, or app/', () => {
        const src = readFile(filePath)
        const bad = illegalImports(src)
        expect(
          bad,
          `${block.file} has illegal imports: ${bad.join(', ')}`
        ).toHaveLength(0)
      })
    })
  }
})
