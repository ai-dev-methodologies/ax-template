---
title: "L2 form blocks — accept onSubmit prop; never import server actions directly"
impact: HIGH
impactDescription: "Importing a server action or fetch inside an L2 block couples the block to a domain, breaking the layer contract and preventing reuse across domains."
tags:
  - l2-layer
  - server-actions
  - decoupling
  - form-blocks
  - nextjs
applicable_to:
  - nextjs
spec_ref: "specs/react-practices-l0.yaml#REACT-PRACTICES-L2-001"
verification:
  type: review
  status: manual
  notes: "For each L2 form block, verify: (a) no `import ... from 'app/actions/...'` or `import ... from 'lib/...'` in the block file, (b) the form accepts an `onSubmit` callback prop, (c) the callback prop is typed in the exported interface. check-imports.sh statically enforces the L3/L4 import boundary; the app/actions and lib ban in (a) is review-tier (the script does not inspect those paths)."
provenance:
  pilot: true
  pipeline_version: "2026-05-18"
  pipeline_steps: [implementation_observed, phaseB_audit_4check, phaseC_codex_consensus]
audit:
  accuracy:
    status: verified
    last_verified: "2026-05-18"
  freshness:
    status: current
    last_verified: "2026-05-18"
    next_review_by: "2026-08-16"
  completeness:
    status: complete
    amendments:
      - "Observed during SP7 implementation: LoginForm, SignupForm, CrudCreateForm, CrudEditForm, PaymentCheckoutForm all required this discipline"
  gap_check:
    status: complete
evidence:
  - source_type: external
    citation: "React documentation — Responding to Events (Passing event handlers as props): a child component receives event handlers such as onSubmit from its parent as props, keeping the child agnostic to what the handler does. (The server-action/lib import ban itself is an ax-template layer decision — this citation anchors only the generic callback-prop principle it rests on.)"
    url: "https://react.dev/learn/responding-to-events"
    quoted_at: "2026-07-14"
sibling_rules:
  - l2-prefer-data-prop-over-direct-fetch
  - async-api-routes
provenance_class: internal_design
protects_template_id: templates/L2/blocks/
failing_fixture_path: practices/evals/fixtures/l2-prefer-onsubmit-prop/fail_server_action_import/
decided_at: "2026-05-18"
---

## L2 form blocks — accept `onSubmit` prop; never import server actions directly

**Impact: HIGH — Importing a server action inside an L2 block couples the block to a specific domain and breaks the layer contract.**

### The violation (do NOT do this in L2)

```typescript
// ❌ WRONG — L2 block importing a server action directly
import { loginAction } from 'app/actions/auth'
import { createProductAction } from 'app/actions/products'

export default function LoginForm() {
  async function handleSubmit(formData: FormData) {
    await loginAction(formData)  // domain import — block is no longer reusable
  }
  return <form action={handleSubmit}>...</form>
}
```

### Correct — props-only callback

```typescript
// ✅ CORRECT — L2 block accepts onSubmit from caller (L4 injects the action)
export interface LoginFormProps {
  onSubmit: (values: { email: string; password: string }) => void
  isLoading?: boolean
  errorMessage?: string
}

export default function LoginForm({ onSubmit, isLoading, errorMessage }: LoginFormProps) {
  function handleSubmit(e: React.FormEvent<HTMLFormElement>) {
    e.preventDefault()
    onSubmit({ email, password })
  }
  return <form onSubmit={handleSubmit}>...</form>
}
```

### L4 wires the action to the prop

```typescript
// app/(auth)/login/page.tsx — L4 provides the domain glue
import LoginForm from 'templates/L2/blocks/login-form'
import { loginAction } from './actions'

export default function LoginPage() {
  return (
    <LoginForm
      onSubmit={async (values) => {
        await loginAction(values)
      }}
    />
  )
}
```

### Why this rule exists

During SP7 block implementation, every auth, CRUD, and payment form block was a candidate for inlining a server action call. Keeping `onSubmit` as a prop kept each block:

1. **Domain-agnostic** — LoginForm works for any auth domain without modification.
2. **Testable** — tests pass a spy function; no server action mocking needed.
3. **Layer-clean** — ax-verify-L2 `check-imports.sh` enforces the import boundary statically.

### Layer enforcement

`bash skills/ax-verify-L2/scripts/check-imports.sh` fails with `ILLEGAL_IMPORT` if any L2 file contains an import referencing `templates/L3/` or `templates/L4/`. (The `app/actions/…` and `lib/…` import ban this rule headlines is review-tier — check-imports.sh does not yet inspect those paths.)
