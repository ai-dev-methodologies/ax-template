'use client';

import React, { useState } from 'react';
import { Check, Repeat } from 'lucide-react';
import { Alert, Button, ConfirmDialog } from '@ax/ui';
import {
  InterfacesCard,
  InterfacesCardContent,
  InterfacesCardDescription,
  InterfacesCardHeader,
  InterfacesCardTitle,
} from '@ax/blocks';
import { PageHeader } from '@/components/page-header';
import { SubscriptionStatusBadge } from '@/components/status';
import { ScreenEmpty, ScreenError, ScreenLoading } from '@/components/screen-states';
import {
  useCancelSubscription,
  usePlans,
  useSubscribe,
  useSubscriptions,
} from '@/features/billing/hooks';
import type { Plan, Subscription } from '@/lib/api/billingClient';
import { formatMinor } from '@/lib/money';
import { formatDate } from '@/lib/format';

const CYCLE_LABEL: Record<Plan['billingCycle'], string> = {
  MONTHLY: '월간',
  YEARLY: '연간',
};

/**
 * Subscriptions / plans. Lists the admin-managed plans (subscribe) and the
 * user's subscriptions with the billing state machine (cancel -> CANCELLED).
 * Subscription / plan amounts are MINOR units (formatMinor). Status renders via
 * the catalog StatusBadge (SubscriptionStatusBadge). Cards compose the catalog
 * InterfacesCard family; the cancel confirmation uses the catalog ConfirmDialog.
 */
export default function SubscriptionsPage() {
  const plans = usePlans();
  const subs = useSubscriptions();
  const subscribe = useSubscribe();
  const cancel = useCancelSubscription();
  const [cancelTarget, setCancelTarget] = useState<Subscription | null>(null);

  if (plans.isLoading || subs.isLoading) return <ScreenLoading label="구독 정보 불러오는 중" />;
  if (plans.error) return <ScreenError error={plans.error as Error} />;
  if (subs.error) return <ScreenError error={subs.error as Error} />;

  const planList = plans.data ?? [];
  const subList = subs.data?.items ?? [];

  const handleSubscribe = async (plan: Plan): Promise<void> => {
    try {
      await subscribe.mutateAsync({ planId: plan.id, provider: 'toss' });
    } catch {
      // surfaced via subscribe.error
    }
  };

  const handleCancel = async (): Promise<void> => {
    if (!cancelTarget) return;
    try {
      await cancel.mutateAsync({ id: cancelTarget.id });
      setCancelTarget(null);
    } catch {
      // surfaced via cancel.error
    }
  };

  return (
    <div className="space-y-8">
      <PageHeader
        title="구독 · 요금제"
        description="요금제를 선택해 구독하고, 구독 상태(체험중 · 구독중 · 연체 · 해지됨)를 관리합니다."
      />

      {subscribe.error ? (
        <Alert variant="error">{(subscribe.error as Error).message}</Alert>
      ) : null}
      {cancel.error ? <Alert variant="error">{(cancel.error as Error).message}</Alert> : null}

      <section aria-label="요금제" className="space-y-3">
        <h2 className="text-sm font-semibold uppercase tracking-wide text-muted-foreground">
          요금제
        </h2>
        {planList.length === 0 ? (
          <ScreenEmpty
            icon={<Repeat className="h-7 w-7" />}
            title="등록된 요금제가 없습니다"
            description="관리자(ADMIN)가 요금제를 생성하면 여기에 표시됩니다."
          />
        ) : (
          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
            {planList.map((plan) => (
              <InterfacesCard key={plan.id} className="flex flex-col shadow-sm">
                <InterfacesCardHeader>
                  <InterfacesCardTitle>{plan.name}</InterfacesCardTitle>
                  <InterfacesCardDescription>{CYCLE_LABEL[plan.billingCycle]} 요금제</InterfacesCardDescription>
                </InterfacesCardHeader>
                <InterfacesCardContent className="flex flex-1 flex-col justify-between gap-4">
                  <p className="ax-money text-2xl font-semibold tracking-tight text-foreground">
                    {formatMinor(plan.amount, plan.currency)}
                    <span className="ml-1 text-sm font-normal text-muted-foreground">
                      / {CYCLE_LABEL[plan.billingCycle]}
                    </span>
                  </p>
                  <Button
                    onClick={() => handleSubscribe(plan)}
                    loading={subscribe.isPending}
                    className="w-full"
                  >
                    <Check aria-hidden /> 구독하기
                  </Button>
                </InterfacesCardContent>
              </InterfacesCard>
            ))}
          </div>
        )}
      </section>

      <section aria-label="내 구독" className="space-y-3">
        <h2 className="text-sm font-semibold uppercase tracking-wide text-muted-foreground">
          내 구독
        </h2>
        {subList.length === 0 ? (
          <ScreenEmpty title="구독 내역이 없습니다" description="위 요금제에서 구독을 시작하세요." />
        ) : (
          <ul className="divide-y divide-border rounded-[var(--radius)] border border-border bg-card shadow-sm">
            {subList.map((sub) => (
              <li
                key={sub.id}
                className="flex flex-wrap items-center justify-between gap-3 px-4 py-3"
              >
                <div className="min-w-0 space-y-0.5">
                  <div className="flex items-center gap-2">
                    <SubscriptionStatusBadge status={sub.status} />
                    <span className="ax-money text-sm font-medium tabular-nums text-foreground">
                      {formatMinor(sub.amount, sub.currency)}
                    </span>
                  </div>
                  <p className="text-xs text-muted-foreground">
                    {sub.provider} · 시작 {formatDate(sub.startedAt)}
                  </p>
                </div>
                {sub.status !== 'CANCELLED' ? (
                  <Button variant="outline" size="sm" onClick={() => setCancelTarget(sub)}>
                    해지
                  </Button>
                ) : (
                  <span className="text-xs text-muted-foreground">해지됨</span>
                )}
              </li>
            ))}
          </ul>
        )}
      </section>

      <ConfirmDialog
        open={cancelTarget !== null}
        onOpenChange={(open) => {
          if (!open) setCancelTarget(null);
        }}
        tone="destructive"
        title="구독을 해지하시겠습니까?"
        description="해지 시 구독 상태가 '해지됨'으로 변경되며 되돌릴 수 없습니다."
        confirmLabel="구독 해지"
        loading={cancel.isPending}
        onConfirm={handleCancel}
      />
    </div>
  );
}
