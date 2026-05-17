import { QueryClient } from '@tanstack/react-query';

let browserQueryClient: QueryClient | undefined = undefined;

function makeQueryClient(): QueryClient {
  return new QueryClient({
    defaultOptions: {
      queries: { staleTime: 60_000 },
    },
  });
}

export function getQueryClient(): QueryClient {
  if (typeof window === 'undefined') {
    // Server-side: always create a new client (no singleton sharing between requests)
    return makeQueryClient();
  }
  if (!browserQueryClient) {
    browserQueryClient = makeQueryClient();
  }
  return browserQueryClient;
}
