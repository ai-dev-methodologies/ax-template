/**
 * L4/notification composition.spec.ts — TDD anchor (SP16)
 *
 * RED phase:  fails before templates/L4/notification/ is written.
 * GREEN phase: passes after all L4/notification files are present and correctly structured.
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
 * Notification-specific assertions:
 *   6. inbox/page.tsx imports notification-list and virtualized-table
 *   7. inbox/page.tsx has backend_operation_id: listNotifications
 *   8. [id]/page.tsx has backend_operation_id: markNotificationRead
 *   9. settings/page.tsx has backend_operation_id: getNotificationPreferences
 *  10. notification-bell in app-header imports notification-bell block
 */
import { test, expect } from '@playwright/test'
import * as fs from 'fs'
import * as path from 'path'

const REPO_ROOT = path.resolve(__dirname, '../../../..')
const L4_NOTIFICATION = path.join(REPO_ROOT, 'templates/L4/notification')

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
      // L4/notification must NOT import from other L4 domains
      if (/templates\/L4\/(auth|crud|payment|practices|audit-log|file-storage)/.test(line)) {
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
    description: 'root redirect to /(notification)/inbox',
  },
  {
    file: 'app/providers.tsx',
    description: 'client provider tree (QueryClient)',
  },
  {
    file: 'app/(notification)/layout.tsx',
    description: 'notification route group layout with AppShell + Sidebar + AppHeader + NotificationBell',
    mustImport: ['notification-bell'],
  },
  {
    file: 'app/(notification)/inbox/page.tsx',
    description: 'INBOX — notification list using notification-list + virtualized-table',
    backendOpId: 'listNotifications',
    mustImport: ['notification-list', 'virtualized-table'],
  },
  {
    file: 'app/(notification)/settings/page.tsx',
    description: 'SETTINGS — notification preferences form',
    backendOpId: 'getNotificationPreferences',
  },
  {
    file: 'app/(notification)/[id]/page.tsx',
    description: 'DETAIL — single notification + mark-read action',
    backendOpId: 'markNotificationRead',
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

test.describe('L4/notification composition contract', () => {
  test('templates/L4/notification/ directory exists', () => {
    expect(
      fileExists(L4_NOTIFICATION),
      'templates/L4/notification/ must exist — SP16 creates it'
    ).toBe(true)
  })

  for (const entry of REQUIRED_FILES) {
    const filePath = path.join(L4_NOTIFICATION, entry.file)

    test(`[exists] ${entry.file} — ${entry.description}`, () => {
      expect(
        fileExists(filePath),
        `Missing: templates/L4/notification/${entry.file}`
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

      test(`[domain] ${entry.file} has domain: notification`, () => {
        const src = readFile(filePath)
        expect(src).toContain('domain: notification')
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
    const readmePath = path.join(L4_NOTIFICATION, 'README.md')
    const src = readFile(readmePath)
    expect(src).toContain('fork')
    expect(src).toContain('copy')
  })

  // Notification-specific: inbox imports virtualized-table (SP15 dependency)
  test('[notification-specific] inbox/page.tsx uses VirtualizedTable for large notification lists', () => {
    const inboxPath = path.join(L4_NOTIFICATION, 'app/(notification)/inbox/page.tsx')
    const src = readFile(inboxPath)
    expect(src).toContain('virtualized-table')
  })

  // Notification-specific: mark-read updates state
  test('[notification-specific] [id]/page.tsx has mark-read mutation pattern', () => {
    const detailPath = path.join(L4_NOTIFICATION, 'app/(notification)/[id]/page.tsx')
    const src = readFile(detailPath)
    expect(src).toMatch(/mark.?read|markRead|PATCH|read.*true/i)
  })

  // Notification-specific: bell shows unread count
  test('[notification-specific] (notification)/layout.tsx integrates NotificationBell with unread count', () => {
    const layoutPath = path.join(L4_NOTIFICATION, 'app/(notification)/layout.tsx')
    const src = readFile(layoutPath)
    expect(src).toContain('notification-bell')
    expect(src).toMatch(/unread|badge|count/i)
  })

  // Notification-specific: settings form submits preferences
  test('[notification-specific] settings/page.tsx has preferences form submit', () => {
    const settingsPath = path.join(L4_NOTIFICATION, 'app/(notification)/settings/page.tsx')
    const src = readFile(settingsPath)
    expect(src).toMatch(/preference|channel|in.?app|email/i)
  })
})
