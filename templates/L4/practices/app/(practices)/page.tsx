/*
---
template_id: L4/practices/app/(practices)/page
layer: L4
domain: practices
domain_mode: frontend_only
backend_operation_id: null
static_source_ref:
  - practices/AGENTS.md
  - practices-react/AGENTS.md
evidence:
  - source_type: internal
    rationale: "L4 practices vertical — INDEX page: loads all Java + React rules via load-rules (RSC filesystem IO), groups by catalog, renders RuleCard list."
  - source_type: external
    citation: "Next.js 15 App Router — Server Components with filesystem reads"
    url: "https://nextjs.org/docs/app/building-your-application/rendering/server-components"
provenance_class: internal_design
imports_from: [L1, L2, L3]
imports_forbidden: [L4/auth, L4/crud, L4/payment]
---
*/
import React from 'react'
import Link from 'next/link'
import { loadAllRules, loadAllPrefixes } from '../../lib/load-rules'
import type { ParsedRule } from '../../lib/rule-parser'

// ─── subcomponents ───────────────────────────────────────────────────────────

function ImpactBadge({ impact }: { impact: string }) {
  const colorClass =
    impact === 'HIGH'
      ? 'bg-red-100 text-red-700'
      : impact === 'MEDIUM'
        ? 'bg-yellow-100 text-yellow-700'
        : 'bg-green-100 text-green-700'

  return (
    <span className={`rounded px-2 py-0.5 text-xs font-semibold uppercase tracking-wide ${colorClass}`}>
      {impact}
    </span>
  )
}

function TagChip({ tag }: { tag: string }) {
  return (
    <span className="rounded-full border border-border bg-surface-subtle px-2 py-0.5 text-xs text-muted">
      {tag}
    </span>
  )
}

function RuleCard({ rule }: { rule: ParsedRule }) {
  const { frontmatter, id, catalog, prefix } = rule
  return (
    <Link
      href={`/practices/rule/${id}`}
      className="group block rounded-lg border border-border bg-surface p-4 transition-colors hover:border-primary hover:bg-surface-subtle"
      aria-label={`View rule: ${frontmatter.title}`}
    >
      <div className="flex items-start justify-between gap-2">
        <h3 className="text-sm font-semibold leading-snug text-text group-hover:text-primary">
          {frontmatter.title}
        </h3>
        <ImpactBadge impact={frontmatter.impact} />
      </div>

      <div className="mt-1 flex flex-wrap gap-1">
        <span className="rounded bg-primary/10 px-1.5 py-0.5 text-xs text-primary font-medium">
          {catalog === 'java' ? 'Java' : 'React'}
        </span>
        <Link
          href={`/practices/category/${prefix}`}
          onClick={(e) => e.stopPropagation()}
          className="rounded bg-surface-subtle px-1.5 py-0.5 text-xs text-muted hover:text-primary"
        >
          {prefix}
        </Link>
        {frontmatter.tags?.slice(0, 3).map((tag) => (
          <TagChip key={tag} tag={tag} />
        ))}
      </div>
    </Link>
  )
}

function CatalogSection({
  title,
  rules,
  catalog,
}: {
  title: string
  rules: ParsedRule[]
  catalog: 'java' | 'react'
}) {
  const filtered = rules.filter((r) => r.catalog === catalog)
  return (
    <section aria-labelledby={`${catalog}-heading`}>
      <div className="mb-4 flex items-center gap-3">
        <h2 id={`${catalog}-heading`} className="text-xl font-semibold text-text">
          {title}
        </h2>
        <span className="rounded-full bg-primary/10 px-2.5 py-0.5 text-xs font-medium text-primary">
          {filtered.length} rules
        </span>
      </div>
      {filtered.length === 0 ? (
        <p className="text-sm text-muted">No rules found.</p>
      ) : (
        <ul className="grid gap-3 sm:grid-cols-2 xl:grid-cols-3" role="list">
          {filtered.map((rule) => (
            <li key={`${catalog}-${rule.id}`}>
              <RuleCard rule={rule} />
            </li>
          ))}
        </ul>
      )}
    </section>
  )
}

// ─── page ────────────────────────────────────────────────────────────────────

/**
 * PracticesIndexPage — L4 practices INDEX page.
 *
 * Server Component: reads all rules from practices/rules/ and
 * practices-react/rules/ at request time using load-rules.ts.
 * No client-side data fetching — pure RSC filesystem reads.
 *
 * Sections:
 *   1. Java / Spring rules (practices/rules/)
 *   2. React / Next.js rules (practices-react/rules/)
 *   3. Category navigation (sidebar + prefix links)
 *
 * Fork instructions:
 *   1. Replace catalog paths in load-rules.ts if your fork renames practices/.
 *   2. Add search via client component SearchInput if needed.
 *   3. Adjust grid columns via Tailwind classes or tokens.
 */
export default async function PracticesIndexPage() {
  const allRules = loadAllRules()
  const prefixes = loadAllPrefixes()

  const javaCount = allRules.filter((r) => r.catalog === 'java').length
  const reactCount = allRules.filter((r) => r.catalog === 'react').length

  return (
    <main className="mx-auto max-w-7xl space-y-10 px-4 py-8" aria-label="Practices catalog">
      {/* Hero header */}
      <div>
        <h1 className="text-3xl font-bold tracking-tight text-text">
          Practices Catalog
        </h1>
        <p className="mt-2 text-base text-muted">
          {javaCount + reactCount} rules across Java/Spring and React/Next.js — evidence-anchored best practices.
        </p>

        {/* Category quick-links */}
        <nav
          aria-label="Rule categories"
          className="mt-4 flex flex-wrap gap-2"
        >
          {prefixes.map((prefix) => (
            <Link
              key={prefix}
              href={`/practices/category/${prefix}`}
              className="rounded-full border border-border bg-surface px-3 py-1 text-sm text-muted hover:border-primary hover:text-primary transition-colors"
            >
              {prefix}
            </Link>
          ))}
        </nav>
      </div>

      {/* Java rules */}
      <CatalogSection title="Java / Spring" rules={allRules} catalog="java" />

      {/* React rules */}
      <CatalogSection title="React / Next.js" rules={allRules} catalog="react" />
    </main>
  )
}
