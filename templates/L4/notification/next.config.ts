/*
---
template_id: L4/notification/next.config
layer: L4
domain: notification
domain_mode: full_trio
backend_operation_id: null
evidence:
  - source_type: internal
    rationale: "L4 notification vertical — minimal Next.js config for the notification domain: API proxy rewrite to Spring Boot backend."
  - source_type: external
    citation: "Next.js 15 Configuration Reference — next.config.ts"
    url: "https://nextjs.org/docs/app/api-reference/config/next-config-js"
provenance_class: internal_design
imports_from: []
imports_forbidden: [L4/auth, L4/crud, L4/payment, L4/practices]
---
*/
import type { NextConfig } from 'next'

/**
 * Next.js configuration for the L4 notification domain vertical.
 *
 * API rewrite:
 *   /api/* → ${API_BASE_URL}/api/* (proxies to Spring Boot backend)
 *
 * Fork instructions:
 *   1. Set NEXT_PUBLIC_API_BASE in your .env.local for the client-side base URL.
 *   2. Set API_BASE_URL in your .env.local for the server-side proxy target.
 *   3. Add image domains if notification thumbnails/avatars use external CDN.
 */
const nextConfig: NextConfig = {
  rewrites: async () => [
    {
      source: '/api/:path*',
      destination: `${process.env.API_BASE_URL ?? 'http://localhost:8080'}/api/:path*`,
    },
  ],

  // Compiler options
  typescript: {
    // Templates ship with known peer-dependency type gaps (next, react not installed here).
    // Remove this when you have the full package.json set up.
    ignoreBuildErrors: false,
  },
}

export default nextConfig
