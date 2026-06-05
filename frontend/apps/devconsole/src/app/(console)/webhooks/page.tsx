'use client';

import React, { useState } from 'react';
import { Trash2, Webhook } from 'lucide-react';
import {
  Alert,
  Button,
  Card,
  CardContent,
  CardHeader,
  CardTitle,
  ConfirmDialog,
  Field,
} from '@ax/ui';
import {
  CodeSnippet,
  DataGrid,
  GridStatus,
  StatusBadge,
  type DataGridColumn,
  type DataGridStatus,
} from '@ax/blocks';
import { PageHeader } from '@/components/page-header';
import { ExchangeView } from '@/components/exchange-view';
import { EmptyState, ErrorState, LoadingState } from '@/components/screen-states';
import {
  useDeleteWebhook,
  useRegisterWebhook,
  useWebhookDeliveries,
  useWebhookEndpoints,
} from '@/features/webhooks/hooks';
import type {
  WebhookDeliveryStatus,
  WebhookEndpoint,
  WebhookEndpointWithSecret,
} from '@/lib/api/webhookClient';
import type { HttpExchange } from '@/lib/api/rawFetch';

type EndpointColumn = 'url' | 'event' | 'active' | 'created' | 'actions';
type DeliveryColumn = 'event' | 'status' | 'attempts' | 'code' | 'lastError';

const ENDPOINT_COLUMNS: ReadonlyArray<DataGridColumn<EndpointColumn>> = [
  { key: 'url', header: 'URL' },
  { key: 'event', header: '이벤트 필터' },
  { key: 'active', header: '활성' },
  { key: 'created', header: '생성' },
  { key: 'actions', header: '' },
];

const DELIVERY_COLUMNS: ReadonlyArray<DataGridColumn<DeliveryColumn>> = [
  { key: 'event', header: '이벤트' },
  { key: 'status', header: '상태' },
  { key: 'attempts', header: '시도', numeric: true },
  { key: 'code', header: '응답코드', numeric: true },
  { key: 'lastError', header: '마지막 오류' },
];

const DELIVERY_STATUS_MAP: Record<WebhookDeliveryStatus, DataGridStatus> = {
  PENDING: 'info',
  PENDING_RETRY: 'warning',
  SUCCEEDED: 'success',
  FAILED_PERMANENT: 'danger',
};

/** An illustrative signed payload + HMAC header, built from the live signing secret. */
function signedExample(secret: string): string {
  return [
    'POST https://example.com/hooks/devconsole HTTP/1.1',
    'Content-Type: application/json',
    'X-Webhook-Id: 6f1c…',
    'X-Webhook-Timestamp: 1780660852',
    `X-Webhook-Signature: sha256=HMAC_SHA256(secret, timestamp + "." + body)`,
    '',
    '{',
    '  "event": "order.created",',
    '  "data": { "orderId": "ord_123", "amount": 42.5 }',
    '}',
    '',
    `# signing secret (이번만 표시): ${secret}`,
  ].join('\n');
}

