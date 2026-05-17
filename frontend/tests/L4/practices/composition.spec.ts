/**
 * L4/practices composition.spec.ts — TDD anchor (SP11)
 *
 * RED phase:  fails before templates/L4/practices/ is written.
 * GREEN phase: passes after all L4/practices files are present and correctly structured.
 *
 * Strategy: file-existence + frontmatter + import-boundary checks.
 * Runs under @playwright/test (used by ax-verify-L4 playwright step).
 * No browser / server needed — all checks are static file analysis.
 *
 * Key differences from crud (full_trio):
 *   - domain_mode: frontend_only  (not full_trio)
 *   - All pages have backend_operation_id: null
 *   - static_source_ref must be present in frontmatter
 *   - No MSW / QueryClient needed (pure static file reads via RSC)
 */
import { test, expect } from '@playwright/test'
import * as fs from 'fs'
import * as path from 'path'

const REPO_ROOT = path.resolve(__dirname, '../../../..')
const L4_PRACTICES = path.join(REPO_ROOT, 'templates/L4/practices')

function readFile(filePath: string): string {
  return fs.readFileSync(filePath, 'utf-8')
}

function fileExists(filePath: string): boolean {
  return fs.existsSync(filePath)
}

function hasFrontmatter(src: string): boolean {
  return src.includes('template_id:') && src.includes('evidence:')
}

function hasFrontendOnlyMode(src: string): boolean {
  return src.includes('domain_mode: frontend_only')
}

function hasNullBackendOpId(src: string): boolean {
  return src.includes('backend_operation_id: null')
}

function hasStaticSourceRef(src: string): boolean {
  return src.includes('static_source_ref:')
}

function hasImportFrom(src: string, target: string): boolean {
  return src.includes(target)
}

