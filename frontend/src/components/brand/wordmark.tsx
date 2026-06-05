import { cn } from '@/lib/utils';

interface WordmarkProps {
  /** Visual size of the lockup. */
  size?: 'sm' | 'md' | 'lg';
  /** Hide the "transform" descriptor (used in tight nav contexts). */
  compact?: boolean;
  className?: string;
}

const SIZES = {
  sm: { mark: 'h-7 w-7 text-sm', word: 'text-base', sub: 'text-[0.6rem]' },
  md: { mark: 'h-9 w-9 text-base', word: 'text-lg', sub: 'text-[0.65rem]' },
  lg: { mark: 'h-12 w-12 text-xl', word: 'text-2xl', sub: 'text-xs' },
} as const;

/**
 * The product wordmark: a monospace "ax" glyph in a dark tile paired with the
 * "transform" descriptor. Mono treatment signals the AI-tooling identity and
 * keeps the lockup distinct from generic SaaS logos.
 */
export function Wordmark({ size = 'md', compact = false, className }: WordmarkProps) {
  const s = SIZES[size];
  return (
    <span className={cn('inline-flex items-center gap-2.5', className)}>
      <span
        aria-hidden="true"
        className={cn(
          'grid place-items-center rounded-[calc(var(--radius)-1px)] bg-primary font-mono font-semibold tracking-tight text-primary-foreground shadow-sm',
          'ring-1 ring-inset ring-white/10',
          s.mark,
        )}
      >
        ax
      </span>
      <span className="flex flex-col leading-none">
        <span className={cn('font-semibold tracking-tight text-foreground', s.word)}>
          ax<span className="text-muted-foreground">·template</span>
        </span>
        {!compact && (
          <span className={cn('mt-1 font-mono uppercase tracking-[0.2em] text-muted-foreground', s.sub)}>
            ai transform
          </span>
        )}
      </span>
    </span>
  );
}
