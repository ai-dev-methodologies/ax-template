import * as React from 'react';
import { AlertCircle } from 'lucide-react';
import { cn } from '@/lib/utils';
import { Label } from './label';
import { Input } from './input';

interface FieldProps extends React.InputHTMLAttributes<HTMLInputElement> {
  /** Visible field label, associated via htmlFor/id. */
  label: React.ReactNode;
  /** Stable id; also seeds the hint/error description ids. */
  id: string;
  /** Helper text shown below the input when there is no error. */
  hint?: React.ReactNode;
  /** Error message; when present, marks the input invalid and replaces the hint. */
  error?: React.ReactNode;
  className?: string;
}

/**
 * A labelled input with inline hint/error and full aria wiring:
 * label[for] -> input[id]; input[aria-describedby] -> hint/error; aria-invalid
 * toggles the error visual + announces the message to assistive tech.
 */
const Field = React.forwardRef<HTMLInputElement, FieldProps>(
  ({ label, id, hint, error, className, ...inputProps }, ref) => {
    const hintId = `${id}-hint`;
    const errorId = `${id}-error`;
    const describedBy = error ? errorId : hint ? hintId : undefined;

    return (
      <div className={cn('space-y-1.5', className)}>
        <Label htmlFor={id}>{label}</Label>
        <Input
          ref={ref}
          id={id}
          aria-invalid={error ? true : undefined}
          aria-describedby={describedBy}
          {...inputProps}
        />
        {error ? (
          <p
            id={errorId}
            role="alert"
            className="flex items-center gap-1.5 text-xs font-medium text-destructive"
          >
            <AlertCircle aria-hidden="true" className="h-3.5 w-3.5 shrink-0" />
            <span>{error}</span>
          </p>
        ) : hint ? (
          <p id={hintId} className="text-xs text-muted-foreground">
            {hint}
          </p>
        ) : null}
      </div>
    );
  },
);
Field.displayName = 'Field';

export { Field };