/** Returns any illegal cross-L4 domain imports */
function illegalCrossL4Imports(src: string): string[] {
  const illegal: string[] = []
  for (const line of src.split('\n')) {
    if (/^import\s/.test(line) || /from\s+['"]/.test(line)) {
      // L4/practices must NOT import from other L4 domains
      if (/templates\/L4\/(auth|crud|payment)/.test(line)) {
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
  mustImport?: string
  checkStaticRef?: boolean
}

const REQUIRED_FILES: FileEntry[] = [
  {
    file: 'app/layout.tsx',
    description: 'root layout with html/body shell',
  },
  {
    file: 'app/page.tsx',
    description: 'root redirect to /practices',
  },
  {
    file: 'app/providers.tsx',
    description: 'minimal client provider tree (no QueryClient needed)',
  },
  {
    file: 'app/(practices)/layout.tsx',
    description: 'practices route group layout with AppShell',
  },
  {
    file: 'app/(practices)/page.tsx',
    description: 'redirect to /practices list',
  },
  {
    file: 'app/(practices)/category/[prefix]/page.tsx',
    description: 'CATEGORY — filter rules by prefix',
    checkStaticRef: true,
  },
  {
    file: 'app/(practices)/rule/[id]/page.tsx',
    description: 'DETAIL — single rule rendered as markdown',
    checkStaticRef: true,
  },
  {
    file: 'lib/load-rules.ts',
    description: 'SERVER-ONLY: read practices/**/*.md, parse frontmatter, return Rule[]',
  },
  {
    file: 'lib/rule-parser.ts',
    description: 'markdown frontmatter parsing utilities',
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

test.describe('L4/practices composition contract', () => {
  test('templates/L4/practices/ directory exists', () => {
    expect(
      fileExists(L4_PRACTICES),
      'templates/L4/practices/ must exist — SP11 creates it'
    ).toBe(true)
  })

  for (const entry of REQUIRED_FILES) {
    const filePath = path.join(L4_PRACTICES, entry.file)

    test(`[exists] ${entry.file} — ${entry.description}`, () => {
      expect(
        fileExists(filePath),
        `Missing: templates/L4/practices/${entry.file}`
      ).toBe(true)
    })

    // Frontmatter checks for TSX/TS files (skip README/next.config)
    if (entry.file.endsWith('.tsx') || entry.file.endsWith('.ts')) {
      test(`[frontmatter] ${entry.file} has template_id and evidence`, () => {
        expect(fileExists(filePath), `Missing file: templates/L4/practices/${entry.file}`).toBe(true)
        const src = readFile(filePath)
        expect(
          hasFrontmatter(src),
          `${entry.file} must have template_id: and evidence: in frontmatter comment`
        ).toBe(true)
      })

      test(`[layer] ${entry.file} has layer: L4`, () => {
        expect(fileExists(filePath), `Missing file: templates/L4/practices/${entry.file}`).toBe(true)
        const src = readFile(filePath)
        expect(src).toContain('layer: L4')
      })

      test(`[domain_mode] ${entry.file} has domain_mode: frontend_only`, () => {
        expect(fileExists(filePath), `Missing file: templates/L4/practices/${entry.file}`).toBe(true)
        const src = readFile(filePath)
        expect(
          hasFrontendOnlyMode(src),
          `${entry.file} must have domain_mode: frontend_only (practices is a static catalog viewer)`
        ).toBe(true)
      })

      test(`[backend_op_null] ${entry.file} has backend_operation_id: null`, () => {
        expect(fileExists(filePath), `Missing file: templates/L4/practices/${entry.file}`).toBe(true)
        const src = readFile(filePath)
        expect(
          hasNullBackendOpId(src),
          `${entry.file} must declare backend_operation_id: null (frontend_only mode)`
        ).toBe(true)
      })

      test(`[no cross-L4] ${entry.file} has no imports from other L4 domains`, () => {
        expect(fileExists(filePath), `Missing file: templates/L4/practices/${entry.file}`).toBe(true)
        const src = readFile(filePath)
        const bad = illegalCrossL4Imports(src)
        expect(
          bad,
          `${entry.file} has illegal cross-L4 imports: ${bad.join(', ')}`
        ).toHaveLength(0)
      })
    }

    // static_source_ref check for pages that read from practices/
    if (entry.checkStaticRef && (entry.file.endsWith('.tsx') || entry.file.endsWith('.ts'))) {
      test(`[static_source_ref] ${entry.file} has static_source_ref in frontmatter`, () => {
        expect(fileExists(filePath), `Missing file: templates/L4/practices/${entry.file}`).toBe(true)
        const src = readFile(filePath)
        expect(
          hasStaticSourceRef(src),
          `${entry.file} must declare static_source_ref (frontend_only: reads from practices/**)`
        ).toBe(true)
      })
    }

    // Import checks for specific lib files
    if (entry.mustImport) {
      test(`[import] ${entry.file} imports from ${entry.mustImport}`, () => {
        expect(fileExists(filePath), `Missing file: templates/L4/practices/${entry.file}`).toBe(true)
        const src = readFile(filePath)
        expect(
          hasImportFrom(src, entry.mustImport!),
          `${entry.file} must import from ${entry.mustImport}`
        ).toBe(true)
      })
    }
  }

  // Index page (LIST) checks
  test('[list] (practices)/page.tsx uses load-rules and renders INDEX', () => {
    const filePath = path.join(L4_PRACTICES, 'app/(practices)/page.tsx')
    if (!fileExists(filePath)) {
      // If this file doesn't exist, the [exists] test will catch it
      return
    }
    const src = readFile(filePath)
    // Must import load-rules
    expect(src).toContain('load-rules')
    // Must have backend_operation_id: null
    expect(src).toContain('backend_operation_id: null')
  })

  // README content check
  test('[readme] README.md has "how to fork" instructions', () => {
    const readmePath = path.join(L4_PRACTICES, 'README.md')
    if (!fileExists(readmePath)) return
    const src = readFile(readmePath)
    expect(src).toContain('fork')
  })

  // Spec Trio presence check (frontend_only)
  test('[spec-trio] specs/practices-frontend-l0.yaml exists', () => {
    const specPath = path.join(REPO_ROOT, 'specs/practices-frontend-l0.yaml')
    expect(
      fileExists(specPath),
      'specs/practices-frontend-l0.yaml must exist for frontend_only trio_integrity_guard'
    ).toBe(true)
  })

  test('[spec-trio] contracts/practices-ui.yaml exists', () => {
    const contractPath = path.join(REPO_ROOT, 'contracts/practices-ui.yaml')
    expect(
      fileExists(contractPath),
      'contracts/practices-ui.yaml must exist for frontend_only trio_integrity_guard'
    ).toBe(true)
  })

  test('[spec-trio] blueprints/practices-ui-manifest.yaml exists', () => {
    const blueprintPath = path.join(REPO_ROOT, 'blueprints/practices-ui-manifest.yaml')
    expect(
      fileExists(blueprintPath),
      'blueprints/practices-ui-manifest.yaml must exist for frontend_only trio_integrity_guard'
    ).toBe(true)
  })

  // domain_mode: frontend_only in spec files
  test('[spec-trio] specs/practices-frontend-l0.yaml has domain_mode: frontend_only', () => {
    const specPath = path.join(REPO_ROOT, 'specs/practices-frontend-l0.yaml')
    if (!fileExists(specPath)) return
    const src = readFile(specPath)
    expect(src).toContain('domain_mode: frontend_only')
  })

  test('[spec-trio] contracts/practices-ui.yaml has domain_mode: frontend_only', () => {
    const contractPath = path.join(REPO_ROOT, 'contracts/practices-ui.yaml')
    if (!fileExists(contractPath)) return
    const src = readFile(contractPath)
    expect(src).toContain('domain_mode: frontend_only')
  })

  // lib files check
  test('[lib] load-rules.ts contains loadAllRules export', () => {
    const libPath = path.join(L4_PRACTICES, 'lib/load-rules.ts')
    if (!fileExists(libPath)) return
    const src = readFile(libPath)
    expect(src).toContain('loadAllRules')
  })

  test('[lib] rule-parser.ts contains parseFrontmatter export', () => {
    const parserPath = path.join(L4_PRACTICES, 'lib/rule-parser.ts')
    if (!fileExists(parserPath)) return
    const src = readFile(parserPath)
    expect(src).toContain('parseFrontmatter')
  })
})
