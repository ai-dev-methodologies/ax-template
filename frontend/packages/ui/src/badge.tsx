import * as React from 'react';
import { cva, type VariantProps } from 'class-variance-authority';
import { cn } from './utils';

// Status pills resolve against the WCAG-locked ax status tokens (globals.css).
const badgeVariants = cva(
  'inline-flex items-center gap-1 rounded-full border px-2.5 py-0.5 text-xs font-medium leading-none',
  {
    variants: {
      tone: {
        success:
          'border-[color-mix(in_oklab,var(--ax-status-success-fg)_25%,transparent)] bg-[var(--ax-status-success-bg)] text-[var(--ax-status-success-fg)]',
        danger:
          'border-[color-mix(in_oklab,var(--ax-status-danger-fg)_25%,transparent)] bg-[var(--ax-status-danger-bg)] text-[var(--ax-status-danger-fg)]',
        warning:
          'border-[color-mix(in_oklab,var(--ax-status-warning-fg)_25%,transparent)] bg-[var(--ax-status-warning-bg)] text-[var(--ax-status-warning-fg)]',
        info:
          'border-[color-mix(in_oklab,var(--ax-status-info-fg)_25%,transparent)] bg-[var(--ax-status-info-bg)] text-[var(--ax-status-info-fg)]',
        neutral:
          'border-[color-mix(in_oklab,var(--ax-status-neutral-fg)_20%,transparent)] bg-[var(--ax-status-neutral-bg)] text-[var(--ax-status-neutral-fg)]',
        accent:
          'border-[color-mix(in_oklab,var(--ax-status-accent-fg)_25%,transparent)] bg-[var(--ax-status-accent-bg)] text-[var(--ax-status-accent-fg)]',
      },
    },
    defaultVariants: { tone: 'neutral' },
  },
);

interface BadgeProps
  extends React.HTMLAttributes<HTMLSpanElement>,
    VariantProps<typeof badgeVariants> {}

function Badge({ className, tone, ...props }: BadgeProps) {
  return <span className={cn(badgeVariants({ tone }), className)} {...props} />;
}

export { Badge, badgeVariants };
