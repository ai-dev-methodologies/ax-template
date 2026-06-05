import React from 'react';
import { Alert, Spinner } from '@ax/ui';

/**
 * Shared screen-state presenters. Composed from catalog primitives only
 * (Spinner / Alert) — no app-local UI primitive is defined. These keep every
 * studio surface's loading / error / empty handling identical and accessible.
 */

export function ScreenLoading({ label = '불러오는 중' }: { label?: string }) {
  return (
    <div className="grid min-h-48 place-items-center" role="status" aria-live="polite">
      <Spinner className="h-7 w-7 text-[var(--ax-status-accent-fg)]" label={label} />
    </div>
  );
}

export function ScreenError({ error }: { error: Error }) {
  return (
    <Alert variant="error">
      <span className="font-semibold">요청을 처리하지 못했어요.</span>{' '}
      <span className="text-sm">{error.message}</span>
    </Alert>
  );
}

export function ScreenEmpty({
  icon,
  title,
  description,
  action,
}: {
  icon?: React.ReactNode;
  title: string;
  description?: string;
  action?: React.ReactNode;
}) {
  return (
    <div className="flex flex-col items-center justify-center gap-3 rounded-[var(--radius)] border-2 border-dashed border-border bg-card/50 px-6 py-16 text-center shadow-sm">
      {icon ? (
        <span aria-hidden className="text-[var(--ax-status-accent-fg)]">
          {icon}
        </span>
      ) : null}
      <p className="ax-display text-xl font-bold text-foreground">{title}</p>
      {description ? (
        <p className="max-w-sm text-sm text-muted-foreground">{description}</p>
      ) : null}
      {action ? <div className="mt-2">{action}</div> : null}
    </div>
  );
}
