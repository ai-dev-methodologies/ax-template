'use client';

import { useState } from 'react';
import * as AlertDialog from '@radix-ui/react-alert-dialog';
import { MonitorSmartphone, RefreshCw } from 'lucide-react';
import { Alert } from '@/components/ui/alert';
import { Button } from '@/components/ui/button';
import { useForceRevokeSession, useSessions } from '@/features/sessions/hooks';
import type { SessionRecord } from '@/lib/api/sessionClient';
import { PageHeader, EmptyState, ErrorPanel, LoadingPanel, TableShell } from '../_components/console-ui';
import { StatusBadge } from '../_components/status-badge';
import { formatRelative, formatTimestamp } from '../_components/format';

export default function SessionsPage() {
  const query = useSessions();
  const revoke = useForceRevokeSession();
  const [target, setTarget] = useState<SessionRecord | null>(null);

  const sessions = query.data?.items ?? [];

  const confirmRevoke = (): void => {
    if (!target) return;
    revoke.mutate(target.id, { onSettled: () => setTarget(null) });
  };

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
        <TableShell>
          <thead>
            <tr className="border-b border-border text-left text-xs uppercase tracking-wide text-muted-foreground">
              <th scope="col" className="px-4 py-3 font-medium">기기</th>
              <th scope="col" className="px-4 py-3 font-medium">IP (마스킹)</th>
              <th scope="col" className="px-4 py-3 font-medium">최근 활동</th>
              <th scope="col" className="px-4 py-3 font-medium">만료</th>
              <th scope="col" className="px-4 py-3 font-medium">상태</th>
              <th scope="col" className="px-4 py-3 text-right font-medium">작업</th>
            </tr>
          </thead>
          <tbody>
            {sessions.map((session) => (
              <tr key={session.id} className="border-b border-border last:border-0 hover:bg-secondary/40">
                <td className="px-4 py-3">
                  <span className="font-medium text-foreground">{session.deviceLabel || '알 수 없는 기기'}</span>
                  <span className="block text-xs text-muted-foreground">{session.userAgentSummary || '—'}</span>
                </td>
                <td className="px-4 py-3 font-mono text-xs text-muted-foreground">{session.ipAddressMasked || '—'}</td>
                <td className="whitespace-nowrap px-4 py-3 text-muted-foreground">
                  {session.lastSeenAt ? formatRelative(session.lastSeenAt) : formatRelative(session.createdAt)}
                </td>
                <td className="whitespace-nowrap px-4 py-3 tabular-nums text-muted-foreground">
                  {formatTimestamp(session.expiresAt)}
                </td>
                <td className="px-4 py-3">
                  <StatusBadge status={session.expired ? 'REVOKED' : session.status} />
                </td>
                <td className="px-4 py-3 text-right">
                  <Button
                    variant="outline"
                    size="sm"
                    onClick={() => setTarget(session)}
                    disabled={session.status === 'REVOKED'}
                  >
                    강제 로그아웃
                  </Button>
                </td>
              </tr>
            ))}
          </tbody>
        </TableShell>
      )}

      <AlertDialog.Root open={Boolean(target)} onOpenChange={(open) => !open && setTarget(null)}>
        <AlertDialog.Portal>
          <AlertDialog.Overlay className="fixed inset-0 z-50 bg-foreground/30 backdrop-blur-[1px] data-[state=open]:animate-in data-[state=open]:fade-in motion-reduce:animate-none" />
          <AlertDialog.Content className="fixed left-1/2 top-1/2 z-50 w-full max-w-md -translate-x-1/2 -translate-y-1/2 rounded-[calc(var(--radius)+0.35rem)] border border-border bg-card p-6 shadow-lg focus:outline-none data-[state=open]:animate-in data-[state=open]:fade-in data-[state=open]:zoom-in-95 motion-reduce:animate-none">
            <AlertDialog.Title className="text-base font-semibold text-foreground">세션을 강제 로그아웃할까요?</AlertDialog.Title>
            <AlertDialog.Description className="mt-2 text-sm text-muted-foreground">
              <span className="font-medium text-foreground">{target?.deviceLabel || '이 기기'}</span> 세션이 즉시 해제됩니다. 해당 기기는 다시 로그인해야 합니다.
            </AlertDialog.Description>
            <div className="mt-6 flex justify-end gap-2">
              <AlertDialog.Cancel asChild>
                <Button variant="outline" size="sm">취소</Button>
              </AlertDialog.Cancel>
              <Button variant="destructive" size="sm" onClick={confirmRevoke} loading={revoke.isPending}>
                강제 로그아웃
              </Button>
            </div>
          </AlertDialog.Content>
        </AlertDialog.Portal>
      </AlertDialog.Root>
    </div>
  );
}
