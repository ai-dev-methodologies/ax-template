'use client';

import React from 'react';
import { AlertTriangle, Inbox, RefreshCw } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Spinner } from '@/components/ui/spinner';
import { cn } from '@/lib/utils';

interface PageHeaderProps {
  eyebrow: string;
  title: string;
  description: string;
  /** Optional right-aligned actions (e.g. a refresh or create button). */
  actions?: React.ReactNode;
}

/** Restrained enterprise page header: mono eyebrow, low scale-contrast title. */
export function PageHeader({ eyebrow, title, description, actions }: PageHeaderProps) {
  return (
    <header className="flex flex-col gap-3 border-b border-border pb-5 sm:flex-row sm:items-end sm:justify-between">
      <div className="space-y-1">
        <p className="font-mono text-[0.7rem] uppercase tracking-[0.18em] text-muted-foreground">
          {eyebrow}
        </p>
        <h1 className="text-2xl font-semibold tracking-tight text-foreground">{title}</h1>
        <p className="text-sm text-muted-foreground">{description}</p>
      </div>
      {actions ? <div className="flex shrink-0 items-center gap-2">{actions}</div> : null}
    </header>
  );
}

/** Centered loading panel with an accessible status spinner. */
export function LoadingPanel({ label }: { label: string }) {
  return (
    <div className="flex items-center justify-center gap-3 rounded-[var(--radius)] border border-dashed border-border bg-card/40 py-16 text-sm text-muted-foreground">
      <Spinner className="h-5 w-5" label={label} />
      <span>{label}</span>
    </div>
  );
}

interface ErrorPanelProps {
  message: string;
  onRetry?: () => void;
}

/** Assertive error panel (role=alert) with an optional retry. */
export function ErrorPanel({ message, onRetry }: ErrorPanelProps) {
  return (
    <div
      role="alert"
      className="flex flex-col items-start gap-3 rounded-[var(--radius)] border border-[color-mix(in_oklab,var(--ax-status-danger-fg)_25%,transparent)] bg-[var(--ax-status-danger-bg)] px-4 py-5 text-sm text-[var(--ax-status-danger-fg)] sm:flex-row sm:items-center sm:justify-between"
    >
      <span className="flex items-center gap-2 leading-relaxed">
        <AlertTriangle aria-hidden="true" className="h-4 w-4 shrink-0" />
        {message}
      </span>
      {onRetry ? (
        <Button variant="outline" size="sm" onClick={onRetry} className="shrink-0">
          <RefreshCw aria-hidden="true" />
          다시 시도
        </Button>
      ) : null}
    </div>
  );
}

interface EmptyStateProps {
  title: string;
  description: string;
  icon?: React.ReactNode;
  action?: React.ReactNode;
}

/** Neutral empty state shown when a query succeeds with zero rows. */
export function EmptyState({ title, description, icon, action }: EmptyStateProps) {
  return (
    <div className="flex flex-col items-center justify-center gap-2 rounded-[var(--radius)] border border-dashed border-border bg-card/40 px-6 py-16 text-center">
      <span className="grid h-11 w-11 place-items-center rounded-full bg-secondary text-muted-foreground">
        {icon ?? <Inbox aria-hidden="true" className="h-5 w-5" />}
      </span>
      <p className="text-sm font-medium text-foreground">{title}</p>
      <p className="max-w-sm text-sm text-muted-foreground">{description}</p>
      {action ? <div className="mt-2">{action}</div> : null}
    </div>
  );
}

/** A dense, bordered table surface shared by the console data screens. */
export function TableShell({ children, className }: { children: React.ReactNode; className?: string }) {
  return (
    <div className={cn('overflow-x-auto rounded-[var(--radius)] border border-border bg-card', className)}>
      <table className="w-full border-collapse text-sm">{children}</table>
    </div>
  );
}
