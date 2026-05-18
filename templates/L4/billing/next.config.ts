/*
---
template_id: L4/billing/next.config
layer: L4
domain: billing
domain_mode: full_trio
evidence:
  - source_type: internal
    rationale: "L4 billing — Next.js App Router config for billing domain. Fork to wire real API base URL."
provenance_class: internal_design
---
*/

import type { NextConfig } from 'next'

/**
 * Next.js configuration for the billing domain.
 *
 * Fork instructions:
 * 1. Replace NEXT_PUBLIC_API_BASE_URL with your real API base (e.g., https://api.example.com)
 * 2. Configure rewrites if billing API is under a different path
 * 3. Add CSP headers for billing domain assets
 */
const nextConfig: NextConfig = {
  // Billing API proxy — rewrites /api/billing/** to backend
  // Fork: uncomment and configure for your deployment
  // async rewrites() {
  //   return [
  //     {
  //       source: '/api/subscriptions/:path*',
  //       destination: `${process.env.BILLING_API_URL}/api/subscriptions/:path*`,
  //     },
  //     {
  //       source: '/api/billing/:path*',
  //       destination: `${process.env.BILLING_API_URL}/api/billing/:path*`,
  //     },
  //     {
  //       source: '/api/admin/billing/:path*',
  //       destination: `${process.env.BILLING_API_URL}/api/admin/billing/:path*`,
  //     },
  //   ]
  // },

  env: {
    // Fork: set these in .env.local
    NEXT_PUBLIC_BILLING_API_URL: process.env.NEXT_PUBLIC_BILLING_API_URL ?? '',
  },
}

export default nextConfig
