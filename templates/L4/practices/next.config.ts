/*
---
template_id: L4/practices/next.config
layer: L4
domain: practices
domain_mode: frontend_only
backend_operation_id: null
evidence:
  - source_type: internal
    rationale: "L4 practices vertical — minimal Next.js config for frontend_only domain. No API proxy needed (static file reads via RSC)."
  - source_type: external
    citation: "Next.js 15 — next.config.ts reference"
    url: "https://nextjs.org/docs/app/api-reference/config/next-config-js"
provenance_class: internal_design
imports_from: [L1, L2, L3]
imports_forbidden: [L4/auth, L4/crud, L4/payment]
---
*/
import type { NextConfig } from 'next'

/**
 * next.config.ts — minimal config for L4/practices fork-receiver build test.
 *
 * No API proxy needed: the practices domain is frontend_only.
 * All rule data is read directly from the filesystem via Server Components.
 *
 * Fork instructions:
 *   1. Add `serverExternalPackages` if you add a markdown parser package.
 *   2. Enable React strict mode (strictMode: true) for production forks.
 *   3. Add `output: 'standalone'` for containerized deployments.
 */
const nextConfig: NextConfig = {
  // No rewrites needed — frontend_only domain reads local markdown files
}

export default nextConfig
