'use client';

import React, { useState } from 'react';
import { Plus, ToggleRight } from 'lucide-react';
import { Alert } from '@/components/ui/alert';
import { Button } from '@/components/ui/button';
import { Field } from '@/components/ui/field';
import { Switch } from '@/components/ui/switch';
import { useCreateFeatureFlag, useFeatureFlags, useToggleFeatureFlag } from '@/features/featureFlags/hooks';
import { PageHeader, EmptyState, ErrorPanel, LoadingPanel, TableShell } from '../_components/console-ui';
import { formatTimestamp } from '../_components/format';

const NAME_PATTERN = /^[a-z][a-z0-9-]{1,62}$/;

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
        <TableShell>
          <thead>
            <tr className="border-b border-border text-left text-xs uppercase tracking-wide text-muted-foreground">
              <th scope="col" className="px-4 py-3 font-medium">이름</th>
              <th scope="col" className="px-4 py-3 font-medium">설명</th>
              <th scope="col" className="px-4 py-3 font-medium">수정 시각</th>
              <th scope="col" className="px-4 py-3 text-right font-medium">상태</th>
            </tr>
          </thead>
          <tbody>
            {flags.map((flag) => (
              <tr key={flag.name} className="border-b border-border last:border-0 hover:bg-secondary/40">
                <td className="px-4 py-3 font-mono text-sm font-medium text-foreground">{flag.name}</td>
                <td className="px-4 py-3 text-muted-foreground">{flag.description || '—'}</td>
                <td className="whitespace-nowrap px-4 py-3 tabular-nums text-muted-foreground">
                  {formatTimestamp(flag.updatedAt)}
                </td>
                <td className="px-4 py-3">
                  <div className="flex items-center justify-end gap-2">
                    <span className="text-xs text-muted-foreground">{flag.enabled ? '켜짐' : '꺼짐'}</span>
                    <Switch
                      checked={flag.enabled}
                      onCheckedChange={(checked) => toggle.mutate({ name: flag.name, enabled: checked })}
                      aria-label={`${flag.name} 기능 플래그 ${flag.enabled ? '끄기' : '켜기'}`}
                    />
                  </div>
                </td>
              </tr>
            ))}
          </tbody>
        </TableShell>
      )}
    </div>
  );
}
