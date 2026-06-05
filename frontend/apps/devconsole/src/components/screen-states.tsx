'use client';

import React from 'react';
import { AlertCircle } from 'lucide-react';
import { Spinner } from '@ax/ui';
import { errorMessage } from '@/lib/errors';

/**
 * Small presentational state helpers shared across the console screens. These are
 * NOT UI primitives (no Button/Card/etc. naming) — they are app-local layout
 * scaffolding around the catalog Spinner + token classes, so the boundary rule is
 * satisfied while loading/error/empty look consistent.
 */

export function LoadingState({ label = '불러오는 중' }: { label?: string }) {
  return (
    <div className="flex items-center justify-center gap-2 py-12 text-muted-foreground">
      <Spinner className="h-5 w-5" label={label} />
      <span className="font-mono text-xs">{label}</span>
    </div>
  );
}

export function ErrorState({ error }: { error: unknown }) {
  return (
    <div
      role="alert"
      className="flex items-start gap-2 rounded border border-destructive/40 bg-[var(--ax-status-danger-bg)] px-4 py-3 text-sm text-[var(--ax-status-danger-fg)]"
    >
      <AlertCircle aria-hidden className="mt-0.5 h-4 w-4 shrink-0" />
      <span>{errorMessage(error)}</span>
    </div>
  );
}

export function EmptyState({ children }: { children: React.ReactNode }) {
  return (
    <div className="rounded border border-dashed border-border px-4 py-10 text-center font-mono text-xs text-muted-foreground">
      {children}
    </div>
  );
}
