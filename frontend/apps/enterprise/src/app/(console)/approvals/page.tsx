'use client';

import { useState } from 'react';
import { ClipboardCheck, RefreshCw } from 'lucide-react';
import { Button } from '@ax/ui';
import { DataGrid, type DataGridColumn } from '@ax/blocks';
import { PageHeader, EmptyState, ErrorPanel, LoadingPanel } from '@/components/console-ui';
import { StatusPill } from '@/components/status-pill';
import { formatRelative, shortenId } from '@/lib/format';
import { useApprovalInbox } from '@/features/approvals/hooks';
import { ApprovalDrawer } from './approval-drawer';

type ColumnKey = 'type' | 'title' | 'requester' | 'createdAt' | 'status' | 'action';

const COLUMNS: ReadonlyArray<DataGridColumn<ColumnKey>> = [
  { key: 'type', header: '유형' },
  { key: 'title', header: '제목' },
  { key: 'requester', header: '요청자' },
  { key: 'createdAt', header: '요청 시각' },
  { key: 'status', header: '상태' },
  { key: 'action', header: '작업' },
];

export default function ApprovalsPage() {
  const query = useApprovalInbox();
  const [selected, setSelected] = useState<{ requestId: string; stepId: string } | null>(null);

  const items = query.data?.items ?? [];

  const rows = items.map((entry) => ({
    type: <span className="font-medium text-foreground">{entry.type}</span>,
    title: <span className="text-muted-foreground">{entry.title ?? '제목 없음'}</span>,
    requester: <span className="font-mono text-xs text-muted-foreground">{shortenId(entry.requesterUserId)}</span>,
    createdAt: <span className="whitespace-nowrap text-muted-foreground">{formatRelative(entry.createdAt)}</span>,
    status: <StatusPill status={entry.status} />,
    action: (
      <Button
        variant="secondary"
        size="sm"
        onClick={() => setSelected({ requestId: entry.requestId, stepId: entry.stepId })}
      >
        검토
      </Button>
    ),
  }));

  return (
    <div className="space-y-6">
      <PageHeader
        eyebrow="Approvals"
        title="결재함"
        description="내가 처리할 결재 요청을 검토하고 승인 또는 반려합니다."
        actions={
          <Button variant="outline" size="sm" onClick={() => query.refetch()} loading={query.isRefetching}>
            <RefreshCw aria-hidden />
            <span className="hidden sm:inline">새로고침</span>
          </Button>
        }
      />

      {query.isLoading ? (
        <LoadingPanel label="결재함을 불러오는 중" />
      ) : query.isError ? (
        <ErrorPanel message={query.error.message} onRetry={() => query.refetch()} />
      ) : items.length === 0 ? (
        <EmptyState
          icon={<ClipboardCheck aria-hidden className="h-5 w-5" />}
          title="처리할 결재가 없습니다"
          description="대기 중인 결재 요청이 들어오면 여기에 표시됩니다."
        />
      ) : (
        <DataGrid<ColumnKey> caption="결재 대기 목록" columns={COLUMNS} rows={rows} />
      )}

      <ApprovalDrawer
        requestId={selected?.requestId ?? null}
        stepId={selected?.stepId ?? null}
        onClose={() => setSelected(null)}
      />
    </div>
  );
}
