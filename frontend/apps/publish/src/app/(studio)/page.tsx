'use client';

import React, { useMemo, useState } from 'react';
import Link from 'next/link';
import { FileText, PenLine, Tag as TagIcon } from 'lucide-react';
import { Button, cn } from '@ax/ui';
import {
  InterfacesCard,
  InterfacesCardContent,
  InterfacesCardHeader,
  InterfacesCardTitle,
} from '@ax/blocks';
import { EmptyState, ErrorState, LoadingState } from '@/components/screen-states';
import { useArticles } from '@/features/articles/hooks';
import { useTags } from '@/features/tags/hooks';
import { excerpt, formatRelative } from '@/lib/format';
import { extractCover } from '@/lib/article-body';
import { errorMessage } from '@/lib/errors';
import type { Article } from '@/lib/api/articleClient';

/** A single library entry — InterfacesCard (catalog) wrapped in a deep link. */
function ArticleCard({ article }: { article: Article }) {
  const cover = extractCover(article.description);
  const summary = excerpt(article.description, 120);
  return (
    <li>
      <Link
        href={`/article/${article.id}`}
        className="group block h-full focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 focus-visible:ring-offset-background"
      >
        <InterfacesCard className="h-full overflow-hidden transition-colors duration-200 group-hover:border-foreground">
          {cover ? (
            // Cover is a relative /api/files URL via the proxy; native <img> is
            // intentional (no Next Image optimizer in this app).
            <img
              src={cover}
              alt=""
              className="aspect-[3/2] w-full border-b border-border object-cover"
            />
          ) : (
            <div className="grid aspect-[3/2] w-full place-items-center border-b border-border bg-secondary/40">
              <FileText aria-hidden className="h-8 w-8 text-muted-foreground/50" />
            </div>
          )}
          <InterfacesCardHeader className="border-b-0">
            <InterfacesCardTitle className="font-display text-xl leading-snug tracking-tight">
              {article.title}
            </InterfacesCardTitle>
          </InterfacesCardHeader>
          <InterfacesCardContent className="flex flex-1 flex-col gap-3 pt-0">
            {summary ? (
              <p className="line-clamp-3 text-sm leading-relaxed text-muted-foreground">{summary}</p>
            ) : (
              <p className="text-sm italic text-muted-foreground/70">본문이 비어 있습니다.</p>
            )}
            <span className="mt-auto text-[0.7rem] uppercase tracking-[0.1em] text-muted-foreground">
              {formatRelative(article.updatedAt ?? article.createdAt)}
            </span>
          </InterfacesCardContent>
        </InterfacesCard>
      </Link>
    </li>
  );
}

export default function LibraryPage() {
  const articles = useArticles(0, 24);
  const tags = useTags();
  const [activeTag, setActiveTag] = useState<string | null>(null);

  const items = articles.data?.data ?? [];

  // Tag filtering is a client-side title/body substring narrowing on the active
  // tag NAME — the CRUD list endpoint has no tag-join query, so this keeps the
  // filter honest (it never claims server-side tag filtering it cannot do).
  const filtered = useMemo(() => {
    if (!activeTag) return items;
    const needle = activeTag.toLowerCase();
    return items.filter(
      (a) =>
        a.title.toLowerCase().includes(needle) ||
        (a.description ?? '').toLowerCase().includes(needle),
    );
  }, [items, activeTag]);

  const tagItems = tags.data?.items ?? [];

  return (
    <div className="space-y-8">
      <header className="flex flex-wrap items-end justify-between gap-4 border-b border-foreground pb-6">
        <div>
          <p className="text-xs uppercase tracking-[0.22em] text-muted-foreground">에디토리얼 라이브러리</p>
          <h1 className="mt-2 font-display text-5xl font-bold tracking-tight text-foreground sm:text-6xl">
            발행물
          </h1>
        </div>
        <Button asChild size="lg">
          <Link href="/editor">
            <PenLine aria-hidden />새 글 쓰기
          </Link>
        </Button>
      </header>

      {tagItems.length > 0 ? (
        <div className="flex flex-wrap items-center gap-2" role="group" aria-label="태그 필터">
          <button
            type="button"
            onClick={() => setActiveTag(null)}
            aria-pressed={activeTag === null}
            className={cn(
              'border px-3 py-1 text-xs uppercase tracking-[0.08em] transition-colors',
              'focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 focus-visible:ring-offset-background',
              activeTag === null
                ? 'border-foreground bg-foreground text-background'
                : 'border-border text-muted-foreground hover:border-foreground hover:text-foreground',
            )}
          >
            전체
          </button>
          {tagItems.map((tag) => (
            <button
              key={tag.id}
              type="button"
              onClick={() => setActiveTag(tag.name)}
              aria-pressed={activeTag === tag.name}
              className={cn(
                'inline-flex items-center gap-1 border px-3 py-1 text-xs uppercase tracking-[0.08em] transition-colors',
                'focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 focus-visible:ring-offset-background',
                activeTag === tag.name
                  ? 'border-foreground bg-foreground text-background'
                  : 'border-border text-muted-foreground hover:border-foreground hover:text-foreground',
              )}
            >
              <TagIcon aria-hidden className="h-3 w-3" />
              {tag.name}
            </button>
          ))}
        </div>
      ) : null}

      {articles.isLoading ? (
        <LoadingState label="발행물을 불러오는 중" />
      ) : articles.isError ? (
        <ErrorState message={errorMessage(articles.error)} onRetry={() => articles.refetch()} />
      ) : items.length === 0 ? (
        <EmptyState
          icon={<FileText aria-hidden className="h-6 w-6" />}
          title="아직 발행물이 없습니다"
          description="첫 번째 글을 작성하여 라이브러리를 채워 보세요."
          action={
            <Button asChild>
              <Link href="/editor">
                <PenLine aria-hidden />첫 글 쓰기
              </Link>
            </Button>
          }
        />
      ) : filtered.length === 0 ? (
        <EmptyState
          icon={<TagIcon aria-hidden className="h-6 w-6" />}
          title="일치하는 글이 없습니다"
          description={`'${activeTag}' 태그와 일치하는 발행물을 찾지 못했습니다.`}
          action={
            <Button variant="outline" onClick={() => setActiveTag(null)}>
              필터 해제
            </Button>
          }
        />
      ) : (
        <ul className="grid grid-cols-1 gap-px bg-border sm:grid-cols-2 lg:grid-cols-3">
          {filtered.map((article) => (
            <ArticleCard key={article.id} article={article} />
          ))}
        </ul>
      )}
    </div>
  );
}
