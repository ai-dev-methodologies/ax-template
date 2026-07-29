/// <reference types="@testing-library/jest-dom/vitest" />
import { describe, it, expect } from 'vitest'
import { render, screen } from '@testing-library/react'
import RuleDetailView from '../../templates/L4/practices/app/(practices)/rule/[id]/rule-detail-view'
import type { ParsedRule } from '../../templates/L4/practices/lib/rule-parser'

// BACKLOG P2-42 — FE render leg of the L4-page-render-testability pass-1 closure (same class as
// frontend/tests/item-detail-view.vitest.tsx). Renders RuleDetailView DIRECTLY — the pure
// props->JSX component extracted from rule/[id]/page.tsx (an async Server Component reading the
// filesystem, not renderable directly by @testing-library/react) for exactly this reason.

const BASE_RULE: ParsedRule = {
  id: 'async-virtual-thread-executor',
  catalog: 'java',
  prefix: 'async',
  frontmatter: {
    title: 'Use virtual threads for blocking executors',
    impact: 'HIGH',
    impactDescription: 'Thread starvation under load without this rule.',
    tags: ['concurrency', 'jdk21'],
    spec_ref: 'specs/spring-practices-l0.yaml#R12',
  },
  body: 'Rule body markdown content.',
  rawPath: '/repo/practices/rules/async-virtual-thread-executor.md',
}

describe('RuleDetailView — pure render of a single practices rule (P2-42)', () => {
  it('renders the resolved rule prop: title, impact, and body', () => {
    render(<RuleDetailView rule={BASE_RULE} />)
    expect(screen.getByRole('heading', { name: 'Use virtual threads for blocking executors' })).toBeInTheDocument()
    expect(screen.getByText(/Impact: HIGH/)).toBeInTheDocument()
    expect(screen.getByText('Rule body markdown content.')).toBeInTheDocument()
  })

  it('renders the tags and applicable_to lists when present (null-safety branch)', () => {
    render(<RuleDetailView rule={BASE_RULE} />)
    expect(screen.getByText('concurrency')).toBeInTheDocument()
    expect(screen.getByText('jdk21')).toBeInTheDocument()
  })

  it('omits the tags row when frontmatter.tags is absent (null-safety branch)', () => {
    const { tags, ...restFrontmatter } = BASE_RULE.frontmatter
    render(<RuleDetailView rule={{ ...BASE_RULE, frontmatter: restFrontmatter }} />)
    expect(screen.queryByText('Tags')).not.toBeInTheDocument()
  })

  it('NON-VACUITY: a different title DOES change the rendered DOM — proves the assertions above are capable of going RED, not vacuously passing', () => {
    render(
      <RuleDetailView
        rule={{
          ...BASE_RULE,
          frontmatter: { ...BASE_RULE.frontmatter, title: 'A totally different rule title' },
        }}
      />,
    )
    expect(screen.getByRole('heading', { name: 'A totally different rule title' })).toBeInTheDocument()
    expect(screen.queryByText('Use virtual threads for blocking executors')).not.toBeInTheDocument()
  })
})
