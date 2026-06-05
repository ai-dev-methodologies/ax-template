import { keepPreviousData, useQuery } from '@tanstack/react-query';
import { auditLogClient, type AuditLogFilter, type AuditLogPage } from '@/lib/api/auditLogClient';

export const auditLogKeys = {
  all: ['audit-logs'] as const,
  list: (filter: AuditLogFilter) => [...auditLogKeys.all, 'list', filter] as const,
};

export function useAuditLogs(filter: AuditLogFilter) {
  return useQuery<AuditLogPage>({
    queryKey: auditLogKeys.list(filter),
    queryFn: () => auditLogClient.list(filter),
    placeholderData: keepPreviousData,
  });
}
