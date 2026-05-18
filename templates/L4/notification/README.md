# L4/notification — Notification Domain Reference Workload

**Layer**: L4 (full vertical)  
**Domain**: notification  
**Mode**: `full_trio` — bound to `contracts/notification-openapi.yaml` + `contracts/notification-ui.yaml`

This directory is the **reference workload** for the notification domain. It is the
composition of L1 (shadcn wrappers) + L2 (feature blocks) + L3 (page templates)
mapped to the backend Spec Trio (`specs/notification-l0.yaml` + `contracts/notification-openapi.yaml`
+ `blueprints/notification-manifest.yaml`).

---

## What's included

| File | Purpose |
|---|---|
| `app/layout.tsx` | Root HTML shell + Providers (QueryClient) |
| `app/page.tsx` | Redirect → `/inbox` |
| `app/providers.tsx` | TanStack Query provider |
| `app/(notification)/layout.tsx` | Authenticated layout: AppShell + Sidebar + AppHeader + **NotificationBell** |
| `app/(notification)/inbox/page.tsx` | **INBOX** — NotificationList (VirtualizedTable) + UNREAD/READ/ALL filter |
| `app/(notification)/settings/page.tsx` | **SETTINGS** — Channel preferences form (in-app / email toggles) |
| `app/(notification)/[id]/page.tsx` | **DETAIL** — Single notification view; auto mark-read; dismiss action |
| `next.config.ts` | API proxy rewrite to Spring Boot backend |

---

## How to fork

### 1. Copy the directory into your project

```bash
# From repo root — copy the app/ directory and next.config.ts
cp -r templates/L4/notification/app <your-nextjs-project>/app
cp templates/L4/notification/next.config.ts <your-nextjs-project>/next.config.ts
```

### 2. Copy L1/L2 dependencies

```bash
# L2 blocks used by this domain
cp templates/L2/blocks/notification-bell.tsx   src/components/blocks/
cp templates/L2/blocks/notification-list.tsx   src/components/blocks/
cp templates/L2/blocks/notification-item.tsx   src/components/blocks/
cp templates/L2/blocks/virtualized-table.tsx   src/components/blocks/
cp templates/L2/blocks/filter-bar.tsx          src/components/blocks/
cp templates/L2/blocks/empty-state.tsx         src/components/blocks/
cp templates/L2/blocks/app-shell.tsx           src/components/blocks/
cp templates/L2/blocks/app-header.tsx          src/components/blocks/
cp templates/L2/blocks/sidebar.tsx             src/components/blocks/
```

### 3. Install dependencies

```bash
npm install @tanstack/react-query @tanstack/react-virtual
```

### 4. Configure environment

```bash
# .env.local
API_BASE_URL=http://localhost:8080
NEXT_PUBLIC_API_BASE=http://localhost:8080
```

### 5. Wire authentication

All notification endpoints require a Bearer JWT. Inject the auth token via:
- A Next.js route handler (BFF proxy) — recommended for SSR
- A fetch interceptor that reads the access token from memory

---

## File structure

```
app/
├── layout.tsx                        # Root html/body + Providers
├── page.tsx                          # Redirect → /inbox
├── providers.tsx                     # QueryClientProvider
└── (notification)/
    ├── layout.tsx                    # AppShell + Sidebar + AppHeader + NotificationBell
    ├── inbox/
    │   └── page.tsx                  # → GET  /api/notifications (listNotifications)
    ├── settings/
    │   └── page.tsx                  # → GET+PATCH /api/notifications/preferences
    └── [id]/
        └── page.tsx                  # → GET /api/notifications/{id} + PATCH /read + DELETE
next.config.ts
README.md
```

---

## Backend operation bindings

| Route | HTTP | Operation ID | Backend endpoint |
|-------|------|--------------|------------------|
| /inbox | GET | `listNotifications` | `/api/notifications?status&page&size` |
| /settings | GET | `getNotificationPreferences` | `/api/notifications/preferences` |
| /settings | PATCH | `updateNotificationPreferences` | `/api/notifications/preferences` |
| /[id] | GET | `getNotification` | `/api/notifications/{id}` |
| /[id] | PATCH | `markNotificationRead` | `/api/notifications/{id}/read` |
| /[id] | DELETE | `dismissNotification` | `/api/notifications/{id}` |

All operation IDs resolve against `contracts/notification-openapi.yaml`.

---

## L2 blocks used

| Block | Purpose |
|-------|---------|
| `notification-bell` | Header bell with unread badge (polls X-Unread-Count) |
| `notification-list` | Full inbox: VirtualizedTable + filter + mutations |
| `notification-item` | Single notification card with type badge + actions |
| `virtualized-table` | DOM-efficient large list rendering (SP15) |
| `filter-bar` | Status filter (UNREAD/READ/ALL) |
| `empty-state` | Empty inbox / error state |
| `app-shell` | Authenticated shell (sidebar + header) |
| `app-header` | Top bar with title + bell slot |
| `sidebar` | Navigation sidebar |

---

## Spec Trio binding

| Artifact | File |
|---|---|
| Backend Page Compliance Spec | `specs/notification-l0.yaml` |
| Frontend Page Compliance Spec | `specs/notification-frontend-l0.yaml` |
| Backend OpenAPI Contract | `contracts/notification-openapi.yaml` |
| Frontend UI Contract | `contracts/notification-ui.yaml` |
| Backend Policy Manifest | `blueprints/notification-manifest.yaml` |
| Frontend UI Manifest | `blueprints/notification-ui-manifest.yaml` |

---

## Fork customisation checklist

- [ ] Replace `fetch()` calls with your API client / tRPC calls
- [ ] Update `NEXT_PUBLIC_API_BASE` and `API_BASE_URL` env vars
- [ ] Wire auth token to notification fetch headers
- [ ] Adjust `NotificationBell.pollIntervalMs` or replace with SSE
- [ ] Add `ToastQueue` (L2 toast-queue) for success/error feedback
- [ ] Extend settings page with additional preference channels
- [ ] Configure MSW handlers for local development mocking

---

## Verification

```bash
# From repo root
bash skills/ax-verify-domain/scripts/run.sh notification
bash skills/ax-verify-L4/scripts/run.sh
bash skills/ax-verify-L2/scripts/run.sh
```

All three must exit 0 before shipping.

## Recipe Composition

applied_recipe: saas-subscription
applied_recipe_secondary: e-commerce
applied_recipe_tertiary: crm
applied_recipes:
  - booking
  - community
  - crm
  - e-commerce
  - marketplace
  - saas-subscription
