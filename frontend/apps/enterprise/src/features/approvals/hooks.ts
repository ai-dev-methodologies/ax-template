import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  approvalClient,
  type ApprovalInboxResponse,
  type ApprovalRequest,
  type StepActionInput,
} from '@/lib/api/approvalClient';

export const approvalKeys = {
  all: ['approvals'] as const,
  inbox: () => [...approvalKeys.all, 'inbox'] as const,
  detail: (id: string) => [...approvalKeys.all, 'detail', id] as const,
};

export function useApprovalInbox() {
  return useQuery<ApprovalInboxResponse>({
    queryKey: approvalKeys.inbox(),
    queryFn: () => approvalClient.inbox(),
  });
}

export function useApprovalRequest(requestId: string | null) {
  return useQuery<ApprovalRequest>({
    queryKey: approvalKeys.detail(requestId ?? ''),
    queryFn: () => approvalClient.get(requestId as string),
    enabled: Boolean(requestId),
  });
}

/** Approve or reject a step, then invalidate the inbox + detail caches. */
export function useStepAction(action: 'approve' | 'reject') {
  const queryClient = useQueryClient();
  return useMutation<ApprovalRequest, Error, StepActionInput>({
    mutationFn: (input) =>
      action === 'approve'
        ? approvalClient.approveStep(input)
        : approvalClient.rejectStep(input),
    onSuccess: (updated) => {
      queryClient.setQueryData(approvalKeys.detail(updated.id), updated);
      void queryClient.invalidateQueries({ queryKey: approvalKeys.inbox() });
    },
  });
}
