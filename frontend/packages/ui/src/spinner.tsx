import { Loader2 } from 'lucide-react';
import { cn } from './utils';

interface SpinnerProps extends React.SVGProps<SVGSVGElement> {
  /** Optional accessible label; defaults to a visually-hidden "로딩 중". */
  label?: string;
}

/**
 * A token-driven loading spinner. `animate-spin` is paused under
 * prefers-reduced-motion via the global rule in globals.css.
 */
export function Spinner({ className, label = '로딩 중', ...props }: SpinnerProps) {
  return (
    <span role="status" className="inline-flex items-center">
      <Loader2
        aria-hidden="true"
        className={cn('h-4 w-4 animate-spin text-current', className)}
        {...props}
      />
      <span className="sr-only">{label}</span>
    </span>
  );
}
