/*
---
template_id: L4/practices/app/(practices)/category/[prefix]/page
layer: L4
domain: practices
domain_mode: frontend_only
backend_operation_id: null
static_source_ref:
  - practices/rules/async-virtual-thread-executor.md
  - practices-react/rules/async-parallel.md
evidence:
  - source_type: internal
    rationale: "L4 practices vertical — CATEGORY page: filters rules by prefix (e.g. async, cache, security). Reads from both Java and React catalogs via load-rules RSC."
  - source_type: external
    citation: "Next.js 15 App Router — Dynamic Routes with generateStaticParams"
    url: "https://nextjs.org/docs/app/building-your-application/routing/dynamic-routes"
provenance_class: internal_design
imports_from: [L1, L2, L3]
imports_forbidden: [L4/auth, L4/crud, L4/payment]
---
*/
import React from 'react'
import Link from 'next/link'
import { notFound } from 'next/navigation'
import { loadRulesByPrefix, loadAllPrefixes } from '../../../../lib/load-rules'
import type { ParsedRule } from '../../../../lib/rule-parser'

// ─── params ─────────────────────────────────────────────────────────────────

interface CategoryPageProps {
  params: Promise<{ prefix: string }>
}

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

function RuleRow({ rule }: { rule: ParsedRule }) {
  const { frontmatter, id, catalog } = rule
  return (
    <Link
      href={`/practices/rule/${id}`}
      className="group flex items-start justify-between gap-4 rounded-lg border border-border bg-surface p-4 transition-colors hover:border-primary hover:bg-surface-subtle"
      aria-label={`View rule: ${frontmatter.title}`}
    >
      <div className="min-w-0 flex-1">
        <div className="flex items-center gap-2">
          <span className="rounded bg-primary/10 px-1.5 py-0.5 text-xs font-medium text-primary">
            {catalog === 'java' ? 'Java' : 'React'}
          </span>
          <code className="text-xs text-muted font-mono">{id}</code>
        </div>
        <h3 className="mt-1 text-sm font-semibold leading-snug text-text group-hover:text-primary">
          {frontmatter.title}
        </h3>
        {frontmatter.impactDescription && (
          <p className="mt-1 line-clamp-2 text-xs text-muted">
            {frontmatter.impactDescription}
          </p>
        )}
      </div>
      <ImpactBadge impact={frontmatter.impact} />
    </Link>
  )
}

// ─── page ────────────────────────────────────────────────────────────────────

/**
 * CategoryPage — L4 practices CATEGORY page.
 *
 * Server Component: filters rules by prefix (first dash-segment of rule id).
 * e.g. /practices/category/async → shows all async-* rules from both catalogs.
 *
 * Returns 404 if no rules match the prefix (unknown prefix).
 *
 * Fork instructions:
 *   1. Customize the empty state message for your domain.
 *   2. Add sorting (by impact level, alphabetical) via URL search params.
 */
export default async function CategoryPage({ params }: CategoryPageProps) {
  const { prefix } = await params
  const rules = loadRulesByPrefix(prefix)

  if (rules.length === 0) {
    notFound()
  }

  const javaRules = rules.filter((r) => r.catalog === 'java')
  const reactRules = rules.filter((r) => r.catalog === 'react')

  return (
    <main
      className="mx-auto max-w-4xl space-y-8 px-4 py-8"
      aria-label={`${prefix} rules`}
    >
      {/* Breadcrumb */}
      <nav aria-label="Breadcrumb" className="flex items-center gap-2 text-sm text-muted">
        <Link href="/practices" className="hover:text-primary">
          Practices
        </Link>
        <span aria-hidden="true">/</span>
        <span className="text-text font-medium">{prefix}</span>
      </nav>

      {/* Header */}
      <div>
        <h1 className="text-2xl font-bold tracking-tight text-text">
          <code className="font-mono">{prefix}-</code> rules
        </h1>
        <p className="mt-1 text-sm text-muted">
          {rules.length} rule{rules.length !== 1 ? 's' : ''} across Java and React catalogs.
        </p>
      </div>

      {/* Java section */}
      {javaRules.length > 0 && (
        <section aria-labelledby="java-heading">
          <h2 id="java-heading" className="mb-3 text-base font-semibold text-text">
            Java / Spring
            <span className="ml-2 text-sm font-normal text-muted">({javaRules.length})</span>
          </h2>
          <ul className="space-y-3" role="list">
            {javaRules.map((rule) => (
              <li key={rule.id}>
                <RuleRow rule={rule} />
              </li>
            ))}
          </ul>
        </section>
      )}

      {/* React section */}
      {reactRules.length > 0 && (
        <section aria-labelledby="react-heading">
          <h2 id="react-heading" className="mb-3 text-base font-semibold text-text">
            React / Next.js
            <span className="ml-2 text-sm font-normal text-muted">({reactRules.length})</span>
          </h2>
          <ul className="space-y-3" role="list">
            {reactRules.map((rule) => (
              <li key={rule.id}>
                <RuleRow rule={rule} />
              </li>
            ))}
          </ul>
        </section>
      )}
    </main>
  )
}

/**
 * generateStaticParams — pre-render all known category pages at build time.
 *
 * Fork instructions:
 *   Remove this export to switch to dynamic rendering (on-demand).
 */
export async function generateStaticParams() {
  const prefixes = loadAllPrefixes()
  return prefixes.map((prefix) => ({ prefix }))
}
