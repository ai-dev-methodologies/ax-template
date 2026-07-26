'use client';

import React, { useMemo, useState } from 'react';
import { CheckCircle2, KeyRound, RefreshCw } from 'lucide-react';
import {
  Alert,
  Button,
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
  Field,
} from '@ax/ui';
import { PageHeader } from '@/components/page-header';
import { PaymentStatus } from '@/components/status';
import { useCreatePayment } from '@/features/payments/hooks';
import { newIdempotencyKey } from '@/lib/api/payFetch';
import type { Payment } from '@/lib/api/paymentClient';
import { formatMajor, formatMinor } from '@/lib/money';
import { formatDateTime, shortId } from '@/lib/format';

const CURRENCIES = ['KRW', 'USD'] as const;

/**
 * Checkout / new payment. Composes the catalog Field + Button + Card. The
 * defining money-safety detail: an Idempotency-Key is held in component state
 * and shown to the operator. Submitting the SAME attempt twice REPLAYS (the
 * backend returns the same payment, no double charge); "새 결제" rotates the key
 * for a genuinely new charge. The result + state render via the catalog
 * StatusBadge (PaymentStatus).
 */
export function CheckoutScreen() {
  const [amount, setAmount] = useState('12900');
  const [currency, setCurrency] = useState<(typeof CURRENCIES)[number]>('KRW');
  const [orderId, setOrderId] = useState(() => `order-${Date.now().toString().slice(-8)}`);
  const [idempotencyKey, setIdempotencyKey] = useState(() => newIdempotencyKey());
  const [result, setResult] = useState<Payment | null>(null);
  const [replayed, setReplayed] = useState(false);

  const create = useCreatePayment();

  const amountError = useMemo(() => {
    if (amount.trim() === '') return '금액을 입력하세요.';
    if (!/^\d+(\.\d+)?$/.test(amount.trim())) return '숫자만 입력하세요.';
    const n = Number(amount);
    if (n <= 0) return '0보다 큰 금액이어야 합니다.';
    if (currency === 'KRW' && !Number.isInteger(n)) return '원화(KRW)는 소수점을 사용할 수 없습니다.';
    return undefined;
  }, [amount, currency]);

  const handleSubmit = async (e: React.FormEvent): Promise<void> => {
    e.preventDefault();
    if (amountError) return;
    const wasReplay = result !== null && result.orderId === orderId;
    try {
      const payment = await create.mutateAsync({
        // amount as a STRING — the backend rejects JSON floats (see CreatePaymentInput).
        input: { amount: amount.trim(), currency, orderId, paymentMethodToken: 'tok_demo_card' },
        idempotencyKey,
      });
      setResult(payment);
      setReplayed(wasReplay);
    } catch {
      // surfaced via create.error below
    }
  };

  const resetForNewPayment = (): void => {
    setIdempotencyKey(newIdempotencyKey());
    setOrderId(`order-${Date.now().toString().slice(-8)}`);
    setResult(null);
    setReplayed(false);
    create.reset();
  };

  return (
    <div className="space-y-6">
      <PageHeader
        title="결제하기"
        description="멱등키(Idempotency-Key)로 중복 제출을 방지합니다. 같은 시도를 두 번 보내면 중복 청구가 아니라 재생(replay)됩니다."
      />

      <div className="grid grid-cols-1 gap-6 lg:grid-cols-2">
        <Card className="shadow-sm">
          <CardHeader>
            <CardTitle as="h2" className="text-lg">결제 정보</CardTitle>
            <CardDescription>토큰화된 결제수단으로만 처리합니다 (PAN 미수집).</CardDescription>
          </CardHeader>
          <CardContent>
            <form onSubmit={handleSubmit} className="space-y-4" noValidate>
              <div className="grid grid-cols-[1fr_auto] gap-3">
                <Field
                  id="amount"
                  label="금액"
                  inputMode="decimal"
                  value={amount}
                  onChange={(e) => setAmount(e.target.value)}
                  error={amountError}
                  hint={currency === 'KRW' ? '원 단위 정수' : '달러(소수 2자리)'}
                  required
                />
                <div className="space-y-1.5">
                  <label htmlFor="currency" className="text-sm font-medium text-foreground">
                    통화
                  </label>
                  <select
                    id="currency"
                    value={currency}
                    onChange={(e) => setCurrency(e.target.value as (typeof CURRENCIES)[number])}
                    className="h-10 rounded-[var(--radius)] border border-input bg-background px-3 text-sm focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
                  >
                    {CURRENCIES.map((c) => (
                      <option key={c} value={c}>
                        {c}
                      </option>
                    ))}
                  </select>
                </div>
              </div>

              <Field
                id="orderId"
                label="주문번호"
                value={orderId}
                onChange={(e) => setOrderId(e.target.value)}
                required
              />

              <div className="rounded-[var(--radius)] border border-border bg-secondary/50 px-3 py-2.5">
                <div className="flex items-center justify-between gap-2">
                  <span className="flex items-center gap-1.5 text-xs font-medium text-muted-foreground">
                    <KeyRound aria-hidden className="h-3.5 w-3.5" />
                    Idempotency-Key
                  </span>
                  <button
                    type="button"
                    onClick={() => setIdempotencyKey(newIdempotencyKey())}
                    className="rounded-[var(--radius)] text-xs text-[var(--ax-status-accent-fg)] hover:underline focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
                  >
                    키 새로 생성
                  </button>
                </div>
                <p className="ax-money mt-1 break-all font-mono text-xs text-foreground">
                  {idempotencyKey}
                </p>
              </div>

              {create.error ? (
                <Alert variant="error">{(create.error as Error).message}</Alert>
              ) : null}

              <div className="flex flex-wrap gap-3">
                <Button type="submit" loading={create.isPending} disabled={Boolean(amountError)}>
                  {formatMajor(amountError ? 0 : Number(amount), currency)} 결제
                </Button>
                <Button type="button" variant="outline" onClick={resetForNewPayment}>
                  <RefreshCw aria-hidden /> 새 결제
                </Button>
              </div>
              <p className="text-xs text-muted-foreground">
                같은 키로 다시 제출하면 동일 결제가 재생됩니다(중복 청구 없음).
              </p>
            </form>
          </CardContent>
        </Card>

        <Card className="shadow-sm">
          <CardHeader>
            <CardTitle as="h2" className="text-lg">결제 결과</CardTitle>
            <CardDescription>상태와 금액은 백엔드 결제 상태머신을 그대로 반영합니다.</CardDescription>
          </CardHeader>
          <CardContent>
            {result ? (
              <div className="space-y-4">
                {replayed ? (
                  <Alert variant="info">
                    멱등 재생(replay): 같은 키라 새로 청구하지 않고 기존 결제를 반환했습니다.
                  </Alert>
                ) : (
                  <Alert variant="success">
                    <CheckCircle2 aria-hidden className="h-4 w-4" /> 결제가 생성되었습니다.
                  </Alert>
                )}
                <dl className="divide-y divide-border rounded-[var(--radius)] border border-border">
                  <ResultRow label="상태" value={<PaymentStatus state={result.state} />} />
                  <ResultRow
                    label="결제금액"
                    value={
                      <span className="ax-money font-medium tabular-nums">
                        {formatMinor(result.amount, result.currency)}
                      </span>
                    }
                  />
                  <ResultRow
                    label="매입금액"
                    value={
                      <span className="ax-money tabular-nums">
                        {formatMinor(result.capturedAmount, result.currency)}
                      </span>
                    }
                  />
                  <ResultRow
                    label="잔액"
                    value={
                      <span className="ax-money tabular-nums">
                        {formatMinor(result.balance, result.currency)}
                      </span>
                    }
                  />
                  <ResultRow label="주문번호" value={result.orderId} />
                  <ResultRow
                    label="결제 ID"
                    value={<span className="font-mono text-xs">{shortId(result.id)}</span>}
                  />
                  <ResultRow label="생성시각" value={formatDateTime(result.createdAt)} />
                </dl>
              </div>
            ) : (
              <p className="py-10 text-center text-sm text-muted-foreground">
                아직 결제 결과가 없습니다. 왼쪽에서 결제를 생성하세요.
              </p>
            )}
          </CardContent>
        </Card>
      </div>
    </div>
  );
}

function ResultRow({ label, value }: { label: string; value: React.ReactNode }) {
  return (
    <div className="flex items-center justify-between gap-3 px-3.5 py-2.5">
      <dt className="text-sm text-muted-foreground">{label}</dt>
      <dd className="text-sm text-foreground">{value}</dd>
    </div>
  );
}
