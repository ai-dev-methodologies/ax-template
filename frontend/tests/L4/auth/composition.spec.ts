/**
 * L4/auth composition.spec.ts — TDD anchor (SP8)
 *
 * RED phase:  fails before templates/L4/auth/ is written.
 * GREEN phase: passes after all L4/auth files are present and correctly structured.
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
const L4_AUTH = path.join(REPO_ROOT, 'templates/L4/auth')

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
      // L4/auth must NOT import from other L4 domains
      if (/templates\/L4\/(crud|payment|practices)/.test(line)) {
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
  // Auth route group
  {
    file: 'app/(auth)/layout.tsx',
    description: 'auth route group layout (no chrome)',
  },
  {
    file: 'app/(auth)/login/page.tsx',
    description: 'login page using L2 LoginForm',
    backendOpId: 'emailLogin',
    mustImport: 'L2/blocks/login-form',
  },
  {
    file: 'app/(auth)/signup/page.tsx',
    description: 'signup page using L2 SignupForm',
    backendOpId: 'emailSignup',
    mustImport: 'L2/blocks/signup-form',
  },
  {
    file: 'app/(auth)/verify/page.tsx',
    description: 'email verify page using L2 EmailVerifyPanel',
    backendOpId: 'emailVerify',
    mustImport: 'L2/blocks/email-verify-panel',
  },
  {
    file: 'app/(auth)/oauth/callback/page.tsx',
    description: 'OAuth callback page using L2 OAuthCallbackPanel',
    backendOpId: 'oauthCallback',
    mustImport: 'L2/blocks/oauth-callback-panel',
  },

  // Authenticated route group
  {
    file: 'app/(authenticated)/layout.tsx',
    description: 'protected route group layout using L2 ProtectedRoute',
    mustImport: 'L2/blocks/protected-route',
  },
  {
    file: 'app/(authenticated)/dashboard/page.tsx',
    description: 'placeholder protected dashboard page',
    backendOpId: 'getAuthState',
  },

  // Root app
  {
    file: 'app/layout.tsx',
    description: 'root layout with Providers',
  },
  {
    file: 'app/page.tsx',
    description: 'root page redirect to /login',
  },
  {
    file: 'app/providers.tsx',
    description: 'QueryClientProvider + MSW setup',
  },

  // Edge middleware
  {
    file: 'middleware.ts',
    description: 'edge auth guard middleware',
  },

  // Fork instructions
  {
    file: 'README.md',
    description: 'fork receiver instructions',
  },
  {
    file: 'next.config.ts',
    description: 'minimal Next.js config for fork-receiver build test',
  },
]

// ─── suite ───────────────────────────────────────────────────────────────────

test.describe('L4/auth composition contract', () => {
  test('templates/L4/auth/ directory exists', () => {
    expect(
      fileExists(L4_AUTH),
      'templates/L4/auth/ must exist — SP8 creates it'
    ).toBe(true)
  })

  for (const entry of REQUIRED_FILES) {
    const filePath = path.join(L4_AUTH, entry.file)

    test(`[exists] ${entry.file} — ${entry.description}`, () => {
      expect(
        fileExists(filePath),
        `Missing: templates/L4/auth/${entry.file}`
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
    const readmePath = path.join(L4_AUTH, 'README.md')
    const src = readFile(readmePath)
    expect(src).toContain('fork')
    expect(src).toContain('copy')
  })
})