export default function WebhooksPage() {
  const endpoints = useWebhookEndpoints();
  const deliveries = useWebhookDeliveries('FAILED_PERMANENT');
  const register = useRegisterWebhook();
  const remove = useDeleteWebhook();

  const [url, setUrl] = useState('');
  const [eventFilter, setEventFilter] = useState('');
  const [created, setCreated] = useState<WebhookEndpointWithSecret | null>(null);
  const [lastExchange, setLastExchange] = useState<HttpExchange<unknown> | null>(null);
  const [deleteTarget, setDeleteTarget] = useState<WebhookEndpoint | null>(null);

  const handleRegister = async (e: React.FormEvent): Promise<void> => {
    e.preventDefault();
    if (url.trim().length === 0) return;
    try {
      const result = await register.mutateAsync({
        url: url.trim(),
        eventFilter: eventFilter.trim() || undefined,
      });
      setCreated(result.data);
      setLastExchange(result);
      setUrl('');
      setEventFilter('');
    } catch (err) {
      setLastExchange((err as { exchange?: HttpExchange<unknown> }).exchange ?? null);
    }
  };

  const handleDelete = async (): Promise<void> => {
    if (!deleteTarget) return;
    try {
      const result = await remove.mutateAsync(deleteTarget.id);
      setLastExchange(result);
    } catch (err) {
      setLastExchange((err as { exchange?: HttpExchange<unknown> }).exchange ?? null);
    } finally {
      setDeleteTarget(null);
    }
  };

  const endpointItems = endpoints.data?.data ?? [];
  const endpointRows = endpointItems.map((ep) => ({
    url: <span className="font-mono text-xs">{ep.url}</span>,
    event: (
      <span className="font-mono text-[0.7rem] text-muted-foreground">{ep.eventFilter ?? '*'}</span>
    ),
    active: ep.active ? (
      <GridStatus status="success">ON</GridStatus>
    ) : (
      <GridStatus status="neutral">OFF</GridStatus>
    ),
    created: <span className="font-mono text-[0.7rem] text-muted-foreground">{ep.createdAt}</span>,
    actions: (
      <span className="flex justify-end">
        <Button variant="ghost" size="sm" onClick={() => setDeleteTarget(ep)}>
          <Trash2 aria-hidden className="text-[var(--ax-status-danger-fg)]" />
          <span className="sr-only sm:not-sr-only">삭제</span>
        </Button>
      </span>
    ),
  }));

  const deliveryItems = deliveries.data?.data ?? [];
  const deliveryRows = deliveryItems.map((d) => ({
    event: <span className="font-mono text-xs">{d.eventType}</span>,
    status: <GridStatus status={DELIVERY_STATUS_MAP[d.status]}>{d.status}</GridStatus>,
    attempts: d.attemptCount,
    code: d.lastResponseCode ?? '—',
    lastError: (
      <span className="font-mono text-[0.7rem] text-muted-foreground">{d.lastError ?? '—'}</span>
    ),
  }));

  return (
    <div className="space-y-8">
      <PageHeader
        title="웹훅"
        endpoint="/api/admin/webhook-endpoints · /api/admin/webhook-deliveries"
        description="ADMIN 전용. 서명 시크릿은 등록 시 단 한 번만 반환되며, 전달 로그는 영구 실패(dead-letter)건을 보관합니다."
      />

      {/* Register form */}
      <Card>
        <CardHeader>
          <CardTitle as="h2" className="flex items-center gap-2 text-base">
            <Webhook aria-hidden className="h-4 w-4 text-[var(--ax-status-accent-fg)]" /> 엔드포인트 등록
          </CardTitle>
        </CardHeader>
        <CardContent>
          <form onSubmit={handleRegister} className="space-y-4">
            <Field
              id="webhook-url"
              label="콜백 URL"
              type="url"
              placeholder="https://example.com/hooks/orders"
              value={url}
              onChange={(e) => setUrl(e.target.value)}
              required
            />
            <Field
              id="webhook-event"
              label="이벤트 필터 (선택)"
              placeholder="order.created"
              value={eventFilter}
              onChange={(e) => setEventFilter(e.target.value)}
              hint="비워두면 모든 이벤트를 수신합니다."
            />
            <Button type="submit" size="sm" loading={register.isPending}>
              등록
            </Button>
          </form>
        </CardContent>
      </Card>

      {/* One-time signing secret + signed payload example */}
      {created ? (
        <Card>
          <CardHeader>
            <CardTitle as="h2" className="flex items-center gap-2 text-base">
              <StatusBadge status="success" label="ONE-TIME" /> 서명 시크릿 + 예시 페이로드
            </CardTitle>
          </CardHeader>
          <CardContent className="space-y-3">
            <Alert variant="info">
              서명 시크릿은 지금만 표시됩니다. 이 값으로 수신 측에서 HMAC 서명을 검증하세요.
            </Alert>
            <div className="ax-code-pane">
              <CodeSnippet
                code={signedExample(created.signingSecret)}
                language="http"
                filename="signed-delivery.http"
              />
            </div>
          </CardContent>
        </Card>
      ) : null}

      {/* Endpoints list */}
      <section aria-labelledby="webhook-endpoints-heading" className="space-y-3">
        <h2 id="webhook-endpoints-heading" className="text-base font-semibold text-foreground">
          등록된 엔드포인트
        </h2>
        {endpoints.isLoading ? (
          <LoadingState />
        ) : endpoints.isError ? (
          <ErrorState error={endpoints.error} />
        ) : endpointRows.length === 0 ? (
          <EmptyState>아직 웹훅 엔드포인트가 없습니다.</EmptyState>
        ) : (
          <DataGrid<EndpointColumn>
            caption="웹훅 엔드포인트"
            columns={ENDPOINT_COLUMNS}
            rows={endpointRows}
          />
        )}
      </section>

      {/* Delivery / attempt log */}
      <section aria-labelledby="webhook-deliveries-heading" className="space-y-3">
        <h2 id="webhook-deliveries-heading" className="text-base font-semibold text-foreground">
          전달 로그 (영구 실패)
        </h2>
        {deliveries.isLoading ? (
          <LoadingState />
        ) : deliveries.isError ? (
          <ErrorState error={deliveries.error} />
        ) : deliveryRows.length === 0 ? (
          <EmptyState>dead-letter 전달 기록이 없습니다.</EmptyState>
        ) : (
          <DataGrid<DeliveryColumn>
            caption="dead-letter 전달 기록"
            columns={DELIVERY_COLUMNS}
            rows={deliveryRows}
          />
        )}
      </section>

      {/* Live HTTP exchange */}
      {lastExchange ? (
        <section aria-labelledby="webhook-http-heading" className="space-y-3">
          <h2 id="webhook-http-heading" className="text-base font-semibold text-foreground">
            마지막 HTTP 교환
          </h2>
          <ExchangeView exchange={lastExchange} />
        </section>
      ) : null}

      <ConfirmDialog
        open={deleteTarget !== null}
        onOpenChange={(open) => !open && setDeleteTarget(null)}
        title="엔드포인트 삭제"
        description={
          deleteTarget ? `'${deleteTarget.url}' 엔드포인트를 삭제합니다.` : undefined
        }
        confirmLabel="삭제"
        tone="destructive"
        loading={remove.isPending}
        onConfirm={handleDelete}
      />
    </div>
  );
}
