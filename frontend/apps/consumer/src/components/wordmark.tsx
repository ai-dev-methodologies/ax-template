import { cn } from '@ax/ui';

interface WordmarkProps {
  size?: 'sm' | 'md' | 'lg';
  className?: string;
}

const SIZE: Record<NonNullable<WordmarkProps['size']>, string> = {
  sm: 'text-base',
  md: 'text-lg',
  lg: 'text-2xl',
};

/**
 * App wordmark — composes the catalog `cn` helper + the consumer display font
 * (Quicksand via font-display). The rounded "ax" chip in the high-saturation
 * accent token + the soft "moment" label signal the consumer-delight persona.
 */
export function Wordmark({ size = 'md', className }: WordmarkProps) {
  return (
    <span className={cn('inline-flex items-baseline gap-1.5 font-display font-bold', SIZE[size], className)}>
      <span className="ax-wordmark-chip rounded-[calc(var(--radius)*0.6)] px-2 py-0.5">ax</span>
      <span className="tracking-tight text-foreground">moment</span>
    </span>
  );
}
