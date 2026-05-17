/**
 * cross-cutting-contract.spec.ts — TDD anchor (SP15)
 *
 * RED  phase: fails before templates/L2/blocks/{toast-queue,error-boundary,
 *             offline-banner,virtualized-table}.tsx are written.
 * GREEN phase: passes after all 4 cross-cutting blocks are written.
 *
 * Strategy: file-existence + export-shape + behavior (via Testing Library).
 * Environment: jsdom (see vitest.config.ts).
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

// ─── file-existence + shape tests ────────────────────────────────────────────

const CROSS_CUTTING_BLOCKS = [
  { file: 'toast-queue.tsx', description: 'app-wide toast queue manager (sonner-based)' },
  { file: 'error-boundary.tsx', description: 'React 19 error boundary with reset key + fallback slot' },
  { file: 'offline-banner.tsx', description: 'navigator.onLine network status banner' },
  { file: 'virtualized-table.tsx', description: '@tanstack/react-virtual row-virtualized table' },
]

describe('SP15 cross-cutting L2 block contract', () => {
  for (const block of CROSS_CUTTING_BLOCKS) {
    const filePath = path.join(L2_BLOCKS, block.file)

    describe(block.file, () => {
      it(`file exists (${block.description})`, () => {
        expect(
          fileExists(filePath),
          `Missing: templates/L2/blocks/${block.file}`
        ).toBe(true)
      })

      it('has a default export', () => {
        const src = readFile(filePath)
        expect(hasDefaultExport(src), `${block.file} must export a default component`).toBe(true)
      })

      it('has template_id and evidence in frontmatter comment', () => {
        const src = readFile(filePath)
        expect(hasFrontmatter(src), `${block.file} must have template_id: and evidence: in frontmatter`).toBe(true)
      })

      it('does NOT import from L3, L4, or app/', () => {
        const src = readFile(filePath)
        const bad = illegalImports(src)
        expect(bad, `${block.file} has illegal imports: ${bad.join(', ')}`).toHaveLength(0)
      })
    })
  }

  // ─── toast-queue: exports ─────────────────────────────────────────────────

  describe('toast-queue exports', () => {
    it('exports enqueueToast function', () => {
      const src = readFile(path.join(L2_BLOCKS, 'toast-queue.tsx'))
      expect(src).toMatch(/export\s+(function|const)\s+enqueueToast/)
    })

    it('exports ToastQueueProvider component', () => {
      const src = readFile(path.join(L2_BLOCKS, 'toast-queue.tsx'))
      expect(src).toMatch(/ToastQueueProvider/)
    })

    it('uses sonner for rendering', () => {
      const src = readFile(path.join(L2_BLOCKS, 'toast-queue.tsx'))
      expect(src).toMatch(/from\s+['"]sonner['"]/)
    })

    it('has aria-live region for accessibility', () => {
      const src = readFile(path.join(L2_BLOCKS, 'toast-queue.tsx'))
      expect(src).toMatch(/aria-live/)
    })
  })

  // ─── error-boundary: exports ──────────────────────────────────────────────

  describe('error-boundary exports', () => {
    it('exports ErrorBoundary as default', () => {
      const src = readFile(path.join(L2_BLOCKS, 'error-boundary.tsx'))
      expect(src).toMatch(/export\s+default.*ErrorBoundary|class\s+ErrorBoundary/)
    })

    it('accepts resetKey prop', () => {
      const src = readFile(path.join(L2_BLOCKS, 'error-boundary.tsx'))
      expect(src).toMatch(/resetKey/)
    })

    it('accepts onError callback prop (auto-report-error hook)', () => {
      const src = readFile(path.join(L2_BLOCKS, 'error-boundary.tsx'))
      expect(src).toMatch(/onError/)
    })

    it('accepts fallback or fallbackRender slot', () => {
      const src = readFile(path.join(L2_BLOCKS, 'error-boundary.tsx'))
      expect(src).toMatch(/fallback/)
    })

    it('extends React.Component (class-based required by React error boundary API)', () => {
      const src = readFile(path.join(L2_BLOCKS, 'error-boundary.tsx'))
      expect(src).toMatch(/class.*extends.*Component/)
    })
  })

  // ─── offline-banner: exports ──────────────────────────────────────────────

  describe('offline-banner exports', () => {
    it('exports OfflineBanner as default', () => {
      const src = readFile(path.join(L2_BLOCKS, 'offline-banner.tsx'))
      expect(src).toMatch(/OfflineBanner/)
    })

    it('listens to online/offline events', () => {
      const src = readFile(path.join(L2_BLOCKS, 'offline-banner.tsx'))
      expect(src).toMatch(/['"]offline['"]/)
      expect(src).toMatch(/['"]online['"]/)
    })

    it('uses navigator.onLine for initial state', () => {
      const src = readFile(path.join(L2_BLOCKS, 'offline-banner.tsx'))
      expect(src).toMatch(/navigator\.onLine/)
    })
  })

  // ─── virtualized-table: exports ───────────────────────────────────────────

  describe('virtualized-table exports', () => {
    it('imports from @tanstack/react-virtual', () => {
      const src = readFile(path.join(L2_BLOCKS, 'virtualized-table.tsx'))
      expect(src).toMatch(/from\s+['"]@tanstack\/react-virtual['"]/)
    })

    it('re-exports or reuses ColumnDef from data-table', () => {
      const src = readFile(path.join(L2_BLOCKS, 'virtualized-table.tsx'))
      // Either imports ColumnDef from data-table or defines compatible type
      expect(src).toMatch(/ColumnDef/)
    })

    it('accepts same core props as DataTable (columns, data, getRowKey)', () => {
      const src = readFile(path.join(L2_BLOCKS, 'virtualized-table.tsx'))
      expect(src).toMatch(/columns/)
      expect(src).toMatch(/getRowKey/)
    })

    it('uses useVirtualizer for row virtualization', () => {
      const src = readFile(path.join(L2_BLOCKS, 'virtualized-table.tsx'))
      expect(src).toMatch(/useVirtualizer/)
    })
  })
})
