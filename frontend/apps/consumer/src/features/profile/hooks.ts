import { useQuery } from '@tanstack/react-query';
import { profileClient, type ConsumerProfile } from '@/lib/api/profileClient';

export const profileKeys = {
  me: ['profile', 'me'] as const,
};

/**
 * Resolve the live profile (email, id) for the shell header. `enabled` defers
 * the call until a token exists so we never 401 on first paint.
 */
export function useProfile(enabled: boolean) {
  return useQuery<ConsumerProfile>({
    queryKey: profileKeys.me,
    queryFn: () => profileClient.me(),
    enabled,
    staleTime: 5 * 60_000,
    retry: false,
  });
}
