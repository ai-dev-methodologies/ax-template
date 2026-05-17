# error-page — L3 Next.js Error State Bundle

Three Next.js App Router special files that handle error states in a route
segment. Copy the relevant files into your L4 route segment and customize.

## Files

| File | Convention | When rendered |
|---|---|---|
| `loading.tsx` | `loading.tsx` | Automatic Suspense boundary during navigation |
| `not-found.tsx` | `not-found.tsx` | When `notFound()` is called or no route matches |
| `error.tsx` | `error.tsx` (**Client Component**) | Uncaught errors in the route segment |

## Slot contract

These files follow Next.js built-in conventions — no custom slot props.

| File | Props | Notes |
|---|---|---|
| `loading.tsx` | none | Automatically injected by Next.js Suspense |
| `not-found.tsx` | none | Called by `notFound()` in server components |
| `error.tsx` | `{ error: Error & { digest?: string }, reset: () => void }` | Must be `'use client'` |

## Usage (L4 example)

Copy the files directly into your route directory:

```
app/
└── products/
    ├── loading.tsx     ← copy from L3/pages/error-page/loading.tsx
    ├── not-found.tsx   ← copy from L3/pages/error-page/not-found.tsx
    ├── error.tsx       ← copy from L3/pages/error-page/error.tsx
    └── page.tsx        ← your L4 list page
```

Then customize:
- `loading.tsx`: match skeleton shape to your list/detail/form layout
- `not-found.tsx`: update copy and "Go home" href for your domain
- `error.tsx`: add error reporting (Sentry, Datadog) in the `useEffect`

## Accessibility

- `loading.tsx`: `aria-busy="true"` on `<main>`
- `not-found.tsx`: semantic `<h1>` + descriptive copy
- `error.tsx`: `role="alert"` + `aria-live="assertive"` for screen reader announcement

## Layer dependencies

- **L1**: No direct imports
- **L2**: No L2 blocks (error states are layout-independent)
- **L4**: Provides error reporting integration and domain-specific copy
