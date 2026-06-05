import { useMutation } from '@tanstack/react-query';
import {
  optlockClient,
  type OptlockResource,
  type OptlockUpdateInput,
} from '@/lib/api/optlockClient';
import type { HttpExchange } from '@/lib/api/rawFetch';

/**
 * Optimistic-locking demo is a stepwise conditional-request flow (create -> GET
 * ETag -> PUT If-Match success -> PUT stale -> 412), each step fired imperatively
 * and rendered as its own captured exchange. Mutations, not cached queries.
 */

export function useOptlockCreate() {
  return useMutation<HttpExchange<OptlockResource>, Error, OptlockUpdateInput>({
    mutationFn: (body) => optlockClient.create(body),
  });
}

export function useOptlockGet() {
  return useMutation<HttpExchange<OptlockResource>, Error, string>({
    mutationFn: (id) => optlockClient.get(id),
  });
}

export function useOptlockUpdate() {
  return useMutation<
    HttpExchange<OptlockResource>,
    Error,
    { id: string; body: OptlockUpdateInput; ifMatch?: string }
  >({
    mutationFn: ({ id, body, ifMatch }) => optlockClient.update(id, body, ifMatch),
  });
}
