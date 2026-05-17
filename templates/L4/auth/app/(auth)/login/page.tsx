/*
---
template_id: L4/auth/app/(auth)/login/page
layer: L4
domain: auth
domain_mode: full_trio
backend_operation_id: emailLogin
evidence:
  - source_type: internal
    rationale: "L4 auth vertical — login page composing L2/blocks/login-form with auth store binding."
  - source_type: external
    citation: "Next.js 15 App Router — client component pattern for form pages"
    url: "https://nextjs.org/docs/app/building-your-application/routing/pages"
provenance_class: internal_design
imports_from: [L1, L2, L3]
imports_forbidden: [other L4 domains]
---
*/
'use client'

import React, { useState } from 'react'
import Link from 'next/link'
import { useRouter } from 'next/navigation'
import LoginForm from '../../../../../L2/blocks/login-form'

const API_BASE = process.env.NEXT_PUBLIC_API_BASE ?? '/api'
const OAUTH_PROVIDERS = [
  { id: 'google', label: 'Continue with Google' },
  { id: 'naver', label: 'Continue with Naver' },
  { id: 'kakao', label: 'Continue with Kakao' },
] as const

/**
 * LoginPage — L4 auth vertical login route.
 *
 * Composes L2/blocks/login-form with:
 *   - auth store binding (replace useAuthStore with your state manager)
 *   - OAuth redirect buttons in oauthSlot
 *   - post-login navigation to /dashboard
 *
 * Backend operation: POST /auth/email/login (operationId: emailLogin)
 *
 * Fork instructions:
 *   1. Replace `useAuthStore` import with your auth hook
 *   2. Set NEXT_PUBLIC_API_BASE in your environment
 *   3. Adjust OAuth provider list as needed
 */
export default function LoginPage() {
  const router = useRouter()
  const [isLoading, setIsLoading] = useState(false)
  const [errorMessage, setErrorMessage] = useState<string | undefined>()

  async function handleSubmit({ email, password }: { email: string; password: string }) {
    setIsLoading(true)
    setErrorMessage(undefined)
    try {
      const res = await fetch(`${API_BASE}/auth/email/login`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        credentials: 'include',
        body: JSON.stringify({ email, password }),
      })
      if (!res.ok) {
        const body = await res.json().catch(() => ({}))
        setErrorMessage(body?.message ?? 'Login failed. Please try again.')
        return
      }
      router.push('/dashboard')
    } catch {
      setErrorMessage('Network error. Please try again.')
    } finally {
      setIsLoading(false)
    }
  }

  function handleOAuth(provider: string) {
    window.location.href = `${API_BASE}/auth/oauth/${provider}/authorize`
  }

  const oauthSlot = (
    <div className="flex flex-col gap-2 mt-2">
      <div className="relative">
        <div className="absolute inset-0 flex items-center">
          <span className="w-full border-t" />
        </div>
        <div className="relative flex justify-center text-xs uppercase">
          <span className="bg-background px-2 text-muted-foreground">or</span>
        </div>
      </div>
      {OAUTH_PROVIDERS.map(p => (
        <button
          key={p.id}
          type="button"
          onClick={() => handleOAuth(p.id)}
          className="inline-flex w-full items-center justify-center rounded-md border border-input bg-background px-4 py-2 text-sm font-medium shadow-sm hover:bg-accent hover:text-accent-foreground focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring"
        >
          {p.label}
        </button>
      ))}
    </div>
  )

  return (
    <div className="w-full max-w-sm space-y-6">
      <div className="space-y-1 text-center">
        <h1 className="text-2xl font-semibold tracking-tight">Sign in</h1>
        <p className="text-sm text-muted-foreground">
          Enter your email and password
        </p>
      </div>

      <LoginForm
        onSubmit={handleSubmit}
        isLoading={isLoading}
        errorMessage={errorMessage}
        oauthSlot={oauthSlot}
      />

      <p className="text-center text-sm text-muted-foreground">
        Don&apos;t have an account?{' '}
        <Link href="/signup" className="underline underline-offset-4 hover:text-primary">
          Sign up
        </Link>
      </p>
    </div>
  )
}
