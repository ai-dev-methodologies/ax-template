/*
---
template_id: L4/feature-flags/next.config
layer: L4
domain: feature-flags
provenance_class: internal_design
evidence:
  - source_type: external
    citation: "Next.js Docs — next.config.ts rewrites for proxying API requests"
    url: "https://nextjs.org/docs/app/api-reference/config/next-config-js/rewrites"
usage: |
  Rewrites /api/v1/** to the backend URL so feature-flag API calls work
  from the browser (same-origin, no CORS needed).
  Set BACKEND_API_BASE env var to your Spring Boot server URL.
---
*/
import type { NextConfig } from 'next'

const BACKEND_API_BASE =
  process.env.BACKEND_API_BASE ?? 'http://localhost:8080'

const nextConfig: NextConfig = {
  async rewrites() {
    return [
      {
        source: '/api/v1/:path*',
        destination: `${BACKEND_API_BASE}/api/v1/:path*`,
      },
    ]
  },
}

export default nextConfig
