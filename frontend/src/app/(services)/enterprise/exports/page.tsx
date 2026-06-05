'use client';

import React, { useState } from 'react';
import { Download, FileSpreadsheet } from 'lucide-react';
import { Alert } from '@/components/ui/alert';
import { Button } from '@/components/ui/button';
import { Field } from '@/components/ui/field';
import { useCreateExport, useDownloadExport, useExportJobs } from '@/features/exports/hooks';
import type { ExportJob } from '@/lib/api/exportClient';
import { PageHeader, EmptyState, ErrorPanel, LoadingPanel, TableShell } from '../_components/console-ui';
import { StatusBadge } from '../_components/status-badge';
import { formatBytes, formatRelative } from '../_components/format';

const FORMATS: { value: 'csv' | 'xlsx'; label: string }[] = [
  { value: 'csv', label: 'CSV' },
  { value: 'xlsx', label: 'XLSX' },
];

export default function ExportsPage() {
  const query = useExportJobs();
  const create = useCreateExport();
  const download = useDownloadExport();

  const [format, setFormat] = useState<'csv' | 'xlsx'>('csv');
  const [name, setName] = useState('');
  const [downloadingId, setDownloadingId] = useState<string | null>(null);

  const jobs = query.data?.items ?? [];

  const submit = (event: React.FormEvent): void => {
    event.preventDefault();
    create.mutate(
      { format, name: name.trim() || undefined },
      { onSuccess: () => setName('') },
    );
  };

  const onDownload = (job: ExportJob): void => {
    setDownloadingId(job.jobId);
    download.mutate({ jobId: job.jobId }, { onSettled: () => setDownloadingId(null) });
  };

  return (
    <div className="space-y-6">
      <PageHeader
        eyebrow="Report Export"
        title="리포트 추출"
        description="추출 작업을 생성하면 백그라운드 워커가 처리하고 완료 시 내려받을 수 있습니다."
      />

      <form
        onSubmit={submit}
        aria-label="추출 작업 생성"
        className="grid gap-3 rounded-[var(--radius)] border border-border bg-card p-4 sm:grid-cols-[1.5fr_1fr_auto] sm:items-end"
      >
        <Field
          id="export-name"
          label="작업 이름 (선택)"
          placeholder="6월 감사 로그"
          value={name}
          onChange={(e) => setName(e.target.value)}
        />
        <div className="space-y-1.5">
          <label htmlFor="export-format" className="text-sm font-medium leading-none text-foreground">
            형식
          </label>
          <select
            id="export-format"
            value={format}
            onChange={(e) => setFormat(e.target.value as 'csv' | 'xlsx')}
            className="h-11 w-full rounded-[var(--radius)] border border-input bg-background px-3 text-sm text-foreground shadow-sm focus-visible:border-ring focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring/40"
          >
            {FORMATS.map((f) => (
              <option key={f.value} value={f.value}>{f.label}</option>
            ))}
          </select>
        </div>
        <Button type="submit" loading={create.isPending}>
          <FileSpreadsheet aria-hidden />
          추출 시작
        </Button>
      </form>

      {create.isError ? <ErrorPanel message={create.error.message} /> : null}
      {download.isError ? <Alert variant="error">다운로드에 실패했습니다. {download.error.message}</Alert> : null}

      {query.isLoading ? (
        <LoadingPanel label="추출 작업을 불러오는 중" />
      ) : query.isError ? (
        <ErrorPanel message={query.error.message} onRetry={() => query.refetch()} />
      ) : jobs.length === 0 ? (
        <EmptyState
          icon={<Download aria-hidden className="h-5 w-5" />}
          title="추출 작업이 없습니다"
          description="위 양식으로 첫 추출 작업을 시작하세요."
        />
      ) : (
        <TableShell>
          <thead>
            <tr className="border-b border-border text-left text-xs uppercase tracking-wide text-muted-foreground">
              <th scope="col" className="px-4 py-3 font-medium">이름</th>
              <th scope="col" className="px-4 py-3 font-medium">형식</th>
              <th scope="col" className="px-4 py-3 font-medium">행 수</th>
              <th scope="col" className="px-4 py-3 font-medium">크기</th>
              <th scope="col" className="px-4 py-3 font-medium">생성</th>
              <th scope="col" className="px-4 py-3 font-medium">상태</th>
              <th scope="col" className="px-4 py-3 text-right font-medium">작업</th>
            </tr>
          </thead>
          <tbody>
            {jobs.map((job) => (
              <tr key={job.jobId} className="border-b border-border last:border-0 hover:bg-secondary/40">
                <td className="px-4 py-3 font-medium text-foreground">{job.name || '이름 없음'}</td>
                <td className="px-4 py-3 font-mono text-xs text-muted-foreground">{job.format}</td>
                <td className="px-4 py-3 tabular-nums text-muted-foreground">{job.rowCount ?? '—'}</td>
                <td className="px-4 py-3 tabular-nums text-muted-foreground">{formatBytes(job.sizeBytes)}</td>
                <td className="whitespace-nowrap px-4 py-3 text-muted-foreground">{formatRelative(job.createdAt)}</td>
                <td className="px-4 py-3"><StatusBadge status={job.status} /></td>
                <td className="px-4 py-3 text-right">
                  <Button
                    variant="secondary"
                    size="sm"
                    onClick={() => onDownload(job)}
                    disabled={!job.downloadAvailable}
                    loading={download.isPending && downloadingId === job.jobId}
                  >
                    <Download aria-hidden />
                    내려받기
                  </Button>
                </td>
              </tr>
            ))}
          </tbody>
        </TableShell>
      )}
    </div>
  );
}
