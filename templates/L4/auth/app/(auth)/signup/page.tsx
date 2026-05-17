/*
---
template_id: L4/auth/app/(auth)/signup/page
layer: L4
domain: auth
domain_mode: full_trio
backend_operation_id: emailSignup
evidence:
  - source_type: internal
    rationale: "L4 auth vertical — signup page composing L2/blocks/signup-form with email-registration flow."
  - source_type: external
    citation: "OWASP Authentication Cheat Sheet — registration and email verification flow"
    url: "https://cheatsheetseries.owasp.org/cheatsheets/Authentication_Cheat_Sheet.html"
provenance_class: internal_design
imports_from: [L1, L2, L3]
imports_forbidden: [other L4 domains]
---
*/
'use client'

import React, { useState } from 'react'
import Link from 'next/link'
import SignupForm from '../../../../../L2/blocks/signup-form'

const API_BASE = process.env.NEXT_PUBLIC_API_BASE ?? '/api'

/**
 * SignupPage — L4 auth vertical signup route.
 *
 * Composes L2/blocks/signup-form with:
 *   - fetch-based signup call to POST /auth/email/signup
 *   - success state (shows "check your email" panel)
 *   - validation error surfacing
 *
 * Backend operation: POST /auth/email/signup (operationId: emailSignup)
 *
 * Fork instructions:
 *   1. Replace fetch logic with your auth hook / tRPC / React Query mutation
 *   2. Adjust password requirements to match your backend config
 *   3. Update successHref for your post-signup flow
 */
export default function SignupPage() {
  const [done, setDone] = useState(false)
  const [isLoading, setIsLoading] = useState(false)
  const [errorMessage, setErrorMessage] = useState<string | undefined>()

  async function handleSubmit({
    name,
    email,
    password,
  }: {
    name: string
    email: string
    password: string
  }) {
    setIsLoading(true)
    setErrorMessage(undefined)
    try {
      const res = await fetch(`${API_BASE}/auth/email/signup`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ name, email, password }),
      })
      if (!res.ok) {
        const body = await res.json().catch(() => ({}))
        setErrorMessage(body?.message ?? 'Signup failed. Please try again.')
        return
      }
      setDone(true)
    } catch {
      setErrorMessage('Network error. Please try again.')
    } finally {
      setIsLoading(false)
    }
  }

  if (done) {
    return (
      <div className="w-full max-w-sm space-y-4 text-center">
        <div className="mx-auto flex h-12 w-12 items-center justify-center rounded-full bg-green-100 text-green-700 text-xl">
          ✓
        </div>
        <h2 className="text-xl font-semibold">Check your email</h2>
        <p className="text-sm text-muted-foreground">
          We sent a verification link. Click it to activate your account.
        </p>
        <Link
          href="/login"
          className="inline-flex items-center text-sm underline underline-offset-4 hover:text-primary"
        >
          Back to sign in
        </Link>
      </div>
    )
  }

  const tosSlot = (
    <p className="text-xs text-muted-foreground text-center">
      By signing up you agree to our{' '}
      <a href="/terms" className="underline underline-offset-4 hover:text-primary">
        Terms
      </a>{' '}
      and{' '}
      <a href="/privacy" className="underline underline-offset-4 hover:text-primary">
        Privacy Policy
      </a>
      .
    </p>
  )

  return (
    <div className="w-full max-w-sm space-y-6">
      <div className="space-y-1 text-center">
        <h1 className="text-2xl font-semibold tracking-tight">Create an account</h1>
        <p className="text-sm text-muted-foreground">
          Enter your details to get started
        </p>
      </div>

      <SignupForm
        onSubmit={handleSubmit}
        isLoading={isLoading}
        errorMessage={errorMessage}
        tosSlot={tosSlot}
      />

      <p className="text-center text-sm text-muted-foreground">
        Already have an account?{' '}
        <Link href="/login" className="underline underline-offset-4 hover:text-primary">
          Sign in
        </Link>
      </p>
    </div>
  )
}
