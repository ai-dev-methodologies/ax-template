'use client';

import React, { use, useState } from 'react';
import Link from 'next/link';
import { ArrowLeft, Pencil, Tag as TagIcon } from 'lucide-react';
import { Button } from '@ax/ui';
import { SplitText } from '@ax/blocks';
import { LoadingState, ErrorState } from '@/components/screen-states';
import { useArticle } from '@/features/articles/hooks';
import { useArticleTags } from '@/features/tags/hooks';
import { extractCover, stripCover } from '@/lib/article-body';
import { formatDateline } from '@/lib/format';
import { errorMessage } from '@/lib/errors';

// Use the dramatic SplitText hero only for short titles; long headlines render
// as a plain serif masthead so the line-reveal effect stays legible.
const SPLIT_TITLE_MAX = 24;

/** Cover image that removes itself if the referenced /api/files id is gone (404/401). */
function CoverFigure({ src }: { src: string }) {
  const [ok, setOk] = useState(true);
  if (!ok) return null;
  return (
    <figure className="w-full">
      {/* Cover is a relative /api/files URL served through the proxy; the
          native <img> is intentional (no Next Image optimizer in this app). */}
      <img
        src={src}
        alt=""
        onError={() => setOk(false)}
        className="w-full border border-border object-cover"
      />
    </figure>
  );
}

export default function ArticleReadPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = use(params);
  const article = useArticle(id);
  const tags = useArticleTags(id);

  if (article.isLoading) return <LoadingState label="글을 불러오는 중" />;
  if (article.isError) {
    return <ErrorState message={errorMessage(article.error)} onRetry={() => article.refetch()} />;
  }
  if (!article.data) return <ErrorState message="글을 찾을 수 없습니다." />;

  const a = article.data;
  const cover = extractCover(a.description);
  const bodyHtml = stripCover(a.description);
  const assigned = tags.data?.items ?? [];
  const useSplit = a.title.length <= SPLIT_TITLE_MAX;

  return (
    <article className="space-y-10">
      <nav>
        <Button asChild variant="ghost" size="sm">
          <Link href="/">
            <ArrowLeft aria-hidden />라이브러리로
          </Link>
        </Button>
      </nav>

      <header className="space-y-6 border-b border-foreground pb-8">
        <p className="text-xs uppercase tracking-[0.22em] text-muted-foreground">
          {formatDateline(a.createdAt)}
          {a.createdBy ? ` · ${a.createdBy}` : ''}
        </p>

        {useSplit ? (
          <div className="h-44 w-full sm:h-56">
            <SplitText
              text={
                <span className="px-2 text-center font-display text-5xl font-black tracking-tight sm:text-7xl">
                  {a.title}
                </span>
              }
              accent="var(--ax-status-accent-fg)"
              className="border border-border !bg-background !p-0"
            />
          </div>
        ) : (
          <h1 className="font-display text-4xl font-black leading-[1.05] tracking-tight text-foreground sm:text-6xl">
            {a.title}
          </h1>
        )}

        {assigned.length > 0 ? (
          <div className="flex flex-wrap items-center gap-2">
            <TagIcon aria-hidden className="h-4 w-4 text-muted-foreground" />
            {assigned.map((tag) => (
              <span
                key={tag.id}
                className="border border-border px-2.5 py-0.5 text-xs uppercase tracking-[0.08em] text-muted-foreground"
              >
                {tag.name}
              </span>
            ))}
          </div>
        ) : null}
      </header>

      {cover ? <CoverFigure src={cover} /> : null}

      {bodyHtml.trim() ? (
        // The body HTML is authored by the studio's own RichTextEditor (Tiptap
        // StarterKit) — first-party content, not arbitrary user paste — and is
        // rendered through the .ax-prose editorial typography scale.
        <div
          className="ax-prose mx-auto max-w-2xl text-foreground"
          dangerouslySetInnerHTML={{ __html: bodyHtml }}
        />
      ) : (
        <p className="mx-auto max-w-2xl text-center italic text-muted-foreground">
          본문이 비어 있습니다.
        </p>
      )}

      <footer className="flex justify-center border-t border-border pt-8">
        <Button asChild variant="outline">
          <Link href={`/editor/${a.id}`}>
            <Pencil aria-hidden />이 글 편집
          </Link>
        </Button>
      </footer>
    </article>
  );
}
