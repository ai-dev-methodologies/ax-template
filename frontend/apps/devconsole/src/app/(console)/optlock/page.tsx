'use client';

import React, { useState } from 'react';
import { GitCompareArrows } from 'lucide-react';
import { Alert, Button, Card, CardContent, CardHeader, CardTitle } from '@ax/ui';
import { StatusBadge } from '@ax/blocks';
import { PageHeader } from '@/components/page-header';
import { ExchangeView } from '@/components/exchange-view';
import { ErrorState } from '@/components/screen-states';
import {
  useOptlockCreate,
  useOptlockGet,
  useOptlockUpdate,
} from '@/features/optlock/hooks';
import { readEtag } from '@/lib/api/optlockClient';
import type { HttpExchange } from '@/lib/api/rawFetch';

interface Step {
  label: string;
  exchange: HttpExchange<unknown>;
}

export default function OptlockPage() {
  const createRes = useOptlockCreate();
  const getRes = useOptlockGet();
  const updateRes = useOptlockUpdate();

  const [resourceId, setResourceId] = useState<string | null>(null);
  const [currentEtag, setCurrentEtag] = useState<string | null>(null);
  const [staleEtag, setStaleEtag] = useState<string | null>(null);
  const [steps, setSteps] = useState<Step[]>([]);

  const push = (label: string, exchange: HttpExchange<unknown>): void => {
    setSteps((prev) => [...prev, { label, exchange }]);
  };

  const stepCreate = async (): Promise<void> => {
    const result = await createRes.mutateAsync({ name: 'widget', quantity: 5 });
    const etag = readEtag(result) ?? null;
    setResourceId(result.data.id);
    setCurrentEtag(etag);
    setStaleEtag(etag); // capture the v0 ETag as the future "stale" validator
    setSteps([{ label: '1. 생성 (POST → 201 + ETag)', exchange: result }]);
  };

  const stepGet = async (): Promise<void> => {
    if (!resourceId) return;
    const result = await getRes.mutateAsync(resourceId);
    setCurrentEtag((curr) => readEtag(result) ?? curr);
    push('2. 조회 (GET → 200 + ETag)', result);
  };

  const stepUpdateOk = async (): Promise<void> => {
    if (!resourceId || !currentEtag) return;
    const result = await updateRes.mutateAsync({
      id: resourceId,
      body: { name: 'widget-v2', quantity: 6 },
      ifMatch: currentEtag,
    });
    setCurrentEtag((curr) => readEtag(result) ?? curr);
    push('3. 수정 (PUT If-Match 일치 → 200 + 새 ETag)', result);
  };

  const stepUpdateStale = async (): Promise<void> => {
    if (!resourceId || !staleEtag) return;
    const result = await updateRes.mutateAsync({
      id: resourceId,
      body: { name: 'widget-v3', quantity: 7 },
      ifMatch: staleEtag,
    });
    push('4. 수정 (PUT 오래된 If-Match → 412)', result);
  };

  const stepUpdateMissing = async (): Promise<void> => {
    if (!resourceId) return;
    const result = await updateRes.mutateAsync({
      id: resourceId,
      body: { name: 'widget-x', quantity: 9 },
    });
    push('수정 (PUT If-Match 없음 → 428)', result);
  };

  const reset = (): void => {
    setResourceId(null);
    setCurrentEtag(null);
    setStaleEtag(null);
    setSteps([]);
  };

  const hasResource = resourceId !== null;

  return (
    <div className="space-y-8">
      <PageHeader
        title="낙관적 잠금 데모"
        endpoint="GET · PUT /api/optlock/resources (ETag · If-Match)"
        description="강한 ETag와 If-Match 조건부 요청으로 잃어버린 갱신을 방지합니다. 단계별로 실행해 428 / 412 흐름을 확인하세요."
      />

      <Card>
        <CardHeader>
          <CardTitle as="h2" className="flex items-center gap-2 text-base">
            <GitCompareArrows aria-hidden className="h-4 w-4 text-[var(--ax-status-accent-fg)]" /> 조건부 요청 흐름
          </CardTitle>
        </CardHeader>
        <CardContent className="space-y-4">
          <div className="flex flex-wrap items-center gap-3 text-xs">
            <span className="flex items-center gap-2">
              <span className="font-mono uppercase tracking-wide text-muted-foreground">현재 ETag</span>
              {currentEtag ? (
                <StatusBadge status="success" label={currentEtag} />
              ) : (
                <span className="font-mono text-muted-foreground">—</span>
              )}
            </span>
          </div>

          <div className="flex flex-wrap gap-2">
            <Button type="button" size="sm" loading={createRes.isPending} onClick={stepCreate}>
              1. 생성
            </Button>
            <Button type="button" variant="secondary" size="sm" disabled={!hasResource} loading={getRes.isPending} onClick={stepGet}>
              2. 조회
            </Button>
            <Button type="button" variant="secondary" size="sm" disabled={!hasResource || !currentEtag} loading={updateRes.isPending} onClick={stepUpdateOk}>
              3. 수정 (일치)
            </Button>
            <Button type="button" variant="outline" size="sm" disabled={!hasResource || !staleEtag} loading={updateRes.isPending} onClick={stepUpdateStale}>
              4. 오래된 ETag → 412
            </Button>
            <Button type="button" variant="outline" size="sm" disabled={!hasResource} loading={updateRes.isPending} onClick={stepUpdateMissing}>
              If-Match 없음 → 428
            </Button>
            <Button type="button" variant="ghost" size="sm" onClick={reset}>
              초기화
            </Button>
          </div>

          <Alert variant="info">
            1 → 2 → 3 순서로 실행한 뒤 “3. 수정(일치)”로 ETag가 바뀌면, “4. 오래된 ETag”가 412를 반환하는 것을 확인하세요.
          </Alert>
        </CardContent>
      </Card>

      {createRes.isError ? <ErrorState error={createRes.error} /> : null}

      {steps.length === 0 ? null : (
        <section aria-labelledby="optlock-steps-heading" className="space-y-5">
          <h2 id="optlock-steps-heading" className="text-base font-semibold text-foreground">
            단계별 HTTP 교환
          </h2>
          {steps.map((s, i) => (
            <div key={`${s.label}-${i}`} className="space-y-2">
              <span className="text-sm font-medium text-foreground">{s.label}</span>
              <ExchangeView exchange={s.exchange} />
            </div>
          ))}
        </section>
      )}
    </div>
  );
}
