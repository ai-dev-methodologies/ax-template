/*
---
template_id: L4/practices/lib/rule-parser
layer: L4
domain: practices
domain_mode: frontend_only
backend_operation_id: null
evidence:
  - source_type: internal
    rationale: "L4 practices vertical — minimal YAML frontmatter parser for rule markdown files. No external dependency needed; rules use a simple --- block."
  - source_type: external
    citation: "YAML 1.2 specification — document markers (---)"
    url: "https://yaml.org/spec/1.2.2/#document-markers"
provenance_class: internal_design
imports_from: [L1, L2, L3]
imports_forbidden: [L4/auth, L4/crud, L4/payment]
---
*/

// SERVER-ONLY — this module is imported only by load-rules.ts (RSC)

export interface RuleFrontmatter {
  title: string
  impact: 'HIGH' | 'MEDIUM' | 'LOW' | string
  impactDescription?: string
  tags?: string[]
  spec_ref?: string
  applicable_to?: string[]
  verification?: unknown
  evidence?: unknown[]
}

export interface ParsedRule {
  id: string            // filename without .md
  catalog: 'java' | 'react'
  prefix: string        // first dash-segment of id (e.g. "async")
  frontmatter: RuleFrontmatter
  body: string          // markdown content after frontmatter
  rawPath: string       // absolute file path
}

/**
 * parseFrontmatter — extract YAML frontmatter from a markdown file.
 *
 * Expects the file to start with `---\n...\n---\n` (YAML front matter).
 * Returns { frontmatter, body } where frontmatter is the parsed YAML object
 * and body is the remaining markdown text after the closing `---`.
 *
 * Uses a minimal inline YAML parser for the specific fields used in
 * practices/rules/*.md — no external yaml library required.
 */
export function parseFrontmatter(raw: string): {
  frontmatter: RuleFrontmatter
  body: string
} {
  const DELIMITER = '---'

  if (!raw.startsWith(DELIMITER)) {
    return { frontmatter: { title: 'Untitled', impact: 'MEDIUM' }, body: raw }
  }

  // Find closing ---
  const afterOpen = raw.slice(DELIMITER.length).replace(/^\n/, '')
  const closingIdx = afterOpen.indexOf('\n---')
  if (closingIdx === -1) {
    return { frontmatter: { title: 'Untitled', impact: 'MEDIUM' }, body: raw }
  }

  const yamlBlock = afterOpen.slice(0, closingIdx)
  const body = afterOpen.slice(closingIdx + '\n---'.length).replace(/^\n/, '')

  const frontmatter = parseYamlSubset(yamlBlock)
  return { frontmatter, body }
}

/**
 * parseYamlSubset — parse the subset of YAML used in rule frontmatter files.
 *
 * Handles:
 *   - top-level scalar fields: title, impact, impactDescription, spec_ref
 *   - simple list fields: tags, applicable_to
 *   - ignores nested objects (evidence, verification, upstream)
 */
function parseYamlSubset(yamlText: string): RuleFrontmatter {
  const result: Record<string, unknown> = {}
  const lines = yamlText.split('\n')

  let i = 0
  while (i < lines.length) {
    const line = lines[i]
    // Skip empty lines and comments
    if (!line.trim() || line.trim().startsWith('#')) {
      i++
      continue
    }

    // Top-level key: value or key:
    const scalarMatch = /^(\w[\w-]*):\s+(.+)/.exec(line)
    if (scalarMatch) {
      const key = scalarMatch[1]
      const value = scalarMatch[2].replace(/^["']|["']$/g, '') // strip quotes
      result[key] = value
      i++
      continue
    }

    // Top-level key: (list follows)
    const listKeyMatch = /^(\w[\w-]*):\s*$/.exec(line)
    if (listKeyMatch) {
      const key = listKeyMatch[1]
      const items: string[] = []
      i++
      while (i < lines.length && /^\s+-\s+/.test(lines[i])) {
        const item = lines[i].replace(/^\s+-\s+/, '').replace(/^["']|["']$/g, '')
        items.push(item)
        i++
      }
      result[key] = items
      continue
    }

    i++
  }

  return {
    title: String(result['title'] ?? 'Untitled'),
    impact: String(result['impact'] ?? 'MEDIUM') as RuleFrontmatter['impact'],
    impactDescription: result['impactDescription'] != null
      ? String(result['impactDescription'])
      : undefined,
    tags: Array.isArray(result['tags'])
      ? (result['tags'] as string[])
      : undefined,
    spec_ref: result['spec_ref'] != null ? String(result['spec_ref']) : undefined,
    applicable_to: Array.isArray(result['applicable_to'])
      ? (result['applicable_to'] as string[])
      : undefined,
  }
}
