'use client';

import React, { useState } from 'react';
import { Download, FileSpreadsheet, ScrollText } from 'lucide-react';
import { Alert, Button, Input } from '@ax/ui';
import { DataGrid, type DataGridColumn } from '@ax/blocks';
import { PageHeader } from '@/components/page-header';
import { ExportStatus } from '@/components/status';
import { ScreenEmpty, ScreenError, ScreenLoading } from '@/components/screen-states';
import { useCreateExport, useDownloadExport, useExportJobs } from '@/features/exports/hooks';
import { formatBytes, formatDateTime } from '@/lib/format';

type Col = 'name' | 'format' | 'status' | 'rows' | 'size' | 'created' | 'action';

const COLUMNS: ReadonlyArray<DataGridColumn<Col>> = [
  { key: 'name', header: '명세서' },
  { key: 'format', header: '형식' },
  { key: 'status', header: '상태' },
  { key: 'rows', header: '행 수', numeric: true },
  { key: 'size', header: '크기', numeric: true },
  { key: 'created', header: '생성시각' },
  { key: 'action', header: '' },
];

/**
 * Statements / exports. Creates a settlement export (CSV / XLSX) via the
 * report-export domain, lists the jobs in the catalog DataGrid (auto-polling
 * while any job is PENDING/RUNNING), and downloads a completed job's binary.
 */
export default function StatementsPage() {
  const jobs = useExportJobs();
  const createExport = useCreateExport();
  const download = useDownloadExport();
  const [name, setName] = useState('결제 정산 명세서');

  const handleCreate = async (format: 'csv' | 'xlsx'): Promise<void> => {
    try {
      await createExport.mutateAsync({ format, name });
    } catch {
      // surfaced below
    }
  };

  return (
    <div className="space-y-6">
      <PageHeader
        title="정산 명세서"
        description="결제·정산 데이터를 CSV / XLSX로 내보냅니다. 생성 작업이 완료되면 내려받을 수 있습니다."
      />

      <div className="flex flex-wrap items-end gap-3 rounded-[var(--radius)] border border-border bg-card p-4 shadow-sm">
        <div className="min-w-[14rem] flex-1 space-y-1.5">
          <label htmlFor="export-name" className="text-sm font-medium text-foreground">
            명세서 이름
          </label>
          <Input
            id="export-name"
            value={name}
            onChange={(e) => setName(e.target.value)}
          />
        </div>
        <Button onClick={() => handleCreate('csv')} loading={createExport.isPending}>
          <ScrollText aria-hidden /> CSV 생성
        </Button>
        <Button variant="secondary" onClick={() => handleCreate('xlsx')} loading={createExport.isPending}>
          <FileSpreadsheet aria-hidden /> XLSX 생성
        </Button>
      </div>

      {createExport.error ? (
        <Alert variant="error">{(createExport.error as Error).message}</Alert>
      ) : null}
      {download.error ? <Alert variant="error">{(download.error as Error).message}</Alert> : null}

      {jobs.isLoading ? (
        <ScreenLoading label="명세서 목록 불러오는 중" />
      ) : jobs.error ? (
        <ScreenError error={jobs.error as Error} />
      ) : (jobs.data?.items.length ?? 0) === 0 ? (
        <ScreenEmpty
          icon={<ScrollText className="h-7 w-7" />}
          title="생성된 명세서가 없습니다"
          description="위에서 CSV 또는 XLSX 명세서를 생성하세요."
        />
      ) : (
        <DataGrid<Col>
          caption="정산 명세서 작업"
          columns={COLUMNS}
          rows={(jobs.data?.items ?? []).map((job) => ({
            name: <span className="truncate">{job.name ?? '명세서'}</span>,
            format: <span className="font-mono text-xs">{job.format}</span>,
            status: <ExportStatus status={job.status} />,
            rows: <span className="ax-money">{job.rowCount?.toLocaleString('ko-KR') ?? '—'}</span>,
            size: <span className="ax-money">{formatBytes(job.sizeBytes)}</span>,
            created: <span className="text-muted-foreground">{formatDateTime(job.createdAt)}</span>,
            action: job.downloadAvailable ? (
              <Button
                variant="outline"
                size="sm"
                onClick={() => download.mutate({ jobId: job.jobId })}
                loading={download.isPending && download.variables?.jobId === job.jobId}
                aria-label={`${job.name ?? '명세서'} 내려받기`}
              >
                <Download aria-hidden /> 내려받기
              </Button>
            ) : (
              <span className="text-xs text-muted-foreground">—</span>
            ),
          }))}
        />
      )}
    </div>
  );
}
