'use client';

import React, { useState } from 'react';
import { Download, FileSpreadsheet } from 'lucide-react';
import { Alert, Button, Field } from '@ax/ui';
import { DataGrid, type DataGridColumn } from '@ax/blocks';
import { PageHeader, EmptyState, ErrorPanel, LoadingPanel, SelectField } from '@/components/console-ui';
import { StatusPill } from '@/components/status-pill';
import { formatBytes, formatRelative } from '@/lib/format';
import { useCreateExport, useDownloadExport, useExportJobs } from '@/features/exports/hooks';
import type { ExportJob } from '@/lib/api/exportClient';

const FORMATS: { value: 'csv' | 'xlsx'; label: string }[] = [
  { value: 'csv', label: 'CSV' },
  { value: 'xlsx', label: 'XLSX' },
];

type ColumnKey = 'name' | 'format' | 'rowCount' | 'size' | 'createdAt' | 'status' | 'action';

const COLUMNS: ReadonlyArray<DataGridColumn<ColumnKey>> = [
  { key: 'name', header: '이름' },
  { key: 'format', header: '형식' },
  { key: 'rowCount', header: '행 수', numeric: true },
  { key: 'size', header: '크기', numeric: true },
  { key: 'createdAt', header: '생성' },
  { key: 'status', header: '상태' },
  { key: 'action', header: '작업' },
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
    create.mutate({ format, name: name.trim() || undefined }, { onSuccess: () => setName('') });
  };

  const onDownload = (job: ExportJob): void => {
    setDownloadingId(job.jobId);
    download.mutate({ jobId: job.jobId }, { onSettled: () => setDownloadingId(null) });
  };

  const rows = jobs.map((job) => ({
    name: <span className="font-medium text-foreground">{job.name || '이름 없음'}</span>,
    format: <span className="font-mono text-xs text-muted-foreground">{job.format}</span>,
    rowCount: <span className="text-muted-foreground">{job.rowCount ?? '—'}</span>,
    size: <span className="text-muted-foreground">{formatBytes(job.sizeBytes)}</span>,
    createdAt: <span className="whitespace-nowrap text-muted-foreground">{formatRelative(job.createdAt)}</span>,
    status: <StatusPill status={job.status} />,
    action: (
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
    ),
  }));

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
        <SelectField
          id="export-format"
          label="형식"
          value={format}
          onChange={(e) => setFormat(e.target.value as 'csv' | 'xlsx')}
        >
          {FORMATS.map((f) => (
            <option key={f.value} value={f.value}>
              {f.label}
            </option>
          ))}
        </SelectField>
        <Button type="submit" loading={create.isPending}>
          <FileSpreadsheet aria-hidden />
          추출 시작
        </Button>
      </form>

      {create.isError ? <ErrorPanel message={create.error.message} /> : null}
      {download.isError ? (
        <Alert variant="error">다운로드에 실패했습니다. {download.error.message}</Alert>
      ) : null}

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
        <DataGrid<ColumnKey> caption="추출 작업 목록" columns={COLUMNS} rows={rows} />
      )}
    </div>
  );
}
