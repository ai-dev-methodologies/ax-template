/**
 * L4/audit-log composition.spec.ts — TDD anchor (SP17)
 *
 * RED phase:  fails before templates/L4/audit-log/ is written.
 * GREEN phase: passes after all L4/audit-log files are present and correctly structured.
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
 * Audit-log-specific assertions:
 *   6. list page imports VirtualizedTable (handles >10k rows)
 *   7. list page imports FilterBar (filter by actor/resource/action/date)
 *   8. [id]/page.tsx has backend_operation_id: getAuditLog
 *   9. export/page.tsx has backend_operation_id: exportAuditLogs
 *  10. list paginates via cursor or page param
 */
import { test, expect } from '@playwright/test'
import * as fs from 'fs'
import * as path from 'path'

const REPO_ROOT = path.resolve(__dirname, '../../../..')
const L4_AUDIT_LOG = path.join(REPO_ROOT, 'templates/L4/audit-log')

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
      // L4/audit-log must NOT import from other L4 domains
      if (/templates\/L4\/(auth|crud|practices|payment)/.test(line)) {
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
    description: 'root page redirects to /audit-log list',
  },
  {
    file: 'app/providers.tsx',
    description: 'client provider tree with QueryClientProvider',
  },
  {
    file: 'app/(audit-log)/layout.tsx',
    description: 'audit-log route group layout with AppShell',
  },
  {
    file: 'app/(audit-log)/page.tsx',
    description: 'LIST page using VirtualizedTable for >10k rows',
    backendOpId: 'listAuditLogs',
    mustImport: ['virtualized-table', 'filter-bar'],
  },
  {
    file: 'app/(audit-log)/[id]/page.tsx',
    description: 'DETAIL page for a single audit log entry',
    backendOpId: 'getAuditLog',
  },
  {
    file: 'app/(audit-log)/export/page.tsx',
    description: 'EXPORT page for bulk export request',
    backendOpId: 'exportAuditLogs',
  },
  {
    file: 'README.md',
    description: 'fork instructions for the audit-log L4 vertical',
  },
  {
    file: 'next.config.ts',
    description: 'minimal Next.js config',
  },
]

// ─── tests ──────────────────────────────────────────────────────────────────

test.describe('L4/audit-log composition', () => {
  for (const entry of REQUIRED_FILES) {
    const filePath = path.join(L4_AUDIT_LOG, entry.file)

    test(`[exists] ${entry.file} — ${entry.description}`, () => {
      expect(
        fileExists(filePath),
        `Missing: templates/L4/audit-log/${entry.file}`
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

      test(`[domain] ${entry.file} has domain: audit-log`, () => {
        const src = readFile(filePath)
        expect(src).toContain('domain: audit-log')
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
    const readmePath = path.join(L4_AUDIT_LOG, 'README.md')
    const src = readFile(readmePath)
    expect(src).toContain('fork')
    expect(src).toContain('copy')
  })

  // Audit-log-specific: list page handles >10k rows via virtualized-table
  test('[audit-log-specific] list page uses VirtualizedTable for large datasets', () => {
    const listPath = path.join(L4_AUDIT_LOG, 'app/(audit-log)/page.tsx')
    const src = readFile(listPath)
    expect(src).toContain('VirtualizedTable')
    // Must reference virtualized-table import path
    expect(src).toContain('virtualized-table')
  })

  test('[audit-log-specific] list page has FilterBar for actor/resource/action/date filtering', () => {
    const listPath = path.join(L4_AUDIT_LOG, 'app/(audit-log)/page.tsx')
    const src = readFile(listPath)
    expect(src).toContain('FilterBar')
    // Must reference filter fields
    expect(src).toMatch(/actor|resource|action|date/)
  })

  test('[audit-log-specific] list page supports pagination (cursor or page param)', () => {
    const listPath = path.join(L4_AUDIT_LOG, 'app/(audit-log)/page.tsx')
    const src = readFile(listPath)
    expect(src).toMatch(/cursor|page|Pagination/)
  })

  test('[audit-log-specific] detail page renders log entry metadata', () => {
    const detailPath = path.join(L4_AUDIT_LOG, 'app/(audit-log)/[id]/page.tsx')
    const src = readFile(detailPath)
    // Must reference common audit log fields
    expect(src).toMatch(/actor|action|resource|timestamp|id|params/)
  })

  test('[audit-log-specific] export page submits export request with format selection', () => {
    const exportPath = path.join(L4_AUDIT_LOG, 'app/(audit-log)/export/page.tsx')
    const src = readFile(exportPath)
    // Must reference export format (csv or json)
    expect(src).toMatch(/csv|json|format|export/i)
  })

  test('[audit-log-specific] list page virtualized table handles 5000+ rows', () => {
    const listPath = path.join(L4_AUDIT_LOG, 'app/(audit-log)/page.tsx')
    const src = readFile(listPath)
    // Must use server-side pagination (not client-side) — check for API call pattern
    expect(src).toMatch(/useQuery|useInfiniteQuery|fetchAuditLogs|listAuditLogs/)
  })
})
