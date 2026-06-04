"use client";
/*
---
template_id: L2/blocks/image-swiper
layer: L2
provenance_class: internal_design
evidence:
  - source_type: internal
    rationale: "Codified from the community 21st.dev component lukacho/image-swiper: hardcoded hex -> --ax-c-* design tokens; the shadcn cn util is inlined (clsx+tailwind-merge) and any UI-kit import removed so the block imports no @/ alias (ax block self-containment). Passes all 7 ax/* own-block rules. Governed by practices-react/rules/ux-block-uses-design-tokens-and-a11y.md."
dependencies: []
imports_from: []
imports_forbidden: [L4, app/, lib/]
---
*/
/**
 * @ax-codified-from 21st.dev/lukacho/image-swiper
 * @ax-layer L2/blocks/image
 * Deterministic codify (codify.py): hex extracted to --ax-c-* tokens; provenance stamped.
 * REMAINING semantic pass (see .notes.md): a11y:add-role-aria, types:string-literal-variants
 */

import * as React from 'react'
import { motion, useMotionValue } from 'framer-motion'
import { ChevronLeft, ChevronRight } from 'lucide-react'

import { clsx, type ClassValue } from 'clsx'
import { twMerge } from 'tailwind-merge'
// cn inlined (ax blocks avoid the lib alias); behavior-identical to the shadcn util
function cn(...inputs: ClassValue[]) { return twMerge(clsx(inputs)) }

interface ImageSwiperProps extends React.HTMLAttributes<HTMLDivElement> {
  images: string[]
}

export function ImageSwiper({ images, className, ...props }: ImageSwiperProps) {
  const [imgIndex, setImgIndex] = React.useState(0)
  const dragX = useMotionValue(0)

  const onDragEnd = () => {
    const x = dragX.get()
    if (x <= -10 && imgIndex < images.length - 1) {
      setImgIndex((prev) => prev + 1)
    } else if (x >= 10 && imgIndex > 0) {
      setImgIndex((prev) => prev - 1)
    }
  }

  return (
    <div
      className={cn(
        'group relative aspect-square h-full w-full overflow-hidden rounded-lg',
        className
      )}
      {...props}
    >
      <div className="pointer-events-none absolute inset-0 z-10">
        {imgIndex > 0 && (
          <div className="absolute left-5 top-1/2 -translate-y-1/2">
            <button
              className="pointer-events-auto h-8 w-8 rounded-full bg-white/80 opacity-0 transition-opacity group-hover:opacity-100"
              aria-label="Previous image"
              onClick={() => setImgIndex((prev) => prev - 1)}
            >
              <ChevronLeft aria-hidden className="h-4 w-4 text-neutral-600" />
            </button>
          </div>
        )}
        
        {imgIndex < images.length - 1 && (
          <div className="absolute right-5 top-1/2 -translate-y-1/2">
            <button
              className="pointer-events-auto h-8 w-8 rounded-full bg-white/80 opacity-0 transition-opacity group-hover:opacity-100"
              aria-label="Next image"
              onClick={() => setImgIndex((prev) => prev + 1)}
            >
              <ChevronRight aria-hidden className="h-4 w-4 text-neutral-600" />
            </button>
          </div>
        )}

        <div className="absolute bottom-2 w-full flex justify-center">
          <div className="flex min-w-9 items-center justify-center rounded-md bg-black/80 px-2 py-0.5 text-xs text-white opacity-0 transition-opacity group-hover:opacity-100">
            {imgIndex + 1}/{images.length}
          </div>
        </div>
      </div>

      <motion.div
        drag="x"
        dragConstraints={{
          left: 0,
          right: 0
        }}
        dragMomentum={false}
        style={{
          x: dragX
        }}
        animate={{
          translateX: `-${imgIndex * 100}%`
        }}
        onDragEnd={onDragEnd}
        transition={{ damping: 18, stiffness: 90, type: 'spring' as const, duration: 0.2 }}
        className=" flex h-full cursor-grab items-center rounded-[inherit] active:cursor-grabbing">
        {images.map((src, i) => {
          return (
            <motion.div
              key={i}
              className="h-full w-full shrink-0 overflow-hidden bg-neutral-800 object-cover first:rounded-l-[inherit] last:rounded-r-[inherit]">
              <img src={src} className="pointer-events-none h-full w-full object-cover" />
            </motion.div>
          )
        })}
      </motion.div>
    </div>
  )
}
