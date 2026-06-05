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
 * Console wordmark — composes the catalog `cn` helper + the persona mono family.
 * The filled "ax" cyan chip + a terminal `⟩` prompt glyph + the "console"
 * lowercase mono label signal the developer-tool persona: dark, technical, tight
 * 4px corners. Distinct from the editorial serif masthead.
 */
export function Wordmark({ size = 'md', className }: WordmarkProps) {
  return (
    <span className={cn('inline-flex items-center gap-2 font-mono', SIZE[size], className)}>
      <span className="ax-wordmark-chip rounded px-1.5 py-0.5 font-bold leading-none">ax</span>
      <span aria-hidden className="text-[var(--ax-status-accent-fg)]">⟩</span>
      <span className="font-semibold lowercase tracking-tight text-foreground">console</span>
    </span>
  );
}
