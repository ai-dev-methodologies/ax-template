/**
 * Webhook client — /api/admin/webhook-* . Backend: WebhookAdminController.
 * Auth: under /api/admin/** -> ROLE_ADMIN (the demo account is ADMIN). The
 * `signingSecret` is returned ONCE on register, NEVER on subsequent GETs.
 * Domain client (NOT a UI primitive) — composes the console's rawFetch so each
 * call returns the captured HTTP exchange for the CodeSnippet surface.
 *
 * Curl-verified (2026-06-05, demo@ax.dev ADMIN):
 *   GET    /api/admin/webhook-endpoints                 -> 200 EndpointResponse[]
 *   POST   /api/admin/webhook-endpoints {url,eventFilter} -> 200 EndpointWithSecret (incl. signingSecret)
 *   DELETE /api/admin/webhook-endpoints/{id}            -> 204
 *   GET    /api/admin/webhook-deliveries?status=&page=&size= -> 200 DeliveryResponse[]
 *   POST   /api/admin/webhook-deliveries/{id}/replay    -> 200 DeliveryResponse
 */
import { rawFetch, type HttpExchange } from './rawFetch';

export type WebhookDeliveryStatus =
  | 'PENDING'
  | 'PENDING_RETRY'
  | 'SUCCEEDED'
  | 'FAILED_PERMANENT';

/** GET endpoint row — secret omitted. */
export interface WebhookEndpoint {
  id: string;
  url: string;
  active: boolean;
  eventFilter: string | null;
  createdAt: string;
  updatedAt: string;
}

/** POST register response — carries the one-time signing secret. */
export interface WebhookEndpointWithSecret extends WebhookEndpoint {
  signingSecret: string;
}

export interface WebhookDelivery {
  id: string;
  endpointId: string;
  eventType: string;
  status: WebhookDeliveryStatus;
  attemptCount: number;
  nextAttemptAt: string | null;
  lastResponseCode: number | null;
  lastAttemptAt: string | null;
  lastError: string | null;
  createdAt: string;
}

export interface RegisterWebhookInput {
  url: string;
  eventFilter?: string;
}

export const webhookClient = {
  listEndpoints: (): Promise<HttpExchange<WebhookEndpoint[]>> =>
    rawFetch<WebhookEndpoint[]>('/admin/webhook-endpoints'),

  register: (input: RegisterWebhookInput): Promise<HttpExchange<WebhookEndpointWithSecret>> =>
    rawFetch<WebhookEndpointWithSecret>('/admin/webhook-endpoints', {
      method: 'POST',
      body: input,
    }),

  remove: (id: string): Promise<HttpExchange<unknown>> =>
    rawFetch<unknown>(`/admin/webhook-endpoints/${id}`, { method: 'DELETE' }),

  listDeliveries: (
    status: WebhookDeliveryStatus,
  ): Promise<HttpExchange<WebhookDelivery[]>> =>
    rawFetch<WebhookDelivery[]>(`/admin/webhook-deliveries?status=${status}&page=0&size=20`),

  /** Re-enqueue a delivery for another send attempt (POST .../{id}/replay). */
  replay: (id: string): Promise<HttpExchange<WebhookDelivery>> =>
    rawFetch<WebhookDelivery>(`/admin/webhook-deliveries/${id}/replay`, { method: 'POST' }),
};
