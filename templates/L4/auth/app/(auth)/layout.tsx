/*
---
template_id: L4/auth/app/(auth)/layout
layer: L4
domain: auth
domain_mode: full_trio
backend_operation_id: null
evidence:
  - source_type: internal
    rationale: "L4 auth vertical — unauthenticated route group layout; no chrome (no sidebar/nav)."
provenance_class: internal_design
imports_from: [L1, L2, L3]
imports_forbidden: [other L4 domains]
---
*/
import React from 'react'

interface AuthLayoutProps {
  children: React.ReactNode
}

/**
 * Auth route group layout — no chrome.
 *
 * All unauthenticated routes (login, signup, verify, oauth/callback) share
 * this layout. No sidebar, no nav header — just the page content centered.
 *
 * L4 fork usage:
 *   Place this file at app/(auth)/layout.tsx in your Next.js project.
 *   Wrap with your theme / font provider if needed.
 */
export default function AuthLayout({ children }: AuthLayoutProps) {
  return (
    <div className="flex min-h-svh items-center justify-center bg-background px-4">
      {children}
    </div>
  )
}
