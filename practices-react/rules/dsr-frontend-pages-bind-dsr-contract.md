---
title: "DSR (data-subject-rights) UI must realize the GDPR rights contract — dashboard with SLA, access/rectify/portability flows, and destructive erasure/restrict behind a confirm dialog"
rule_id: dsr-frontend-pages-bind-dsr-contract
impact: HIGH
impactDescription: "A data-subject-rights UI that fires erasure or processing-restriction without a confirm dialog destroys or freezes data on a misclick; one with no SLA due date on the dashboard hides that a statutory deadline is approaching; a lift-restriction or extend-window with no required justification leaves a privileged change unaccountable. The DSR surface implements legal rights (GDPR Art 15-20) — its destructive actions and deadlines must be handled deliberately."
tags:
  - dsr
  - gdpr
  - frontend
  - privacy
  - confirm-dialog
  - contract-first
applicable_to:
  - react
  - nextjs
spec_ref: "specs/data-subject-rights-frontend-l0.yaml#DSR-FE-002"
verification:
  type: review
  notes: |
    Reviewer confirms the DSR UI against specs/data-subject-rights-frontend-l0.yaml: the privacy dashboard
    lists the subject's DSR requests with status + SLA due date, tracking each (DSR-FE-001). The access
    page submits via dsrOpenAccess and renders the returned export (002). The rectify page renders a
    field-level correction form (field_path / current_value / corrected_value) (003). The erasure page
    guards the destructive request behind a confirm-dialog and calls dsrErasure on confirm (004). The
    portability page lets the subject choose json/csv and calls dsrPortability, exposing the download
    (005). The restrict page guards the processing-freeze behind a confirm-dialog and calls dsrRestrict
    (006), and offers a lift-restriction requiring a justification via dsrLiftRestrict (007). The request
    detail page shows the tracking envelope (status, received_at, due_at, closed_at) (008) and offers an
    extend-window action (extensionDays + extensionReason) calling the extend endpoint (009). Every
    destructive/irreversible action (erasure, restrict) is behind a confirm dialog; lift/extend require a
    justification.
evidence:
  - source_type: external
    citation: "GDPR Article 15(1) — Right of access: the access page (dsrOpenAccess) realizes the subject's right to obtain confirmation + access to their personal data (DSR-FE-002)"
    url: "https://gdpr-info.eu/art-15-gdpr/"
    quote: "The data subject shall have the right to obtain from the controller confirmation as to whether or not personal data concerning him or her are being processed, and, where that is the case, access to the personal data and the following information:"
    quoted_at: "2026-06-06"
  - source_type: external
    citation: "React Docs — Reacting to input with state (declarative UI): DSR pages render request states + guard destructive actions behind a confirm-dialog state (DSR-FE-004/006)"
    url: "https://react.dev/learn/reacting-to-input-with-state"
    quote: "React provides a declarative way to manipulate the UI. Instead of manipulating individual pieces of the UI directly, you describe the different states that your component can be in, and switch between them in response to the user input."
    quoted_at: "2026-06-06"
decided_at: "2026-06-06"
---

## DSR UI must realize the GDPR rights contract — dashboard with SLA, access/rectify/portability flows, destructive erasure/restrict behind a confirm dialog

**Impact: HIGH — The data-subject-rights surface implements legal entitlements under GDPR Articles 15-20 — per Art 15(1), *the data subject shall have the right to obtain from the controller confirmation as to whether or not personal data concerning him or her are being processed, and, where that is the case, access to the personal data*. Two classes of defect are serious here: a destructive action (erasure, processing-restriction) fired without a confirm dialog destroys or freezes a subject's data on a misclick and is irreversible; and a dashboard that hides the SLA due date lets a statutory response deadline pass silently. The DSR-frontend spec binds each right to its page, endpoint, and the guards its severity demands. React renders the request states and the confirm-dialog gate declaratively — *you describe the different states that your component can be in*.**

There are nine load-bearing requirements — the items of `specs/data-subject-rights-frontend-l0.yaml`, all governed by this rule (the DSR backend is `specs/data-subject-rights-l0.yaml`).

**Dashboard + access + rectify (DSR-FE-001..003).** The privacy dashboard lists the subject's DSR requests with status + SLA due date (001). The access page submits via `dsrOpenAccess` and renders the returned export (002). The rectify page renders a field-level correction form — `field_path` / `current_value` / `corrected_value` (003).

**Destructive flows behind confirm (DSR-FE-004, 006, 007).** The erasure page guards the request behind a confirm-dialog and calls `dsrErasure` on confirm (004). The restrict page guards the processing-freeze behind a confirm-dialog and calls `dsrRestrict` (006), and offers a lift-restriction action requiring a justification via `dsrLiftRestrict` (007).

**Portability + detail + extend (DSR-FE-005, 008, 009).** The portability page lets the subject choose json/csv and calls `dsrPortability`, exposing the download (005). The request detail page shows the tracking envelope — status, received_at, due_at, closed_at (008) — and offers an extend-window action (`extensionDays` + `extensionReason`) calling the extend endpoint (009).

**Incorrect — erasure with no confirm; dashboard hides the SLA; lift with no justification:**

```tsx
<button onClick={() => dsrErasure(id)}>Erase my data</button>   {/* VIOLATION: destructive, no confirm dialog (DSR-FE-004) */}
<RequestRow status={r.status} />                                {/* VIOLATION: no SLA due date shown (DSR-FE-001) */}
<button onClick={() => dsrLiftRestrict(id)}>Lift</button>       {/* VIOLATION: no required justification (DSR-FE-007) */}
```

**Correct — confirm-gated erasure/restrict, SLA on the dashboard, justification required:**

```tsx
<ConfirmDialog title="Erase all your data?" destructive                       // DSR-FE-004
  onConfirm={() => dsrErasure(id)}><button>Erase my data</button></ConfirmDialog>
<RequestRow status={r.status} dueAt={r.due_at} />                              // SLA due date (DSR-FE-001)
<LiftRestrictionForm onSubmit={({justification}) => dsrLiftRestrict(id, justification)} required />  // DSR-FE-007
// access page: const export = await dsrOpenAccess(); render export  (DSR-FE-002, GDPR Art 15)
```

Verification: review-tier. DSR-UI correctness is a legal + safety property with no compile signal — an unconfirmed erasure compiles and destroys data on a misclick. Verify by review against `specs/data-subject-rights-frontend-l0.yaml`: the dashboard shows status + SLA; access/rectify/portability call their endpoints; erasure and restrict are behind confirm dialogs; lift and extend require a justification; the detail page shows the tracking envelope. When a fork-receiver wires real tests (erasure requires confirm; lift requires justification; dashboard shows due_at), this rule's verification may be upgraded from review to a test-tag binding.

Reference: [GDPR Article 15 — Right of access](https://gdpr-info.eu/art-15-gdpr/)

Reference: [React — Reacting to input with state](https://react.dev/learn/reacting-to-input-with-state)
