/**
 * Approval-workflow client. Backend: ApprovalController (/api/approvals).
 * The console operates the approver INBOX: list pending steps, open a request,
 * and approve/reject a step. Visibility is enforced server-side.
 * Domain client — composes the shared @ax/core authed fetch.
 */
import { apiFetch } from '@ax/core';

export type ApprovalRequestStatus = 'DRAFT' | 'SUBMITTED' | 'APPROVED' | 'REJECTED' | 'CANCELLED';
export type ApprovalStepStatus = 'PENDING' | 'APPROVED' | 'REJECTED';

export interface ApprovalInboxEntry {
  requestId: string;
  stepId: string;
  type: string;
  title: string | null;
  status: ApprovalStepStatus;
  requesterUserId: string;
  createdAt: string;
}

export interface ApprovalInboxResponse {
  items: ApprovalInboxEntry[];
  totalElements: number;
}

export interface ApprovalStep {
  id: string;
  orderIndex: number;
  approverUserId: string;
  status: ApprovalStepStatus;
  actedByUserId: string | null;
  actedAt: string | null;
  comment: string | null;
}

export interface ApprovalRequest {
  id: string;
  requesterUserId: string;
  type: string;
  title: string | null;
  status: ApprovalRequestStatus;
  payload: Record<string, unknown>;
  steps: ApprovalStep[];
  createdAt: string;
  submittedAt: string | null;
  completedAt: string | null;
}

export interface StepActionInput {
  requestId: string;
  stepId: string;
  comment?: string;
}

export const approvalClient = {
  inbox: (): Promise<ApprovalInboxResponse> => apiFetch<ApprovalInboxResponse>('/approvals/inbox'),

  get: (requestId: string): Promise<ApprovalRequest> =>
    apiFetch<ApprovalRequest>(`/approvals/${requestId}`),

  approveStep: ({ requestId, stepId, comment }: StepActionInput): Promise<ApprovalRequest> =>
    apiFetch<ApprovalRequest>(`/approvals/${requestId}/steps/${stepId}/approve`, {
      method: 'POST',
      body: { comment: comment ?? null },
    }),

  rejectStep: ({ requestId, stepId, comment }: StepActionInput): Promise<ApprovalRequest> =>
    apiFetch<ApprovalRequest>(`/approvals/${requestId}/steps/${stepId}/reject`, {
      method: 'POST',
      body: { comment: comment ?? null },
    }),
};
