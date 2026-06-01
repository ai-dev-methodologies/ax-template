---
title: Destructive admin actions MUST confirm with explicit side-effect enumeration
impact: HIGH
impactDescription: "A bare-onClick destructive action under pager-driven triage produces single-misclick incidents (duplicate deliveries, voided approvals, lost notes) — the confirm copy must spell out which side effects will happen"
tags:
  - admin
  - destructive-action
  - confirm-dialog
  - incident-prevention
spec_ref: "specs/scheduled-task-l0.yaml#SCHED-EXECUTE-001"
verification:
  type: review
  source: "templates/L4/webhook/app/(admin)/webhooks/deliveries/page.tsx, templates/L4/scheduled-task/app/(admin)/scheduled-tasks/page.tsx, templates/L4/favorites-bookmarks/app/(favorites)/page.tsx"
  pattern: "window.confirm with verbatim enumeration of downstream side effects (HTTP POST to partner / db writes / notifications / audit-trail invalidation / quota voided) BEFORE the mutation fires"
upstream:
  - "https://www.w3.org/WAI/WCAG22/Understanding/error-prevention-legal-financial-data.html"
  - "https://owasp.org/www-project-application-security-verification-standard/"
evidence:
  - source_type: external
    citation: "WCAG 2.2 — Success Criterion 3.3.4 Error Prevention (Legal, Financial, Data) (Level AA)"
    url: "https://www.w3.org/WAI/WCAG22/Understanding/error-prevention-legal-financial-data.html"
    quote: "A mechanism is available for reviewing, confirming, and correcting information before finalizing the submission."
    quoted_at: "2026-05-25"
---

## Destructive admin actions MUST confirm with explicit side-effect enumeration

**Impact: HIGH — a bare onClick on a destructive admin action turns one misclick into one incident**

Webhook delete / replay, scheduled-task trigger, mark-all-read, favorite-remove-with-note, approval-cancel-with-priors — these all have downstream side effects that are not trivially reversible:

- Webhook **replay** sends another POST to a partner. The partner may not implement idempotency. Duplicate side effects (double charge, double notification, double inventory move) cascade.
- Scheduled-task **trigger** runs the cron job out of cycle. The job fires its own POST/email/db writes as if scheduled.
- **Mark-all-read** on a notification surface clears server-side audit fact "did the operator actually read this?" — even when the inbox was wrong.
- **Delete favorite** with a note destroys the note (often a Korean enterprise 결재 / follow-up context).
- **Cancel approval** with one or more upstream approvals already granted voids those decisions in the audit trail.

The catalog convention since R43 / R46 / R48 / R49 is: confirm with **verbatim enumeration of the consequences**, not a generic "Are you sure?". The operator needs to read the side effects in the dialog so a 3am-pager-fatigue mind can stop before the click.

**Incorrect — bare onClick with no consequence enumeration:**

```tsx
<button
  type="button"
  aria-busy={replay.isPending || undefined}
  aria-disabled={replay.isPending || undefined}
  onClick={() => {
    if (replay.isPending) return
    replay.mutate(delivery.id)
  }}
>
  Replay
</button>
```

A pager-driven SRE during incident response can misclick this. The aria-busy + click guard prevents double-fire mid-flight (R47 rule), but the operator's intent is not verified before the first click commits. Replay fires immediately. The partner endpoint receives a duplicate delivery. There is no recovery from the partner side.

**Correct — confirm with side-effect enumeration:**

```tsx
<button
  onClick={() => {
    const ok = window.confirm(
      `Re-enqueue this delivery?\n\n${delivery.eventType} (attempt ${delivery.attemptCount})\nendpoint ${delivery.endpointId}\n\nThis sends another HTTP POST to the partner endpoint. If the original eventually succeeded server-side, the partner receives duplicate side effects.`,
    )
    if (!ok) return
    replay.mutate(delivery.id)
  }}
>
  Replay
</button>
```

The dialog text MUST satisfy three properties:
1. **Name the action** in past tense framing of consequence ("Re-enqueue this delivery?" not "Are you sure?")
2. **Show identifying context** for the specific object (event type, request id, approver chain, etc.) — so an operator with multiple windows knows which row this refers to
3. **List the side effects** as plain sentences. Korean enterprise partners frequently lack idempotent receivers; financial side effects (PG, inventory, billing) cascade

`window.confirm` is the catalog baseline — a fork-receiver may replace it with a styled Dialog primitive, but the three properties survive the swap. Native `confirm` is a11y-degraded vs a styled modal (separate rule `mutation-in-flight-uses-aria-busy` covers the in-flight state), but for the one-shot destructive-confirm path it is the lowest-common-denominator that catches misclick.

**When to apply this rule**: any mutation where (a) the server commits an irreversible side effect, (b) the side effect cascades to a third party (partner endpoint, downstream system, audit log), OR (c) reversibility requires multi-party coordination (re-issue a webhook secret + notify all downstream verifiers, re-file a cancelled approval, restore a deleted comment with note). For (a)+(b)+(c) any single condition triggers the rule.

**When NOT to apply**: trivially-reversible actions (toggle favorite, mark single notification read, edit a draft) — confirm there adds friction without preventing meaningful loss.

Reference: [WCAG 2.2 SC 3.3.4 — Error Prevention (Legal, Financial, Data)](https://www.w3.org/WAI/WCAG22/Understanding/error-prevention-legal-financial-data.html)

Reference: [OWASP ASVS V14.3 — Error Prevention](https://owasp.org/www-project-application-security-verification-standard/)
