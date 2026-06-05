'use client';

import React, { use } from 'react';
import { ArticleEditor } from '@/components/article-editor';
import { LoadingState, ErrorState } from '@/components/screen-states';
import { useArticle } from '@/features/articles/hooks';
import { errorMessage } from '@/lib/errors';

/** Edit-article route. Loads the article, then mounts the editor in edit mode. */
export default function EditArticlePage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = use(params);
  const article = useArticle(id);

  if (article.isLoading) return <LoadingState label="글을 불러오는 중" />;
  if (article.isError) {
    return <ErrorState message={errorMessage(article.error)} onRetry={() => article.refetch()} />;
  }
  if (!article.data) return <ErrorState message="글을 찾을 수 없습니다." />;

  return <ArticleEditor article={article.data} />;
}
