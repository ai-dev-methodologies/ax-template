---
title: Return on first failure unless the API contract requires collecting all errors
impact: LOW-MEDIUM
impactDescription: "Skips unnecessary processing once the result is determined. Sibling of async-defer-await for sync control flow. Don't apply when the consumer needs the full set of errors (form validation showing all field issues at once)."
tags: [javascript, functions, optimization, early-return, control-flow]
applicable_to: [react, nextjs, vite]
spec_ref: "specs/react-practices-l0.yaml#REACT-PRACTICES-JS-009"
verification:
  type: review
  status: manual
  notes: "Reviewer checks that the loop/function returns as soon as the result is determined, and confirms the early exit does not silently drop cases the caller needs (e.g. form validation that must report every failing field, not just the first)."
provenance: { pilot: true, pipeline_version: "2026-05-16", pipeline_steps: [phaseA_multi_source, phaseB_audit_4check, phaseC_codex_consensus] }
audit:
  accuracy: { status: verified, last_verified: "2026-05-16" }
  freshness: { status: current, last_verified: "2026-05-16", next_review_by: "2026-08-14" }
  completeness:
    status: complete
    amendments:
      - "Exception: collect-all when UX/API contract needs full diagnostics"
      - "Cross-link to async-defer-await (async analog)"
  gap_check: { status: complete }
upstream:
  - id: vercel-react-best-practices
    title: "Vercel agent skill: js-early-exit"
    url: "https://github.com/vercel-labs/agent-skills/blob/main/skills/react-best-practices/rules/js-early-exit.md"
    role: seed
evidence:
  - upstream_id: vercel-react-best-practices
    section: "js-early-exit"
    quote: "Return early when result is determined to skip unnecessary processing."
codex_consensus: { reviewer: "codex-cli 0.130.0, model_reasoning_effort=medium", reviewed_at: "2026-05-16", verdict: SHIP_WITH_AMEND }
sibling_rules: [async-defer-await]
---

## Return on first failure unless the API contract requires collecting all errors

**Impact: LOW-MEDIUM — Sync analog of `async-defer-await`. Stop work once the answer is known.**

### Incorrect — keeps processing after the answer is known

```typescript
function validate(users: User[]) {
  let hasError = false
  let errorMessage = ''
  for (const u of users) {
    if (!u.email) { hasError = true; errorMessage = 'Email required' }
    if (!u.name)  { hasError = true; errorMessage = 'Name required' }
  }
  return hasError ? { valid: false, error: errorMessage } : { valid: true }
}
```

### Correct — return immediately

```typescript
function validate(users: User[]) {
  for (const u of users) {
    if (!u.email) return { valid: false, error: 'Email required' }
    if (!u.name) return { valid: false, error: 'Name required' }
  }
  return { valid: true }
}
```

### Exception — collect-all when the consumer needs it

Form validation that shows every field error at once should collect, not short-circuit:

```typescript
function collectFieldErrors(users: User[]): FieldError[] {
  const errors: FieldError[] = []
  for (let i = 0; i < users.length; i++) {
    if (!users[i].email) errors.push({ row: i, field: 'email', msg: 'Required' })
    if (!users[i].name) errors.push({ row: i, field: 'name', msg: 'Required' })
  }
  return errors
}
```

If the consumer just wants "valid yes/no" or "first error", short-circuit. If the consumer needs the full diagnostic set, collect.

### Cross-rule

`async-defer-await` is the async equivalent — don't await data that the early-return path discards.

Sources:
- [Vercel: js-early-exit](https://github.com/vercel-labs/agent-skills/blob/main/skills/react-best-practices/rules/js-early-exit.md)
