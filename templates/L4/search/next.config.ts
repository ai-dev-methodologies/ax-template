/*
---
template_id: L4/search/next.config.ts
layer: L4
provenance_class: internal_design
evidence:
  - source_type: external
    citation: "Next.js Configuration — next.config.ts: TypeScript-based configuration file for Next.js 15+. Merge search-domain rewrites into your root next.config.ts."
    url: "https://nextjs.org/docs/app/api-reference/config/next-config-js"
imports_from: []
imports_forbidden: []
---
*/
import type { NextConfig } from 'next'

/**
 * Search domain Next.js config fragment.
 *
 * Fork instructions:
 *   Merge the `rewrites` and `env` blocks into your root next.config.ts.
 *   Do NOT copy this file as-is — it will conflict with your existing config.
 */
const searchDomainConfig: Partial<NextConfig> = {
  // Proxy /api/v1/search to the Spring Boot backend during development.
  // In production, configure your reverse proxy (nginx, Vercel rewrites, etc.).
  async rewrites() {
    return [
      {
        source: '/api/v1/search/:path*',
        destination: `${process.env.BACKEND_URL ?? 'http://localhost:8080'}/api/v1/search/:path*`,
      },
    ]
  },
}

export default searchDomainConfig
