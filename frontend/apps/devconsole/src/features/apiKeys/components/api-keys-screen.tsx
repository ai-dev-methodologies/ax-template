'use client';

import React, { useState } from 'react';
import { KeyRound, RotateCw, Trash2 } from 'lucide-react';
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
} from '@ax/blocks';
import { PageHeader } from '@/components/page-header';
import { ExchangeView } from '@/components/exchange-view';
import { EmptyState, ErrorState, LoadingState } from '@/components/screen-states';
import {
  useApiKeys,
  useCreateApiKey,
  useRevokeApiKey,
  useRotateApiKey,
} from '@/features/apiKeys/hooks';
import { errorMessage } from '@/lib/errors';
import type { ApiKey, ApiKeyScope, ApiKeySecret } from '@/lib/api/apiKeyClient';
import type { HttpExchange } from '@/lib/api/rawFetch';

type KeyColumn = 'prefix' | 'name' | 'scopes' | 'status' | 'lastUsed' | 'actions';

const COLUMNS: ReadonlyArray<DataGridColumn<KeyColumn>> = [
  { key: 'prefix', header: 'Prefix' },
  { key: 'name', header: '이름' },
  { key: 'scopes', header: '스코프' },
  { key: 'status', header: '상태' },
  { key: 'lastUsed', header: '마지막 사용' },
  { key: 'actions', header: '' },
];

const SCOPE_OPTIONS: ApiKeyScope[] = ['READ', 'WRITE'];

