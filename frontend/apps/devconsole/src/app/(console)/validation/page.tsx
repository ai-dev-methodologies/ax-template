'use client';

import React, { useState } from 'react';
import { ShieldCheck } from 'lucide-react';
import { Alert, Button, Card, CardContent, CardHeader, CardTitle } from '@ax/ui';
import { StatusBadge } from '@ax/blocks';
import { PageHeader } from '@/components/page-header';
import { ExchangeView } from '@/components/exchange-view';
import { useValidationSubmit } from '@/features/playground/hooks';
import type { CreateOrderInput } from '@/lib/api/requestValidationClient';
import type { HttpExchange } from '@/lib/api/rawFetch';

interface Submission {
  label: string;
  exchange: HttpExchange<unknown>;
}

// A deliberately-malformed body: blank customer, negative amount, inverted date
// range, bad postal code + blank city, blank sku + zero quantity — exercises the
// full errors[] array (schema + nested + collection + cross-field).
const MALFORMED_BODY: CreateOrderInput = {
  customer: '',
  amount: -5,
  priority: 'HIGH',
  startDate: '2026-06-10',
  endDate: '2026-06-01',
  address: { postalCode: '12', city: '' },
  items: [{ sku: '', quantity: 0 }],
};

// A valid body — the response echoes the normalized customer (collapsed spaces).
const VALID_BODY: CreateOrderInput = {
  customer: '  Ada  Lovelace ',
  amount: 42.5,
  priority: 'HIGH',
  startDate: '2026-06-01',
  endDate: '2026-06-10',
  address: { postalCode: '12345', city: 'Seoul' },
  items: [{ sku: 'SKU-1', quantity: 2 }],
};

/** Count the errors[] entries in a captured problem body (if any). */
function errorCount(exchange: HttpExchange<unknown>): number | undefined {
  const body = exchange.response.body;
  if (body && typeof body === 'object' && 'errors' in body) {
    const errors = (body as { errors: unknown }).errors;
    if (Array.isArray(errors)) return errors.length;
  }
  return undefined;
}

export default function ValidationPage() {
  const submit = useValidationSubmit();
  const [submissions, setSubmissions] = useState<Submission[]>([]);

  const fire = async (label: string, body: CreateOrderInput): Promise<void> => {
    const result = await submit.mutateAsync(body);
    setSubmissions((prev) => [{ label, exchange: result }, ...prev]);
  };

  return (
    <div className="space-y-8">
      <PageHeader
        title="요청 검증 플레이그라운드"
        endpoint="POST /api/request-validation/orders"
        description="잘못된 본문은 모든 위반을 한 번에 errors[] 배열(RFC 6901 포인터 포함)로 반환하고, 올바른 본문은 정규화된 결과와 함께 201을 반환합니다."
      />

      <Card>
        <CardHeader>
          <CardTitle as="h2" className="flex items-center gap-2 text-base">
            <ShieldCheck aria-hidden className="h-4 w-4 text-[var(--ax-status-accent-fg)]" /> 본문 전송
          </CardTitle>
        </CardHeader>
        <CardContent className="space-y-4">
          <div className="flex flex-wrap gap-2">
            <Button
              type="button"
              variant="outline"
              size="sm"
              loading={submit.isPending}
              onClick={() => fire('잘못된 본문 → 400 errors[]', MALFORMED_BODY)}
            >
              잘못된 본문 전송
            </Button>
            <Button
              type="button"
              size="sm"
              loading={submit.isPending}
              onClick={() => fire('올바른 본문 → 201', VALID_BODY)}
            >
              올바른 본문 전송
            </Button>
          </div>
          <Alert variant="info">
            잘못된 본문은 schema · nested · collection · cross-field 위반을 한 응답에 모두 담습니다 (fail-fast 아님).
          </Alert>
        </CardContent>
      </Card>

      {submissions.length === 0 ? null : (
        <section aria-labelledby="validation-results-heading" className="space-y-5">
          <h2 id="validation-results-heading" className="text-base font-semibold text-foreground">
            전송 기록 (최신순)
          </h2>
          {submissions.map((s, i) => {
            const count = errorCount(s.exchange);
            const ok = s.exchange.response.status >= 200 && s.exchange.response.status < 300;
            return (
              <div key={`${s.label}-${i}`} className="space-y-2">
                <div className="flex flex-wrap items-center gap-2">
                  <span className="text-sm font-medium text-foreground">{s.label}</span>
                  {ok ? (
                    <StatusBadge status="success" label="CREATED" />
                  ) : count !== undefined ? (
                    <StatusBadge status="failed" label={`${count} ERRORS`} />
                  ) : null}
                </div>
                <ExchangeView exchange={s.exchange} responseLabel={ok ? 'response.json' : 'problem.json'} />
              </div>
            );
          })}
        </section>
      )}
    </div>
  );
}
