'use client';

import { useState } from 'react';
import { MonitorSmartphone, RefreshCw } from 'lucide-react';
import { Alert, Button, ConfirmDialog } from '@ax/ui';
import { DataGrid, type DataGridColumn } from '@ax/blocks';
import { PageHeader, EmptyState, ErrorPanel, LoadingPanel } from '@/components/console-ui';
import { StatusPill } from '@/components/status-pill';
import { formatRelative, formatTimestamp } from '@/lib/format';
import { useForceRevokeSession, useSessions } from '@/features/sessions/hooks';
import type { SessionRecord } from '@/lib/api/sessionClient';

type ColumnKey = 'device' | 'ip' | 'lastSeen' | 'expires' | 'status' | 'action';

const COLUMNS: ReadonlyArray<DataGridColumn<ColumnKey>> = [
  { key: 'device', header: '기기' },
  { key: 'ip', header: 'IP (마스킹)' },
  { key: 'lastSeen', header: '최근 활동' },
  { key: 'expires', header: '만료' },
  { key: 'status', header: '상태' },
  { key: 'action', header: '작업' },
];

export function SessionsScreen() {
  const query = useSessions();
  const revoke = useForceRevokeSession();
  const [target, setTarget] = useState<SessionRecord | null>(null);

  const sessions = query.data?.items ?? [];

  const confirmRevoke = (): void => {
    if (!target) return;
    revoke.mutate(target.id, { onSettled: () => setTarget(null) });
  };

  const rows = sessions.map((session) => ({
    device: (
      <span>
        <span className="font-medium text-foreground">{session.deviceLabel || '알 수 없는 기기'}</span>
        <span className="block text-xs text-muted-foreground">{session.userAgentSummary || '—'}</span>
      </span>
    ),
    ip: <span className="font-mono text-xs text-muted-foreground">{session.ipAddressMasked || '—'}</span>,
    lastSeen: (
      <span className="whitespace-nowrap text-muted-foreground">
        {session.lastSeenAt ? formatRelative(session.lastSeenAt) : formatRelative(session.createdAt)}
      </span>
    ),
    expires: (
      <span className="whitespace-nowrap tabular-nums text-muted-foreground">
        {formatTimestamp(session.expiresAt)}
      </span>
    ),
    status: <StatusPill status={session.expired ? 'REVOKED' : session.status} />,
    action: (
      <Button
        variant="outline"
        size="sm"
        onClick={() => setTarget(session)}
        disabled={session.status === 'REVOKED'}
      >
        강제 로그아웃
      </Button>
    ),
  }));

  return (
    <div className="space-y-6">
      <PageHeader
        eyebrow="Sessions"
        title="세션"
        description="활성 세션을 확인하고 필요 시 강제 로그아웃합니다. IP·기기는 마스킹됩니다."
        actions={
          <Button variant="outline" size="sm" onClick={() => query.refetch()} loading={query.isRefetching}>
            <RefreshCw aria-hidden />
            <span className="hidden sm:inline">새로고침</span>
          </Button>
        }
      />

      {revoke.isError ? <Alert variant="error">강제 로그아웃에 실패했습니다. {revoke.error.message}</Alert> : null}

      {query.isLoading ? (
        <LoadingPanel label="세션을 불러오는 중" />
      ) : query.isError ? (
        <ErrorPanel message={query.error.message} onRetry={() => query.refetch()} />
      ) : sessions.length === 0 ? (
        <EmptyState
          icon={<MonitorSmartphone aria-hidden className="h-5 w-5" />}
          title="활성 세션이 없습니다"
          description="로그인한 기기 세션이 여기에 표시됩니다."
        />
      ) : (
        <DataGrid<ColumnKey> caption="활성 세션 목록" columns={COLUMNS} rows={rows} />
      )}

      <ConfirmDialog
        open={Boolean(target)}
        onOpenChange={(open) => !open && setTarget(null)}
        title="세션을 강제 로그아웃할까요?"
        description={
          <>
            <span className="font-medium text-foreground">{target?.deviceLabel || '이 기기'}</span> 세션이 즉시
            해제됩니다. 해당 기기는 다시 로그인해야 합니다.
          </>
        }
        confirmLabel="강제 로그아웃"
        tone="destructive"
        loading={revoke.isPending}
        onConfirm={confirmRevoke}
      />
    </div>
  );
}
