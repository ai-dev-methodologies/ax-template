import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  webhookClient,
  type RegisterWebhookInput,
  type WebhookDelivery,
  type WebhookDeliveryStatus,
  type WebhookEndpoint,
  type WebhookEndpointWithSecret,
} from '@/lib/api/webhookClient';
import type { HttpExchange } from '@/lib/api/rawFetch';

export const webhookKeys = {
  all: ['webhooks'] as const,
  endpoints: () => [...webhookKeys.all, 'endpoints'] as const,
  deliveries: (status: WebhookDeliveryStatus) =>
    [...webhookKeys.all, 'deliveries', status] as const,
};

/** Registered webhook endpoints (the DataGrid source + the captured GET exchange). */
export function useWebhookEndpoints() {
  return useQuery<HttpExchange<WebhookEndpoint[]>>({
    queryKey: webhookKeys.endpoints(),
    queryFn: () => webhookClient.listEndpoints(),
  });
}

/** The delivery / attempt log for a given status (default FAILED_PERMANENT). */
export function useWebhookDeliveries(status: WebhookDeliveryStatus) {
  return useQuery<HttpExchange<WebhookDelivery[]>>({
    queryKey: webhookKeys.deliveries(status),
    queryFn: () => webhookClient.listDeliveries(status),
  });
}

export function useRegisterWebhook() {
  const qc = useQueryClient();
  return useMutation<HttpExchange<WebhookEndpointWithSecret>, Error, RegisterWebhookInput>({
    mutationFn: (input) => webhookClient.register(input),
    onSuccess: () => {
      void qc.invalidateQueries({ queryKey: webhookKeys.endpoints() });
    },
  });
}

export function useDeleteWebhook() {
  const qc = useQueryClient();
  return useMutation<HttpExchange<unknown>, Error, string>({
    mutationFn: (id) => webhookClient.remove(id),
    onSuccess: () => {
      void qc.invalidateQueries({ queryKey: webhookKeys.endpoints() });
    },
  });
}
