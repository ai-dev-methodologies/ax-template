---
title: "The audit-log viewer UI must virtualize large lists, filter/paginate, gate export behind RBAC, and degrade with empty/error states"
rule_id: audit-log-frontend-viewer-rbac-virtualized
impact: MEDIUM
impactDescription: "An audit list that renders 10k+ rows into the DOM freezes the tab; an export surface not gated behind ROLE_ADMIN/ROLE_AUDITOR leaks the audit trail to unauthorized users; a list that throws on an API error takes down the whole page instead of offering retry; no EmptyState leaves a blank table that looks broken. The audit viewer handles sensitive, high-volume data — virtualization, RBAC, and graceful degradation are load-bearing."
tags:
  - audit-log
  - frontend
  - virtualization
  - rbac
  - error-boundary
  - contract-first
applicable_to:
  - react
  - nextjs
spec_ref: "specs/audit-log-frontend-l0.yaml#AUDIT-FE-001"
verification:
  type: review
  notes: |
    Reviewer confirms the audit viewer against specs/audit-log-frontend-l0.yaml: the list renders a
    VirtualizedTable handling >10k rows without DOM explosion (only visible rows, @tanstack/react-virtual)
    (001); a FilterBar filters by actor/resource-type/action/outcome/date-range and updates the query
    (002); pagination via page+size query params with prev/next links (003). The detail page renders full
    metadata for an entry by id (004). The export page renders a CSV/JSON format selector + optional
    filters, calling exportAuditLogs (POST) (005), and shows an access-denied notice (NOT the form) when
    the user lacks ROLE_ADMIN/ROLE_AUDITOR (006). The list shows an EmptyState when no entries match,
    prompting to clear filters (007), and an ErrorBoundary with a retry prompt when listAuditLogs fails,
    without crashing the page (008).
evidence:
  - source_type: external
    citation: "React Docs — Reacting to input with state (declarative UI): the viewer renders loaded/empty/error states declaratively and gates the export form on role (AUDIT-FE-006/007/008)"
    url: "https://react.dev/learn/reacting-to-input-with-state"
    quote: "React provides a declarative way to manipulate the UI. Instead of manipulating individual pieces of the UI directly, you describe the different states that your component can be in, and switch between them in response to the user input."
    quoted_at: "2026-06-06"
  - source_type: external
    citation: "OWASP ASVS v4.0.3 V4.1.3 — Access Control (least privilege): the export surface is gated to ROLE_ADMIN/ROLE_AUDITOR (AUDIT-FE-006)"
    url: "https://raw.githubusercontent.com/OWASP/ASVS/v4.0.3/4.0/en/0x12-V4-Access-Control.md"
    quote: "Verify that the principle of least privilege exists - users should only be able to access functions, data files, URLs, controllers, services, and other resources, for which they possess specific authorization."
    quoted_at: "2026-06-06"
decided_at: "2026-06-06"
---

## The audit-log viewer UI must virtualize large lists, filter/paginate, gate export behind RBAC, and degrade gracefully

**Impact: MEDIUM — An audit log is high-volume, sensitive data, and its viewer must handle both facts. Volume: rendering 10k+ rows into the DOM freezes the tab, so the list MUST virtualize (render only visible rows). Sensitivity: the export surface dumps the audit trail to a file, so it MUST be gated behind `ROLE_ADMIN`/`ROLE_AUDITOR` — an ungated export is a data leak, and ASVS demands *the principle of least privilege ... users should only be able to access ... resources for which they possess specific authorization*. And because audit data loads over the network, the viewer must degrade gracefully — an EmptyState on no-match, an ErrorBoundary with retry on API failure — not a blank table or a whole-page crash. React renders these states declaratively — *you describe the different states that your component can be in*.**

There are eight load-bearing requirements — the items of `specs/audit-log-frontend-l0.yaml`, all governed by this rule.

**List (AUDIT-FE-001..003, 007, 008).** A VirtualizedTable handling >10k rows without DOM explosion — only visible rows (001); a FilterBar over actor/resource-type/action/outcome/date-range that updates the query (002); pagination via page+size query params with prev/next (003); an EmptyState when nothing matches, prompting to clear filters (007); an ErrorBoundary with a retry prompt when `listAuditLogs` fails, without crashing the page (008).

**Detail (AUDIT-FE-004).** Full metadata for a single entry by id (actor, action, resource, outcome, timestamp, ...).

**Export with RBAC (AUDIT-FE-005..006).** An export page with a CSV/JSON format selector + optional filters calling `exportAuditLogs` (POST) (005); an access-denied notice — NOT the form — when the user lacks `ROLE_ADMIN`/`ROLE_AUDITOR` (006).

**Incorrect — full list in the DOM, ungated export, list crashes on error:**

```tsx
<table>{allEntries.map(e => <Row key={e.id} entry={e} />)}</table>   {/* VIOLATION: 10k rows in DOM (AUDIT-FE-001) */}
<ExportForm onSubmit={exportAuditLogs} />                            {/* VIOLATION: no RBAC gate (AUDIT-FE-006) */}
const data = useQuery(...).data;                                     {/* VIOLATION: no ErrorBoundary; throw crashes page (AUDIT-FE-008) */}
```

**Correct — virtualized, RBAC-gated export, empty + error-boundary states:**

```tsx
<VirtualizedTable rows={entries} />                                  // only visible rows (AUDIT-FE-001)
{entries.length === 0 && <EmptyState onClear={clearFilters} />}      // AUDIT-FE-007
// export page
if (!hasRole('ROLE_ADMIN','ROLE_AUDITOR')) return <AccessDenied />;  // AUDIT-FE-006 (least privilege)
<ExportForm formats={['CSV','JSON']} onSubmit={exportAuditLogs} />   // AUDIT-FE-005
// list wrapped:
<ErrorBoundary fallback={<RetryPrompt onRetry={refetch} />}><AuditList /></ErrorBoundary>  // AUDIT-FE-008
```

Verification: review-tier. Viewer correctness is a performance + access-control + resilience property with no compile signal. Verify by review against `specs/audit-log-frontend-l0.yaml`: the list virtualizes >10k rows; filter + pagination drive the query; the export form is gated to ROLE_ADMIN/ROLE_AUDITOR (access-denied notice otherwise); an EmptyState and an ErrorBoundary-with-retry handle no-match and API failure. When a fork-receiver wires real tests (non-admin sees access-denied; API error shows retry not a crash), this rule's verification may be upgraded from review to a test-tag binding.

Reference: [React — Reacting to input with state](https://react.dev/learn/reacting-to-input-with-state)

Reference: [OWASP ASVS V4 — Access Control](https://raw.githubusercontent.com/OWASP/ASVS/v4.0.3/4.0/en/0x12-V4-Access-Control.md)
