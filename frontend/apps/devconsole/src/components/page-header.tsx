import React from 'react';

/**
 * App-local screen header — a title + monospace route caption + optional action
 * slot. Pure layout around token classes (no primitive naming), so it composes
 * the catalog without re-implementing it.
 */
interface PageHeaderProps {
  title: string;
  /** Monospace route/endpoint caption shown beneath the title. */
  endpoint: string;
  description?: string;
  action?: React.ReactNode;
}

export function PageHeader({ title, endpoint, description, action }: PageHeaderProps) {
  return (
    <header className="mb-6 flex flex-wrap items-start justify-between gap-4 border-b border-border pb-4">
      <div className="min-w-0">
        <h1 className="text-xl font-semibold tracking-tight text-foreground">{title}</h1>
        <p className="mt-1 truncate font-mono text-xs text-[var(--ax-status-accent-fg)]">{endpoint}</p>
        {description ? (
          <p className="mt-2 max-w-2xl text-sm text-muted-foreground">{description}</p>
        ) : null}
      </div>
      {action ? <div className="shrink-0">{action}</div> : null}
    </header>
  );
}
