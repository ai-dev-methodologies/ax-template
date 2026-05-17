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
import Link from 'next/link'
import { notFound } from 'next/navigation'
import { loadRuleById, loadAllRules } from '../../../../lib/load-rules'
import type { ParsedRule } from '../../../../lib/rule-parser'

// ─── params ─────────────────────────────────────────────────────────────────

interface RuleDetailPageProps {
  params: Promise<{ id: string }>
}

// ─── subcomponents ───────────────────────────────────────────────────────────

function ImpactBadge({ impact, description }: { impact: string; description?: string }) {
  const colorClass =
    impact === 'HIGH'
      ? 'bg-red-100 text-red-700 border-red-200'
      : impact === 'MEDIUM'
        ? 'bg-yellow-100 text-yellow-700 border-yellow-200'
        : 'bg-green-100 text-green-700 border-green-200'
  return (
    <div className={`rounded-lg border p-3 ${colorClass}`}>
      <div className="flex items-center gap-2">
        <span className="font-semibold uppercase tracking-wide text-xs">
          Impact: {impact}
        </span>
      </div>
      {description && (
        <p className="mt-1 text-sm">{description}</p>
      )}
    </div>
  )
}

function TagList({ tags }: { tags: string[] }) {
  return (
    <div className="flex flex-wrap gap-1.5" role="list" aria-label="Tags">
      {tags.map((tag) => (
        <span
          key={tag}
          role="listitem"
          className="rounded-full border border-border bg-surface-subtle px-2.5 py-1 text-xs text-muted"
        >
          {tag}
        </span>
      ))}
    </div>
  )
}

/**
 * MarkdownBody — renders rule markdown as preformatted text.
 *
 * For a production fork, replace this with a proper markdown renderer
 * (e.g. react-markdown + rehype-highlight for syntax highlighting).
 *
 * Fork instructions:
 *   Replace <pre> with ReactMarkdown or a custom MDX component.
 */
function MarkdownBody({ body }: { body: string }) {
  return (
    <article
      className="prose prose-sm max-w-none text-text"
      aria-label="Rule content"
    >
      <pre className="whitespace-pre-wrap break-words rounded-lg bg-surface-subtle p-4 text-sm font-mono leading-relaxed text-text">
        {body}
      </pre>
    </article>
  )
}

function MetadataSection({ rule }: { rule: ParsedRule }) {
  const { frontmatter, catalog, prefix } = rule
  return (
    <aside
      className="space-y-4 rounded-lg border border-border bg-surface p-4"
      aria-label="Rule metadata"
    >
      <h2 className="text-sm font-semibold text-muted uppercase tracking-wide">
        Metadata
      </h2>

      <dl className="space-y-3 text-sm">
        <div>
          <dt className="font-medium text-muted">Catalog</dt>
          <dd className="mt-0.5 text-text">{catalog === 'java' ? 'Java / Spring' : 'React / Next.js'}</dd>
        </div>
        <div>
          <dt className="font-medium text-muted">Category</dt>
          <dd className="mt-0.5">
            <Link
              href={`/practices/category/${prefix}`}
              className="text-primary underline-offset-4 hover:underline"
            >
              {prefix}
            </Link>
          </dd>
        </div>
        {frontmatter.spec_ref && (
          <div>
            <dt className="font-medium text-muted">Spec ref</dt>
            <dd className="mt-0.5 font-mono text-xs text-text break-all">
              {frontmatter.spec_ref}
            </dd>
          </div>
        )}
        {frontmatter.applicable_to && frontmatter.applicable_to.length > 0 && (
          <div>
            <dt className="font-medium text-muted">Applicable to</dt>
            <dd className="mt-0.5">
              <TagList tags={frontmatter.applicable_to} />
            </dd>
          </div>
        )}
        {frontmatter.tags && frontmatter.tags.length > 0 && (
          <div>
            <dt className="font-medium text-muted">Tags</dt>
            <dd className="mt-0.5">
              <TagList tags={frontmatter.tags} />
            </dd>
          </div>
        )}
      </dl>
    </aside>
  )
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

  const { frontmatter, body, catalog, prefix } = rule

  return (
    <main
      className="mx-auto max-w-6xl px-4 py-8"
      aria-label={`Rule: ${frontmatter.title}`}
    >
      {/* Breadcrumb */}
      <nav aria-label="Breadcrumb" className="mb-6 flex items-center gap-2 text-sm text-muted">
        <Link href="/practices" className="hover:text-primary">
          Practices
        </Link>
        <span aria-hidden="true">/</span>
        <Link href={`/practices/category/${prefix}`} className="hover:text-primary">
          {prefix}
        </Link>
        <span aria-hidden="true">/</span>
        <span className="font-mono text-text">{id}</span>
      </nav>

      {/* Header */}
      <div className="mb-6">
        <div className="flex items-center gap-2 mb-1">
          <span className="rounded bg-primary/10 px-2 py-0.5 text-xs font-medium text-primary">
            {catalog === 'java' ? 'Java' : 'React'}
          </span>
          <code className="text-xs text-muted font-mono">{id}</code>
        </div>
        <h1 className="text-2xl font-bold tracking-tight text-text">
          {frontmatter.title}
        </h1>
      </div>

      {/* Impact banner */}
      <div className="mb-6">
        <ImpactBadge
          impact={frontmatter.impact}
          description={frontmatter.impactDescription}
        />
      </div>

      {/* Two-column layout: body + metadata */}
      <div className="grid gap-8 lg:grid-cols-[1fr_280px]">
        <MarkdownBody body={body} />
        <MetadataSection rule={rule} />
      </div>
    </main>
  )
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
