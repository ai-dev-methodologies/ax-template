'use client';

import React, { useState } from 'react';
import { ChevronLeft, ChevronRight, ScrollText, Search } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Field } from '@/components/ui/field';
import { useAuditLogs } from '@/features/auditLog/hooks';
import type { AuditOutcome } from '@/lib/api/auditLogClient';
import { PageHeader, EmptyState, ErrorPanel, LoadingPanel, TableShell } from '../_components/console-ui';
import { StatusBadge } from '../_components/status-badge';
import { formatTimestamp, shortenId } from '../_components/format';

const PAGE_SIZE = 20;
const OUTCOMES: { value: '' | AuditOutcome; label: string }[] = [
  { value: '', label: '전체' },
  { value: 'SUCCESS', label: '성공' },
  { value: 'FAILURE', label: '실패' },
];

export default function AuditLogsPage() {
  const [page, setPage] = useState(0);
  const [actorDraft, setActorDraft] = useState('');
  const [actionDraft, setActionDraft] = useState('');
  const [actorId, setActorId] = useState('');
  const [action, setAction] = useState('');
  const [outcome, setOutcome] = useState<'' | AuditOutcome>('');

  const query = useAuditLogs({
    page,
    size: PAGE_SIZE,
    actorId: actorId || undefined,
    action: action || undefined,
    outcome: outcome || undefined,
  });

  const applyFilters = (event: React.FormEvent): void => {
    event.preventDefault();
    setActorId(actorDraft.trim());
    setAction(actionDraft.trim());
    setPage(0);
  };

  const data = query.data;
  const totalPages = data?.totalPages ?? 0;

  return (
    <div className="space-y-6">
      <PageHeader
        eyebrow="Audit Log"
        title="감사 로그"
        description="모든 운영 행위의 행위자 · 동작 · 시각을 추적합니다."
      />

      <form
        onSubmit={applyFilters}
        aria-label="감사 로그 필터"
        className="grid gap-3 rounded-[var(--radius)] border border-border bg-card p-4 sm:grid-cols-2 lg:grid-cols-4"
      >
        <Field
          id="filter-actor"
          label="행위자 ID"
          placeholder="user-id"
          value={actorDraft}
          onChange={(e) => setActorDraft(e.target.value)}
        />
        <Field
          id="filter-action"
          label="동작"
          placeholder="CREATE / UPDATE / DELETE"
          value={actionDraft}
          onChange={(e) => setActionDraft(e.target.value)}
        />
        <div className="space-y-1.5">
          <label htmlFor="filter-outcome" className="text-sm font-medium leading-none text-foreground">
            결과
          </label>
          <select
            id="filter-outcome"
            value={outcome}
            onChange={(e) => {
              setOutcome(e.target.value as '' | AuditOutcome);
              setPage(0);
            }}
            className="h-11 w-full rounded-[var(--radius)] border border-input bg-background px-3 text-sm text-foreground shadow-sm focus-visible:border-ring focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring/40"
          >
            {OUTCOMES.map((o) => (
              <option key={o.value} value={o.value}>
                {o.label}
              </option>
            ))}
          </select>
        </div>
        <div className="flex items-end">
          <Button type="submit" variant="secondary" className="w-full">
            <Search aria-hidden />
            검색
          </Button>
        </div>
      </form>

      {query.isLoading ? (
        <LoadingPanel label="감사 로그를 불러오는 중" />
      ) : query.isError ? (
        <ErrorPanel message={query.error.message} onRetry={() => query.refetch()} />
      ) : !data || data.content.length === 0 ? (
        <EmptyState
          icon={<ScrollText aria-hidden className="h-5 w-5" />}
          title="감사 로그가 없습니다"
          description="조건에 맞는 기록이 없습니다. 필터를 바꾸거나 운영 작업을 수행해 보세요."
        />
      ) : (
        <>
          <TableShell>
            <thead>
              <tr className="border-b border-border text-left text-xs uppercase tracking-wide text-muted-foreground">
                <th scope="col" className="px-4 py-3 font-medium">시각</th>
                <th scope="col" className="px-4 py-3 font-medium">행위자</th>
                <th scope="col" className="px-4 py-3 font-medium">동작</th>
                <th scope="col" className="px-4 py-3 font-medium">리소스</th>
                <th scope="col" className="px-4 py-3 font-medium">결과</th>
                <th scope="col" className="px-4 py-3 font-medium">IP</th>
              </tr>
            </thead>
            <tbody>
              {data.content.map((row) => (
                <tr key={row.id} className="border-b border-border last:border-0 hover:bg-secondary/40">
                  <td className="whitespace-nowrap px-4 py-3 tabular-nums text-muted-foreground">
                    {formatTimestamp(row.timestamp)}
                  </td>
                  <td className="px-4 py-3 font-mono text-xs text-foreground">{shortenId(row.actorId)}</td>
                  <td className="px-4 py-3 font-medium text-foreground">{row.action}</td>
                  <td className="px-4 py-3 text-muted-foreground">
                    {row.resourceType ?? '—'}
                    {row.resourceId && row.resourceId !== 'unknown' ? (
                      <span className="ml-1 font-mono text-xs">· {shortenId(row.resourceId)}</span>
                    ) : null}
                  </td>
                  <td className="px-4 py-3"><StatusBadge status={row.outcome} /></td>
                  <td className="whitespace-nowrap px-4 py-3 font-mono text-xs text-muted-foreground">
                    {row.actorIp ?? '—'}
                  </td>
                </tr>
              ))}
            </tbody>
          </TableShell>

          <div className="flex items-center justify-between text-sm text-muted-foreground">
            <span aria-live="polite">
              전체 <span className="tabular-nums text-foreground">{data.totalElements}</span>건 · {page + 1} / {Math.max(totalPages, 1)} 페이지
            </span>
            <div className="flex items-center gap-2">
              <Button
                variant="outline"
                size="sm"
                onClick={() => setPage((p) => Math.max(0, p - 1))}
                disabled={page === 0}
              >
                <ChevronLeft aria-hidden />
                이전
              </Button>
              <Button
                variant="outline"
                size="sm"
                onClick={() => setPage((p) => p + 1)}
                disabled={page + 1 >= totalPages}
              >
                다음
                <ChevronRight aria-hidden />
              </Button>
            </div>
          </div>
        </>
      )}
    </div>
  );
}
