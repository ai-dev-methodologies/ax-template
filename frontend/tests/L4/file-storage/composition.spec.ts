/**
 * L4/file-storage composition.spec.ts — TDD anchor (SP18)
 *
 * RED phase:  fails before templates/L4/file-storage/ is written.
 * GREEN phase: passes after all L4/file-storage files are present and correctly structured.
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
 *   5. Key L1/L2 blocks are correctly imported where required
 *
 * File-storage-specific assertions:
 *   6. upload/page.tsx imports file-dropzone (L1) and error-boundary, toast-queue (L2)
 *   7. upload/page.tsx has backend_operation_id: uploadFile
 *   8. files/page.tsx has backend_operation_id: listFiles
 *   9. files/page.tsx imports data-table
 *  10. files/[id]/page.tsx has backend_operation_id: getFile
 *  11. files/[id]/page.tsx references refetchInterval (PENDING polling)
 *  12. README.md has "fork" and "copy" instructions
 */
import { test, expect } from '@playwright/test'
import * as fs from 'fs'
import * as path from 'path'

const REPO_ROOT = path.resolve(__dirname, '../../../..')
const L4_FILE_STORAGE = path.join(REPO_ROOT, 'templates/L4/file-storage')

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
      // L4/file-storage must NOT import from other L4 domains
      if (/templates\/L4\/(auth|crud|payment|practices|notification|audit-log)/.test(line)) {
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
    description: 'root redirect to /(file-storage)/upload',
  },
  {
    file: 'app/providers.tsx',
    description: 'client provider tree (QueryClient, staleTime 60s)',
  },
  {
    file: 'app/(file-storage)/layout.tsx',
    description: 'file-storage route group layout with AppShell + Sidebar + AppHeader',
  },
  {
    file: 'app/(file-storage)/page.tsx',
    description: 'redirect to /upload',
  },
  {
    file: 'app/(file-storage)/upload/page.tsx',
    description: 'UPLOAD — uses L1 FileDropzone + L2 error-boundary + toast-queue',
    backendOpId: 'uploadFile',
    mustImport: [
      'file-dropzone',
      'error-boundary',
      'toast-queue',
    ],
  },
  {
    file: 'app/(file-storage)/files/page.tsx',
    description: 'LIST — DataTable of files with status badges',
    backendOpId: 'listFiles',
    mustImport: ['data-table'],
  },
  {
    file: 'app/(file-storage)/files/[id]/page.tsx',
    description: 'DETAIL — file metadata + download + delete; polls PENDING',
    backendOpId: 'getFile',
  },
  {
    file: 'README.md',
    description: 'fork instructions with spec cross-reference table',
  },
  {
    file: 'next.config.ts',
    description: 'minimal Next.js config with /api proxy',
  },
]

// ─── suite ───────────────────────────────────────────────────────────────────

test.describe('L4/file-storage composition contract', () => {
  test('templates/L4/file-storage/ directory exists', () => {
    expect(
      fileExists(L4_FILE_STORAGE),
      'templates/L4/file-storage/ must exist — SP18 creates it'
    ).toBe(true)
  })

  for (const entry of REQUIRED_FILES) {
    const filePath = path.join(L4_FILE_STORAGE, entry.file)

    test(`[exists] ${entry.file} — ${entry.description}`, () => {
      expect(
        fileExists(filePath),
        `Missing: templates/L4/file-storage/${entry.file}`
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

      test(`[domain] ${entry.file} has domain: file-storage`, () => {
        const src = readFile(filePath)
        expect(src).toContain('domain: file-storage')
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

    // Import checks for specific L1/L2 blocks
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
    const readmePath = path.join(L4_FILE_STORAGE, 'README.md')
    const src = readFile(readmePath)
    expect(src).toContain('fork')
    expect(src).toContain('copy')
  })

  // File-storage-specific: upload page uses L1 FileDropzone (SP14 baseline)
  test('[file-storage-specific] upload/page.tsx uses L1 file-dropzone', () => {
    const uploadPath = path.join(L4_FILE_STORAGE, 'app/(file-storage)/upload/page.tsx')
    const src = readFile(uploadPath)
    expect(src).toContain('file-dropzone')
    // FileDropzone must receive the accept prop
    expect(src).toMatch(/accept[=\s{]|maxSize/)
  })

  // File-storage-specific: upload page maps error types to user messages (FILE-FE-ERROR-001)
  test('[file-storage-specific] upload/page.tsx has quota-exceeded error message mapping', () => {
    const uploadPath = path.join(L4_FILE_STORAGE, 'app/(file-storage)/upload/page.tsx')
    const src = readFile(uploadPath)
    expect(src).toContain('quota-exceeded')
  })

  // File-storage-specific: list page has status badge with text labels (FILE-FE-A11Y-002)
  test('[file-storage-specific] files/page.tsx has StatusBadge with text labels', () => {
    const listPath = path.join(L4_FILE_STORAGE, 'app/(file-storage)/files/page.tsx')
    const src = readFile(listPath)
    // Must have text labels (not just color classes) for WCAG 1.4.1
    expect(src).toMatch(/Scanning|Ready|Quarantined/)
  })

  // File-storage-specific: detail page polls PENDING status (FILE-FE-ERROR-002)
  test('[file-storage-specific] files/[id]/page.tsx has refetchInterval for PENDING polling', () => {
    const detailPath = path.join(L4_FILE_STORAGE, 'app/(file-storage)/files/[id]/page.tsx')
    const src = readFile(detailPath)
    expect(src).toContain('refetchInterval')
    expect(src).toMatch(/PENDING|pending/)
  })

  // File-storage-specific: detail page has 'Scan in progress' for PENDING download button
  test('[file-storage-specific] files/[id]/page.tsx shows "Scan in progress" for PENDING files', () => {
    const detailPath = path.join(L4_FILE_STORAGE, 'app/(file-storage)/files/[id]/page.tsx')
    const src = readFile(detailPath)
    expect(src).toMatch(/Scan in progress/)
  })

  // File-storage-specific: providers use correct staleTime for files domain
  test('[file-storage-specific] providers.tsx uses 60s staleTime (not payment 30s)', () => {
    const providersPath = path.join(L4_FILE_STORAGE, 'app/providers.tsx')
    const src = readFile(providersPath)
    // 60 seconds staleTime from manifest
    expect(src).toMatch(/60\s*\*\s*1000|60000/)
  })
})
