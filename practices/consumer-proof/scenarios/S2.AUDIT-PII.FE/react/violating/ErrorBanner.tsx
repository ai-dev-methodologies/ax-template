'use client'
// VIOLATING variant.
// A very natural AI-generated shortcut: fetch, check res.ok, and on failure
// read the ProblemDetail body directly instead of routing it through the
// catalog's parseError() helper (templates/L0/fork-receiver-kit/parse-error.ts).
// This bypasses the deny-list entirely (sanitizeStoredError never runs) — a
// backend that echoes a submitted RRN/email/phone into `detail` (a very
// common validation-error shape) reaches the DOM verbatim.
import { useState } from 'react'

export function DocumentUploadForm() {
  const [errorMsg, setErrorMsg] = useState<string | null>(null)

  async function onSubmit(formData: FormData) {
    const res = await fetch('/api/documents', { method: 'POST', body: formData })
    if (!res.ok) {
      const body = await res.json()
      setErrorMsg(body.detail || body.message || 'Upload failed')
      return
    }
    setErrorMsg(null)
  }

  return (
    <div>
      {errorMsg && <p role="alert">{errorMsg}</p>}
    </div>
  )
}
