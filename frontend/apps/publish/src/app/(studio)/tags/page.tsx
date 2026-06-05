'use client';

import React, { useState } from 'react';
import { Check, Pencil, Plus, Tags as TagsIcon, Trash2, X } from 'lucide-react';
import {
  Alert,
  Button,
  Card,
  CardContent,
  ConfirmDialog,
  Field,
  cn,
} from '@ax/ui';
import { EmptyState, ErrorState, LoadingState } from '@/components/screen-states';
import {
  useCreateTag,
  useDeleteTag,
  useTags,
  useUpdateTag,
} from '@/features/tags/hooks';
import { formatRelative } from '@/lib/format';
import { errorMessage } from '@/lib/errors';
import type { Tag } from '@/lib/api/tagClient';

/** One tag row — inline edit + delete (ADMIN; the demo account is ADMIN). */
function TagRow({ tag }: { tag: Tag }) {
  const [editing, setEditing] = useState(false);
  const [name, setName] = useState(tag.name);
  const [confirmOpen, setConfirmOpen] = useState(false);
  const update = useUpdateTag();
  const remove = useDeleteTag();

  const save = async (): Promise<void> => {
    if (!name.trim()) return;
    try {
      await update.mutateAsync({ id: tag.id, input: { name: name.trim() } });
      setEditing(false);
    } catch {
      // surfaced via update.error
    }
  };

  const del = async (): Promise<void> => {
    try {
      await remove.mutateAsync(tag.id);
      setConfirmOpen(false);
    } catch {
      // surfaced via remove.error (kept open to retry)
    }
  };

  return (
    <li className="flex items-center justify-between gap-4 border-b border-border bg-card px-4 py-3">
      {editing ? (
        <div className="flex flex-1 items-center gap-2">
          <Field
            id={`tag-name-${tag.id}`}
            label="태그 이름"
            value={name}
            onChange={(e) => setName(e.target.value)}
            className="flex-1"
          />
          <Button size="sm" onClick={save} loading={update.isPending}>
            <Check aria-hidden />저장
          </Button>
          <Button
            size="sm"
            variant="ghost"
            onClick={() => {
              setName(tag.name);
              setEditing(false);
            }}
          >
            <X aria-hidden />취소
          </Button>
        </div>
      ) : (
        <>
          <div className="min-w-0">
            <p className="truncate font-display text-lg text-foreground">{tag.name}</p>
            <p className="text-xs uppercase tracking-[0.08em] text-muted-foreground">
              {tag.slug} · {formatRelative(tag.createdAt)}
            </p>
          </div>
          <div className="flex shrink-0 items-center gap-1">
            <Button size="sm" variant="ghost" onClick={() => setEditing(true)}>
              <Pencil aria-hidden />
              <span className="sr-only">편집</span>
            </Button>
            <Button
              size="sm"
              variant="ghost"
              onClick={() => setConfirmOpen(true)}
              disabled={remove.isPending}
            >
              <Trash2 aria-hidden />
              <span className="sr-only">삭제</span>
            </Button>
            <ConfirmDialog
              open={confirmOpen}
              onOpenChange={setConfirmOpen}
              title={`'${tag.name}' 태그를 삭제할까요?`}
              description="이 태그가 글에 지정되어 있어도 연결이 함께 제거됩니다."
              confirmLabel="삭제"
              tone="destructive"
              loading={remove.isPending}
              onConfirm={del}
            />
          </div>
        </>
      )}
    </li>
  );
}

export default function TagsPage() {
  const tags = useTags();
  const create = useCreateTag();
  const [name, setName] = useState('');

  const items = tags.data?.items ?? [];

  const add = async (e: React.FormEvent): Promise<void> => {
    e.preventDefault();
    if (!name.trim()) return;
    try {
      await create.mutateAsync({ name: name.trim() });
      setName('');
    } catch {
      // surfaced via create.error
    }
  };

  return (
    <div className="space-y-8">
      <header className="border-b border-foreground pb-6">
        <p className="text-xs uppercase tracking-[0.22em] text-muted-foreground">분류</p>
        <h1 className="mt-2 font-display text-5xl font-bold tracking-tight text-foreground">태그</h1>
        <p className="mt-2 text-sm text-muted-foreground">
          글을 분류할 태그를 만들고 관리합니다. (관리자 전용)
        </p>
      </header>

      <Card>
        <CardContent className="py-5">
          <form onSubmit={add} className={cn('flex items-end gap-3')}>
            <Field
              id="new-tag"
              label="새 태그"
              value={name}
              onChange={(e) => setName(e.target.value)}
              placeholder="예: 디자인"
              className="flex-1"
            />
            <Button type="submit" loading={create.isPending}>
              <Plus aria-hidden />추가
            </Button>
          </form>
          {create.error ? (
            <Alert variant="error" className="mt-3">
              {errorMessage(create.error)}
            </Alert>
          ) : null}
        </CardContent>
      </Card>

      {tags.isLoading ? (
        <LoadingState label="태그를 불러오는 중" />
      ) : tags.isError ? (
        <ErrorState message={errorMessage(tags.error)} onRetry={() => tags.refetch()} />
      ) : items.length === 0 ? (
        <EmptyState
          icon={<TagsIcon aria-hidden className="h-6 w-6" />}
          title="태그가 없습니다"
          description="위에서 첫 태그를 만들어 글을 분류해 보세요."
        />
      ) : (
        <ul className="border-t border-border">
          {items.map((tag) => (
            <TagRow key={tag.id} tag={tag} />
          ))}
        </ul>
      )}
    </div>
  );
}
