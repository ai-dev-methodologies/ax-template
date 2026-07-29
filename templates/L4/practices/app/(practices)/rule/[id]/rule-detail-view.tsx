/*
---
template_id: L4/practices/app/(practices)/rule/[id]/rule-detail-view
layer: L4
domain: practices
domain_mode: frontend_only
evidence:
  - source_type: internal
    rationale: "Pure presentational extraction from rule/[id]/page.tsx (BACKLOG P2-42
      render-testability pass-1 closure — same class as (crud)/items/[id]/item-detail-view.tsx):
      the page is an async Server Component that reads practices/rules/*.md off the filesystem
      (loadRuleById), which a vitest rendering this view directly does not need to reproduce.
      Splitting the resolved-rule->JSX render surface into its own (non-async, plain props) file
      makes it directly renderable. Only the `ParsedRule` TYPE is imported from
      ../../../../lib/rule-parser (server-only per that file's own header comment) — a type-only
      import is erased at compile time and carries no runtime filesystem dependency."
---
*/
import * as React from 'react'
import type { ParsedRule } from '../../../../lib/rule-parser'

// ─── sub-components ─────────────────────────────────────────────────────────

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
            <a
              href={`/practices/category/${prefix}`}
              className="text-primary underline-offset-4 hover:underline"
            >
              {prefix}
            </a>
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

// ─── component ──────────────────────────────────────────────────────────────

export interface RuleDetailViewProps {
  rule: ParsedRule
}

/**
 * RuleDetailView — pure presentational render of a single practices rule.
 *
 * Deliberately has ZERO filesystem/async dependencies (no loadRuleById) — the caller
 * (`rule/[id]/page.tsx`, an async Server Component) owns the fs read + 404 handling and passes
 * the resolved `rule` in. This keeps the component a plain props -> JSX function, which is what
 * makes it renderable in a unit test. Uses a plain <a> instead of next/link's Link — next/link is
 * unresolvable outside frontend/, same class of gap as cmdk/@tanstack/* (see
 * (crud)/items/[id]/item-detail-view.tsx's own plain-<a> precedent).
 */
export default function RuleDetailView({ rule }: RuleDetailViewProps) {
  const { id, frontmatter, body, catalog, prefix } = rule

  return (
    <main
      className="mx-auto max-w-6xl px-4 py-8"
      aria-label={`Rule: ${frontmatter.title}`}
    >
      {/* Breadcrumb */}
      <nav aria-label="Breadcrumb" className="mb-6 flex items-center gap-2 text-sm text-muted">
        <a href="/practices" className="hover:text-primary">
          Practices
        </a>
        <span aria-hidden="true">/</span>
        <a href={`/practices/category/${prefix}`} className="hover:text-primary">
          {prefix}
        </a>
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
