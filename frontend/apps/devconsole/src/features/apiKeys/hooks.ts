import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  apiKeyClient,
  type ApiKeyList,
  type ApiKeySecret,
  type CreateApiKeyInput,
} from '@/lib/api/apiKeyClient';
import type { HttpExchange } from '@/lib/api/rawFetch';

export const apiKeyKeys = {
  all: ['api-keys'] as const,
  list: () => [...apiKeyKeys.all, 'list'] as const,
};

/** The api-key list (the DataGrid source + the captured GET exchange). */
export function useApiKeys() {
  return useQuery<HttpExchange<ApiKeyList>>({
    queryKey: apiKeyKeys.list(),
    queryFn: () => apiKeyClient.list(),
  });
}

export function useCreateApiKey() {
  const qc = useQueryClient();
  return useMutation<HttpExchange<ApiKeySecret>, Error, CreateApiKeyInput>({
    mutationFn: (input) => apiKeyClient.create(input),
    onSuccess: () => {
      void qc.invalidateQueries({ queryKey: apiKeyKeys.list() });
    },
  });
}

export function useRotateApiKey() {
  const qc = useQueryClient();
  return useMutation<HttpExchange<ApiKeySecret>, Error, string>({
    mutationFn: (id) => apiKeyClient.rotate(id),
    onSuccess: () => {
      void qc.invalidateQueries({ queryKey: apiKeyKeys.list() });
    },
  });
}

export function useRevokeApiKey() {
  const qc = useQueryClient();
  return useMutation<HttpExchange<unknown>, Error, string>({
    mutationFn: (id) => apiKeyClient.revoke(id),
    onSuccess: () => {
      void qc.invalidateQueries({ queryKey: apiKeyKeys.list() });
    },
  });
}
