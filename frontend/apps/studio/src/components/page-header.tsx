import React from 'react';

interface PageHeaderProps {
  title: string;
  description?: string;
  /** Optional trailing action(s), e.g. an upload button. */
  action?: React.ReactNode;
}

/**
 * Screen masthead — a chunky display title (the persona's high scale-contrast
 * typographic signature) + an optional description and trailing action. Pure
 * presentational layout; no catalog primitive is redefined here.
 */
export function PageHeader({ title, description, action }: PageHeaderProps) {
  return (
    <div className="mb-7 flex flex-wrap items-end justify-between gap-4">
      <div className="space-y-1">
        <h1 className="ax-display text-3xl font-extrabold tracking-tight text-foreground sm:text-4xl">
          {title}
        </h1>
        {description ? (
          <p className="max-w-2xl text-[0.95rem] text-muted-foreground">{description}</p>
        ) : null}
      </div>
      {action ? <div className="shrink-0">{action}</div> : null}
    </div>
  );
}
