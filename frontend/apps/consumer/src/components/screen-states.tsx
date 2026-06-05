import React from 'react';
import { Alert, Button, Card, CardContent, Spinner } from '@ax/ui';

/**
 * Shared async-screen states. These are app-local COMPOSITIONS of catalog
 * primitives (@ax/ui Card / Spinner / Alert / Button) — NOT redefined primitives
 * — so they satisfy ax/no-app-local-ui-primitives while keeping the feed,
 * favorites, notifications and search screens consistent.
 */

export function LoadingState({ label = '불러오는 중' }: { label?: string }) {
  return (
    <div className="grid place-items-center py-16">
      <Spinner className="h-6 w-6 text-muted-foreground" label={label} />
    </div>
  );
}

interface ErrorStateProps {
  message?: string;
  onRetry?: () => void;
}

export function ErrorState({ message, onRetry }: ErrorStateProps) {
  return (
    <Alert variant="error" className="flex flex-col gap-3">
      <span>{message ?? '문제가 발생했어요. 잠시 후 다시 시도해 주세요.'}</span>
      {onRetry ? (
        <span>
          <Button variant="outline" size="sm" onClick={onRetry}>
            다시 시도
          </Button>
        </span>
      ) : null}
    </Alert>
  );
}

interface EmptyStateProps {
  icon?: React.ReactNode;
  title: string;
  description?: string;
  action?: React.ReactNode;
}

export function EmptyState({ icon, title, description, action }: EmptyStateProps) {
  return (
    <Card className="border-dashed">
      <CardContent className="flex flex-col items-center gap-3 py-14 text-center">
        {icon ? (
          <span className="grid h-14 w-14 place-items-center rounded-full bg-[var(--ax-status-accent-bg)] text-[var(--ax-status-accent-fg)]">
            {icon}
          </span>
        ) : null}
        <h2 className="text-lg font-semibold text-foreground">{title}</h2>
        {description ? (
          <p className="max-w-sm text-sm text-muted-foreground">{description}</p>
        ) : null}
        {action}
      </CardContent>
    </Card>
  );
}
