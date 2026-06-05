import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { sessionClient, type SessionListResponse } from '@/lib/api/sessionClient';

export const sessionKeys = {
  all: ['sessions'] as const,
  list: () => [...sessionKeys.all, 'list'] as const,
};

export function useSessions() {
  return useQuery<SessionListResponse>({
    queryKey: sessionKeys.list(),
    queryFn: () => sessionClient.list(),
  });
}

/** ROLE_ADMIN force-logout; invalidate the list on success. */
export function useForceRevokeSession() {
  const queryClient = useQueryClient();
  return useMutation<void, Error, string>({
    mutationFn: (id) => sessionClient.forceRevoke(id),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: sessionKeys.list() });
    },
  });
}
