'use client';

import React, { useState } from 'react';
import Link from 'next/link';
import { Search as SearchIcon, SearchX } from 'lucide-react';
import { Input } from '@ax/ui';
import { EmptyState, ErrorState, LoadingState } from '@/components/screen-states';
import { useSearch } from '@/features/search/hooks';
import { useDebounce } from '@/lib/use-debounce';
import { formatRelative } from '@/lib/format';
import { errorMessage } from '@/lib/errors';
import type { SearchHit } from '@/lib/api/searchClient';

/** Pull the deep-link target (article id) + title out of a hit's metadata JSON. */
function metaOf(hit: SearchHit): { objectId: string | null; title: string | null } {
  if (!hit.metadata) return { objectId: null, title: null };
  try {
    const parsed: unknown = JSON.parse(hit.metadata);
    if (parsed && typeof parsed === 'object') {
      const obj = parsed as Record<string, unknown>;
      return {
        objectId: typeof obj.objectId === 'string' ? obj.objectId : null,
        title: typeof obj.title === 'string' ? obj.title : null,
      };
    }
  } catch {
    // metadata is not JSON — fall through
  }
  return { objectId: null, title: null };
}

export function SearchScreen() {
  const [term, setTerm] = useState('');
  const debounced = useDebounce(term, 300);
  const search = useSearch(debounced);

  const trimmed = debounced.trim();
  const hits = search.data?.hits ?? [];

  return (
    <div className="space-y-8">
      <header className="border-b border-foreground pb-6">
        <p className="text-xs uppercase tracking-[0.22em] text-muted-foreground">발견</p>
        <h1 className="mt-2 font-display text-5xl font-bold tracking-tight text-foreground">검색</h1>
        <p className="mt-2 text-sm text-muted-foreground">발행된 글을 찾아보세요.</p>
      </header>

      <div className="relative">
        <SearchIcon
          aria-hidden
          className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground"
        />
        <label htmlFor="search-input" className="sr-only">
          검색어
        </label>
        <Input
          id="search-input"
          value={term}
          onChange={(e) => setTerm(e.target.value)}
          placeholder="제목이나 본문 키워드"
          className="pl-9"
          autoComplete="off"
        />
      </div>

      {trimmed.length === 0 ? (
        <EmptyState
          icon={<SearchIcon aria-hidden className="h-6 w-6" />}
          title="무엇을 찾고 계신가요?"
          description="위 검색창에 키워드를 입력하면 발행된 글이 나타납니다."
        />
      ) : search.isLoading ? (
        <LoadingState label="검색 중" />
      ) : search.isError ? (
        <ErrorState message={errorMessage(search.error)} onRetry={() => search.refetch()} />
      ) : hits.length === 0 ? (
        <EmptyState
          icon={<SearchX aria-hidden className="h-6 w-6" />}
          title="결과가 없습니다"
          description={`'${trimmed}'에 대한 글을 찾지 못했습니다.`}
        />
      ) : (
        <>
          <p className="text-xs uppercase tracking-[0.1em] text-muted-foreground">
            {search.data?.totalHits ?? hits.length}개 결과 · {search.data?.processingTimeMs ?? 0}ms
          </p>
          <ul className="border-t border-border">
            {hits.map((hit) => {
              const { objectId, title } = metaOf(hit);
              const heading = title ?? hit.content.slice(0, 60);
              const row = (
                <div className="flex flex-col gap-1 px-4 py-4">
                  <span className="font-display text-xl tracking-tight text-foreground">
                    {heading}
                  </span>
                  <span className="line-clamp-2 text-sm text-muted-foreground">{hit.content}</span>
                  <span className="text-[0.7rem] uppercase tracking-[0.1em] text-muted-foreground">
                    {formatRelative(hit.indexedAt)}
                  </span>
                </div>
              );
              return (
                <li key={hit.id} className="border-b border-border bg-card">
                  {objectId ? (
                    <Link
                      href={`/article/${encodeURIComponent(objectId)}`}
                      className="block transition-colors hover:bg-secondary/40 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-inset focus-visible:ring-ring"
                    >
                      {row}
                    </Link>
                  ) : (
                    row
                  )}
                </li>
              );
            })}
          </ul>
        </>
      )}
    </div>
  );
}
