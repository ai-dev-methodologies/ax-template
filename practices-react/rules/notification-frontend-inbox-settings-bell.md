---
title: "Notification UI must realize the notification contract — virtualized inbox with status filter, mark-read/dismiss actions, preference toggles (partial update), and an unread-count bell"
rule_id: notification-frontend-inbox-settings-bell
impact: MEDIUM
impactDescription: "A notification inbox that renders every notification into the DOM degrades over time; a mark-read that does not PATCH leaves the unread count permanently wrong; a settings form that PUTs the whole preferences object instead of a partial PATCH clobbers fields the user did not change; a bell with no unread badge hides pending notifications. The notification surface is high-frequency UI where each defect erodes trust in the count."
tags:
  - notification
  - frontend
  - virtualization
  - forms
  - contract-first
applicable_to:
  - react
  - nextjs
spec_ref: "specs/notification-frontend-l0.yaml#NOTIF-FE-001"
verification:
  type: review
  notes: |
    Reviewer confirms the notification UI against specs/notification-frontend-l0.yaml: the inbox renders
    the list via VirtualizedTable and supports a status filter (UNREAD/READ/ALL) (001). The detail page
    loads a single notification and renders a mark-read action calling PATCH /api/notifications/{id} (002).
    The settings page renders a preference form with inAppEnabled + emailEnabled toggles (controlled)
    (003) and submits a PARTIAL update via PATCH /api/notifications/preferences — not a full-object PUT
    (004). The NotificationBell in the app header displays an unread badge count, updated by polling or SSE
    (005). The dismiss action on the detail page calls DELETE /api/notifications/{id} and navigates away
    (006).
evidence:
  - source_type: external
    citation: "React Docs — <input> (controlled inputs): notification preference toggles are controlled (value/checked + onChange) (NOTIF-FE-003)"
    url: "https://react.dev/reference/react-dom/components/input"
    quote: "To render a controlled input, pass the value prop to it (or checked for checkboxes and radios). React will force the input to always have the value you passed."
    quoted_at: "2026-06-06"
  - source_type: external
    citation: "React Docs — Reacting to input with state (declarative UI): inbox renders filtered/unread/read states and the bell badge declaratively (NOTIF-FE-001/005)"
    url: "https://react.dev/learn/reacting-to-input-with-state"
    quote: "React provides a declarative way to manipulate the UI. Instead of manipulating individual pieces of the UI directly, you describe the different states that your component can be in, and switch between them in response to the user input."
    quoted_at: "2026-06-06"
decided_at: "2026-06-06"
---

## Notification UI must realize the notification contract — virtualized inbox, mark-read/dismiss, partial-update preferences, unread-count bell

**Impact: MEDIUM — Notifications are high-frequency UI, and the user's trust hinges on the unread count being right. The defects compound: an inbox that renders every notification degrades as they accumulate; a mark-read that updates local state but never PATCHes the server leaves the count wrong on the next load; a settings form that PUTs the entire preferences object instead of a PATCH partial-update clobbers a toggle the user did not touch; a header bell with no badge hides that anything is pending. The notification-frontend spec binds each surface to its endpoint and the right update shape. React supplies the primitives — preference toggles are controlled (*to render a controlled input, pass the value prop ... React will force the input to always have the value you passed*) and the inbox/bell render their states declaratively.**

There are six load-bearing requirements — the items of `specs/notification-frontend-l0.yaml`, all governed by this rule.

**Inbox + detail (NOTIF-FE-001, 002, 006).** The inbox renders the list via VirtualizedTable with a status filter (UNREAD/READ/ALL) (001). The detail page loads a single notification and renders a mark-read action calling `PATCH /api/notifications/{id}` (002). The dismiss action calls `DELETE /api/notifications/{id}` and navigates away (006).

**Settings (NOTIF-FE-003, 004).** A preference form with `inAppEnabled` + `emailEnabled` controlled toggles (003), submitting a PARTIAL update via `PATCH /api/notifications/preferences` — never a full-object PUT that clobbers untouched fields (004).

**Bell (NOTIF-FE-005).** The NotificationBell in the app header displays an unread badge count, updated by polling or SSE.

**Incorrect — full list in DOM, local-only mark-read, full PUT of preferences:**

```tsx
{notifications.map(n => <Item key={n.id} n={n} />)}              {/* VIOLATION: not virtualized (NOTIF-FE-001) */}
function markRead(n) { setLocal(n.id); }                         {/* VIOLATION: no PATCH → count wrong on reload (NOTIF-FE-002) */}
await api.put('/notifications/preferences', allPrefs);           {/* VIOLATION: full PUT clobbers untouched fields (NOTIF-FE-004) */}
```

**Correct — virtualized inbox, server PATCH mark-read, partial-update preferences, badge bell:**

```tsx
<VirtualizedTable rows={filtered} />                             // NOTIF-FE-001 (+ status filter)
async function markRead(id) { await api.patch(`/notifications/${id}`, { read: true }); }  // NOTIF-FE-002
<Toggle checked={prefs.inAppEnabled} onChange={v =>                                        // controlled (NOTIF-FE-003)
  api.patch('/notifications/preferences', { inAppEnabled: v })} />                          // PARTIAL patch (NOTIF-FE-004)
<NotificationBell unread={unreadCount} />                        // badge, polled/SSE (NOTIF-FE-005)
async function dismiss(id) { await api.delete(`/notifications/${id}`); router.back(); }    // NOTIF-FE-006
```

Verification: review-tier. Notification-contract fidelity is a UI-to-API + performance property with no compile signal. Verify by review against `specs/notification-frontend-l0.yaml`: the inbox virtualizes with a status filter; mark-read PATCHes and dismiss DELETEs the server; preferences submit a partial PATCH; the bell shows an unread badge. When a fork-receiver wires real tests (mark-read PATCHes; preferences PATCH is partial), this rule's verification may be upgraded from review to a test-tag binding.

Reference: [React — <input> (controlled inputs)](https://react.dev/reference/react-dom/components/input)

Reference: [React — Reacting to input with state](https://react.dev/learn/reacting-to-input-with-state)
