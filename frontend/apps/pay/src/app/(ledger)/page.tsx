'use client';

import React from 'react';
import Link from 'next/link';
import { ArrowRight, BadgeDollarSign, ReceiptText, RotateCcw, Wallet } from 'lucide-react';
import { Button, Card } from '@ax/ui';
import { CategoryBarChart } from '@ax/blocks';
import { PageHeader } from '@/components/page-header';
import { MetricTile } from '@/components/metric-tile';
import { ScreenError, ScreenLoading } from '@/components/screen-states';
import { usePayments } from '@/features/payments/hooks';
import { useSubscriptions } from '@/features/billing/hooks';
import { formatMajor } from '@/lib/money';

/**
 * Overview — revenue / volume metric tiles (tabular money) + the catalog
 * CategoryBarChart, derived from the live payment ledger. The numbers are the
 * persona's signature surface: tabular figures so columns align with no jitter.
 */
export default function OverviewPage() {
  const payments = usePayments(0, 100);
  const subs = useSubscriptions();

  if (payments.isLoading) return <ScreenLoading label="개요 불러오는 중" />;
  if (payments.error) return <ScreenError error={payments.error as Error} />;

  const rows = payments.data?.content ?? [];
  // The reference currency is whatever the ledger is denominated in (KRW here).
  const currency = rows[0]?.currency ?? 'KRW';

  // Captured balance = realized revenue (amount actually held after any refund).
  const capturedTotal = rows
    .filter((p) => p.state === 'CAPTURED' || p.state === 'PARTIAL_REFUNDED')
    .reduce((sum, p) => sum + (p.balance ?? 0), 0);

  const refundedTotal = rows
    .filter((p) => p.state === 'REFUNDED' || p.state === 'PARTIAL_REFUNDED')
    .reduce((sum, p) => sum + ((p.capturedAmount ?? 0) - (p.balance ?? 0)), 0);

  const grossTotal = rows.reduce((sum, p) => sum + (p.amount ?? 0), 0);
  const activeSubs = (subs.data?.items ?? []).filter(
    (s) => s.status === 'ACTIVE' || s.status === 'TRIAL',
  ).length;

  return (
    <div className="space-y-8">
      <PageHeader
        title="개요"
        description="결제 거래 원장에서 집계한 실시간 매출·정산 지표입니다."
        action={
          <Button asChild size="sm">
            <Link href="/checkout">
              결제하기 <ArrowRight aria-hidden />
            </Link>
          </Button>
        }
      />

      <section
        aria-label="핵심 지표"
        className="grid grid-cols-1 gap-4 sm:grid-cols-2 xl:grid-cols-4"
      >
        <MetricTile
          label="순 매출 (정산 잔액)"
          value={formatMajor(capturedTotal, currency)}
          sublabel={`매입 완료 + 부분 환불 반영`}
          icon={<Wallet className="h-4 w-4" />}
        />
        <MetricTile
          label="총 결제 금액"
          value={formatMajor(grossTotal, currency)}
          sublabel={`거래 ${rows.length.toLocaleString('ko-KR')}건`}
          icon={<BadgeDollarSign className="h-4 w-4" />}
        />
        <MetricTile
          label="환불 금액"
          value={formatMajor(refundedTotal, currency)}
          sublabel="전체/부분 환불 합계"
          icon={<RotateCcw className="h-4 w-4" />}
        />
        <MetricTile
          label="활성 구독"
          value={`${activeSubs.toLocaleString('ko-KR')}건`}
          sublabel="구독중 + 체험중"
          icon={<ReceiptText className="h-4 w-4" />}
        />
      </section>

      <section aria-label="매출 채널 분포" className="grid grid-cols-1 gap-4 lg:grid-cols-3">
        <div className="lg:col-span-2">
          {/* CategoryBarChart is a self-contained catalog block (its own demo data);
              under the trust theme it inherits the restrained palette + radius. */}
          <CategoryBarChart className="shadow-sm" />
        </div>
        <RecentLedger
          rows={rows.slice(0, 6).map((p) => ({
            id: p.id,
            label: p.orderId,
            value: formatMajor(p.amount, p.currency),
            state: p.state,
          }))}
        />
      </section>
    </div>
  );
}

function RecentLedger({
  rows,
}: {
  rows: { id: string; label: string; value: string; state: string }[];
}) {
  return (
    <Card className="flex flex-col">
      <div className="flex items-center justify-between border-b border-border px-4 py-3">
        <h2 className="text-sm font-medium text-muted-foreground">최근 거래</h2>
        <Link
          href="/transactions"
          className="rounded-[var(--radius)] text-xs font-medium text-[var(--ax-status-accent-fg)] hover:underline focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
        >
          전체 보기
        </Link>
      </div>
      <ul className="divide-y divide-border">
        {rows.length === 0 ? (
          <li className="px-4 py-6 text-center text-sm text-muted-foreground">거래가 없습니다.</li>
        ) : (
          rows.map((r) => (
            <li key={r.id} className="flex items-center justify-between gap-3 px-4 py-2.5">
              <span className="min-w-0 truncate text-sm text-foreground">{r.label}</span>
              <span className="ax-money shrink-0 text-sm font-medium tabular-nums text-foreground">
                {r.value}
              </span>
            </li>
          ))
        )}
      </ul>
    </Card>
  );
}
