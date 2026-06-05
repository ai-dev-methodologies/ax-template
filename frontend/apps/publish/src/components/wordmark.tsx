import { cn } from '@ax/ui';

interface WordmarkProps {
  size?: 'sm' | 'md' | 'lg';
  className?: string;
}

const SIZE: Record<NonNullable<WordmarkProps['size']>, string> = {
  sm: 'text-lg',
  md: 'text-2xl',
  lg: 'text-4xl',
};

/**
 * Masthead wordmark — composes the catalog `cn` helper + the editorial display
 * serif (Playfair Display via font-display). The sharp "ax" ink chip + the
 * letterspaced "PRESS" caption signal the editorial-luxury persona: high
 * contrast, zero radius, serif at scale.
 */
export function Wordmark({ size = 'md', className }: WordmarkProps) {
  return (
    <span className={cn('inline-flex items-baseline gap-2 font-display', SIZE[size], className)}>
      <span className="ax-wordmark-chip px-2 py-0.5 font-bold italic leading-none">ax</span>
      <span className="font-semibold uppercase tracking-[0.18em] text-foreground">Press</span>
    </span>
  );
}
