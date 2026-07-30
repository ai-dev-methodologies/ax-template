/*
---
template_id: L2/blocks/billing-history
layer: L2
provenance_class: internal_design
evidence:
  - source_type: external
    citation: "P1-07a: billing-history completes billing L4 page composition (invoice list + payment history view)"
    url: "https://ax-template.internal/audit"
  - source_type: upstream_id
    upstream_id: stripe-billing-2026-05
    section: "Webhook events"
    quote: "Sent when a subscription starts or changes."
dependencies: []
imports_from: []
imports_forbidden: [L4, app/, lib/payment/]
---
*/

'use client'

import * as React from 'react'

export interface BillingHistoryItem {
  id: string
  subscriptionId: string
  eventType: string
  idempotencyKey: string
  providerEventId: string | null
  occurredAt: string
}

export interface BillingHistoryProps {
  events: BillingHistoryItem[]
  isLoading?: boolean
}

const EVENT_TYPE_LABELS: Record<string, string> = {
  SUBSCRIPTION_CREATED: '구독 생성',
  TRIAL_END: '체험 종료',
  PAYMENT_SUCCEEDED: '결제 성공',
  PAYMENT_FAILED: '결제 실패',
  SUBSCRIPTION_RENEWED: '구독 갱신',
  SUBSCRIPTION_CANCELLED: '구독 취소',
  PLAN_CHANGED: '플랜 변경',
  WEBHOOK_RECEIVED: '웹훅 수신',
  ADMIN_OVERRIDE: '관리자 변경',
}

const EVENT_TYPE_ICON: Record<string, string> = {
  PAYMENT_SUCCEEDED: '✓',
  SUBSCRIPTION_RENEWED: '↻',
  PAYMENT_FAILED: '✗',
  SUBSCRIPTION_CANCELLED: '⊗',
  TRIAL_END: '⏰',
  SUBSCRIPTION_CREATED: '✦',
  PLAN_CHANGED: '⇌',
  default: '•',
}

const EVENT_TYPE_COLOR: Record<string, string> = {
  PAYMENT_SUCCEEDED: 'text-green-600',
  SUBSCRIPTION_RENEWED: 'text-green-600',
  PAYMENT_FAILED: 'text-destructive',
  SUBSCRIPTION_CANCELLED: 'text-muted-foreground',
  TRIAL_END: 'text-yellow-600',
  default: 'text-primary',
}

/**
 * BillingHistory — L2 timeline of billing lifecycle events.
 *
 * Displays a chronological list of BillingEvents from the billing domain.
 * Boundary: no import from payment L4 or payment L2.
 *
 * ```tsx
 * <BillingHistory events={billingEvents} />
 * ```
 */
export function BillingHistory({ events, isLoading }: BillingHistoryProps) {
  if (isLoading) {
    return (
      <div className="space-y-3" aria-busy aria-label="결제 내역 로딩 중">
        {Array.from({ length: 4 }).map((_, i) => (
          <div key={i} className="h-10 animate-pulse rounded-lg bg-muted" />
        ))}
      </div>
    )
  }

  if (events.length === 0) {
    return (
      <div className="rounded-lg border border-dashed p-8 text-center text-muted-foreground">
        결제 내역이 없습니다.
      </div>
    )
  }

  return (
    <ol className="relative border-l border-border pl-4" aria-label="결제 이력">
      {events.map((event) => {
        const icon = EVENT_TYPE_ICON[event.eventType] ?? EVENT_TYPE_ICON.default
        const colorClass = EVENT_TYPE_COLOR[event.eventType] ?? EVENT_TYPE_COLOR.default
        const label = EVENT_TYPE_LABELS[event.eventType] ?? event.eventType

        return (
          <li key={event.id} className="mb-6 ml-3">
            <span
              aria-hidden
              className={`absolute -left-1.5 flex h-3 w-3 items-center justify-center rounded-full
                          border-2 border-background bg-current ${colorClass}`}
            />
            <div className="flex items-center justify-between gap-2">
              <span className={`text-sm font-semibold ${colorClass}`}>
                {icon} {label}
              </span>
              <time
                dateTime={event.occurredAt}
                className="text-xs tabular-nums text-muted-foreground"
              >
                {new Date(event.occurredAt).toLocaleString('ko-KR')}
              </time>
            </div>
            {event.providerEventId && (
              <p className="mt-0.5 text-xs text-muted-foreground">
                Provider: {event.providerEventId}
              </p>
            )}
          </li>
        )
      })}
    </ol>
  )
}
