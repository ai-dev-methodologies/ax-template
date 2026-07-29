/*
---
template_id: L4/practices/app/(practices)/rule/[id]/page
layer: L4
domain: practices
domain_mode: frontend_only
backend_operation_id: null
static_source_ref:
  - practices/rules/async-virtual-thread-executor.md
  - practices-react/rules/async-parallel.md
evidence:
  - source_type: internal
    rationale: "L4 practices vertical — DETAIL page: loads a single rule by id from practices/rules/ or practices-react/rules/, renders its markdown body + metadata."
  - source_type: external
    citation: "Next.js 15 App Router — Dynamic Routes with generateStaticParams"
    url: "https://nextjs.org/docs/app/building-your-application/routing/dynamic-routes"
provenance_class: internal_design
imports_from: [L1, L2, L3]
imports_forbidden: [L4/auth, L4/crud, L4/payment]
---
*/
import React from 'react'
import { notFound } from 'next/navigation'
import { loadRuleById, loadAllRules } from '../../../../lib/load-rules'
import RuleDetailView from './rule-detail-view'

// ─── params ─────────────────────────────────────────────────────────────────

interface RuleDetailPageProps {
  params: Promise<{ id: string }>
}

// ─── page ────────────────────────────────────────────────────────────────────

/**
 * RuleDetailPage — L4 practices DETAIL page.
 *
 * Server Component: loads a single rule by id from the practices filesystem.
 * Returns 404 for unknown rule ids.
 *
 * Layout:
 *   - Left: rule markdown body
 *   - Right: metadata sidebar (impact, catalog, tags, spec_ref)
 *
 * Fork instructions:
 *   1. Replace MarkdownBody with a proper renderer (react-markdown, MDX).
 *   2. Add syntax highlighting with rehype-highlight or shiki.
 *   3. Add "Edit this rule" link pointing to the source markdown file.
 */
export default async function RuleDetailPage({ params }: RuleDetailPageProps) {
  const { id } = await params
  const rule = loadRuleById(id)

  if (!rule) {
    notFound()
  }

  return <RuleDetailView rule={rule} />
}

/**
 * generateStaticParams — pre-render all known rule detail pages at build time.
 *
 * Fork instructions:
 *   Remove this export to switch to dynamic rendering (on-demand).
 */
export async function generateStaticParams() {
  const allRules = loadAllRules()
  return allRules.map((rule) => ({ id: rule.id }))
}