export function ApiKeysScreen() {
  const keys = useApiKeys();
  const createKey = useCreateApiKey();
  const rotateKey = useRotateApiKey();
  const revokeKey = useRevokeApiKey();

  const [name, setName] = useState('');
  const [scopes, setScopes] = useState<ApiKeyScope[]>(['READ']);
  const [secret, setSecret] = useState<ApiKeySecret | null>(null);
  const [lastExchange, setLastExchange] = useState<HttpExchange<unknown> | null>(null);
  const [revokeTarget, setRevokeTarget] = useState<ApiKey | null>(null);

  const toggleScope = (scope: ApiKeyScope): void => {
    setScopes((prev) =>
      prev.includes(scope) ? prev.filter((s) => s !== scope) : [...prev, scope],
    );
  };

  const handleCreate = async (e: React.FormEvent): Promise<void> => {
    e.preventDefault();
    if (scopes.length === 0) return;
    try {
      const result = await createKey.mutateAsync({ name: name.trim() || 'unnamed-key', scopes });
      setSecret(result.data);
      setLastExchange(result);
      setName('');
    } catch (err) {
      setLastExchange((err as { exchange?: HttpExchange<unknown> }).exchange ?? null);
    }
  };

  const handleRotate = async (id: string): Promise<void> => {
    try {
      const result = await rotateKey.mutateAsync(id);
      setSecret(result.data);
      setLastExchange(result);
    } catch (err) {
      setLastExchange((err as { exchange?: HttpExchange<unknown> }).exchange ?? null);
    }
  };

  const handleRevoke = async (): Promise<void> => {
    if (!revokeTarget) return;
    try {
      const result = await revokeKey.mutateAsync(revokeTarget.id);
      setLastExchange(result);
    } catch (err) {
      setLastExchange((err as { exchange?: HttpExchange<unknown> }).exchange ?? null);
    } finally {
      setRevokeTarget(null);
    }
  };

  // Build a Set once so the scope-toggle render does not call Array.includes per
  // option (ax/no-array-includes-in-loop).
  const selectedScopes = new Set<ApiKeyScope>(scopes);

  const items = keys.data?.data.items ?? [];
  const rows = items.map((key) => ({
      prefix: <span className="font-mono text-xs">{key.prefix}</span>,
      name: key.name,
      scopes: (
        <span className="font-mono text-[0.7rem] text-muted-foreground">
          {key.scopes.join(' · ')}
        </span>
      ),
      status:
        key.status === 'ACTIVE' ? (
          <GridStatus status="success">ACTIVE</GridStatus>
        ) : (
          <GridStatus status="neutral">REVOKED</GridStatus>
        ),
      lastUsed: (
        <span className="font-mono text-[0.7rem] text-muted-foreground">
          {key.lastUsedAt ?? '—'}
        </span>
      ),
      actions: (
        <span className="flex justify-end gap-1.5">
          <Button
            variant="outline"
            size="sm"
            onClick={() => handleRotate(key.id)}
            disabled={key.status !== 'ACTIVE' || rotateKey.isPending}
          >
            <RotateCw aria-hidden />
            <span className="sr-only sm:not-sr-only">회전</span>
          </Button>
          <Button
            variant="ghost"
            size="sm"
            onClick={() => setRevokeTarget(key)}
            disabled={key.status !== 'ACTIVE'}
          >
            <Trash2 aria-hidden className="text-[var(--ax-status-danger-fg)]" />
            <span className="sr-only sm:not-sr-only">폐기</span>
          </Button>
        </span>
      ),
  }));

  return (
    <div className="space-y-8">
      <PageHeader
        title="API 키"
        endpoint="GET · POST · DELETE /api/api-keys"
        description="JWT로 인증된 사용자별 키. 평문 시크릿은 생성·회전 시 단 한 번만 반환됩니다 (이후 GET에는 prefix만)."
      />

      {/* Create form */}
      <Card>
        <CardHeader>
          <CardTitle as="h2" className="flex items-center gap-2 text-base">
            <KeyRound aria-hidden className="h-4 w-4 text-[var(--ax-status-accent-fg)]" />새 API 키 발급
          </CardTitle>
        </CardHeader>
        <CardContent>
          <form onSubmit={handleCreate} className="grid gap-4 sm:grid-cols-[1fr_auto] sm:items-end">
            <Field
              id="key-name"
              label="키 이름"
              placeholder="ci-pipeline"
              value={name}
              onChange={(e) => setName(e.target.value)}
              hint="스코프를 하나 이상 선택하세요."
            />
            <div className="space-y-1.5">
              <span className="block text-sm font-medium text-foreground">스코프</span>
              <div className="flex gap-2">
                {SCOPE_OPTIONS.map((scope) => {
                  const active = selectedScopes.has(scope);
                  return (
                    <Button
                      key={scope}
                      type="button"
                      variant={active ? 'default' : 'outline'}
                      size="sm"
                      aria-pressed={active}
                      onClick={() => toggleScope(scope)}
                    >
                      {scope}
                    </Button>
                  );
                })}
                <Button type="submit" size="sm" loading={createKey.isPending} disabled={scopes.length === 0}>
                  발급
                </Button>
              </div>
            </div>
          </form>
        </CardContent>
      </Card>

      {/* One-time secret reveal */}
      {secret ? (
        <Card>
          <CardHeader>
            <CardTitle as="h2" className="flex items-center gap-2 text-base">
              <StatusBadge status="success" label="ONE-TIME" /> 평문 시크릿
            </CardTitle>
          </CardHeader>
          <CardContent className="space-y-3">
            <Alert variant="info">
              이 값은 지금만 표시됩니다. 안전한 곳에 저장하세요 — 다시 조회할 수 없습니다.
            </Alert>
            <div className="ax-code-pane">
              <CodeSnippet code={secret.value} language="text" filename={`${secret.name} · secret`} />
            </div>
          </CardContent>
        </Card>
      ) : null}

      {/* List */}
      <section aria-labelledby="keys-list-heading" className="space-y-3">
        <h2 id="keys-list-heading" className="text-base font-semibold text-foreground">
          발급된 키
        </h2>
        {keys.isLoading ? (
          <LoadingState />
        ) : keys.isError ? (
          <ErrorState error={keys.error} />
        ) : rows.length === 0 ? (
          <EmptyState>아직 API 키가 없습니다. 위에서 첫 키를 발급하세요.</EmptyState>
        ) : (
          <DataGrid<KeyColumn>
            caption="이 계정의 API 키"
            columns={COLUMNS}
            rows={rows}
          />
        )}
        {createKey.isError ? <ErrorState error={createKey.error} /> : null}
      </section>

      {/* Live HTTP exchange */}
      {lastExchange ? (
        <section aria-labelledby="keys-http-heading" className="space-y-3">
          <h2 id="keys-http-heading" className="text-base font-semibold text-foreground">
            마지막 HTTP 교환
          </h2>
          <ExchangeView exchange={lastExchange} />
        </section>
      ) : null}

      <ConfirmDialog
        open={revokeTarget !== null}
        onOpenChange={(open) => !open && setRevokeTarget(null)}
        title="API 키 폐기"
        description={
          revokeTarget
            ? `'${revokeTarget.name}' (${revokeTarget.prefix}) 키를 폐기합니다. 이 작업은 되돌릴 수 없습니다.`
            : undefined
        }
        confirmLabel="폐기"
        tone="destructive"
        loading={revokeKey.isPending}
        onConfirm={handleRevoke}
      />

      {revokeKey.isError ? <p className="sr-only">{errorMessage(revokeKey.error)}</p> : null}
    </div>
  );
}
