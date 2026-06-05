import { cn } from '@ax/ui';

interface WordmarkProps {
  size?: 'sm' | 'md' | 'lg';
  className?: string;
}

const SIZE: Record<NonNullable<WordmarkProps['size']>, string> = {
  sm: 'text-base',
  md: 'text-xl',
  lg: 'text-3xl',
};

/**
 * Pay wordmark — composes the catalog `cn` helper + the persona grotesk family.
 * A calm filled "ax" trust-navy chip + a thin separator + the "pay" label set in
 * a settled medium weight signal the fintech-trust persona: precise, restrained,
 * money-handling. Distinct from the dark console's mono `⟩` prompt mark.
 */
export function Wordmark({ size = 'md', className }: WordmarkProps) {
  return (
    <span className={cn('inline-flex items-center gap-2 font-semibold', SIZE[size], className)}>
      <span className="ax-wordmark-chip rounded-[var(--radius)] px-1.5 py-0.5 font-bold leading-none tracking-tight">
        ax
      </span>
      <span aria-hidden className="text-border">|</span>
      <span className="font-medium tracking-tight text-foreground">pay</span>
    </span>
  );
}
