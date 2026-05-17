/*
---
template_id: L3/pages/error-page/not-found
layer: L3
provenance_class: internal_design
evidence:
  - source_type: external
    citation: "Next.js 15 App Router not-found.tsx — notFound() and 404 UI"
    url: "https://nextjs.org/docs/app/api-reference/file-conventions/not-found"
  - source_type: internal
    rationale: "Generic 404 skeleton for ax-template L4. Rendered when notFound() is called in a route segment. No domain logic."
dependencies: []
---
*/

/**
 * NotFound — Next.js App Router not-found.tsx convention.
 *
 * This file is rendered when `notFound()` is called in a route segment or
 * when no matching route exists. No props accepted per Next.js convention.
 *
 * L4 usage: Copy this file to your L4 route segment directory and adjust
 * the copy and links to match your domain context.
 */
export default function NotFound() {
  return (
    <main className="flex min-h-svh flex-col items-center justify-center px-4 text-center space-y-6">
      {/* Status code */}
      <p className="text-8xl font-bold text-muted-foreground/30 select-none" aria-hidden="true">
        404
      </p>

      <div className="space-y-2">
        <h1 className="text-2xl font-semibold tracking-tight">Page not found</h1>
        <p className="text-sm text-muted-foreground max-w-xs">
          The page you&apos;re looking for doesn&apos;t exist or has been moved.
        </p>
      </div>

      <a
        href="/"
        className="inline-flex items-center justify-center rounded-md bg-primary px-6 py-2 text-sm font-medium text-primary-foreground hover:opacity-90 transition-opacity"
      >
        Go home
      </a>
    </main>
  )
}
