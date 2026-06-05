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
 * Studio wordmark — composes the catalog `cn` helper + the persona display face.
 * A chunky rounded "ax" chip on a vivid magenta→tangerine gradient + a bold
 * "studio" label set in the display font signal the playful-creator persona:
 * joyful, vibrant, creative. Distinct from the fintech trust-navy chip and the
 * dark console's mono prompt mark.
 */
export function Wordmark({ size = 'md', className }: WordmarkProps) {
  return (
    <span className={cn('ax-display inline-flex items-center gap-2 font-extrabold', SIZE[size], className)}>
      <span className="ax-wordmark-chip rounded-[calc(var(--radius)/2)] px-2 py-0.5 leading-none tracking-tight">
        ax
      </span>
      <span className="tracking-tight text-foreground">studio</span>
    </span>
  );
}
