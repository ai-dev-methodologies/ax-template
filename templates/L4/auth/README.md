# L4/auth — Auth Domain Reference Workload

**Layer**: L4 (full vertical)  
**Domain**: auth  
**Mode**: `full_trio` — bound to `contracts/auth-openapi.yaml` + `contracts/auth-ui.yaml`

This directory is the **reference workload** for the auth domain. It is the
composition of L1 (shadcn wrappers) + L2 (feature blocks) + L3 (page templates)
mapped to the backend Spec Trio (`specs/auth-asvs-l1.yaml` + `contracts/auth-openapi.yaml`
+ `blueprints/auth-manifest.yaml`).

---

## How to fork

Copy `templates/L4/auth/` to your Next.js project's `app/` directory:

```bash
# From your project root
cp -r ax-template/templates/L4/auth/app/(auth)     your-app/app/(auth)
cp -r ax-template/templates/L4/auth/app/(authenticated) your-app/app/(authenticated)
cp    ax-template/templates/L4/auth/app/layout.tsx  your-app/app/layout.tsx
cp    ax-template/templates/L4/auth/app/page.tsx    your-app/app/page.tsx
cp    ax-template/templates/L4/auth/app/providers.tsx your-app/app/providers.tsx
cp    ax-template/templates/L4/auth/middleware.ts   your-app/middleware.ts
cp    ax-template/templates/L4/auth/next.config.ts  your-app/next.config.ts
```

Then copy the L1/L2/L3 dependencies:

```bash
cp -r ax-template/templates/L1/components  your-app/src/components/ui
cp -r ax-template/templates/L2/blocks      your-app/src/components/blocks
cp -r ax-template/templates/L3/pages       your-app/src/templates
```

---

## File structure

```
app/
├── (auth)/                          # unauthenticated routes
│   ├── layout.tsx                   # no chrome (no sidebar/nav)
│   ├── login/page.tsx               # → POST /auth/email/login (emailLogin)
│   ├── signup/page.tsx              # → POST /auth/email/signup (emailSignup)
│   ├── verify/page.tsx              # → GET  /auth/email/verify-email (emailVerify)
│   └── oauth/callback/page.tsx      # → GET  /auth/oauth/{provider}/callback (oauthCallback)
├── (authenticated)/                 # protected routes
│   ├── layout.tsx                   # L2 ProtectedRoute guard + router.replace('/login')
│   └── dashboard/page.tsx           # → GET  /auth/me (getAuthState)
├── layout.tsx                       # root html/body + Providers
├── page.tsx                         # redirect → /login
└── providers.tsx                    # QueryClientProvider + MSW dev setup
middleware.ts                        # edge cookie-check → redirect /login
next.config.ts                       # API proxy rewrite + build config
```

---

## Backend operation bindings

| Route | HTTP | Operation ID | Backend endpoint |
|-------|------|--------------|------------------|
| /login | POST | `emailLogin` | `/auth/email/login` |
| /signup | POST | `emailSignup` | `/auth/email/signup` |
| /verify | GET | `emailVerify` | `/auth/email/verify-email?token=` |
| /oauth/callback | GET | `oauthCallback` | `/auth/oauth/{provider}/callback` |
| /dashboard | GET | `getAuthState` | `/auth/me` |

All operation IDs resolve against `contracts/auth-openapi.yaml`.

---

## Required dependencies

```json
{
  "dependencies": {
    "next": "^15.0.0",
    "react": "^19.0.0",
    "react-dom": "^19.0.0",
    "@tanstack/react-query": "^5.0.0"
  },
  "devDependencies": {
    "msw": "^2.0.0",
    "@types/react": "^19.0.0",
    "@types/node": "^22.0.0",
    "tailwindcss": "^4.0.0"
  }
}
```

---

## Fork customisation checklist

- [ ] Replace `fetch()` calls with your auth store (Zustand / next-auth / tRPC)
- [ ] Update `NEXT_PUBLIC_API_BASE` env var for your backend URL
- [ ] Set `API_BASE_URL` in `next.config.ts` for the API proxy rewrite
- [ ] Update OAuth providers list in `(auth)/login/page.tsx`
- [ ] Configure cookie name in `middleware.ts` to match your auth backend
- [ ] Update `middleware.ts` matcher to cover all your protected routes
- [ ] Remove MSW from `providers.tsx` if you don't need mock development setup
- [ ] Add your real font imports and global CSS to `app/layout.tsx`

---

## Verification

```bash
# From repo root
bash skills/ax-verify-domain/scripts/run.sh auth
bash skills/ax-verify-L4/scripts/run.sh
```

Both must exit 0 before shipping.

## Recipe Composition

applied_recipe: saas-subscription
applied_recipes:
  - b2b-admin
  - cms
  - community
  - lms
  - saas-subscription
