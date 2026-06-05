/**
 * Billing client. Backend: SubscriptionController (/api/subscriptions),
 * BillingAdminController (/api/admin/billing/plans).
 *
 * Money: plan + subscription `amount` is in MINOR units (long) — KRW 29000 means
 * ₩29,000 (0 minor digits), USD 2999 means $29.99. Render with formatMinor
 * (lib/money.ts); the integer->decimal step is string-based, no float drift.
 *
 * Subscription state machine: TRIAL -> ACTIVE / PAST_DUE -> CANCELLED (terminal).
 * createSubscription starts in TRIAL; cancel drives to CANCELLED.
 *
 * Curl-verified (2026-06-05, demo@ax.dev):
 *   GET  /api/admin/billing/plans                                   -> [PlanResponse]
 *   POST /api/admin/billing/plans {name,amount:29000,currency,billingCycle} -> 201 PlanResponse
 *   GET  /api/subscriptions                                         -> {items,totalElements}
 *   POST /api/subscriptions {planId,provider}                       -> 201 status TRIAL
 *   POST /api/subscriptions/{id}/cancel                             -> status CANCELLED
 *
 * Domain client — composes the shared @ax/core authed fetch.
 */
import { apiFetch } from '@ax/core';

export type BillingCycle = 'MONTHLY' | 'YEARLY';
export type SubscriptionStatus = 'TRIAL' | 'ACTIVE' | 'PAST_DUE' | 'CANCELLED';

export interface Plan {
  id: string;
  name: string;
  /** MINOR units (long). */
  amount: number;
  currency: string;
  billingCycle: BillingCycle;
  createdAt: string;
}

export interface Subscription {
  id: string;
  userId: string;
  planId: string;
  status: SubscriptionStatus;
  provider: string;
  /** MINOR units (long), copied from the plan at subscribe time. */
  amount: number;
  currency: string;
  startedAt: string;
  currentPeriodEnd: string | null;
}

export interface SubscriptionList {
  items: Subscription[];
  totalElements: number;
}

export interface CreatePlanInput {
  name: string;
  /** MINOR units (integer). */
  amount: number;
  currency: string;
  billingCycle: BillingCycle;
}

export const billingClient = {
  // ── plans (admin) ────────────────────────────────────────────────────────
  listPlans: (page = 0, size = 50): Promise<Plan[]> =>
    apiFetch<Plan[]>('/admin/billing/plans', { query: { page, size } }),

  createPlan: (input: CreatePlanInput): Promise<Plan> =>
    apiFetch<Plan>('/admin/billing/plans', { method: 'POST', body: input }),

  // ── subscriptions (user) ──────────────────────────────────────────────────
  listSubscriptions: (page = 0, size = 20): Promise<SubscriptionList> =>
    apiFetch<SubscriptionList>('/subscriptions', { query: { page, size } }),

  subscribe: (planId: string, provider: string): Promise<Subscription> =>
    apiFetch<Subscription>('/subscriptions', {
      method: 'POST',
      body: { planId, provider },
    }),

  cancel: (id: string): Promise<Subscription> =>
    apiFetch<Subscription>(`/subscriptions/${id}/cancel`, { method: 'POST' }),
};
