'use client';

import React, { useState } from 'react';
import { Plus, ToggleRight } from 'lucide-react';
import { Alert, Button, Field, Switch } from '@ax/ui';
import { DataGrid, type DataGridColumn } from '@ax/blocks';
import { PageHeader, EmptyState, ErrorPanel, LoadingPanel } from '@/components/console-ui';
import { formatTimestamp } from '@/lib/format';
import { useCreateFeatureFlag, useFeatureFlags, useToggleFeatureFlag } from '@/features/featureFlags/hooks';

const NAME_PATTERN = /^[a-z][a-z0-9-]{1,62}$/;

type ColumnKey = 'name' | 'description' | 'updatedAt' | 'state';

const COLUMNS: ReadonlyArray<DataGridColumn<ColumnKey>> = [
  { key: 'name', header: '이름' },
  { key: 'description', header: '설명' },
  { key: 'updatedAt', header: '수정 시각' },
  { key: 'state', header: '상태' },
];

export default function FeatureFlagsPage() {
  const query = useFeatureFlags();
  const toggle = useToggleFeatureFlag();
  const create = useCreateFeatureFlag();

  const [name, setName] = useState('');
  const [description, setDescription] = useState('');
  const [nameError, setNameError] = useState<string | undefined>();

  const flags = query.data?.content ?? [];

  const submit = (event: React.FormEvent): void => {
    event.preventDefault();
    const trimmed = name.trim();
    if (!NAME_PATTERN.test(trimmed)) {
      setNameError('소문자로 시작하고 소문자·숫자·하이픈만 사용하세요 (2~63자).');
      return;
    }
    setNameError(undefined);
    create.mutate(
      { name: trimmed, enabled: false, description: description.trim() || undefined },
      {
        onSuccess: () => {
          setName('');
          setDescription('');
        },
      },
    );
  };

  const rows = flags.map((flag) => ({
    name: <span className="font-mono text-sm font-medium text-foreground">{flag.name}</span>,
    description: <span className="text-muted-foreground">{flag.description || '—'}</span>,
    updatedAt: (
      <span className="whitespace-nowrap tabular-nums text-muted-foreground">
        {formatTimestamp(flag.updatedAt)}
      </span>
    ),
    state: (
      <div className="flex items-center gap-2">
        <span className="text-xs text-muted-foreground">{flag.enabled ? '켜짐' : '꺼짐'}</span>
        <Switch
          checked={flag.enabled}
          onCheckedChange={(checked) => toggle.mutate({ name: flag.name, enabled: checked })}
          aria-label={`${flag.name} 기능 플래그 ${flag.enabled ? '끄기' : '켜기'}`}
        />
      </div>
    ),
  }));

  return (
    <div className="space-y-6">
      <PageHeader
        eyebrow="Feature Flags"
        title="기능 플래그"
        description="런타임 기능을 즉시 켜고 끕니다. 변경은 감사 로그에 기록됩니다."
      />

      <form
        onSubmit={submit}
        aria-label="기능 플래그 생성"
        className="grid gap-3 rounded-[var(--radius)] border border-border bg-card p-4 sm:grid-cols-[1fr_1.5fr_auto] sm:items-end"
      >
        <Field
          id="flag-name"
          label="플래그 이름"
          placeholder="new-dashboard"
          value={name}
          onChange={(e) => setName(e.target.value)}
          error={nameError}
        />
        <Field
          id="flag-description"
          label="설명 (선택)"
          placeholder="새 대시보드 점진 출시"
          value={description}
          onChange={(e) => setDescription(e.target.value)}
        />
        <Button type="submit" loading={create.isPending}>
          <Plus aria-hidden />
          추가
        </Button>
      </form>

      {create.isError ? <ErrorPanel message={create.error.message} /> : null}
      {toggle.isError ? (
        <Alert variant="error">토글에 실패했습니다. {toggle.error.message}</Alert>
      ) : null}

      {query.isLoading ? (
        <LoadingPanel label="기능 플래그를 불러오는 중" />
      ) : query.isError ? (
        <ErrorPanel message={query.error.message} onRetry={() => query.refetch()} />
      ) : flags.length === 0 ? (
        <EmptyState
          icon={<ToggleRight aria-hidden className="h-5 w-5" />}
          title="등록된 플래그가 없습니다"
          description="위 양식으로 첫 기능 플래그를 추가하세요."
        />
      ) : (
        <DataGrid<ColumnKey> caption="기능 플래그 목록" columns={COLUMNS} rows={rows} />
      )}
    </div>
  );
}
