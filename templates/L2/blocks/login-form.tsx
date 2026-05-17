/*
---
template_id: L2/blocks/login-form
layer: L2
provenance_class: internal_design
evidence:
  - source_type: external
    citation: "OWASP Authentication Cheat Sheet — username/password auth"
    url: "https://cheatsheetseries.owasp.org/cheatsheets/Authentication_Cheat_Sheet.html"
  - source_type: internal
    rationale: "L2 auth block — collects credentials via props-only callback; no direct backend calls."
dependencies: [button, input, label, form]
imports_from: [L1]
imports_forbidden: [L4, app/, lib/auth/]
---
*/
import * as React from 'react'

export interface LoginFormValues {
  email: string
  password: string
}

export interface LoginFormProps {
  /** Called with validated credentials on submit */
  onSubmit: (values: LoginFormValues) => void
  /** Show loading state while auth request is in-flight */
  isLoading?: boolean
  /** Surface error message from auth failure */
  errorMessage?: string
  /** OAuth slot — rendered below the form (optional) */
  oauthSlot?: React.ReactNode
}

export default function LoginForm({
  onSubmit,
  isLoading = false,
  errorMessage,
  oauthSlot,
}: LoginFormProps) {
  const [email, setEmail] = React.useState('')
  const [password, setPassword] = React.useState('')

  function handleSubmit(e: React.FormEvent<HTMLFormElement>) {
    e.preventDefault()
    onSubmit({ email, password })
  }

  return (
    <form onSubmit={handleSubmit} noValidate className="space-y-4 w-full">
      <div className="space-y-2">
        <label htmlFor="lf-email" className="text-sm font-medium leading-none">
          Email
        </label>
        <input
          id="lf-email"
          type="email"
          autoComplete="email"
          required
          disabled={isLoading}
          value={email}
          onChange={e => setEmail(e.target.value)}
          className="flex h-9 w-full rounded-md border border-input bg-transparent px-3 py-1 text-sm shadow-sm transition-colors placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring disabled:cursor-not-allowed disabled:opacity-50"
          placeholder="you@example.com"
        />
      </div>

      <div className="space-y-2">
        <label htmlFor="lf-password" className="text-sm font-medium leading-none">
          Password
        </label>
        <input
          id="lf-password"
          type="password"
          autoComplete="current-password"
          required
          disabled={isLoading}
          value={password}
          onChange={e => setPassword(e.target.value)}
          className="flex h-9 w-full rounded-md border border-input bg-transparent px-3 py-1 text-sm shadow-sm transition-colors placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring disabled:cursor-not-allowed disabled:opacity-50"
        />
      </div>

      {errorMessage && (
        <p role="alert" className="text-sm text-destructive">
          {errorMessage}
        </p>
      )}

      <button
        type="submit"
        disabled={isLoading}
        className="inline-flex w-full items-center justify-center rounded-md bg-primary px-4 py-2 text-sm font-medium text-primary-foreground shadow hover:bg-primary/90 focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring disabled:pointer-events-none disabled:opacity-50"
      >
        {isLoading ? 'Signing in…' : 'Sign in'}
      </button>

      {oauthSlot && <div className="mt-2">{oauthSlot}</div>}
    </form>
  )
}
