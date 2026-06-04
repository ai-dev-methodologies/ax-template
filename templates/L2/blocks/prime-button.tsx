/*
---
template_id: L2/blocks/prime-button
layer: L2
provenance_class: internal_design
evidence:
  - source_type: internal
    rationale: "Codified from the community 21st.dev component iamsatish4564/prime-button: hardcoded hex -> --ax-c-* design tokens; the shadcn cn util is inlined (clsx+tailwind-merge) and any UI-kit import removed so the block imports no @/ alias (ax block self-containment). Passes all 7 ax/* own-block rules. Governed by practices-react/rules/ux-block-uses-design-tokens-and-a11y.md."
dependencies: []
imports_from: []
imports_forbidden: [L4, app/, lib/]
---
*/
/**
 * @ax-codified-from 21st.dev/iamsatish4564/prime-button
 * @ax-layer L2/blocks/button
 * Deterministic codify (codify.py): hex extracted to --ax-c-* tokens; provenance stamped.
 * REMAINING semantic pass (see .notes.md): a11y:add-role-aria
 */
'use client';

import * as React from 'react';
import { AnimatePresence, motion, type Variants } from 'motion/react';
import { Loader2, Check, X } from 'lucide-react';
import { cva, type VariantProps } from 'class-variance-authority';
import { clsx, type ClassValue } from 'clsx'
import { twMerge } from 'tailwind-merge'
// cn inlined (ax blocks avoid the lib alias); behavior-identical to the shadcn util
function cn(...inputs: ClassValue[]) { return twMerge(clsx(inputs)) }

const buttonVariants = cva(
  'relative inline-flex items-center justify-center rounded-md text-sm font-medium transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 disabled:pointer-events-none disabled:opacity-50 select-none overflow-hidden',
  {
    variants: {
      variant: {
        default:
          'bg-primary text-primary-foreground shadow-sm hover:bg-primary/90',
        destructive:
          'bg-destructive text-destructive-foreground shadow-sm hover:bg-destructive/90',
        outline:
          'border border-input bg-background shadow-sm hover:bg-accent hover:text-accent-foreground',
        secondary:
          'bg-secondary text-secondary-foreground shadow-sm hover:bg-secondary/80',
        ghost: 'hover:bg-accent hover:text-accent-foreground',
        link: 'text-primary underline-offset-4 hover:underline',
      },
      size: {
        default: 'h-9 px-4 py-2',
        sm: 'h-8 rounded-md px-3 text-xs',
        lg: 'h-10 rounded-md px-8',
        icon: 'h-9 w-9',
      },
    },
    defaultVariants: {
      variant: 'default',
      size: 'default',
    },
  }
);

const textVariants: Variants = {
  hidden: {
    y: 12,
    opacity: 0,
    filter: 'blur(4px)',
  },
  visible: {
    y: 0,
    opacity: 1,
    filter: 'blur(0px)',
    transition: {
      type: 'spring',
      stiffness: 500,
      damping: 25,
      mass: 0.5,
    },
  },
  exit: {
    y: -12,
    opacity: 0,
    filter: 'blur(4px)',
    transition: {
      duration: 0.15,
    },
  },
};

type ActionState = 'idle' | 'loading' | 'success' | 'error';

interface PrimeButtonProps
  extends React.ButtonHTMLAttributes<HTMLButtonElement>,
    VariantProps<typeof buttonVariants> {
  /** Current interactive state of the button. */
  actionState?: ActionState;
  /** Text to display during the 'loading' state. */
  loadingText?: string;
  /** Text to display during the 'success' state. */
  successText?: string;
  /** Text to display during the 'error' state. */
  errorText?: string;
  /** Duration (in ms) before the button reverts from success/error back to idle. */
  resetDelay?: number;
}

/**
 * An interactive button component that manages async action states (loading, success, error)
 * with built-in layout animations and auto-reset functionality.
 */
export const PrimeButton = React.forwardRef<
  HTMLButtonElement,
  PrimeButtonProps
>(
  (
    {
      className,
      variant,
      size,
      actionState = 'idle',
      loadingText = 'Processing',
      successText = 'Saved',
      errorText = 'Error',
      resetDelay = 2500,
      children,
      onClick,
      ...props
    },
    ref
  ) => {
    const [internalState, setInternalState] =
      React.useState<ActionState>(actionState);

    React.useEffect(() => {
      setInternalState(actionState);
    }, [actionState]);

    React.useEffect(() => {
      if (
        (internalState === 'success' || internalState === 'error') &&
        resetDelay
      ) {
        const timer = setTimeout(() => {
          setInternalState('idle');
        }, resetDelay);
        return () => clearTimeout(timer);
      }
    }, [internalState, resetDelay]);

    const stateStyles = {
      idle: '',
      loading: 'cursor-wait opacity-90',
      success:
        'bg-[var(--ax-status-success-fg)] text-white hover:bg-[var(--ax-status-success-fg)] border-[var(--ax-status-success-fg)] ring-offset-[var(--ax-status-success-fg)]',
      error:
        'bg-[var(--ax-status-danger-fg)] text-white hover:bg-[var(--ax-status-danger-fg)] border-[var(--ax-status-danger-fg)] ring-offset-[var(--ax-status-danger-fg)]',
    };

    const isIconOnly = size === 'icon';

    return (
      <motion.button
        ref={ref}
        // Automates width animations when content changes (e.g., "Save" -> "Saving...")
        layout
        className={cn(
          buttonVariants({ variant, size, className }),
          stateStyles[internalState],
          'shadow-[inset_0_1px_0_0_rgba(255,255,255,0.1)]'
        )}
        onClick={onClick}
        whileTap={internalState === 'idle' ? { scale: 0.98 } : undefined}
        transition={{ type: 'spring', stiffness: 500, damping: 30 }}
        {...(props as any)} // Cast required to resolve conflict between Motion props and HTMLButton attributes
      >
        <AnimatePresence mode='popLayout' initial={false}>
          {internalState === 'loading' ? (
            <motion.span
              key='loading'
              variants={textVariants}
              initial='hidden'
              animate='visible'
              exit='exit'
              className='flex items-center gap-2'
            >
              <Loader2 className='h-4 w-4 animate-spin' />
              {!isIconOnly && <span>{loadingText}</span>}
            </motion.span>
          ) : internalState === 'success' ? (
            <motion.span
              key='success'
              variants={textVariants}
              initial='hidden'
              animate='visible'
              exit='exit'
              className='flex items-center gap-2 font-semibold'
            >
              <Check className='h-4 w-4' strokeWidth={3} />
              {!isIconOnly && <span>{successText}</span>}
            </motion.span>
          ) : internalState === 'error' ? (
            <motion.span
              key='error'
              variants={textVariants}
              initial='hidden'
              animate='visible'
              exit='exit'
              className='flex items-center gap-2 font-semibold'
            >
              <X className='h-4 w-4' strokeWidth={3} />
              {!isIconOnly && <span>{errorText}</span>}
            </motion.span>
          ) : (
            <motion.span
              key='idle'
              variants={textVariants}
              initial='hidden'
              animate='visible'
              exit='exit'
              className='flex items-center gap-2'
            >
              {children}
            </motion.span>
          )}
        </AnimatePresence>
      </motion.button>
    );
  }
);

PrimeButton.displayName = 'PrimeButton';