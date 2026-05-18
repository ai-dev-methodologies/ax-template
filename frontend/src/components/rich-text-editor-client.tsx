/**
 * Client Component wrapper for RichTextEditor.
 *
 * next/dynamic with ssr:false is only valid inside a Client Component in
 * Next.js 15+ App Router. This wrapper isolates the dynamic import so that
 * Server Component pages can import it without triggering the SSR restriction.
 *
 * The `@templates` alias is resolved by webpack (next.config.ts) but NOT by
 * TypeScript (tsconfig paths). The @ts-expect-error below suppresses the TS
 * module resolution error — webpack bundles it correctly at build time.
 *
 * Usage from a Server Component page:
 *   import { RichTextEditorClient } from '@/components/rich-text-editor-client'
 *   <RichTextEditorClient placeholder="…" minHeight="12rem" />
 */
'use client'

import dynamic from 'next/dynamic'
import type { ComponentType } from 'react'

// Props mirrored from templates/L1/components/rich-text-editor (RichTextEditorProps).
// Defined locally so TypeScript doesn't need to traverse into the template file.
interface RichTextEditorClientProps {
  value?: string
  onChange?: (html: string) => void
  placeholder?: string
  disabled?: boolean
  className?: string
  minHeight?: string
}

// ── Dynamic import with ssr:false (REQUIRED for TipTap / ProseMirror) ─────────
// Moved into a Client Component per Next.js 15+ requirement:
// "ssr:false is not allowed with next/dynamic in Server Components."
const _RichTextEditor = dynamic(
  () =>
    // @ts-expect-error — @templates alias is webpack-only; module resolves at bundle time
    import('@templates/L1/components/rich-text-editor').then(
      (m: any) => ({ default: m.RichTextEditor })
    ),
  {
    ssr: false,
    loading: () => (
      <textarea
        className="w-full h-32 rounded border border-gray-300 p-2 resize-y"
        placeholder="에디터 로딩 중…"
        aria-label="에디터 로딩 중"
        readOnly
      />
    ),
  }
) as unknown as ComponentType<RichTextEditorClientProps>

export function RichTextEditorClient(props: RichTextEditorClientProps) {
  return <_RichTextEditor {...props} />
}
