/*
---
template_id: L2/blocks/invoice-list
layer: L2
provenance_class: internal_design
evidence:
  - source_type: external
    citation: "Audit B.2.11 P2→P1 — InvoiceList for billing history UI"
    url: "https://ax-template.internal/audit"
  - source_type: upstream_id
    upstream_id: stripe-billing-2026-05
    section: "Webhook events"
    quote: "invoice.payment_succeeded — Invoice paid; renew subscription."
dependencies: [currency-input]
imports_from: [L1]
imports_forbidden: [L4, app/, lib/payment/]
---
*/

'use client'

import * as React from 'react'
import { formatCurrencyAmount } from 'templates/L1/components/currency-input'

export interface InvoiceItem {
  id: string
  subscriptionId: string
  /** Amount due in integer minor currency units. */
  amountDue: number
  /** Amount paid in integer minor currency units. */
  amountPaid: number
  currency: string
  status: 'DRAFT' | 'OPEN' | 'PAID' | 'VOID' | 'UNCOLLECTIBLE'
  issuedAt: string | null
  paidAt: string | null
  periodStart: string
  periodEnd: string
  providerInvoiceId: string | null
}

export interface InvoiceListProps {
  invoices: InvoiceItem[]
  onDownload?: (invoiceId: string) => void
  isLoading?: boolean
  locale?: string
}

const STATUS_LABELS: Record<InvoiceItem['status'], string> = {
  DRAFT: '초안',
  OPEN: '미결',
  PAID: '결제 완료',
  VOID: '무효',
  UNCOLLECTIBLE: '미수금',
}

const STATUS_CLASSES: Record<InvoiceItem['status'], string> = {
  DRAFT: 'bg-muted text-muted-foreground',
  OPEN: 'bg-yellow-100 text-yellow-800',
  PAID: 'bg-green-100 text-green-800',
  VOID: 'bg-secondary text-secondary-foreground',
  UNCOLLECTIBLE: 'bg-destructive/10 text-destructive',
}

/**
 * InvoiceList — L2 table of billing invoices.
 *
 * Amounts displayed via formatCurrencyAmount (integer minor units).
 * Boundary: no import from payment L4 or payment L2.
 *
 * ```tsx
 * <InvoiceList invoices={invoices} onDownload={(id) => downloadInvoice(id)} />
 * ```
 */
export function InvoiceList({ invoices, onDownload, isLoading, locale = 'ko-KR' }: InvoiceListProps) {
  if (isLoading) {
    return (
      <div className="space-y-2" aria-busy aria-label="청구서 로딩 중">
        {Array.from({ length: 3 }).map((_, i) => (
          <div key={i} className="h-12 animate-pulse rounded-lg bg-muted" />
        ))}
      </div>
    )
  }

  if (invoices.length === 0) {
    return (
      <div className="rounded-lg border border-dashed p-8 text-center text-muted-foreground">
        청구서가 없습니다.
      </div>
    )
  }

  return (
    <div className="overflow-x-auto">
      <table className="w-full text-sm" aria-label="청구서 목록">
        <thead className="border-b">
          <tr>
            <th scope="col" className="py-2 text-left font-semibold">기간</th>
            <th scope="col" className="py-2 text-right font-semibold">금액</th>
            <th scope="col" className="py-2 text-center font-semibold">상태</th>
            <th scope="col" className="py-2 text-right font-semibold">결제일</th>
            {onDownload && <th scope="col" className="py-2" />}
          </tr>
        </thead>
        <tbody className="divide-y">
          {invoices.map((inv) => (
            <tr key={inv.id} className="hover:bg-muted/30 transition-colors">
              <td className="py-3 tabular-nums text-muted-foreground">
                {inv.periodStart} ~ {inv.periodEnd}
              </td>
              <td className="py-3 text-right tabular-nums font-medium">
                {formatCurrencyAmount(inv.amountDue, inv.currency, locale)}
              </td>
              <td className="py-3 text-center">
                <span className={`inline-flex rounded-full px-2 py-0.5 text-xs font-semibold ${STATUS_CLASSES[inv.status]}`}>
                  {STATUS_LABELS[inv.status]}
                </span>
              </td>
              <td className="py-3 text-right tabular-nums text-muted-foreground">
                {inv.paidAt ? new Date(inv.paidAt).toLocaleDateString('ko-KR') : '—'}
              </td>
              {onDownload && (
                <td className="py-3 text-right">
                  <button
                    type="button"
                    onClick={() => onDownload(inv.id)}
                    aria-label={`청구서 ${inv.id} 다운로드`}
                    className="text-xs text-primary underline hover:no-underline"
                  >
                    PDF
                  </button>
                </td>
              )}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}
