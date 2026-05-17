/*
---
template_id: L4/crud/next.config
layer: L4
domain: crud
domain_mode: full_trio
backend_operation_id: null
evidence:
  - source_type: internal
    rationale: "L4 crud vertical — minimal Next.js config for fork-receiver build test."
  - source_type: external
    citation: "Next.js 15 — next.config.ts reference"
    url: "https://nextjs.org/docs/app/api-reference/config/next-config-js"
provenance_class: internal_design
imports_from: [L1, L2, L3]
imports_forbidden: [other L4 domains]
---
*/
import type { NextConfig } from 'next'

/**
 * next.config.ts — minimal config for L4/crud fork-receiver build test.
 *
 * Fork instructions:
 *   1. Add `rewrites()` to proxy /api/* to your Spring Boot backend.
 *   2. Enable React strict mode (strictMode: true) for production forks.
 *   3. Add image `domains` / `remotePatterns` if rendering remote images.
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
}

export default nextConfig
