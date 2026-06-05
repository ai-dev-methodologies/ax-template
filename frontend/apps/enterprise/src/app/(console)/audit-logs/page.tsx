'use client';

import React, { useState } from 'react';
import { ChevronLeft, ChevronRight, ScrollText, Search } from 'lucide-react';
import { Button, Field } from '@ax/ui';
import { DataGrid, type DataGridColumn } from '@ax/blocks';
import { PageHeader, EmptyState, ErrorPanel, LoadingPanel, SelectField } from '@/components/console-ui';
import { StatusPill } from '@/components/status-pill';
import { formatTimestamp, shortenId } from '@/lib/format';
import { useAuditLogs } from '@/features/auditLog/hooks';
import type { AuditOutcome } from '@/lib/api/auditLogClient';

const PAGE_SIZE = 20;
const OUTCOMES: { value: '' | AuditOutcome; label: string }[] = [
  { value: '', label: '전체' },
  { value: 'SUCCESS', label: '성공' },
  { value: 'FAILURE', label: '실패' },
];

type ColumnKey = 'timestamp' | 'actor' | 'action' | 'resource' | 'outcome' | 'ip';

const COLUMNS: ReadonlyArray<DataGridColumn<ColumnKey>> = [
  { key: 'timestamp', header: '시각' },
  { key: 'actor', header: '행위자' },
  { key: 'action', header: '동작' },
  { key: 'resource', header: '리소스' },
  { key: 'outcome', header: '결과' },
  { key: 'ip', header: 'IP' },
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

  const rows = (data?.content ?? []).map((row) => ({
    timestamp: (
      <span className="whitespace-nowrap tabular-nums text-muted-foreground">
        {formatTimestamp(row.timestamp)}
      </span>
    ),
    actor: <span className="font-mono text-xs">{shortenId(row.actorId)}</span>,
    action: <span className="font-medium text-foreground">{row.action}</span>,
    resource: (
      <span className="text-muted-foreground">
        {row.resourceType ?? '—'}
        {row.resourceId && row.resourceId !== 'unknown' ? (
          <span className="ml-1 font-mono text-xs">· {shortenId(row.resourceId)}</span>
        ) : null}
      </span>
    ),
    outcome: <StatusPill status={row.outcome} />,
    ip: <span className="whitespace-nowrap font-mono text-xs text-muted-foreground">{row.actorIp ?? '—'}</span>,
  }));

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
        className="grid gap-3 rounded-[var(--radius)] border border-border bg-card p-4 sm:grid-cols-2 lg:grid-cols-4 lg:items-end"
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
        <SelectField
          id="filter-outcome"
          label="결과"
          value={outcome}
          onChange={(e) => {
            setOutcome(e.target.value as '' | AuditOutcome);
            setPage(0);
          }}
        >
          {OUTCOMES.map((o) => (
            <option key={o.value} value={o.value}>
              {o.label}
            </option>
          ))}
        </SelectField>
        <Button type="submit" variant="secondary">
          <Search aria-hidden />
          검색
        </Button>
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
          <DataGrid<ColumnKey> caption="감사 로그 기록" columns={COLUMNS} rows={rows} />

          <div className="flex items-center justify-between text-sm text-muted-foreground">
            <span aria-live="polite">
              전체 <span className="tabular-nums text-foreground">{data.totalElements}</span>건 · {page + 1} /{' '}
              {Math.max(totalPages, 1)} 페이지
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
