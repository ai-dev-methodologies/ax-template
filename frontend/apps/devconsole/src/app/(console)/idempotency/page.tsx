'use client';

import React, { useState } from 'react';
import { Repeat2, Zap } from 'lucide-react';
import { Alert, Button, Card, CardContent, CardHeader, CardTitle, Field } from '@ax/ui';
import { StatusBadge } from '@ax/blocks';
import { PageHeader } from '@/components/page-header';
import { ExchangeView } from '@/components/exchange-view';
import { useIdempotencyFire } from '@/features/playground/hooks';
import type { HttpExchange } from '@/lib/api/rawFetch';

interface FiredExchange {
  label: string;
  exchange: HttpExchange<unknown>;
}

function newKey(): string {
  return typeof crypto !== 'undefined' && 'randomUUID' in crypto
    ? crypto.randomUUID()
    : `key-${Date.now()}`;
}

/** Read the Idempotency-Replayed response header from a captured exchange. */
function replayedFlag(exchange: HttpExchange<unknown>): string | undefined {
  const headers = exchange.response.headers;
  return headers['idempotency-replayed'] ?? headers['Idempotency-Replayed'] ?? undefined;
}

export default function IdempotencyPage() {
  const fire = useIdempotencyFire();
  const [key, setKey] = useState(newKey());
  const [body, setBody] = useState('{"sku":"A1","qty":2}');
  const [results, setResults] = useState<FiredExchange[]>([]);
  const [parseError, setParseError] = useState<string | null>(null);

  const send = async (label: string, payload: unknown): Promise<void> => {
    const result = await fire.mutateAsync({ key, body: payload });
    setResults((prev) => [{ label, exchange: result }, ...prev]);
  };

  const parseBody = (): unknown | undefined => {
    try {
      const parsed: unknown = JSON.parse(body);
      setParseError(null);
      return parsed;
    } catch {
      setParseError('본문이 올바른 JSON이 아닙니다.');
      return undefined;
    }
  };

  const fireFirst = async (): Promise<void> => {
    const payload = parseBody();
    if (payload === undefined) return;
    await send('첫 요청 (first-seen)', payload);
  };

  const fireReplay = async (): Promise<void> => {
    const payload = parseBody();
    if (payload === undefined) return;
    await send('동일 키·본문 (replay)', payload);
  };

  const fireMismatch = async (): Promise<void> => {
    await send('동일 키·다른 본문 (422)', { sku: 'CHANGED', qty: 999 });
  };

  const resetKey = (): void => {
    setKey(newKey());
    setResults([]);
  };

  return (
    <div className="space-y-8">
      <PageHeader
        title="멱등성 플레이그라운드"
        endpoint="POST /api/idempotency-demo/resources (Idempotency-Key)"
        description="같은 Idempotency-Key로 동일 요청을 반복하면 캐시된 응답이 재생되고(Idempotency-Replayed: true), 본문이 달라지면 422로 거부됩니다."
      />

      <Card>
        <CardHeader>
          <CardTitle as="h2" className="flex items-center gap-2 text-base">
            <Repeat2 aria-hidden className="h-4 w-4 text-[var(--ax-status-accent-fg)]" /> 요청 구성
          </CardTitle>
        </CardHeader>
        <CardContent className="space-y-4">
          <div className="flex items-end gap-2">
            <Field
              id="idem-key"
              label="Idempotency-Key"
              className="flex-1"
              value={key}
              onChange={(e) => setKey(e.target.value)}
            />
            <Button type="button" variant="outline" size="sm" onClick={resetKey}>
              새 키
            </Button>
          </div>
          <Field
            id="idem-body"
            label="요청 본문 (JSON)"
            value={body}
            onChange={(e) => setBody(e.target.value)}
            error={parseError ?? undefined}
          />
          <div className="flex flex-wrap gap-2">
            <Button type="button" size="sm" loading={fire.isPending} onClick={fireFirst}>
              <Zap aria-hidden /> 첫 요청
            </Button>
            <Button type="button" variant="secondary" size="sm" loading={fire.isPending} onClick={fireReplay}>
              동일 키 재요청
            </Button>
            <Button type="button" variant="outline" size="sm" loading={fire.isPending} onClick={fireMismatch}>
              본문 변경 → 422
            </Button>
          </div>
          <Alert variant="info">
            먼저 “첫 요청”을 보낸 뒤, 같은 키로 “동일 키 재요청”과 “본문 변경”을 차례로 눌러 차이를 비교하세요.
          </Alert>
        </CardContent>
      </Card>

      {results.length === 0 ? null : (
        <section aria-labelledby="idem-results-heading" className="space-y-5">
          <h2 id="idem-results-heading" className="text-base font-semibold text-foreground">
            요청 기록 (최신순)
          </h2>
          {results.map((r, i) => {
            const replayed = replayedFlag(r.exchange);
            return (
              <div key={`${r.label}-${i}`} className="space-y-2">
                <div className="flex flex-wrap items-center gap-2">
                  <span className="text-sm font-medium text-foreground">{r.label}</span>
                  {replayed === 'true' ? (
                    <StatusBadge status="submitted" label="REPLAYED" />
                  ) : replayed === 'false' ? (
                    <StatusBadge status="success" label="FIRST-SEEN" />
                  ) : null}
                </div>
                <ExchangeView exchange={r.exchange} />
              </div>
            );
          })}
        </section>
      )}
    </div>
  );
}
