import * as React from 'react';
import { AlertCircle, CheckCircle2, Info } from 'lucide-react';
import { cva, type VariantProps } from 'class-variance-authority';
import { cn } from './utils';

// Status colors resolve against the WCAG-locked ax status tokens (globals.css),
// never raw hex. fg/bg pairs are pre-verified for >= 4.5:1 contrast.
const alertVariants = cva(
  'flex items-start gap-2.5 rounded-[var(--radius)] border px-3.5 py-3 text-sm',
  {
    variants: {
      variant: {
        error:
          'border-[color-mix(in_oklab,var(--ax-status-danger-fg)_25%,transparent)] bg-[var(--ax-status-danger-bg)] text-[var(--ax-status-danger-fg)]',
        success:
          'border-[color-mix(in_oklab,var(--ax-status-success-fg)_25%,transparent)] bg-[var(--ax-status-success-bg)] text-[var(--ax-status-success-fg)]',
        info:
          'border-[color-mix(in_oklab,var(--ax-status-info-fg)_25%,transparent)] bg-[var(--ax-status-info-bg)] text-[var(--ax-status-info-fg)]',
      },
    },
    defaultVariants: { variant: 'info' },
  },
);

const ICONS = { error: AlertCircle, success: CheckCircle2, info: Info } as const;

interface AlertProps
  extends React.HTMLAttributes<HTMLDivElement>,
    VariantProps<typeof alertVariants> {}

function Alert({ className, variant = 'info', children, ...props }: AlertProps) {
  const Icon = ICONS[variant ?? 'info'];
  // Errors should be announced assertively; status info/success politely.
  const role = variant === 'error' ? 'alert' : 'status';
  return (
    <div role={role} className={cn(alertVariants({ variant }), className)} {...props}>
      <Icon aria-hidden="true" className="mt-0.5 h-4 w-4 shrink-0" />
      <div className="min-w-0 leading-relaxed">{children}</div>
    </div>
  );
}

export { Alert, alertVariants };
