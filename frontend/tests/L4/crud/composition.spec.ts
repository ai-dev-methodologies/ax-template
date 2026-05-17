/**
 * L4/crud composition.spec.ts — TDD anchor (SP9)
 *
 * RED phase:  fails before templates/L4/crud/ is written.
 * GREEN phase: passes after all L4/crud files are present and correctly structured.
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
 *   5. Key L2/L3 blocks are correctly imported where required
 */
import { test, expect } from '@playwright/test'
import * as fs from 'fs'
import * as path from 'path'

const REPO_ROOT = path.resolve(__dirname, '../../../..')
const L4_CRUD = path.join(REPO_ROOT, 'templates/L4/crud')

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
      // L4/crud must NOT import from other L4 domains
      if (/templates\/L4\/(auth|payment|practices)/.test(line)) {
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
  mustImport?: string
}

const REQUIRED_FILES: FileEntry[] = [
  {
    file: 'app/layout.tsx',
    description: 'root layout with Providers wrapper',
  },
  {
    file: 'app/page.tsx',
    description: 'root redirect to /(crud)/items',
  },
  {
    file: 'app/providers.tsx',
    description: 'client provider tree (QueryClient)',
  },
  {
    file: 'app/(crud)/layout.tsx',
    description: 'crud route group layout with breadcrumb slot',
  },
  {
    file: 'app/(crud)/page.tsx',
    description: 'redirect to /items (list)',
  },
  {
    file: 'app/(crud)/items/page.tsx',
    description: 'LIST — uses L3 list-page + L2 DataTable + FilterBar + Pagination + EmptyState',
    backendOpId: 'listItems',
    mustImport: 'data-table',
  },
  {
    file: 'app/(crud)/items/new/page.tsx',
    description: 'CREATE — uses L3 create-page + L2 CrudCreateForm',
    backendOpId: 'createItem',
    mustImport: 'crud-create-form',
  },
  {
    file: 'app/(crud)/items/[id]/page.tsx',
    description: 'DETAIL — uses L3 detail-page',
    backendOpId: 'getItem',
  },
  {
    file: 'app/(crud)/items/[id]/edit/page.tsx',
    description: 'EDIT — uses L3 edit-page + L2 CrudEditForm + CrudDeleteConfirm',
    backendOpId: 'updateItem',
    mustImport: 'crud-edit-form',
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

test.describe('L4/crud composition contract', () => {
  test('templates/L4/crud/ directory exists', () => {
    expect(
      fileExists(L4_CRUD),
      'templates/L4/crud/ must exist — SP9 creates it'
    ).toBe(true)
  })

  for (const entry of REQUIRED_FILES) {
    const filePath = path.join(L4_CRUD, entry.file)

    test(`[exists] ${entry.file} — ${entry.description}`, () => {
      expect(
        fileExists(filePath),
        `Missing: templates/L4/crud/${entry.file}`
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

    // Import check for specific L2/L3 blocks
    if (entry.mustImport) {
      test(`[import] ${entry.file} imports from ${entry.mustImport}`, () => {
        const src = readFile(filePath)
        expect(
          hasImportFrom(src, entry.mustImport!),
          `${entry.file} must import from ${entry.mustImport}`
        ).toBe(true)
      })
    }
  }

  // README content check
  test('[readme] README.md has "how to fork" instructions', () => {
    const readmePath = path.join(L4_CRUD, 'README.md')
    const src = readFile(readmePath)
    expect(src).toContain('fork')
    expect(src).toContain('copy')
  })
})
