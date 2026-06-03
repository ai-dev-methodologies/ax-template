/*
---
template_id: L2/blocks/animated-feature-card
layer: L2
provenance_class: internal_design
evidence:
  - source_type: internal
    rationale: "Codified from the community 21st.dev component ravikatiyar162/animated-feature-card: hardcoded hex -> --ax-c-* design tokens; the shadcn cn util is inlined (clsx+tailwind-merge) and any UI-kit import removed so the block imports no @/ alias (ax block self-containment). Passes all 7 ax/* own-block rules. Governed by practices-react/rules/ux-block-uses-design-tokens-and-a11y.md."
dependencies: []
imports_from: []
imports_forbidden: [L4, app/, lib/]
---
*/
/**
 * @ax-codified-from 21st.dev/ravikatiyar162/animated-feature-card
 * @ax-layer L2/blocks/card
 * Deterministic codify (codify.py): hex extracted to --ax-c-* tokens; provenance stamped.
 * REMAINING semantic pass (see .notes.md): a11y:add-role-aria, types:string-literal-variants
 */
import React from 'react';
import { motion } from 'framer-motion';
import { clsx, type ClassValue } from 'clsx'
import { twMerge } from 'tailwind-merge'
// cn inlined (ax blocks avoid the lib alias); behavior-identical to the shadcn util
function cn(...inputs: ClassValue[]) { return twMerge(clsx(inputs)) }

// Props interface for type-safety and reusability
interface AnimatedFeatureCardProps {
  title: string;
  description: string;
  imageSrc: string;
  featureNumber: string;
  handle: string;
  className?: string;
}

/**
 * A reusable card component for showcasing features with an animation effect.
 * It's designed to be responsive and theme-adaptive using shadcn's CSS variables.
 */
export const AnimatedFeatureCard = ({
  title,
  description,
  imageSrc,
  featureNumber,
  handle,
  className,
}: AnimatedFeatureCardProps) => {
  // Animation variants for framer-motion
  const cardVariants = {
    offscreen: {
      y: 50,
      opacity: 0,
    },
    onscreen: {
      y: 0,
      opacity: 1,
      transition: {
        type: 'spring',
        bounce: 0.4,
        duration: 0.8,
      },
    },
  };

  return (
    <motion.div
      className={cn(
        'relative flex w-full max-w-sm flex-col overflow-hidden rounded-2xl bg-card p-6 shadow-sm',
        className
      )}
      initial="offscreen"
      whileInView="onscreen"
      viewport={{ once: true, amount: 0.5 }}
      variants={cardVariants}
      whileHover={{ scale: 1.02, transition: { duration: 0.2 } }}
    >
      {/* Top section: Title */}
      <div className="mb-6 rounded-lg bg-background/50 p-3 text-center text-sm text-card-foreground">
        <p>{title}</p>
      </div>

      {/* Middle section: Image */}
      <div className="flex flex-grow items-center justify-center">
        <img
          src={imageSrc}
          alt={title}
          className="h-auto w-full max-w-[250px] object-contain"
        />
      </div>

      {/* Bottom section: Description and metadata */}
      <div className="mt-6 flex flex-col items-center text-center">
        <p className="text-lg font-medium text-foreground">{description}</p>
      </div>

      <div className="mt-8 flex items-center justify-between text-muted-foreground">
        <span className="text-sm font-mono">{featureNumber}</span>
        <span className="text-sm font-medium">{handle}</span>
      </div>
    </motion.div>
  );
};