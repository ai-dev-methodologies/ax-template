/**
 * L3 render-contract.spec.ts — TDD anchor (SP6)
 *
 * RED phase:  fails before templates/L3/pages/ templates are written.
 * GREEN phase: passes after all 7 L3 page-template families are written.
 *
 * Strategy: file-existence + export-shape checks (no DOM renderer required)
 * so the test works without Next.js server context. Each family must:
 *   1. Exist at the expected path
 *   2. Export a React component as `default`
 *   3. Have a sibling README.md with a `## Slot contract` section
 */
import { describe, it, expect } from 'vitest'
import * as fs from 'fs'
import * as path from 'path'

const REPO_ROOT = path.resolve(__dirname, '../../..')
const L3_PAGES = path.join(REPO_ROOT, 'templates/L3/pages')

function readFile(filePath: string): string {
  return fs.readFileSync(filePath, 'utf-8')
}

function fileExists(filePath: string): boolean {
  return fs.existsSync(filePath)
}

function hasDefaultExport(src: string): boolean {
  return /export\s+default\s+/.test(src)
}

function hasSlotContractSection(readme: string): boolean {
  return /^#{1,3}\s+(Slots|Slot contract)/im.test(readme)
}

// ─── family registry ────────────────────────────────────────────────────────

interface Family {
  dir: string
  mainFiles: string[]
  description: string
}

const FAMILIES: Family[] = [
  {
    dir: 'list-page',
    mainFiles: ['page.tsx'],
    description: 'generic list view with filter + pagination slots',
  },
  {
    dir: 'detail-page',
    mainFiles: ['[id]/page.tsx'],
    description: 'generic detail view with section + action slots',
  },
  {
    dir: 'create-page',
    mainFiles: ['page.tsx'],
    description: 'generic create form with form slot',
  },
  {
    dir: 'edit-page',
    mainFiles: ['[id]/page.tsx'],
    description: 'generic edit form with form + delete slots',
  },
  {
    dir: 'dashboard-page',
    mainFiles: ['page.tsx'],
    description: 'generic dashboard with widget slots',
  },
  {
    dir: 'auth-callback-page',
    mainFiles: ['page.tsx'],
    description: 'OAuth / email-verify callback skeleton',
  },
  {
    dir: 'error-page',
    mainFiles: ['loading.tsx', 'not-found.tsx', 'error.tsx'],
    description: 'Next.js error-state bundle (loading, not-found, error)',
  },
]

// ─── suite ──────────────────────────────────────────────────────────────────

describe('L3 page template render-contract', () => {
  it('templates/L3/pages/ directory exists', () => {
    expect(
      fileExists(L3_PAGES),
      `templates/L3/pages/ must exist — create it in SP6`
    ).toBe(true)
  })

  it('templates/L3/pages/README.md exists with Slot contract section', () => {
    const readmePath = path.join(L3_PAGES, 'README.md')
    expect(fileExists(readmePath), `Missing: ${readmePath}`).toBe(true)
    const content = readFile(readmePath)
    expect(
      hasSlotContractSection(content),
      `${readmePath} must have a ## Slot contract section`
    ).toBe(true)
  })

  for (const family of FAMILIES) {
    const familyDir = path.join(L3_PAGES, family.dir)
    const readmePath = path.join(familyDir, 'README.md')

    describe(`family: ${family.dir}`, () => {
      it(`directory exists (${family.description})`, () => {
        expect(
          fileExists(familyDir),
          `Missing family dir: templates/L3/pages/${family.dir}/`
        ).toBe(true)
      })

      it(`README.md exists with Slot contract section`, () => {
        expect(fileExists(readmePath), `Missing: ${readmePath}`).toBe(true)
        const content = readFile(readmePath)
        expect(
          hasSlotContractSection(content),
          `${readmePath} must have a ## Slot contract section`
        ).toBe(true)
      })

      for (const mainFile of family.mainFiles) {
        const filePath = path.join(familyDir, mainFile)

        it(`${mainFile} exists`, () => {
          expect(
            fileExists(filePath),
            `Missing: templates/L3/pages/${family.dir}/${mainFile}`
          ).toBe(true)
        })

        it(`${mainFile} has a default export`, () => {
          const src = readFile(filePath)
          expect(
            hasDefaultExport(src),
            `${mainFile} must export a default React component`
          ).toBe(true)
        })

        it(`${mainFile} has evidence frontmatter comment`, () => {
          const src = readFile(filePath)
          expect(
            src.includes('template_id:') && src.includes('evidence:'),
            `${mainFile} must have template_id and evidence in frontmatter comment`
          ).toBe(true)
        })
      }
    })
  }
})
