import * as React from 'react';
import { cn } from './utils';

type InputProps = React.InputHTMLAttributes<HTMLInputElement>;

const Input = React.forwardRef<HTMLInputElement, InputProps>(
  ({ className, type, ...props }, ref) => {
    return (
      <input
        ref={ref}
        type={type}
        className={cn(
          'peer flex h-11 w-full rounded-[var(--radius)] border border-input bg-background px-3.5 py-2 text-sm text-foreground shadow-sm transition-[border-color,box-shadow] duration-200',
          'placeholder:text-muted-foreground/70',
          'focus-visible:border-ring focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring/40',
          'disabled:cursor-not-allowed disabled:opacity-50',
          // aria-invalid drives the error visual without inline styles
          'aria-[invalid=true]:border-destructive aria-[invalid=true]:focus-visible:ring-destructive/30',
          'file:border-0 file:bg-transparent file:text-sm file:font-medium',
          className,
        )}
        {...props}
      />
    );
  },
);
Input.displayName = 'Input';

export { Input };
