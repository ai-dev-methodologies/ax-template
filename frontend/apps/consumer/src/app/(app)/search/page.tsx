'use client';

import React, { useState } from 'react';
import Link from 'next/link';
import { Search as SearchIcon, SearchX } from 'lucide-react';
import { Card, CardContent, Input } from '@ax/ui';
import { EmptyState, ErrorState, LoadingState } from '@/components/screen-states';
import { useSearch } from '@/features/search/hooks';
import { useDebounce } from '@/lib/use-debounce';
import { formatRelative } from '@/lib/format';
import { errorMessage } from '@/lib/errors';
import type { SearchHit } from '@/lib/api/searchClient';

function objectIdOf(hit: SearchHit): string | null {
  if (!hit.metadata) return null;
  try {
    const parsed: unknown = JSON.parse(hit.metadata);
    if (parsed && typeof parsed === 'object' && 'objectId' in parsed) {
      const value = (parsed as { objectId: unknown }).objectId;
      return typeof value === 'string' ? value : null;
    }
  } catch {
    // metadata is not JSON — fall through
  }
  return null;
}

export default function SearchPage() {
  const [term, setTerm] = useState('');
  const debounced = useDebounce(term, 300);
  const search = useSearch(debounced);

  const trimmed = debounced.trim();
  const hits = search.data?.hits ?? [];

  return (
    <div className="space-y-6">
      <header>
        <h1 className="text-2xl font-bold tracking-tight text-foreground">검색</h1>
        <p className="mt-1 text-sm text-muted-foreground">게시물과 콘텐츠를 찾아보세요.</p>
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
          placeholder="검색어를 입력하세요"
          className="pl-9"
          autoComplete="off"
        />
      </div>

      {trimmed.length === 0 ? (
        <EmptyState
          icon={<SearchIcon aria-hidden className="h-6 w-6" />}
          title="무엇을 찾고 있나요?"
          description="위 검색창에 키워드를 입력하면 결과가 나타납니다."
        />
      ) : search.isLoading ? (
        <LoadingState label="검색 중" />
      ) : search.isError ? (
        <ErrorState message={errorMessage(search.error)} onRetry={() => search.refetch()} />
      ) : hits.length === 0 ? (
        <EmptyState
          icon={<SearchX aria-hidden className="h-6 w-6" />}
          title="결과가 없어요"
          description={`'${trimmed}'에 대한 결과를 찾지 못했어요.`}
        />
      ) : (
        <>
          <p className="text-sm text-muted-foreground">
            {search.data?.totalHits ?? hits.length}개 결과 · {search.data?.processingTimeMs ?? 0}ms
          </p>
          <ul className="space-y-3">
            {hits.map((hit) => {
              const objectId = objectIdOf(hit);
              const body = (
                <CardContent className="p-4">
                  <span className="block text-xs font-medium uppercase tracking-wide text-[var(--ax-status-accent-fg)]">
                    {hit.domain}
                  </span>
                  <span className="mt-1 block break-words text-sm text-foreground">{hit.content}</span>
                  <span className="mt-1 block text-xs text-muted-foreground">
                    {formatRelative(hit.indexedAt)}
                  </span>
                </CardContent>
              );
              return (
                <li key={hit.id}>
                  {objectId ? (
                    <Link
                      href={`/post/${encodeURIComponent(objectId)}`}
                      className="block rounded-[var(--radius)] focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 focus-visible:ring-offset-background"
                    >
                      <Card className="shadow-sm transition-transform duration-200 ease-out hover:-translate-y-0.5 motion-reduce:hover:translate-y-0">
                        {body}
                      </Card>
                    </Link>
                  ) : (
                    <Card className="shadow-sm">{body}</Card>
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
