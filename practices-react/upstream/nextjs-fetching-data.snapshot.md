# Snapshot: Next.js 16 — Fetching Data

- **source**: https://nextjs.org/docs/app/getting-started/fetching-data
- **role**: canonical-nextjs
- **fetched_at**: 2026-05-13T00:00:00Z
- **doc_last_updated_at**: 2026-05-13 (per source frontmatter)
- **version_observed**: 16.2.6
- **via**: WebFetch

## Relevant sections (verbatim quotes for evidence anchoring)

### On Server Component data fetching

> "To fetch data with the `fetch` API, turn your component into an asynchronous function, and await the `fetch` call."

> **Good to know:** "Identical `fetch` requests in a React component tree are memoized by default, so you can fetch data in the component that needs it instead of drilling props."

> **Good to know:** "`fetch` requests are not cached by default and will block the page from rendering until the request is complete. Use the `use cache` directive to cache results, or wrap the fetching component in `<Suspense>` to stream fresh data at request time."

### On parallel data fetching (the canonical example for async-parallel rule)

> "Parallel data fetching happens when data requests in a route are eagerly initiated and start at the same time."

> "By default, layouts and pages are rendered in parallel. So each segment starts fetching data as soon as possible."

> "However, within any component, multiple async/await requests can still be sequential if placed after the other."

> "Start multiple requests by calling `fetch`, then await them with `Promise.all`. Requests begin as soon as `fetch` is called."

Canonical code example from the page:

```tsx
async function getArtist(username: string) {
  const res = await fetch(`https://api.example.com/artist/${username}`)
  return res.json()
}

async function getAlbums(username: string) {
  const res = await fetch(`https://api.example.com/artist/${username}/albums`)
  return res.json()
}

export default async function Page({ params }) {
  const { username } = await params

  // Initiate requests
  const artistData = getArtist(username)
  const albumsData = getAlbums(username)

  const [artist, albums] = await Promise.all([artistData, albumsData])

  return (
    <>
      <h1>{artist.name}</h1>
      <Albums list={albums} />
    </>
  )
}
```

> **Good to know:** "If one request fails when using `Promise.all`, the entire operation will fail. To handle this, you can use the `Promise.allSettled` method instead."

### On Client Component data fetching with `use()` API

> "You can use React's `use` API to stream data from the server to client. Start by fetching data in your Server component, and pass the promise to your Client Component as prop."

```tsx
import Posts from '@/app/ui/posts'
import { Suspense } from 'react'

export default function Page() {
  // Don't await the data fetching function
  const posts = getPosts()

  return (
    <Suspense fallback={<div>Loading...</div>}>
      <Posts posts={posts} />
    </Suspense>
  )
}
```

```tsx
'use client'
import { use } from 'react'

export default function Posts({ posts }) {
  const allPosts = use(posts)
  return (
    <ul>{allPosts.map(p => <li key={p.id}>{p.title}</li>)}</ul>
  )
}
```

### Next.js 16-specific: async params

Route handler signature: `params: Promise<{ username: string }>`. The page must `await params` before use; this is a separate sequential await that can be parallelized with other independent work.

## Note

Page metadata at top: `version: 16.2.6`, `lastUpdated: 2026-05-13`, `prerequisites: Getting Started: /docs/app/getting-started`.

AI agent hint embedded by Next.js: "If client-side navigations feel slow, Suspense and streaming alone are not enough. Export `unstable_instant` from the route to ensure instant navigations."
