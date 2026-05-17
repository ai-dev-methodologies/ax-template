/*
---
template_id: L2/blocks/signup-form
layer: L2
provenance_class: internal_design
evidence:
  - source_type: external
    citation: "OWASP Authentication Cheat Sheet — registration"
    url: "https://cheatsheetseries.owasp.org/cheatsheets/Authentication_Cheat_Sheet.html"
  - source_type: internal
    rationale: "L2 auth block — collects signup fields via props callback; no direct backend calls."
dependencies: [button, input, label]
imports_from: [L1]
imports_forbidden: [L4, app/, lib/auth/]
---
*/
import * as React from 'react'

export interface SignupFormValues {
  name: string
  email: string
  password: string
}

export interface SignupFormProps {
  onSubmit: (values: SignupFormValues) => void
  isLoading?: boolean
  errorMessage?: string
  /** Slot for ToS / privacy copy (optional) */
  tosSlot?: React.ReactNode
}

export default function SignupForm({
  onSubmit,
  isLoading = false,
  errorMessage,
  tosSlot,
}: SignupFormProps) {
  const [name, setName] = React.useState('')
  const [email, setEmail] = React.useState('')
  const [password, setPassword] = React.useState('')

  function handleSubmit(e: React.FormEvent<HTMLFormElement>) {
    e.preventDefault()
    onSubmit({ name, email, password })
  }

  return (
    <form onSubmit={handleSubmit} noValidate className="space-y-4 w-full">
      <div className="space-y-2">
        <label htmlFor="sf-name" className="text-sm font-medium leading-none">
          Name
        </label>
        <input
          id="sf-name"
          type="text"
          autoComplete="name"
          required
          disabled={isLoading}
          value={name}
          onChange={e => setName(e.target.value)}
          className="flex h-9 w-full rounded-md border border-input bg-transparent px-3 py-1 text-sm shadow-sm transition-colors focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring disabled:cursor-not-allowed disabled:opacity-50"
          placeholder="Your name"
        />
      </div>

      <div className="space-y-2">
        <label htmlFor="sf-email" className="text-sm font-medium leading-none">
          Email
        </label>
        <input
          id="sf-email"
          type="email"
          autoComplete="email"
          required
          disabled={isLoading}
          value={email}
          onChange={e => setEmail(e.target.value)}
          className="flex h-9 w-full rounded-md border border-input bg-transparent px-3 py-1 text-sm shadow-sm transition-colors focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring disabled:cursor-not-allowed disabled:opacity-50"
          placeholder="you@example.com"
        />
      </div>

      <div className="space-y-2">
        <label htmlFor="sf-password" className="text-sm font-medium leading-none">
          Password
        </label>
        <input
          id="sf-password"
          type="password"
          autoComplete="new-password"
          required
          minLength={8}
          disabled={isLoading}
          value={password}
          onChange={e => setPassword(e.target.value)}
          className="flex h-9 w-full rounded-md border border-input bg-transparent px-3 py-1 text-sm shadow-sm transition-colors focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring disabled:cursor-not-allowed disabled:opacity-50"
        />
      </div>

      {errorMessage && (
        <p role="alert" className="text-sm text-destructive">
          {errorMessage}
        </p>
      )}

      {tosSlot && <div>{tosSlot}</div>}

      <button
        type="submit"
        disabled={isLoading}
        className="inline-flex w-full items-center justify-center rounded-md bg-primary px-4 py-2 text-sm font-medium text-primary-foreground shadow hover:bg-primary/90 focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring disabled:pointer-events-none disabled:opacity-50"
      >
        {isLoading ? 'Creating account…' : 'Create account'}
      </button>
    </form>
  )
}
