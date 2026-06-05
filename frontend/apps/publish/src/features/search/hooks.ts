import { keepPreviousData, useQuery } from '@tanstack/react-query';
import { searchClient, type SearchResultPage } from '@/lib/api/searchClient';

export const searchKeys = {
  all: ['search'] as const,
  query: (q: string) => [...searchKeys.all, 'query', q] as const,
};

/**
 * Run a search whenever `query` is non-blank. The caller debounces `query`
 * before passing it in; a blank query disables the call (the backend rejects
 * blank with a 400) so the results panel can show the idle/empty prompt.
 * Scoped to domain="article" so only published articles surface.
 */
export function useSearch(query: string) {
  const trimmed = query.trim();
  return useQuery<SearchResultPage>({
    queryKey: searchKeys.query(trimmed),
    queryFn: () => searchClient.search({ query: trimmed, domain: 'article' }),
    enabled: trimmed.length > 0,
    placeholderData: keepPreviousData,
  });
}
