/*
---
template_id: L4/auth/next.config
layer: L4
domain: auth
domain_mode: full_trio
backend_operation_id: null
evidence:
  - source_type: internal
    rationale: "L4 auth vertical — minimal Next.js 15 config for fork-receiver build test; no opinionated defaults."
  - source_type: external
    citation: "Next.js 15 — next.config.ts configuration reference"
    url: "https://nextjs.org/docs/app/api-reference/config/next-config-js"
provenance_class: internal_design
imports_from: [L1, L2, L3]
imports_forbidden: [other L4 domains]
---
*/
import type { NextConfig } from 'next'

/**
 * next.config.ts — minimal Next.js config for L4/auth fork receiver.
 *
 * Fork instructions:
 *   1. Add `env` entries for required public env vars (NEXT_PUBLIC_API_BASE)
 *   2. Add `rewrites` / `redirects` if your API proxy config differs
 *   3. Enable `output: 'standalone'` for Docker deployments
 *   4. Add image `remotePatterns` for external image hosts
 */
const nextConfig: NextConfig = {
  // Allow build to proceed even if some pages have TypeScript errors
  // Remove this in production — fix all type errors first
  typescript: {
    ignoreBuildErrors: false,
  },
  // API proxy: forward /api/* to your Spring Boot backend
  async rewrites() {
    const apiBase = process.env.API_BASE_URL ?? 'http://localhost:8080'
    return [
      {
        source: '/api/:path*',
        destination: `${apiBase}/api/:path*`,
      },
    ]
  },
}

export default nextConfig
