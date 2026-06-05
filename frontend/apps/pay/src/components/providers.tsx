'use client';

import React from 'react';
import { QueryClientProvider } from '@tanstack/react-query';
import { getQueryClient } from '@ax/core';

/**
 * App-level providers. Reuses the shared @ax/core QueryClient factory (the same
 * singleton/per-request strategy the catalog ships) — the app does NOT spin up
 * its own client config. No MSW: this app talks to the live backend through the
 * Next rewrite proxy (/api/* -> :8080).
 */
export function Providers({ children }: { children: React.ReactNode }) {
  const queryClient = getQueryClient();
  return <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>;
}
