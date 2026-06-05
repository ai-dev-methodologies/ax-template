import React from 'react';
import { Alert, Spinner } from '@ax/ui';

/**
 * Shared screen-state presenters. Composed from catalog primitives only
 * (Spinner / Alert) — no app-local UI primitive is defined. These keep every
 * surface's loading / error / empty handling identical and accessible.
 */

export function ScreenLoading({ label = '불러오는 중' }: { label?: string }) {
  return (
    <div className="grid min-h-40 place-items-center" role="status" aria-live="polite">
      <Spinner className="h-6 w-6 text-muted-foreground" label={label} />
    </div>
  );
}

export function ScreenError({ error }: { error: Error }) {
  return (
    <Alert variant="error">
      <span className="font-medium">요청을 처리하지 못했습니다.</span>{' '}
      <span className="text-sm">{error.message}</span>
    </Alert>
  );
}

export function ScreenEmpty({
  icon,
  title,
  description,
}: {
  icon?: React.ReactNode;
  title: string;
  description?: string;
}) {
  return (
    <div className="flex flex-col items-center justify-center gap-2 rounded-[var(--radius)] border border-dashed border-border bg-card/40 px-6 py-14 text-center">
      {icon ? <span aria-hidden className="text-muted-foreground">{icon}</span> : null}
      <p className="font-medium text-foreground">{title}</p>
      {description ? <p className="max-w-sm text-sm text-muted-foreground">{description}</p> : null}
    </div>
  );
}
