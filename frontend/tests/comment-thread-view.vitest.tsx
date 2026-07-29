/// <reference types="@testing-library/jest-dom/vitest" />
import { describe, it, expect, vi } from 'vitest'
import { render, screen, fireEvent, cleanup } from '@testing-library/react'
import CommentThreadView, {
  type CommentListResponse,
} from '../../templates/L4/comment-thread/app/(comments)/[entityType]/[entityId]/comment-thread-view'

// BACKLOG P2-42 — FE render leg of the L4-page-render-testability pass-1 closure (same class as
// frontend/tests/item-detail-view.vitest.tsx). Renders CommentThreadView DIRECTLY — the pure
// props->JSX component (incl. the recursive CommentBranch + buildTree helper) extracted from
// (comments)/[entityType]/[entityId]/page.tsx for exactly this reason.

const BASE_DATA: CommentListResponse = {
  items: [
    {
      id: 'c1',
      authorUserId: 'alice',
      entityType: 'product',
      entityId: 'p_1',
      parentCommentId: null,
      body: 'First comment',
      status: 'ACTIVE',
      createdAt: '2026-07-01T00:00:00Z',
      updatedAt: null,
      deletedAt: null,
      deletedByUserId: null,
    },
    {
      id: 'c2',
      authorUserId: 'bob',
      entityType: 'product',
      entityId: 'p_1',
      parentCommentId: 'c1',
      body: 'A reply',
      status: 'ACTIVE',
      createdAt: '2026-07-01T01:00:00Z',
      updatedAt: null,
      deletedAt: null,
      deletedByUserId: null,
    },
  ],
  totalElements: 2,
}

function baseProps(overrides: Partial<Parameters<typeof CommentThreadView>[0]> = {}) {
  return {
    entityType: 'product',
    entityId: 'p_1',
    data: BASE_DATA,
    isLoading: false,
    error: null,
    callerId: 'alice',
    isAdmin: false,
    newBody: '',
    onNewBodyChange: vi.fn(),
    onSubmitTopLevel: vi.fn(),
    submitPending: false,
    onReply: vi.fn(),
    onEdit: vi.fn(),
    onDelete: vi.fn(),
    ...overrides,
  }
}

describe('CommentThreadView — pure render of a polymorphic comment thread (P2-42)', () => {
  it('builds the reply tree from the flat resolved data prop', () => {
    render(<CommentThreadView {...baseProps()} />)
    expect(screen.getByText('First comment')).toBeInTheDocument()
    expect(screen.getByText('A reply')).toBeInTheDocument()
    expect(screen.getByRole('heading', { name: 'product / p_1' })).toBeInTheDocument()
  })

  it('masks a DELETED comment body and hides its action buttons (null-safety branch)', () => {
    const withDeleted: CommentListResponse = {
      items: [
        { ...BASE_DATA.items[0], status: 'DELETED', body: '[deleted]', deletedAt: '2026-07-02T00:00:00Z', deletedByUserId: 'alice' },
      ],
      totalElements: 1,
    }
    render(<CommentThreadView {...baseProps({ data: withDeleted })} />)
    // "[deleted]" also appears once in the view's own static header copy — assert 2 total
    // (header + masked body), not a single unique match.
    expect(screen.getAllByText('[deleted]')).toHaveLength(2)
    expect(screen.queryByRole('button', { name: 'Reply' })).not.toBeInTheDocument()
  })

  it('shows Edit only for the comment author (admin-cannot-rewrite defense-in-depth, R38)', () => {
    render(<CommentThreadView {...baseProps({ callerId: 'alice' })} />)
    expect(screen.getAllByRole('button', { name: 'Edit' })).toHaveLength(1) // only c1 (alice's own)
    cleanup()

    render(<CommentThreadView {...baseProps({ callerId: 'someone-else' })} />)
    expect(screen.queryByRole('button', { name: 'Edit' })).not.toBeInTheDocument()
  })

  it('submitting the top-level form calls onSubmitTopLevel with newBody', () => {
    const onSubmitTopLevel = vi.fn()
    render(<CommentThreadView {...baseProps({ newBody: 'hello world', onSubmitTopLevel })} />)
    fireEvent.click(screen.getByRole('button', { name: 'Post comment' }))
    expect(onSubmitTopLevel).toHaveBeenCalledWith('hello world')
  })

  it('renders the empty state when there are no comments (null-safety branch)', () => {
    render(<CommentThreadView {...baseProps({ data: { items: [], totalElements: 0 } })} />)
    expect(screen.getByText('No comments yet')).toBeInTheDocument()
  })

  it('NON-VACUITY: a different comment body DOES change the rendered DOM — proves the assertions above are capable of going RED, not vacuously passing', () => {
    const mutated: CommentListResponse = {
      ...BASE_DATA,
      items: [{ ...BASE_DATA.items[0], body: 'A totally different comment' }, BASE_DATA.items[1]],
    }
    render(<CommentThreadView {...baseProps({ data: mutated })} />)
    expect(screen.getByText('A totally different comment')).toBeInTheDocument()
    expect(screen.queryByText('First comment')).not.toBeInTheDocument()
  })
})
