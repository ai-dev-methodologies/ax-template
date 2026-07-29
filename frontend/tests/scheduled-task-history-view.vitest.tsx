/// <reference types="@testing-library/jest-dom/vitest" />
import { describe, it, expect, vi } from 'vitest'
import { render, screen, fireEvent } from '@testing-library/react'
import ScheduledTaskHistoryView, {
  type TaskResponse,
  type HistoryRow,
} from '../../templates/L4/scheduled-task/app/(admin)/scheduled-tasks/[id]/scheduled-task-history-view'

// BACKLOG P2-42 — FE render leg of the L4-page-render-testability pass-1 closure (same class as
// frontend/tests/item-detail-view.vitest.tsx). Renders ScheduledTaskHistoryView DIRECTLY — the
// pure props->JSX component extracted from (admin)/scheduled-tasks/[id]/page.tsx for exactly this
// reason.

const BASE_TASK: TaskResponse = {
  id: 'task_1',
  name: 'nightly-report',
  cronExpression: '0 0 * * *',
  status: 'ENABLED',
  handlerBean: 'nightlyReportHandler',
  lastRunAt: '2026-07-20T00:00:00Z',
  createdAt: '2026-01-01T00:00:00Z',
  updatedAt: '2026-01-01T00:00:00Z',
}

const BASE_HISTORY: HistoryRow[] = [
  {
    id: 'hist_1',
    taskName: 'nightly-report',
    startedAt: '2026-07-20T00:00:00Z',
    finishedAt: '2026-07-20T00:00:05Z',
    outcome: 'SUCCESS',
    errorMessage: null,
    hostInstance: 'worker-1',
  },
]

function baseProps(overrides: Partial<Parameters<typeof ScheduledTaskHistoryView>[0]> = {}) {
  return {
    task: BASE_TASK,
    taskLoading: false,
    taskError: null,
    history: BASE_HISTORY,
    historyLoading: false,
    historyError: null,
    historyDataUpdatedAt: 0,
    onBack: vi.fn(),
    onRefetchHistory: vi.fn(),
    ...overrides,
  }
}

describe('ScheduledTaskHistoryView — pure render of a task execution history (P2-42)', () => {
  it('renders the resolved task and history rows', () => {
    render(<ScheduledTaskHistoryView {...baseProps()} />)
    expect(screen.getByRole('heading', { name: 'nightly-report' })).toBeInTheDocument()
    expect(screen.getByText('worker-1', { exact: false })).toBeInTheDocument()
  })

  it('sanitizes a stored error message before rendering it (PII-redaction via sanitizeStoredError)', () => {
    const withError: HistoryRow[] = [
      { ...BASE_HISTORY[0], outcome: 'FAILURE', errorMessage: 'failed for user someone@example.com' },
    ]
    render(<ScheduledTaskHistoryView {...baseProps({ history: withError })} />)
    expect(screen.queryByText(/someone@example\.com/)).not.toBeInTheDocument()
    expect(screen.getByText(/\[REDACTED\]/)).toBeInTheDocument()
  })

  it('renders the empty state when there is no history yet (null-safety branch)', () => {
    render(<ScheduledTaskHistoryView {...baseProps({ history: [] })} />)
    expect(screen.getByText('No execution history yet')).toBeInTheDocument()
  })

  it('renders "Not found" when task is undefined and not loading/erroring (null-safety branch)', () => {
    render(<ScheduledTaskHistoryView {...baseProps({ task: undefined })} />)
    expect(screen.getByText('Not found')).toBeInTheDocument()
  })

  it('clicking Refresh calls onRefetchHistory', () => {
    const onRefetchHistory = vi.fn()
    render(<ScheduledTaskHistoryView {...baseProps({ onRefetchHistory })} />)
    fireEvent.click(screen.getByRole('button', { name: 'Refresh' }))
    expect(onRefetchHistory).toHaveBeenCalled()
  })

  it('NON-VACUITY: a different task name DOES change the rendered DOM — proves the assertions above are capable of going RED, not vacuously passing', () => {
    render(<ScheduledTaskHistoryView {...baseProps({ task: { ...BASE_TASK, name: 'a-totally-different-task' } })} />)
    expect(screen.getByRole('heading', { name: 'a-totally-different-task' })).toBeInTheDocument()
    expect(screen.queryByText('nightly-report')).not.toBeInTheDocument()
  })
})
