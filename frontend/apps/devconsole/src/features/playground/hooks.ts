import { useMutation } from '@tanstack/react-query';
import { idempotencyClient } from '@/lib/api/idempotencyClient';
import { problemClient, type ProblemKind } from '@/lib/api/problemClient';
import {
  requestValidationClient,
  type CreateOrderInput,
} from '@/lib/api/requestValidationClient';
import type { HttpExchange } from '@/lib/api/rawFetch';

/**
 * The playground screens (idempotency / problem-details / request-validation)
 * fire a request on demand and render the captured exchange — so each is a
 * mutation (an imperative action), not a cached query. All tolerate non-2xx so
 * the problem bodies render.
 */

export function useIdempotencyFire() {
  return useMutation<HttpExchange<unknown>, Error, { key: string; body: unknown }>({
    mutationFn: ({ key, body }) => idempotencyClient.create(key, body),
  });
}

export function useProblemTrigger() {
  return useMutation<HttpExchange<unknown>, Error, ProblemKind>({
    mutationFn: (kind) => problemClient.trigger(kind),
  });
}

export function useValidationSubmit() {
  return useMutation<HttpExchange<unknown>, Error, CreateOrderInput>({
    mutationFn: (body) => requestValidationClient.createOrder(body),
  });
}
