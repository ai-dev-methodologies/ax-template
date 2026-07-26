/**
 * Payment client. Backend: PaymentController (/api/payments).
 *
 * Money: `amount` / `capturedAmount` / `balance` are integer MINOR currency units
 * on the wire (PaymentBodyMapper.toBody -> common/Money.toMinorUnits; a $10.99 USD
 * payment arrives as 1099, a ₩12,900 payment as 12900 because KRW's minor unit IS
 * its major unit). Render with formatMinor (lib/money.ts) — formatMajor would show
 * US$1,099.00 for that $10.99 payment. Locked by the committed wire golden
 * frontend/tests/_fixtures/money-contract.golden.json (#paymentUsd).
 *
 * Mutations (create / void / refund) REQUIRE an `Idempotency-Key` header — a
 * double-submit REPLAYS (200 + same payment) instead of double-charging
 * (a second 201). The state machine is CREATED -> AUTHORIZED -> CAPTURED with
 * VOIDED / REFUNDED / PARTIAL_REFUNDED / FAILED / UNKNOWN terminals.
 *
 * Curl-verified (2026-06-05, demo@ax.dev):
 *   POST /api/payments + Idempotency-Key {amount:12900,currency:"KRW",orderId}  -> 201 status CREATED
 *   POST (same key, replay)                                                     -> 200 same id
 *   POST /api/payments/{id}/authorize                                           -> 200 AUTHORIZED
 *   POST /api/payments/{id}/capture                                             -> 200 CAPTURED, balance=amount
 *   POST /api/payments/{id}/refund + Idempotency-Key {amount:4900}              -> 201 refund; payment PARTIAL_REFUNDED, balance reduced
 *   GET  /api/payments?page&size                                                -> {content,page,size,totalElements,totalPages}
 *
 * Domain client — composes payFetch (shared @ax/core token + RFC 9457 parsing,
 * plus the Idempotency-Key header the money flow needs).
 */
import { newIdempotencyKey, payFetch } from './payFetch';

export type PaymentState =
  | 'CREATED'
  | 'AUTHORIZED'
  | 'CAPTURED'
  | 'VOIDED'
  | 'REFUNDED'
  | 'PARTIAL_REFUNDED'
  | 'UNKNOWN'
  | 'FAILED';

export interface Payment {
  id: string;
  paymentId: string;
  orderId: string;
  /** Integer MINOR currency units (USD 1099 == $10.99; KRW 12900 == ₩12,900). */
  amount: number;
  capturedAmount: number | null;
  balance: number | null;
  currency: string;
  status: PaymentState;
  state: PaymentState;
  declineReason: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface PaymentPage {
  content: Payment[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface CreatePaymentInput {
  /**
   * MAJOR currency units, sent as a STRING. The backend MoneyDeserializer
   * REJECTS JSON floats (a binary float cannot losslessly hold most decimals),
   * so a decimal amount like "70.50" MUST go over the wire as a JSON string, not
   * 70.5. KRW integers ("12900") and USD decimals ("70.00") are both valid
   * strings. Curl-verified: 70.00 (float) -> 400, "70.00" (string) -> 201.
   */
  amount: string;
  currency: string;
  orderId: string;
  paymentMethodToken?: string;
  /** Optional mock failure mode for demoing decline/timeout paths. */
  mockFailureMode?: string;
}

export interface Refund {
  id: string;
  paymentId: string;
  amount: number;
  currency: string;
  state: 'PROCESSING' | 'COMPLETED' | 'FAILED';
  createdAt: string;
}

export interface RefundInput {
  /**
   * MAJOR units as a STRING (same MoneyDeserializer float-rejection rule as
   * create); omit to refund the full remaining captured amount.
   */
  amount?: string;
  reason?: string;
}

export const paymentClient = {
  list: (page = 0, size = 20): Promise<PaymentPage> =>
    payFetch<PaymentPage>('/payments', { query: { page, size } }),

  get: (id: string): Promise<Payment> => payFetch<Payment>(`/payments/${id}`),

  /**
   * Create a payment. The caller passes the Idempotency-Key so the SAME key can
   * be reused on a retry to prove a replay (the form holds one key per attempt).
   */
  create: (input: CreatePaymentInput, idempotencyKey: string): Promise<Payment> =>
    payFetch<Payment>('/payments', {
      method: 'POST',
      headers: { 'Idempotency-Key': idempotencyKey },
      body: input,
    }),

  authorize: (id: string): Promise<Payment> =>
    payFetch<Payment>(`/payments/${id}/authorize`, { method: 'POST' }),

  capture: (id: string): Promise<Payment> =>
    payFetch<Payment>(`/payments/${id}/capture`, { method: 'POST' }),

  void: (id: string): Promise<Payment> =>
    payFetch<Payment>(`/payments/${id}/void`, {
      method: 'POST',
      headers: { 'Idempotency-Key': newIdempotencyKey() },
    }),

  refund: (id: string, input: RefundInput): Promise<Refund> =>
    payFetch<Refund>(`/payments/${id}/refund`, {
      method: 'POST',
      headers: { 'Idempotency-Key': newIdempotencyKey() },
      body: { amount: input.amount, reason: input.reason },
    }),
};
