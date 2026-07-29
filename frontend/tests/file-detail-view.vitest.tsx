/// <reference types="@testing-library/jest-dom/vitest" />
import { describe, it, expect, vi } from 'vitest'
import { render, screen, fireEvent, within } from '@testing-library/react'
import FileDetailView, {
  type StoredFile,
} from '../../templates/L4/file-storage/app/(file-storage)/files/[id]/file-detail-view'

// BACKLOG P2-42 — FE render leg of the L4-page-render-testability pass-1 closure (same class as
// frontend/tests/item-detail-view.vitest.tsx). Renders FileDetailView DIRECTLY — the pure
// props->JSX component extracted from (file-storage)/files/[id]/page.tsx for exactly this reason.

const BASE_FILE: StoredFile = {
  id: 'file_1',
  name: 'report.pdf',
  contentType: 'application/pdf',
  sizeBytes: 2048,
  status: 'READY',
  description: 'Quarterly report',
  uploadedAt: '2026-07-01T00:00:00Z',
  expiresAt: null,
  downloadUrl: 'https://cdn.example.com/report.pdf',
}

function baseProps(overrides: Partial<Parameters<typeof FileDetailView>[0]> = {}) {
  return {
    file: BASE_FILE,
    isLoading: false,
    isError: false,
    dataUpdatedAt: 0,
    onBack: vi.fn(),
    onDelete: vi.fn(),
    deletePending: false,
    ...overrides,
  }
}

describe('FileDetailView — pure render of a stored file detail page (P2-42)', () => {
  it('renders the resolved file prop: name, metadata, and a Download link when READY', () => {
    render(<FileDetailView {...baseProps()} />)
    expect(screen.getByRole('heading', { name: 'report.pdf' })).toBeInTheDocument()
    expect(screen.getByText('2.0 KB')).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Download' })).toHaveAttribute(
      'href',
      'https://cdn.example.com/report.pdf',
    )
  })

  it('shows "Scan in progress" instead of Download while PENDING (null-safety branch)', () => {
    render(<FileDetailView {...baseProps({ file: { ...BASE_FILE, status: 'PENDING', downloadUrl: null } })} />)
    expect(screen.getByText('Scan in progress...')).toBeInTheDocument()
    expect(screen.queryByRole('link', { name: 'Download' })).not.toBeInTheDocument()
  })

  it('shows the quarantine warning when QUARANTINED (null-safety branch)', () => {
    render(<FileDetailView {...baseProps({ file: { ...BASE_FILE, status: 'QUARANTINED', downloadUrl: null } })} />)
    expect(screen.getByText('This file has been quarantined')).toBeInTheDocument()
  })

  it('renders the not-found state on isError (null-safety branch)', () => {
    render(<FileDetailView {...baseProps({ isError: true, file: undefined })} />)
    expect(screen.getByText('File not found.')).toBeInTheDocument()
  })

  it('clicking Delete opens the confirm dialog, and confirming calls onDelete', () => {
    const onDelete = vi.fn()
    render(<FileDetailView {...baseProps({ onDelete })} />)

    expect(screen.queryByRole('alertdialog')).not.toBeInTheDocument()
    fireEvent.click(screen.getByRole('button', { name: 'Delete' }))

    const dialog = screen.getByRole('alertdialog')
    expect(dialog).toBeInTheDocument()
    // ConfirmDialog's own confirm button also reads "Delete" (confirmLabel) — scope to the dialog.
    fireEvent.click(within(dialog).getByRole('button', { name: 'Delete' }))
    expect(onDelete).toHaveBeenCalled()
  })

  it('NON-VACUITY: a different file name DOES change the rendered DOM — proves the assertions above are capable of going RED, not vacuously passing', () => {
    render(<FileDetailView {...baseProps({ file: { ...BASE_FILE, name: 'a-totally-different-file.pdf' } })} />)
    expect(screen.getByRole('heading', { name: 'a-totally-different-file.pdf' })).toBeInTheDocument()
    expect(screen.queryByText('report.pdf')).not.toBeInTheDocument()
  })
})
