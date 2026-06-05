'use client';

import { useState } from 'react';
import { ClipboardCheck, RefreshCw } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { useApprovalInbox } from '@/features/approvals/hooks';
import { PageHeader, EmptyState, ErrorPanel, LoadingPanel, TableShell } from '../_components/console-ui';
import { StatusBadge } from '../_components/status-badge';
import { formatRelative, shortenId } from '../_components/format';
import { ApprovalDrawer } from './approval-drawer';

export default function ApprovalsPage() {
  const query = useApprovalInbox();
  const [selected, setSelected] = useState<{ requestId: string; stepId: string } | null>(null);

  const items = query.data?.items ?? [];

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
        <TableShell>
          <thead>
            <tr className="border-b border-border text-left text-xs uppercase tracking-wide text-muted-foreground">
              <th scope="col" className="px-4 py-3 font-medium">유형</th>
              <th scope="col" className="px-4 py-3 font-medium">제목</th>
              <th scope="col" className="px-4 py-3 font-medium">요청자</th>
              <th scope="col" className="px-4 py-3 font-medium">요청 시각</th>
              <th scope="col" className="px-4 py-3 font-medium">상태</th>
              <th scope="col" className="px-4 py-3 text-right font-medium">작업</th>
            </tr>
          </thead>
          <tbody>
            {items.map((entry) => (
              <tr key={entry.stepId} className="border-b border-border last:border-0 hover:bg-secondary/40">
                <td className="px-4 py-3 font-medium text-foreground">{entry.type}</td>
                <td className="px-4 py-3 text-muted-foreground">{entry.title ?? '제목 없음'}</td>
                <td className="px-4 py-3 font-mono text-xs text-muted-foreground">
                  {shortenId(entry.requesterUserId)}
                </td>
                <td className="whitespace-nowrap px-4 py-3 text-muted-foreground">
                  {formatRelative(entry.createdAt)}
                </td>
                <td className="px-4 py-3"><StatusBadge status={entry.status} /></td>
                <td className="px-4 py-3 text-right">
                  <Button
                    variant="secondary"
                    size="sm"
                    onClick={() => setSelected({ requestId: entry.requestId, stepId: entry.stepId })}
                  >
                    검토
                  </Button>
                </td>
              </tr>
            ))}
          </tbody>
        </TableShell>
      )}

      <ApprovalDrawer
        requestId={selected?.requestId ?? null}
        stepId={selected?.stepId ?? null}
        onClose={() => setSelected(null)}
      />
    </div>
  );
}
