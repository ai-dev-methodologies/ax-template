'use client';

import React, { useState } from 'react';
import { FileWarning } from 'lucide-react';
import { Button, Card, CardContent, CardHeader, CardTitle } from '@ax/ui';
import { GridStatus } from '@ax/blocks';
import { PageHeader } from '@/components/page-header';
import { ExchangeView } from '@/components/exchange-view';
import { useProblemTrigger } from '@/features/playground/hooks';
import type { ProblemKind } from '@/lib/api/problemClient';
import type { HttpExchange } from '@/lib/api/rawFetch';

interface Trigger {
  kind: ProblemKind;
  label: string;
  detail: string;
}

const TRIGGERS: Trigger[] = [
  { kind: 'insufficient-funds', label: '잔액 부족 (402)', detail: 'type · code · trace_id · balance · accounts 확장 멤버' },
  { kind: 'validate', label: '검증 실패 (400)', detail: 'errors[] 배열 + RFC 6901 포인터' },
  { kind: 'boom', label: '서버 오류 (500)', detail: 'detail에 스택 미노출 + trace_id 유지' },
];

/** Safely read a string-ish field from the parsed problem body. */
function field(body: unknown, key: string): string | undefined {
  if (body && typeof body === 'object' && key in body) {
    const value = (body as Record<string, unknown>)[key];
    if (value === null || value === undefined) return undefined;
    return typeof value === 'string' ? value : String(value);
  }
  return undefined;
}

export function ProblemsScreen() {
  const trigger = useProblemTrigger();
  const [exchange, setExchange] = useState<HttpExchange<unknown> | null>(null);

  const fire = async (kind: ProblemKind): Promise<void> => {
    const result = await trigger.mutateAsync(kind);
    setExchange(result);
  };

  const body = exchange?.response.body;
  const type = field(body, 'type');
  const status = field(body, 'status');
  const code = field(body, 'code');
  const traceId = field(body, 'trace_id');

  return (
    <div className="space-y-8">
      <PageHeader
        title="문제 응답 탐색기"
        endpoint="POST /api/problem-demo/{insufficient-funds,validate,boom}"
        description="모든 오류는 RFC 9457 application/problem+json 형식으로 반환됩니다. 버튼을 눌러 각 형태를 확인하세요."
      />

      <Card>
        <CardHeader>
          <CardTitle as="h2" className="flex items-center gap-2 text-base">
            <FileWarning aria-hidden className="h-4 w-4 text-[var(--ax-status-accent-fg)]" /> 트리거
          </CardTitle>
        </CardHeader>
        <CardContent>
          <div className="grid gap-3 sm:grid-cols-3">
            {TRIGGERS.map((t) => (
              <button
                key={t.kind}
                type="button"
                onClick={() => fire(t.kind)}
                disabled={trigger.isPending}
                className="flex flex-col items-start gap-1 rounded border border-border bg-secondary/40 p-3 text-left transition-colors hover:border-[var(--ax-status-accent-fg)] hover:bg-secondary/70 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring disabled:opacity-50"
              >
                <span className="text-sm font-semibold text-foreground">{t.label}</span>
                <span className="font-mono text-[0.7rem] text-muted-foreground">{t.detail}</span>
              </button>
            ))}
          </div>
        </CardContent>
      </Card>

      {exchange ? (
        <section aria-labelledby="problem-result-heading" className="space-y-4">
          <h2 id="problem-result-heading" className="text-base font-semibold text-foreground">
            problem+json 응답
          </h2>

          {/* Highlighted key members */}
          <div className="flex flex-wrap items-center gap-x-6 gap-y-2 rounded border border-border bg-secondary/30 px-4 py-3">
            {status ? (
              <span className="flex items-center gap-2">
                <span className="font-mono text-[0.65rem] uppercase tracking-wide text-muted-foreground">status</span>
                <GridStatus status={Number(status) >= 500 ? 'danger' : 'warning'}>{status}</GridStatus>
              </span>
            ) : null}
            {type ? (
              <span className="min-w-0">
                <span className="mr-2 font-mono text-[0.65rem] uppercase tracking-wide text-muted-foreground">type</span>
                <span className="break-all font-mono text-xs text-[var(--ax-status-accent-fg)]">{type}</span>
              </span>
            ) : null}
            {code ? (
              <span>
                <span className="mr-2 font-mono text-[0.65rem] uppercase tracking-wide text-muted-foreground">code</span>
                <span className="font-mono text-xs text-foreground">{code}</span>
              </span>
            ) : null}
            {traceId ? (
              <span className="min-w-0">
                <span className="mr-2 font-mono text-[0.65rem] uppercase tracking-wide text-muted-foreground">trace_id</span>
                <span className="break-all font-mono text-xs text-foreground">{traceId}</span>
              </span>
            ) : null}
          </div>

          <ExchangeView exchange={exchange} responseLabel="problem.json" />
        </section>
      ) : null}
    </div>
  );
}
