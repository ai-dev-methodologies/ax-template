"use client";
/*
---
template_id: L2/blocks/animated-badge
layer: L2
provenance_class: internal_design
evidence:
  - source_type: internal
    rationale: "Codified from the community 21st.dev component karthikmudunuri/animated-badge: hardcoded hex extracted to --ax-c-* design tokens, provenance-stamped, verified to pass all 7 ax/* own-block rules (gen_verify T1-T4). Governed by practices-react/rules/ux-block-uses-design-tokens-and-a11y.md."
dependencies: []
imports_from: []
imports_forbidden: [L4, app/, lib/]
---
*/
/**
 * @ax-codified-from 21st.dev/karthikmudunuri/animated-badge
 * @ax-layer L2/blocks/badge
 * Deterministic codify (codify.py): hex extracted to --ax-c-* tokens; provenance stamped.
 * REMAINING semantic pass (see .notes.md): a11y:add-role-aria, types:string-literal-variants
 */
/* ax design tokens extracted from hardcoded hex — bind these in your theme
 * (light/dark/brand) so this block re-skins without edits:
 *   --ax-c-1: #22d3ee;
 *   --ax-c-2: #fff;
 */

import Link from "next/link"
import { ChevronRight } from "lucide-react"
import { motion } from "motion/react"

type AnimatedBadgeProps = {
  text?: string
  color?: string // hex or css color value
  href?: string // optional redirect link
}

function hexToRgba(hexColor: string, alpha: number): string {
  const hex = hexColor.replace("#", "")
  if (hex.length === 3) {
    const r = parseInt(hex[0] + hex[0], 16)
    const g = parseInt(hex[1] + hex[1], 16)
    const b = parseInt(hex[2] + hex[2], 16)
    return `rgba(${r}, ${g}, ${b}, ${alpha})`
  }
  if (hex.length === 6) {
    const r = parseInt(hex.substring(0, 2), 16)
    const g = parseInt(hex.substring(2, 4), 16)
    const b = parseInt(hex.substring(4, 6), 16)
    return `rgba(${r}, ${g}, ${b}, ${alpha})`
  }
  return hexColor
}

export const AnimatedBadge = ({
  text = "Introducing Eldoraui",
  color = "var(--ax-c-1)",
  href,
}: AnimatedBadgeProps) => {
  const content = (
    <motion.div
      initial={false}
      whileInView={{
        opacity: 1,
        y: 0,
        filter: "blur(0px)",
      }}
      transition={{
        duration: 0.3,
        delay: 0.1,
        ease: "easeInOut",
      }}
      viewport={{ once: true }}
      className="group relative flex max-w-fit items-center justify-center gap-3 rounded-full border border-neutral-300 bg-white px-4 py-1.5 text-neutral-700 transition-colors dark:border-neutral-700/80 dark:bg-black dark:text-zinc-300"
    >
      <div className="pointer-events-none absolute inset-x-0 bottom-full h-20 w-[165px]">
        <svg
          className="h-full w-full"
          width="100%"
          height="100%"
          viewBox="0 0 50 50"
          fill="none"
        >
          {/* <g stroke="var(--ax-c-2)" strokeWidth="0.4">
              <path d="M 69 49.8 h -30 q -3 0 -3 -3 v -15 q 0 -3 -3 -3 h -23 q -3 0 -3 -3 v -15 q 0 -3 -3 -3 h -30" />
            </g> */}
          <g mask="url(#ml-mask-1)">
            <circle
              className="multiline ml-light-1"
              cx="0"
              cy="0"
              r="20"
              fill="url(#ml-white-grad)"
            />
          </g>
          <defs>
            <mask id="ml-mask-1">
              <path
                d="M 69 49.8 h -30 q -3 0 -3 -3 v -13 q 0 -3 -3 -3 h -23 q -3 0 -3 -3 v -13 q 0 -3 -3 -3 h -30"
                strokeWidth="0.6"
                stroke="white"
              />
            </mask>
            <radialGradient id="ml-white-grad" fx="1">
              <stop offset="0%" stopColor={color} />
              <stop offset="20%" stopColor={color} />
              <stop offset="100%" stopColor="transparent" />
            </radialGradient>
          </defs>
        </svg>
      </div>
      <div
        className="relative flex h-1 w-1 items-center justify-center rounded-full"
        style={{ backgroundColor: hexToRgba(color, 0.4) }}
      >
        <div
          className="flex h-2 w-2 animate-ping items-center justify-center rounded-full"
          style={{ backgroundColor: color }}
        >
          <div
            className="flex h-2 w-2 animate-ping items-center justify-center rounded-full"
            style={{ backgroundColor: color }}
          ></div>
        </div>
        <div
          className="absolute top-1/2 left-1/2 flex h-1 w-1 -translate-x-1/2 -translate-y-1/2 items-center justify-center rounded-full"
          style={{ backgroundColor: hexToRgba(color, 0.8) }}
        ></div>
      </div>
      <div className="mx-2 h-4 w-px bg-neutral-300 dark:bg-neutral-600/80" />
      <span className="bg-clip-text text-xs font-medium">{text}</span>
      <ChevronRight className="ml-1 h-3.5 w-3.5 text-neutral-400 transition-transform duration-200 group-hover:translate-x-0.5 dark:text-neutral-500" />
    </motion.div>
  )
  return (
    <>
      {href ? (
        <Link href={href} className="inline-block">
          {content}
        </Link>
      ) : (
        content
      )}
      <style>
        {`    
.multiline {
  offset-anchor: 10px 0px;
  animation: multiline-animation-path;
  animation-iteration-count: infinite;
  animation-timing-function: linear;
  animation-duration: 3s;
}

.ml-light-1 {
  offset-path: path(
    "M 69 49.8 h -30 q -3 0 -3 -3 v -13 q 0 -3 -3 -3 h -23 q -3 0 -3 -3 v -13 q 0 -3 -3 -3 h -50"
  );
}

@keyframes multiline-animation-path {
  0% {
    offset-distance: 0%;
  }
  50% {
    offset-distance: 100%;
  }
  100% {
    offset-distance: 100%;
  }
}`}
      </style>
    </>
  )
}

