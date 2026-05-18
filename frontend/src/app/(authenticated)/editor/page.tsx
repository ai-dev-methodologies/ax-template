/**
 * L4 demo page — RichTextEditor RSC compatibility smoke test.
 *
 * Demonstrates the required Client Component wrapper pattern for TipTap
 * in App Router Server Components (Next.js 15+ pre-mortem #3 mitigation).
 *
 * In Next.js 15+, next/dynamic with ssr:false is only valid inside a Client
 * Component. The RichTextEditorClient wrapper isolates the dynamic import.
 *
 * This page MUST build with exit 0 (SP32 acceptance criteria).
 */
import { RichTextEditorClient } from '@/components/rich-text-editor-client'

// ── Page (Server Component) ───────────────────────────────────────────────────

export default function EditorPage() {
  return (
    <main className="mx-auto max-w-2xl px-4 py-8">
      <h1 className="mb-6 text-2xl font-bold">Rich Text Editor — L4 Demo</h1>
      <p className="mb-4 text-sm text-gray-600">
        This page validates that TipTap renders correctly inside an RSC App Router page
        using the required Client Component wrapper pattern (Next.js 15+:{' '}
        <code>next/dynamic &#123; ssr: false &#125;</code> must live in a Client Component).
      </p>
      {/* RichTextEditorClient wraps the dynamic import in a Client Component */}
      <RichTextEditorClient
        placeholder="여기에 내용을 입력하세요…"
        minHeight="12rem"
      />
    </main>
  )
}
