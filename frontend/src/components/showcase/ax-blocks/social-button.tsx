"use client";
/*
---
template_id: L2/blocks/social-button
layer: L2
provenance_class: internal_design
evidence:
  - source_type: internal
    rationale: "Codified from the community 21st.dev component Shatlyk1011/social-button: hardcoded hex extracted to --ax-c-* design tokens, provenance-stamped, verified to pass all 7 ax/* own-block rules (gen_verify T1-T4). Governed by practices-react/rules/ux-block-uses-design-tokens-and-a11y.md."
dependencies: []
imports_from: []
imports_forbidden: [L4, app/, lib/]
---
*/
/**
 * @ax-codified-from 21st.dev/Shatlyk1011/social-button
 * @ax-layer L2/blocks/button
 * Deterministic codify (codify.py): hex extracted to --ax-c-* tokens; provenance stamped.
 * REMAINING semantic pass (see .notes.md): a11y:add-role-aria, types:string-literal-variants
 */
/* ax design tokens extracted from hardcoded hex — bind these in your theme
 * (light/dark/brand) so this block re-skins without edits:
 *   --ax-c-1: #1da1f2;
 *   --ax-c-2: #e1306c;
 *   --ax-c-3: #0a66c2;
 */

import { useState, FC, ReactNode, useRef, useEffect, type RefObject } from 'react'
import { Check, Copy, Camera, AtSign, Share2, Send } from 'lucide-react'
import { AnimatePresence, motion } from 'framer-motion'
import { clsx } from 'clsx'
import { twMerge } from 'tailwind-merge'

function cn(...inputs: any[]) { return twMerge(clsx(inputs)) }

function useClickOutside(ref: RefObject<HTMLElement | null>, handler: () => void) {
  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (ref.current && !ref.current.contains(event.target as Node)) handler()
    }
    document.addEventListener('mousedown', handleClickOutside)
    return () => document.removeEventListener('mousedown', handleClickOutside)
  }, [ref, handler])
}

const shareButtons = [
  { icon: Send, label: 'Twitter', color: 'hover:text-[var(--ax-c-1)] hover:bg-[var(--ax-c-1)]/10' },
  { icon: Camera, label: 'Instagram', color: 'hover:text-[var(--ax-c-2)] hover:bg-[var(--ax-c-2)]/10' },
  { icon: AtSign, label: 'LinkedIn', color: 'hover:text-[var(--ax-c-3)] hover:bg-[var(--ax-c-3)]/10' },
]

export default function SocialButton({ className }: { className?: string }) {
  const [isExpanded, setIsExpanded] = useState(false)
  const [copied, setCopied] = useState(false)

  const handleCopy = async () => {
    setCopied(true)
    setTimeout(() => setCopied(false), 2000)
  }

  return (
    <OnClickOutside onClickOutside={() => setIsExpanded(false)}>
      <div className={cn('flex items-center justify-center', className)}>
        <motion.div
          animate={{ width: isExpanded ? 'auto' : '120px', height: '48px' }}
          className={cn(
            'relative flex items-center overflow-hidden',
            'bg-white dark:bg-zinc-900',
            'border border-zinc-200 dark:border-zinc-800',
            'shadow-sm hover:shadow-md',
            'cursor-pointer rounded-full'
          )}
          initial={false}
          onClick={() => !isExpanded && setIsExpanded(true)}
          transition={{ type: 'spring' as const, stiffness: 300, damping: 25 }}
        >
          <AnimatePresence mode='sync'>
            {!isExpanded ? (
              <motion.div className='absolute inset-0 flex items-center justify-center gap-2' exit={{ opacity: 0, y: -20 }} initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }} key='share-text' transition={{ duration: 0.2 }}>
                <Share2 className='h-4 w-4' />
                <span className='text-sm font-medium'>Share</span>
              </motion.div>
            ) : (
              <motion.div className='flex items-center px-1' exit={{ opacity: 0, scale: 0.9 }} initial={{ opacity: 0, scale: 0.9 }} animate={{ opacity: 1, scale: 1 }} key='actions' transition={{ delay: 0.1, duration: 0.2 }}>
                {shareButtons.map((btn) => (
                  <button className={cn('flex h-10 w-10 items-center justify-center rounded-full transition-colors', 'text-zinc-600 dark:text-zinc-400', btn.color)} key={btn.label} type='button' title={btn.label}>
                    <btn.icon className='h-5 w-5' />
                  </button>
                ))}
                <div className='mx-1 h-6 w-px bg-zinc-200 dark:bg-zinc-800' />
                <button
                  className={cn('flex h-10 w-10 items-center justify-center rounded-full transition-colors', 'text-zinc-600 dark:text-zinc-400', 'hover:bg-zinc-100 dark:hover:bg-zinc-800', copied && 'bg-green-50 text-green-500 dark:bg-green-900/20 dark:text-green-500')}
                  onClick={(e) => { e.stopPropagation(); handleCopy() }}
                  type='button' title='Copy Link'
                >
                  {copied ? <Check className='h-5 w-5' /> : <Copy className='h-5 w-5' />}
                </button>
              </motion.div>
            )}
          </AnimatePresence>
        </motion.div>
      </div>
    </OnClickOutside>
  )
}

interface OnClickOutsideProps { children: ReactNode; onClickOutside: () => void; classes?: string }
const OnClickOutside: FC<OnClickOutsideProps> = ({ children, onClickOutside, classes }) => {
  const wrapperRef = useRef<HTMLDivElement>(null)
  useClickOutside(wrapperRef, onClickOutside)
  return <div ref={wrapperRef} className={cn(classes)}>{children}</div>
}
