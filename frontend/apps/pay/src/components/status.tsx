import { StatusBadge } from '@ax/blocks';
import type { PaymentState } from '@/lib/api/paymentClient';
import type { SubscriptionStatus } from '@/lib/api/billingClient';
import type { ExportJobStatus } from '@/lib/api/exportClient';

/**
 * Domain-status -> catalog StatusBadge mapping. The catalog StatusBadge owns the
 * pill chrome + the semantic color token + the role="status" a11y; this file
 * only chooses which catalog StatusKind + label a backend enum maps to, plus a
 * Korean label. No app-local pill is defined (that would violate the boundary
 * rule) — every visual is the shared block.
 */

// StatusBadge.StatusKind: pending | failed | success | in_progress | in_review | expired | submitted

const PAYMENT_KIND: Record<
  PaymentState,
  { kind: Parameters<typeof StatusBadge>[0]['status']; label: string }
> = {
  CREATED: { kind: 'submitted', label: '생성됨' },
  AUTHORIZED: { kind: 'in_progress', label: '승인됨' },
  CAPTURED: { kind: 'success', label: '매입완료' },
  VOIDED: { kind: 'expired', label: '취소됨' },
  REFUNDED: { kind: 'expired', label: '환불완료' },
  PARTIAL_REFUNDED: { kind: 'in_review', label: '부분환불' },
  UNKNOWN: { kind: 'pending', label: '확인중' },
  FAILED: { kind: 'failed', label: '실패' },
};

const SUBSCRIPTION_KIND: Record<
  SubscriptionStatus,
  { kind: Parameters<typeof StatusBadge>[0]['status']; label: string }
> = {
  TRIAL: { kind: 'in_review', label: '체험중' },
  ACTIVE: { kind: 'success', label: '구독중' },
  PAST_DUE: { kind: 'pending', label: '연체' },
  CANCELLED: { kind: 'expired', label: '해지됨' },
};

const EXPORT_KIND: Record<
  ExportJobStatus,
  { kind: Parameters<typeof StatusBadge>[0]['status']; label: string }
> = {
  PENDING: { kind: 'pending', label: '대기' },
  RUNNING: { kind: 'in_progress', label: '생성중' },
  COMPLETED: { kind: 'success', label: '완료' },
  FAILED: { kind: 'failed', label: '실패' },
  CANCELLED: { kind: 'expired', label: '취소됨' },
};

export function PaymentStatus({ state }: { state: PaymentState }) {
  const { kind, label } = PAYMENT_KIND[state];
  return <StatusBadge status={kind} label={label} />;
}

export function SubscriptionStatusBadge({ status }: { status: SubscriptionStatus }) {
  const { kind, label } = SUBSCRIPTION_KIND[status];
  return <StatusBadge status={kind} label={label} />;
}

export function ExportStatus({ status }: { status: ExportJobStatus }) {
  const { kind, label } = EXPORT_KIND[status];
  return <StatusBadge status={kind} label={label} />;
}
