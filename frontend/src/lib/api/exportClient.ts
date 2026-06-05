/**
 * Report-export client. Backend: ReportExportController (/api/exports).
 * POST creates a job (202 PENDING); a worker drives it to COMPLETED; download
 * is a binary GET gated on COMPLETED (downloadAvailable).
 */
import { apiDownload, apiFetch } from './enterpriseHttp';

export type ExportFormat = 'CSV' | 'XLSX';
export type ExportJobStatus = 'PENDING' | 'RUNNING' | 'COMPLETED' | 'FAILED' | 'CANCELLED';

export interface ExportJob {
  jobId: string;
  status: ExportJobStatus;
  format: ExportFormat;
  name: string | null;
  createdAt: string;
  startedAt: string | null;
  completedAt: string | null;
  rowCount: number | null;
  sizeBytes: number | null;
  errorMessage: string | null;
  downloadAvailable: boolean;
}

export interface ExportJobListResponse {
  items: ExportJob[];
  page: number;
  size: number;
  totalElements: number;
}

export interface CreateExportInput {
  format: 'csv' | 'xlsx';
  name?: string;
}

export const exportClient = {
  list: (page = 0, size = 20) =>
    apiFetch<ExportJobListResponse>('/exports', { query: { page, size } }),

  create: (input: CreateExportInput) =>
    apiFetch<ExportJob>('/exports', {
      method: 'POST',
      body: { format: input.format, name: input.name, query: {} },
    }),

  download: (jobId: string) => apiDownload(`/exports/${jobId}/download`),
};
