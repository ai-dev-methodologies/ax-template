/*
---
template_id: L4/audit-log/next.config
layer: L4
domain: audit-log
domain_mode: full_trio
backend_operation_id: null
evidence:
  - source_type: internal
    rationale: "L4 audit-log vertical — minimal Next.js config for fork-receiver build test."
  - source_type: external
    citation: "Next.js 15 — next.config.ts reference"
    url: "https://nextjs.org/docs/app/api-reference/config/next-config-js"
provenance_class: internal_design
imports_from: [L1, L2, L3]
imports_forbidden: [L4/auth, L4/crud, L4/practices, L4/payment]
---
*/
import type { NextConfig } from 'next'

/**
 * next.config.ts — minimal config for L4/audit-log fork-receiver build test.
 *
 * Fork instructions:
 *   1. Update rewrites() host/port to match your Spring Boot instance.
 *   2. Enable React strict mode (strictMode: true) for production forks.
 *   3. Configure Content-Security-Policy headers for your deployment.
 *   4. Add image domains / remotePatterns if rendering user avatars.
 */
const nextConfig: NextConfig = {
  // Proxy API calls to Spring Boot backend (update host/port for your env)
  async rewrites() {
    return [
      {
        source: '/api/:path*',
        destination: 'http://localhost:8080/api/:path*',
      },
    ]
  },

  // Security headers
  async headers() {
    return [
      {
        source: '/:path*',
        headers: [
          { key: 'X-Content-Type-Options', value: 'nosniff' },
          { key: 'X-Frame-Options', value: 'DENY' },
          { key: 'Referrer-Policy', value: 'strict-origin-when-cross-origin' },
          {
            key: 'Permissions-Policy',
            value: 'camera=(), microphone=(), geolocation=()',
          },
        ],
      },
    ]
  },
}

export default nextConfig
