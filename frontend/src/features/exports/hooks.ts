import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  exportClient,
  type CreateExportInput,
  type ExportJob,
  type ExportJobListResponse,
} from '@/lib/api/exportClient';

export const exportKeys = {
  all: ['exports'] as const,
  list: () => [...exportKeys.all, 'list'] as const,
};

/** Poll while any job is still in flight so the status badge advances live. */
export function useExportJobs() {
  return useQuery<ExportJobListResponse>({
    queryKey: exportKeys.list(),
    queryFn: () => exportClient.list(),
    refetchInterval: (query) => {
      const data = query.state.data;
      const pending = data?.items.some(
        (job) => job.status === 'PENDING' || job.status === 'RUNNING',
      );
      return pending ? 1500 : false;
    },
  });
}

export function useCreateExport() {
  const queryClient = useQueryClient();
  return useMutation<ExportJob, Error, CreateExportInput>({
    mutationFn: (input) => exportClient.create(input),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: exportKeys.list() });
    },
  });
}

/** Trigger a browser download from the authed binary endpoint. */
export function useDownloadExport() {
  return useMutation<void, Error, { jobId: string }>({
    mutationFn: async ({ jobId }) => {
      const { blob, filename } = await exportClient.download(jobId);
      const url = URL.createObjectURL(blob);
      const anchor = document.createElement('a');
      anchor.href = url;
      anchor.download = filename;
      document.body.appendChild(anchor);
      anchor.click();
      anchor.remove();
      URL.revokeObjectURL(url);
    },
  });
}
