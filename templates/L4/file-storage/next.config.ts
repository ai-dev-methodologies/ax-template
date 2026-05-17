/*
---
template_id: L4/file-storage/next.config
layer: L4
domain: file-storage
domain_mode: full_trio
backend_operation_id: null
evidence:
  - source_type: external
    citation: "Next.js 15 — next.config.ts configuration reference"
    url: "https://nextjs.org/docs/app/api-reference/config/next-config-js"
provenance_class: internal_design
imports_from: []
imports_forbidden: []
---
*/
import type { NextConfig } from 'next'

/**
 * Next.js config for the L4 file-storage vertical.
 *
 * Fork instructions:
 *   1. Add your backend API URL to rewrites() for local dev proxying.
 *   2. Configure allowedDevOrigins for CORS in development.
 *   3. Add image.domains if previewing images via presigned URLs from S3.
 */
const nextConfig: NextConfig = {
  // Proxy /api/** to backend during local development
  async rewrites() {
    return [
      {
        source: '/api/:path*',
        destination: 'http://localhost:8080/api/:path*',
      },
    ]
  },
}

export default nextConfig
