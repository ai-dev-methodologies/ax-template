/**
 * Audit-log client — GET /api/audit-logs (paginated, filterable).
 * Backend: AuditLogController. Auth: any authenticated user.
 * Response shape mirrors AuditLogPage / AuditLogResponse.
 */
import { apiFetch } from './enterpriseHttp';

export type AuditOutcome = 'SUCCESS' | 'FAILURE';

export interface AuditLogEntry {
  id: string;
  actorId: string | null;
  actorIp: string | null;
  action: string;
  resourceType: string | null;
  resourceId: string | null;
  outcome: AuditOutcome;
  timestamp: string;
  correlationId: string | null;
  userAgent: string | null;
}

export interface AuditLogPage {
  content: AuditLogEntry[];
  totalElements: number;
  totalPages: number;
  page: number;
  size: number;
}

export interface AuditLogFilter {
  page?: number;
  size?: number;
  actorId?: string;
  resourceType?: string;
  action?: string;
  outcome?: AuditOutcome;
}

export const auditLogClient = {
  list: (filter: AuditLogFilter) =>
    apiFetch<AuditLogPage>('/audit-logs', {
      query: {
        page: filter.page ?? 0,
        size: filter.size ?? 20,
        actorId: filter.actorId,
        resourceType: filter.resourceType,
        action: filter.action,
        outcome: filter.outcome,
      },
    }),
};
