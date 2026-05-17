/*
---
template_id: L4/practices/lib/load-rules
layer: L4
domain: practices
domain_mode: frontend_only
backend_operation_id: null
static_source_ref:
  - practices/rules/async-virtual-thread-executor.md
  - practices-react/rules/async-parallel.md
evidence:
  - source_type: internal
    rationale: "L4 practices vertical — SERVER-ONLY loader that reads practices/**/*.md and practices-react/**/*.md via Node fs, parses frontmatter, and returns typed Rule[]. Used by all three RSC pages."
  - source_type: external
    citation: "Next.js 15 App Router — Server Components can use Node.js fs API"
    url: "https://nextjs.org/docs/app/building-your-application/rendering/server-components"
  - source_type: external
    citation: "React — cache() for deduplication of expensive server work"
    url: "https://react.dev/reference/react/cache"
provenance_class: internal_design
imports_from: [L1, L2, L3]
imports_forbidden: [L4/auth, L4/crud, L4/payment]
---
*/

import * as fs from 'fs'
import * as path from 'path'
import { cache } from 'react'
import { parseFrontmatter, type ParsedRule } from './rule-parser'

// ─── repo root resolution ────────────────────────────────────────────────────

/**
 * Resolve the monorepo root at runtime (works for both local dev and Vercel).
 * Walks up from __dirname until it finds the practices/ directory.
 */
function findRepoRoot(): string {
  let dir = path.resolve(__dirname, '..') // templates/L4/practices
  for (let i = 0; i < 8; i++) {
    if (fs.existsSync(path.join(dir, 'practices'))) {
      return dir
    }
    dir = path.dirname(dir)
  }
  // Fallback: cwd
  return process.cwd()
}

const REPO_ROOT = findRepoRoot()

// ─── file discovery ──────────────────────────────────────────────────────────

function rulesGlob(catalogDir: string): string[] {
  const rulesDir = path.join(REPO_ROOT, catalogDir, 'rules')
  if (!fs.existsSync(rulesDir)) return []
  return fs
    .readdirSync(rulesDir)
    .filter((f) => f.endsWith('.md'))
    .map((f) => path.join(rulesDir, f))
}

// ─── single rule loader ──────────────────────────────────────────────────────

function loadRuleFile(
  filePath: string,
  catalog: ParsedRule['catalog']
): ParsedRule | null {
  try {
    const raw = fs.readFileSync(filePath, 'utf-8')
    const { frontmatter, body } = parseFrontmatter(raw)
    const basename = path.basename(filePath, '.md')
    const prefix = basename.split('-')[0] ?? 'misc'

    return {
      id: basename,
      catalog,
      prefix,
      frontmatter,
      body,
      rawPath: filePath,
    }
  } catch {
    // File IO error — skip silently in production
    return null
  }
}

// ─── public API (React cache-wrapped) ───────────────────────────────────────

/**
 * loadAllRules — load all Java + React rules.
 *
 * Wrapped in React cache() so RSC pages within the same render tree
 * deduplicate the filesystem reads automatically.
 *
 * Returns rules sorted alphabetically by id within each catalog.
 */
export const loadAllRules = cache((): ParsedRule[] => {
  const javaPaths = rulesGlob('practices')
  const reactPaths = rulesGlob('practices-react')

  const javaRules = javaPaths
    .map((p) => loadRuleFile(p, 'java'))
    .filter((r): r is ParsedRule => r !== null)
    .sort((a, b) => a.id.localeCompare(b.id))

  const reactRules = reactPaths
    .map((p) => loadRuleFile(p, 'react'))
    .filter((r): r is ParsedRule => r !== null)
    .sort((a, b) => a.id.localeCompare(b.id))

  return [...javaRules, ...reactRules]
})

/**
 * loadRulesByPrefix — filter rules by prefix (e.g. "async", "cache").
 *
 * Matches rules whose id starts with `${prefix}-`.
 * Searches both Java and React catalogs.
 */
export const loadRulesByPrefix = cache(
  (prefix: string): ParsedRule[] =>
    loadAllRules().filter((r) => r.prefix === prefix)
)

/**
 * loadRuleById — find a single rule by its id (filename without .md).
 *
 * Returns null if not found (caller should render 404).
 * Searches Java catalog first, then React.
 */
export const loadRuleById = cache(
  (id: string): ParsedRule | null =>
    loadAllRules().find((r) => r.id === id) ?? null
)

/**
 * loadAllPrefixes — unique sorted list of rule prefixes across both catalogs.
 *
 * Used by the category index to render navigation links.
 */
export const loadAllPrefixes = cache((): string[] => {
  const prefixes = new Set(loadAllRules().map((r) => r.prefix))
  return Array.from(prefixes).sort()
})
