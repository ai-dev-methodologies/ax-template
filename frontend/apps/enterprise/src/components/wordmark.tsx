import { cn } from '@ax/ui';

interface WordmarkProps {
  size?: 'sm' | 'md' | 'lg';
  className?: string;
}

const SIZE: Record<NonNullable<WordmarkProps['size']>, string> = {
  sm: 'text-sm',
  md: 'text-base',
  lg: 'text-xl',
};

/**
 * App wordmark — composes the catalog `cn` helper + shared font tokens. The
 * mono "ax" glyph + "operator" label signals the enterprise-operator persona.
 */
export function Wordmark({ size = 'md', className }: WordmarkProps) {
  return (
    <span className={cn('inline-flex items-baseline gap-1.5 font-mono font-semibold', SIZE[size], className)}>
      <span className="rounded-[var(--radius)] bg-foreground px-1.5 py-0.5 text-background">ax</span>
      <span className="tracking-tight text-foreground">operator</span>
    </span>
  );
}
