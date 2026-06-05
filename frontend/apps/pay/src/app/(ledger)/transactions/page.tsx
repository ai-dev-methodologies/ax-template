'use client';

import React, { useState } from 'react';
import { ReceiptText, RotateCcw } from 'lucide-react';
import { Alert, Button, ConfirmDialog } from '@ax/ui';
import { DataGrid, type DataGridColumn } from '@ax/blocks';
import { PageHeader } from '@/components/page-header';
import { PaymentStatus } from '@/components/status';
import { ScreenEmpty, ScreenError, ScreenLoading } from '@/components/screen-states';
import { usePayments, useRefundPayment } from '@/features/payments/hooks';
import type { Payment } from '@/lib/api/paymentClient';
import { formatMajor } from '@/lib/money';
import { formatDateTime, shortId } from '@/lib/format';

type Col = 'order' | 'amount' | 'captured' | 'balance' | 'status' | 'created' | 'action';

const COLUMNS: ReadonlyArray<DataGridColumn<Col>> = [
  { key: 'order', header: '주문번호' },
  { key: 'amount', header: '결제금액', numeric: true },
  { key: 'captured', header: '매입금액', numeric: true },
  { key: 'balance', header: '잔액', numeric: true },
  { key: 'status', header: '상태' },
  { key: 'created', header: '생성시각' },
  { key: 'action', header: '' },
];

/** A captured payment with remaining balance can be refunded. */
function isRefundable(p: Payment): boolean {
  return (
    (p.state === 'CAPTURED' || p.state === 'PARTIAL_REFUNDED') && (p.balance ?? 0) > 0
  );
}

/**
 * Transactions / ledger. The catalog DataGrid renders the payment rows with
 * tabular numeric columns (amounts align). A refund action opens the catalog
 * ConfirmDialog (destructive tone) and posts a full refund of the remaining
 * balance via the live payment domain.
 */
export default function TransactionsPage() {
  const [page, setPage] = useState(0);
  const size = 20;
  const payments = usePayments(page, size);
  const refund = useRefundPayment();
  const [target, setTarget] = useState<Payment | null>(null);

  if (payments.isLoading) return <ScreenLoading label="거래 불러오는 중" />;
  if (payments.error) return <ScreenError error={payments.error as Error} />;

  const rows = payments.data?.content ?? [];
  const total = payments.data?.totalElements ?? 0;
  const totalPages = payments.data?.totalPages ?? 1;

  const handleConfirmRefund = async (): Promise<void> => {
    if (!target) return;
    try {
      // amount omitted -> backend refunds the full remaining captured balance.
      await refund.mutateAsync({ id: target.id, input: { reason: '운영자 환불' } });
      setTarget(null);
    } catch {
      // surfaced via refund.error below
    }
  };

  const gridRows = rows.map((p) => ({
    order: <span className="truncate">{p.orderId}</span>,
    amount: <span className="ax-money">{formatMajor(p.amount, p.currency)}</span>,
    captured: <span className="ax-money">{formatMajor(p.capturedAmount, p.currency)}</span>,
    balance: <span className="ax-money">{formatMajor(p.balance, p.currency)}</span>,
    status: <PaymentStatus state={p.state} />,
    created: <span className="text-muted-foreground">{formatDateTime(p.createdAt)}</span>,
    action: isRefundable(p) ? (
      <Button
        variant="outline"
        size="sm"
        onClick={() => setTarget(p)}
        aria-label={`${p.orderId} 환불`}
      >
        <RotateCcw aria-hidden /> 환불
      </Button>
    ) : (
      <span className="text-xs text-muted-foreground">—</span>
    ),
  }));

  return (
    <div className="space-y-5">
      <PageHeader
        title="거래 원장"
        description={`전체 ${total.toLocaleString('ko-KR')}건의 결제 거래. 금액은 자릿수 정렬로 표시됩니다.`}
      />

      {refund.error ? (
        <Alert variant="error">{(refund.error as Error).message}</Alert>
      ) : null}

      {rows.length === 0 ? (
        <ScreenEmpty
          icon={<ReceiptText className="h-7 w-7" />}
          title="거래가 없습니다"
          description="결제하기에서 첫 결제를 생성하면 여기에 표시됩니다."
        />
      ) : (
        <>
          <DataGrid<Col> caption="결제 거래 원장" columns={COLUMNS} rows={gridRows} />
          {totalPages > 1 ? (
            <div className="flex items-center justify-between">
              <Button
                variant="outline"
                size="sm"
                disabled={page === 0}
                onClick={() => setPage((p) => Math.max(0, p - 1))}
              >
                이전
              </Button>
              <span className="ax-money text-sm tabular-nums text-muted-foreground">
                {page + 1} / {totalPages}
              </span>
              <Button
                variant="outline"
                size="sm"
                disabled={page + 1 >= totalPages}
                onClick={() => setPage((p) => p + 1)}
              >
                다음
              </Button>
            </div>
          ) : null}
        </>
      )}

      <ConfirmDialog
        open={target !== null}
        onOpenChange={(open) => {
          if (!open) setTarget(null);
        }}
        tone="destructive"
        title="환불하시겠습니까?"
        description={
          target
            ? `주문 ${target.orderId}의 잔액 ${formatMajor(target.balance, target.currency)}을(를) 전액 환불합니다. 되돌릴 수 없습니다.`
            : undefined
        }
        confirmLabel="환불 실행"
        loading={refund.isPending}
        onConfirm={handleConfirmRefund}
      />
    </div>
  );
}
