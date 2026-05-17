---
snapshot_id: nextjs-server-actions-16
source: "https://nextjs.org/docs/app/getting-started/mutating-data"
fetched_at: "2026-05-17T13:00:00Z"
version_observed: "next@16.2.6"
via: WebFetch
sha: "b4c5d6e7f8a9b0c1d2e3f4a5b6c7d8e9f0a1b2c3d4e5f6a7b8c9d0e1f2a3b4c5"
---

# Next.js 16 — Server Actions (Mutating Data)

Source: https://nextjs.org/docs/app/getting-started/mutating-data  
Version: next@16.2.6 · lastUpdated: 2026-05-13

## What are Server Functions?

A **Server Function** is an asynchronous function that runs on the server. Called from the client via a network request. In an action/mutation context they are also called **Server Actions**.

> WARNING: Server Functions are reachable via direct POST requests, not just your app's UI.
> Always verify authentication and authorization inside every Server Function.

## Defining Server Actions

Add `'use server'` directive at the top of an async function or at file level:

```ts
// app/lib/actions.ts
import { auth } from '@/lib/auth'

export async function createPost(formData: FormData) {
  'use server'
  const session = await auth()
  if (!session?.user) throw new Error('Unauthorized')

  const title = formData.get('title')
  // Mutate data, then revalidate cache
}
```

## Invocation Patterns

### Forms (Server + Client Components)

```tsx
import { createPost } from '@/app/actions'

export function Form() {
  return (
    <form action={createPost}>
      <input type="text" name="title" />
      <button type="submit">Create</button>
    </form>
  )
}
```

### Event Handlers (Client Components only)

```tsx
'use client'
import { incrementLike } from './actions'

export default function LikeButton({ initialLikes }: { initialLikes: number }) {
  const [likes, setLikes] = useState(initialLikes)
  return (
    <button onClick={async () => {
      const updated = await incrementLike()
      setLikes(updated)
    }}>Like</button>
  )
}
```

## After Mutation: Revalidation & Redirect

```ts
import { revalidatePath } from 'next/cache'
import { redirect } from 'next/navigation'

export async function createPost(formData: FormData) {
  'use server'
  // mutate...
  revalidatePath('/posts')    // refresh data
  redirect('/posts')          // navigate after mutation
}
```

## Pending State with useActionState

```tsx
'use client'
import { useActionState, startTransition } from 'react'

export function Button() {
  const [state, action, pending] = useActionState(createPost, false)
  return (
    <button onClick={() => startTransition(action)}>
      {pending ? 'Creating...' : 'Create Post'}
    </button>
  )
}
```

## Cookie Management

```ts
'use server'
import { cookies } from 'next/headers'

export async function exampleAction() {
  const cookieStore = await cookies()
  cookieStore.get('name')?.value
  cookieStore.set('name', 'value')
  cookieStore.delete('name')
}
```
