/**
 * Request-validation client — /api/request-validation/orders. Backend:
 * RequestValidationDemoController. Auth: authenticated() (JWT). A single command
 * endpoint whose body carries the full declarative validation contract; every
 * rejection is shaped into RFC 9457 problem+json with an errors[] array (RFC 6901
 * pointers). Domain client (NOT a UI primitive) — composes the console's rawFetch
 * with `tolerateError` so the 400 errors[] body RENDERS (then 201 on a valid body).
 *
 * Curl-verified (2026-06-05, demo@ax.dev):
 *   POST {customer:"",amount:-5,startDate>endDate,address bad,items bad} -> 400 errors[7]
 *   POST {valid full body}                                              -> 201 {customer,itemCount}
 */
import { rawFetch, type HttpExchange } from './rawFetch';

export type OrderPriority = 'LOW' | 'NORMAL' | 'HIGH';

export interface OrderAddress {
  postalCode: string;
  city: string;
}

export interface OrderLineItem {
  sku: string;
  quantity: number;
}

export interface CreateOrderInput {
  customer: string;
  amount: number;
  priority: OrderPriority;
  startDate: string;
  endDate: string;
  address: OrderAddress;
  items: OrderLineItem[];
}

export const requestValidationClient = {
  /** POST an order body. Errors tolerated so the 400 errors[] is captured + rendered. */
  createOrder: (body: CreateOrderInput): Promise<HttpExchange<unknown>> =>
    rawFetch<unknown>('/request-validation/orders', {
      method: 'POST',
      body,
      tolerateError: true,
    }),
};
