import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  featureFlagClient,
  type CreateFeatureFlagInput,
  type FeatureFlag,
  type FeatureFlagPage,
} from '@/lib/api/featureFlagClient';

export const featureFlagKeys = {
  all: ['feature-flags'] as const,
  list: () => [...featureFlagKeys.all, 'list'] as const,
};

export function useFeatureFlags() {
  return useQuery<FeatureFlagPage>({
    queryKey: featureFlagKeys.list(),
    queryFn: () => featureFlagClient.list(),
  });
}

/** Optimistic enable/disable toggle with rollback on failure. */
export function useToggleFeatureFlag() {
  const queryClient = useQueryClient();
  return useMutation<
    FeatureFlag,
    Error,
    { name: string; enabled: boolean },
    { previous?: FeatureFlagPage }
  >({
    mutationFn: ({ name, enabled }) => featureFlagClient.setEnabled(name, enabled),
    onMutate: async ({ name, enabled }) => {
      await queryClient.cancelQueries({ queryKey: featureFlagKeys.list() });
      const previous = queryClient.getQueryData<FeatureFlagPage>(featureFlagKeys.list());
      if (previous) {
        queryClient.setQueryData<FeatureFlagPage>(featureFlagKeys.list(), {
          ...previous,
          content: previous.content.map((flag) =>
            flag.name === name ? { ...flag, enabled } : flag,
          ),
        });
      }
      return { previous };
    },
    onError: (_err, _vars, context) => {
      if (context?.previous) {
        queryClient.setQueryData(featureFlagKeys.list(), context.previous);
      }
    },
    onSettled: () => {
      void queryClient.invalidateQueries({ queryKey: featureFlagKeys.list() });
    },
  });
}

export function useCreateFeatureFlag() {
  const queryClient = useQueryClient();
  return useMutation<FeatureFlag, Error, CreateFeatureFlagInput>({
    mutationFn: (input) => featureFlagClient.create(input),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: featureFlagKeys.list() });
    },
  });
}
