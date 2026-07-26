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
import { summarizeByCurrency, type CurrencyTotals } from '@/features/overview/totals';
import { formatMinor } from '@/lib/money';

/**
 * Overview — revenue / volume metric tiles (tabular money) + the catalog
 * CategoryBarChart, derived from the live payment ledger. The numbers are the
 * persona's signature surface: tabular figures so columns align with no jitter.
 *
 * Money is aggregated PER CURRENCY (see features/overview/totals.ts). The tiles
 * show the primary (most rows) currency and every other currency is listed in an
 * explicit breakdown — the screen never adds two currencies together and never
 * invents an FX rate. The wire values are integer MINOR units, so they render
 * through formatMinor.
 */
export function OverviewScreen() {
  const payments = usePayments(0, 100);
  const subs = useSubscriptions();

  if (payments.isLoading) return <ScreenLoading label="개요 불러오는 중" />;
  if (payments.error) return <ScreenError error={payments.error as Error} />;

  const rows = payments.data?.content ?? [];
  // One independent total per currency; [0] is the currency with the most rows.
  // Summing across currencies would render a number that means nothing (₩12,900 +
  // $10.99 is not 13,999 of anything).
  const totals = summarizeByCurrency(rows);
  const primary: CurrencyTotals = totals[0] ?? {
    currency: 'KRW',
    gross: 0,
    captured: 0,
    refunded: 0,
    count: 0,
  };
  const others = totals.slice(1);
  const currency = primary.currency;
  const currencyNote = others.length > 0 ? ` · ${currency} 기준` : '';

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
          value={formatMinor(primary.captured, currency)}
          sublabel={`매입 완료 + 부분 환불 반영${currencyNote}`}
          icon={<Wallet className="h-4 w-4" />}
        />
        <MetricTile
          label="총 결제 금액"
          value={formatMinor(primary.gross, currency)}
          sublabel={`거래 ${primary.count.toLocaleString('ko-KR')}건${currencyNote}`}
          icon={<BadgeDollarSign className="h-4 w-4" />}
        />
        <MetricTile
          label="환불 금액"
          value={formatMinor(primary.refunded, currency)}
          sublabel={`전체/부분 환불 합계${currencyNote}`}
          icon={<RotateCcw className="h-4 w-4" />}
        />
        <MetricTile
          label="활성 구독"
          value={`${activeSubs.toLocaleString('ko-KR')}건`}
          sublabel="구독중 + 체험중"
          icon={<ReceiptText className="h-4 w-4" />}
        />
      </section>

      {others.length > 0 ? <CurrencyBreakdown totals={totals} /> : null}

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
            value: formatMinor(p.amount, p.currency),
            state: p.state,
          }))}
        />
      </section>
    </div>
  );
}

/**
 * Per-currency breakdown — rendered only when the ledger holds more than one
 * currency. The tiles above show the primary currency; this makes the others
 * visible instead of folding them into a meaningless combined figure.
 */
function CurrencyBreakdown({ totals }: { totals: CurrencyTotals[] }) {
  return (
    <section aria-label="통화별 합계">
      <Card className="flex flex-col">
        <div className="flex items-center justify-between border-b border-border px-4 py-3">
          <h2 className="text-sm font-medium text-muted-foreground">통화별 합계</h2>
          <span className="text-xs text-muted-foreground">환율 환산 없음</span>
        </div>
        <ul className="divide-y divide-border">
          {totals.map((t) => (
            <li
              key={t.currency}
              className="flex flex-wrap items-center justify-between gap-x-6 gap-y-1 px-4 py-2.5"
            >
              <span className="text-sm font-medium text-foreground">
                {t.currency}
                <span className="ml-2 text-xs font-normal text-muted-foreground">
                  {t.count.toLocaleString('ko-KR')}건
                </span>
              </span>
              <span className="flex flex-wrap gap-x-4 text-sm text-muted-foreground">
                <span>
                  총 결제{' '}
                  <span className="ax-money font-medium tabular-nums text-foreground">
                    {formatMinor(t.gross, t.currency)}
                  </span>
                </span>
                <span>
                  순 매출{' '}
                  <span className="ax-money font-medium tabular-nums text-foreground">
                    {formatMinor(t.captured, t.currency)}
                  </span>
                </span>
                <span>
                  환불{' '}
                  <span className="ax-money font-medium tabular-nums text-foreground">
                    {formatMinor(t.refunded, t.currency)}
                  </span>
                </span>
              </span>
            </li>
          ))}
        </ul>
      </Card>
    </section>
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
