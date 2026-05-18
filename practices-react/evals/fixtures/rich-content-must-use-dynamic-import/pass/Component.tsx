// TDD anchor — SP32 fixture: PASS case for rich-content-must-use-dynamic-import
// This file uses next/dynamic inside a Client Component wrapper — correct pattern
// for Next.js 15+ App Router. The rule scanner must detect NO violations.
// Created: 2026-05-18 (within applies_to scope)

// ✅ CORRECT — Client Component wrapper with next/dynamic + ssr:false
'use client'

import dynamic from 'next/dynamic'
import type { ComponentType } from 'react'

interface EditorProps {
  value?: string
  onChange?: (html: string) => void
  placeholder?: string
  disabled?: boolean
  className?: string
  minHeight?: string
}

// next/dynamic with ssr:false inside Client Component — valid in Next.js 15+
const _RichTextEditor = dynamic(
  () =>
    import('@/templates/L1/components/rich-text-editor').then(
      (m) => ({ default: m.RichTextEditor })
    ),
  {
    ssr: false,
    loading: () => (
      <textarea
        className="w-full h-32 rounded border p-2 resize-y"
        placeholder="에디터 로딩 중…"
        readOnly
      />
    ),
  }
) as unknown as ComponentType<EditorProps>

export function RichTextEditorClient(props: EditorProps) {
  return <_RichTextEditor {...props} />
}
