// TDD anchor — SP32 fixture: FAIL case for rich-content-must-use-dynamic-import
// This file is INTENTIONALLY WRONG — it statically imports RichTextEditor in a
// Server Component (no 'use client' directive). SSR execution will throw:
// "window is not defined" / ProseMirror browser-only API errors.
// The rule scanner must detect this and return a non-zero exit code.
// Created: 2026-05-18 (within applies_to scope)

// ❌ WRONG — static import of browser-only component in Server Component
import { RichTextEditor } from '@/templates/L1/components/rich-text-editor'

export default function EditorPage() {
  // This crashes on SSR: TipTap's ProseMirror uses window, document.createElement,
  // MutationObserver — none of which exist in the Node.js SSR environment.
  return (
    <main>
      <RichTextEditor placeholder="입력하세요…" />
    </main>
  )
}
