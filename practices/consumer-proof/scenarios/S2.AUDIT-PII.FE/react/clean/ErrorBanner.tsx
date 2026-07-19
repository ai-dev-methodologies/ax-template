'use client'
// CLEAN variant — routes the failure response through the catalog's
// parseError() helper (templates/L0/fork-receiver-kit/parse-error.ts) instead
// of hand-reading the ProblemDetail body. As of the S2.AUDIT-PII.FE dogfood
// closure, parseError() now sanitizes body.detail/body.message through the
// same deny-list as its text/html fallback branch, so this is the correct,
// safe way to surface a failed request's message.
import { useState } from 'react'
import { parseError } from 'templates/L0/fork-receiver-kit/parse-error'

export function DocumentUploadForm() {
  const [errorMsg, setErrorMsg] = useState<string | null>(null)

  async function onSubmit(formData: FormData) {
    const res = await fetch('/api/documents', { method: 'POST', body: formData })
    if (!res.ok) {
      const err = await parseError(res, 'Upload failed')
      setErrorMsg(err.message)
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
