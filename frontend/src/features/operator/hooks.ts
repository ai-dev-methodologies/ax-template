import { useQuery } from '@tanstack/react-query';
import { operatorClient, type OperatorIdentity } from '@/lib/api/operatorClient';

export const operatorKeys = {
  me: ['operator', 'me'] as const,
};

/**
 * Resolve the live operator identity (real role) for the console.
 * `enabled` defers the call until a token exists so we never 401 on first paint.
 */
export function useOperator(enabled: boolean) {
  return useQuery<OperatorIdentity>({
    queryKey: operatorKeys.me,
    queryFn: () => operatorClient.me(),
    enabled,
    staleTime: 5 * 60_000,
    retry: false,
  });
}
