'use client';

import React, { useMemo, useState } from 'react';
import { FolderHeart, FolderPlus } from 'lucide-react';
import { useAuthStore } from '@ax/core';
import { Alert, Button, Field } from '@ax/ui';
import { PageHeader } from '@/components/page-header';
import { MediaCard } from '@/components/media-card';
import { ScreenEmpty, ScreenError, ScreenLoading } from '@/components/screen-states';
import { useMediaIndex, useMediaList } from '@/features/media/hooks';
import { useCreateTag, useTags, useTagsForFiles } from '@/features/tags/hooks';
import type { StoredFile } from '@/lib/api/fileClient';
import { errorMessage } from '@/lib/errors';

interface CollectionGroup {
  id: string;
  name: string;
  files: StoredFile[];
}

/**
 * Collections — tag-based grouping of media. Lists every collection (tag) and
 * the media in it (grouped client-side from tags-by-entity, since the backend
 * has no files-for-a-tag endpoint), plus a form to create a new collection
 * (ROLE_ADMIN on the backend; the demo is ADMIN).
 */
export default function CollectionsPage() {
  const enabled = Boolean(useAuthStore((s) => s.accessToken));
  const index = useMediaIndex(enabled);
  const ids = index.data ?? [];
  const { files, isLoading: filesLoading } = useMediaList(ids, enabled);
  const tags = useTags(enabled);
  const { tagsByFile, isLoading: tagsLoading } = useTagsForFiles(ids, enabled);
  const createTag = useCreateTag();

  const [newName, setNewName] = useState('');

  const groups = useMemo<CollectionGroup[]>(() => {
    const tagList = tags.data?.items ?? [];
    const byId = new Map(files.map((f) => [f.id, f]));
    return tagList
      .map((tag) => {
        const collectionFiles = ids
          .filter((fid) => (tagsByFile[fid] ?? []).some((t) => t.id === tag.id))
          .map((fid) => byId.get(fid))
          .filter((f): f is StoredFile => Boolean(f));
        return { id: tag.id, name: tag.name, files: collectionFiles };
      })
      .filter((g) => g.files.length > 0);
  }, [tags.data, files, ids, tagsByFile]);

  const handleCreate = (e: React.FormEvent): void => {
    e.preventDefault();
    const name = newName.trim();
    if (!name) return;
    createTag.mutate(
      { name },
      {
        onSuccess: () => setNewName(''),
      },
    );
  };

  const busy = index.isLoading || (filesLoading && files.length === 0) || tags.isLoading;

  return (
    <section aria-labelledby="collections-heading" className="space-y-8">
      <span id="collections-heading" className="sr-only">
        컬렉션
      </span>
      <PageHeader
        title="컬렉션"
        description="태그로 작품을 컬렉션으로 묶어 정리하세요. 작품 상세에서 컬렉션에 담을 수 있어요."
      />

      {/* New collection */}
      <form
        onSubmit={handleCreate}
        className="flex flex-wrap items-end gap-3 rounded-[var(--radius)] border border-border bg-card p-4 shadow-sm"
        aria-label="새 컬렉션 만들기"
      >
        <Field
          id="new-collection"
          label="새 컬렉션 이름"
          placeholder="예: 풍경, 인물, 일러스트"
          value={newName}
          onChange={(e) => setNewName(e.target.value)}
          className="min-w-[14rem] flex-1"
        />
        <Button type="submit" size="lg" loading={createTag.isPending} disabled={!newName.trim()}>
          {!createTag.isPending && <FolderPlus aria-hidden />}
          만들기
        </Button>
      </form>
      {createTag.isError ? <Alert variant="error">{errorMessage(createTag.error)}</Alert> : null}

      {/* Groups */}
      {busy ? (
        <ScreenLoading label="컬렉션 불러오는 중" />
      ) : index.isError ? (
        <ScreenError error={new Error(errorMessage(index.error))} />
      ) : groups.length === 0 ? (
        <ScreenEmpty
          icon={<FolderHeart className="h-10 w-10" />}
          title="아직 컬렉션이 비어 있어요"
          description="컬렉션을 만들고, 작품 상세에서 작품을 담아보세요."
        />
      ) : (
        <div className="space-y-10">
          {tagsLoading ? (
            <p className="text-sm text-muted-foreground" role="status" aria-live="polite">
              컬렉션 구성 갱신 중…
            </p>
          ) : null}
          {groups.map((group) => (
            <section key={group.id} aria-label={`${group.name} 컬렉션`} className="space-y-4">
              <div className="flex items-center gap-2">
                <span
                  aria-hidden
                  className="inline-block h-3 w-3 rounded-full bg-[var(--ax-status-accent-fg)]"
                />
                <h2 className="ax-display text-xl font-extrabold tracking-tight text-foreground">
                  {group.name}
                </h2>
                <span className="text-sm font-medium text-muted-foreground">
                  {group.files.length}개
                </span>
              </div>
              <ul className="grid grid-cols-1 gap-5 sm:grid-cols-2 lg:grid-cols-3">
                {group.files.map((file, i) => (
                  <li key={file.id}>
                    <MediaCard file={file} index={i} />
                  </li>
                ))}
              </ul>
            </section>
          ))}
        </div>
      )}
    </section>
  );
}
