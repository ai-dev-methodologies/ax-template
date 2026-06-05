import {
  keepPreviousData,
  useMutation,
  useQuery,
  useQueryClient,
} from '@tanstack/react-query';
import {
  articleClient,
  type Article,
  type ArticleInput,
  type ArticlePage,
} from '@/lib/api/articleClient';
import { searchClient } from '@/lib/api/searchClient';
import { excerpt } from '@/lib/format';

export const articleKeys = {
  all: ['articles'] as const,
  list: (page: number, size: number) => [...articleKeys.all, 'list', page, size] as const,
  detail: (id: string) => [...articleKeys.all, 'detail', id] as const,
};

/** Paginated library list. */
export function useArticles(page = 0, size = 20) {
  return useQuery<ArticlePage>({
    queryKey: articleKeys.list(page, size),
    queryFn: () => articleClient.list(page, size),
    placeholderData: keepPreviousData,
  });
}

/** Single article for the editor / read view. */
export function useArticle(id: string | undefined) {
  return useQuery<Article>({
    queryKey: articleKeys.detail(id ?? ''),
    queryFn: () => articleClient.get(id as string),
    enabled: Boolean(id),
  });
}

/**
 * Index an article into the search domain so it surfaces in search. Best-effort:
 * a failed index must NOT fail the save (the article is already persisted). The
 * metadata carries the objectId so a hit can deep-link to the read view.
 */
async function indexArticle(article: Article): Promise<void> {
  try {
    const content = `${article.title} ${excerpt(article.description, 4000)}`.trim();
    await searchClient.index({
      domain: 'article',
      content,
      metadata: JSON.stringify({ objectId: article.id, title: article.title }),
    });
  } catch {
    // best-effort indexing — swallow so the save still succeeds.
  }
}

/** Create an article, then index it for search. */
export function useCreateArticle() {
  const qc = useQueryClient();
  return useMutation<Article, Error, ArticleInput>({
    mutationFn: async (input) => {
      const created = await articleClient.create(input);
      await indexArticle(created);
      return created;
    },
    onSuccess: () => {
      void qc.invalidateQueries({ queryKey: articleKeys.all });
    },
  });
}

/** Update an article, then re-index it for search. */
export function useUpdateArticle(id: string) {
  const qc = useQueryClient();
  return useMutation<Article, Error, ArticleInput>({
    mutationFn: async (input) => {
      const updated = await articleClient.update(id, input);
      await indexArticle(updated);
      return updated;
    },
    onSuccess: (updated) => {
      qc.setQueryData(articleKeys.detail(id), updated);
      void qc.invalidateQueries({ queryKey: articleKeys.all });
    },
  });
}

/** Hard-delete an article (no soft-delete on this domain). */
export function useDeleteArticle() {
  const qc = useQueryClient();
  return useMutation<void, Error, string>({
    mutationFn: (id) => articleClient.remove(id),
    onSuccess: () => {
      void qc.invalidateQueries({ queryKey: articleKeys.all });
    },
  });
}
