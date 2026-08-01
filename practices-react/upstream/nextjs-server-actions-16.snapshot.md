# nextjs-server-actions-16 — upstream snapshot (2026-08-01 refresh, append-only)

**Source URL(s):** https://nextjs.org/docs/app/getting-started/mutating-data (re-fetched 2026-08-01; every pre-existing section below the divider is preserved verbatim)
**HTTP status:** 200
**Fetched at:** 2026-08-01T01:46:56Z
**Extractor invocation:** `practices/scripts/snapshot-extract.sh https://nextjs.org/docs/app/getting-started/mutating-data`
**Fetch receipt:** `practices/upstream/_FETCH-RECEIPTS.yaml` id `r092`
**Body SHA-256 (below the `---` divider, header excluded):** d8394c16a93d41fb0e71a0173ff29c3eabe4d90b54b101ab12203aaba45f0310

---

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

---

## Upstream refresh 2026-08-01 (verbatim extractor output)

Source: https://nextjs.org/docs/app/getting-started/mutating-data
HTTP status: 200 · extracted bytes: 15671 · sha256: 8489263b572891c90bdbe00b2ba24b8d0785048239d1aeb34a3ba3b5502f8093
Extractor: `practices/scripts/snapshot-extract.sh` (curl -> deterministic HTML->text; no model in the loop)
Fetch receipt: `practices/upstream/_FETCH-RECEIPTS.yaml` id `r092`

Everything above this divider is the previous snapshot, preserved byte-for-byte (append-only:
history is recorded, never rewritten). The block below is the UNMODIFIED extractor output for
the 2026-08-01 re-fetch of the same URL — it is the current upstream text, and any citation that
claims to quote this source verbatim must match it.

Getting Started: Mutating Data | Next.js Skip to content Search documentation... ⌘K Search... ⌘K Showcase Docs Blog Templates Enterprise Search documentation... ⌘K Search... ⌘K Feedback Learn Menu Using App Router Features available in /app Latest Version 16.2.12 Getting Started Installation Project Structure Layouts and Pages Linking and Navigating Server and Client Components Fetching Data Mutating Data Caching Revalidating Error Handling CSS Image Optimization Font Optimization Metadata and OG images Route Handlers Proxy Deploying Upgrading Guides AI Coding Agents Analytics Authentication Backend for Frontend Caching (Previous Model) CDN Caching CI Build Caching Content Security Policy CSS-in-JS Custom Server Data Security Debugging Deploying to Platforms Draft Mode Environment Variables Forms How Revalidation Works ISR Instrumentation Internationalization JSON-LD Lazy Loading Development Environment Next.js MCP Server MDX Memory Usage Migrating App Router Create React App Vite Migrating to Cache Components Multi-tenant Multi-zones OpenTelemetry Package Bundling PPR Platform Guide Prefetching Preserving UI state Preventing Flash Production PWAs Public pages Redirecting Rendering Philosophy Sass Scripts Self-Hosting Server Actions SPAs Static Exports Streaming Tailwind CSS v3 Testing Cypress Jest Playwright Vitest Third Party Libraries Upgrading Codemods Version 14 Version 15 Version 16 Videos View transitions API Reference Directives use cache use cache: private use cache: remote use client use server Components Font Form Component Image Component Link Component Script Component File-system conventions default.js Dynamic Segments error.js forbidden.js instrumentation.js instrumentation-client.js Intercepting Routes layout.js loading.js mdx-components.js not-found.js page.js Parallel Routes proxy.js public route.js Route Groups src template.js unauthorized.js Metadata Files favicon, icon, and apple-icon manifest.json opengraph-image and twitter-image robots.txt sitemap.xml Route Segment Config dynamicParams maxDuration preferredRegion runtime Functions after cacheLife cacheTag unstable_catchError connection cookies draftMode fetch forbidden generateImageMetadata generateMetadata generateSitemaps generateStaticParams generateViewport headers ImageResponse NextRequest NextResponse notFound permanentRedirect redirect refresh revalidatePath revalidateTag unauthorized unstable_cache unstable_noStore unstable_rethrow updateTag useLinkStatus useParams usePathname useReportWebVitals useRouter useSearchParams useSelectedLayoutSegment useSelectedLayoutSegments userAgent Configuration next.config.js adapterPath allowedDevOrigins appDir assetPrefix authInterrupts basePath cacheComponents cacheHandlers cacheLife compress crossOrigin cssChunking deploymentId devIndicators distDir env expireTime exportPathMap generateBuildId generateEtags headers htmlLimitedBots httpAgentOptions images cacheHandler inlineCss logging mdxRs onDemandEntries optimizePackageImports output pageExtensions poweredByHeader productionBrowserSourceMaps proxyClientMaxBodySize reactCompiler reactMaxHeadersLength reactStrictMode redirects rewrites sassOptions serverActions serverComponentsHmrCache serverExternalPackages staleTimes staticGeneration* taint trailingSlash transpilePackages turbopack turbopackFileSystemCache turbopack.ignoreIssue turbopackLocalPostcssConfig typedRoutes typescript urlImports useLightningcss useTypeScriptCli viewTransition webpack webVitalsAttribution TypeScript ESLint CLI create-next-app next CLI Adapters Configuration Creating an Adapter API Reference Testing Adapters Routing with @next/routing Implementing PPR in an Adapter Runtime Integration Invoking Entrypoints Output Types Routing Information Use Cases Edge Runtime Turbopack Glossary Architecture Accessibility Fast Refresh Next.js Compiler Supported Browsers Community Contribution Guide Rspack On this page What are Server Functions? Creating Server Functions Server Components Client Components Passing actions as props Invoking Server Functions Forms Event Handlers Examples Showing a pending state Refresh data Revalidate data Redirect after a mutation Cookies useEffect Next steps Edit this page on GitHub Scroll to top This page is also available as Markdown at /docs/app/getting-started/mutating-data.md . For an index of Next.js documentation , see /docs/llms.txt . App Router Getting Started Mutating Data Copy page Mutating Data Last updated June 23, 2026 You can mutate data in Next.js using React Server Functions . This page will go through how you can create and invoke Server Functions. For Next.js-specific behaviors (single-roundtrip response, sequential dispatch, security, deployment), see Server Actions and Mutations . What are Server Functions? A Server Function is an asynchronous function that runs on the server. You can call them from the client through a network request, which is why they must be asynchronous. In an action or mutation context, they are also called Server Actions . By convention, a Server Action is an async function used with startTransition . This happens automatically when the function is: Passed to a <form> using the action prop. Passed to a <button> using the formAction prop. When an action is invoked, Next.js can return both the updated UI and new data in a single server roundtrip. Behind the scenes, actions use the POST method, and only this HTTP method can invoke them. Server Functions are reachable via direct POST requests, not just through your application's UI. Always verify authentication and authorization inside every Server Function. See the Data Security guide for recommended patterns. Good to know: A Server Action is a Server Function used in a specific way (for handling form submissions and mutations). Server Function is the broader term. Creating Server Functions A Server Function can be defined by using the use server directive. You can place the directive at the top of an asynchronous function to mark the function as a Server Function, or at the top of a separate file to mark all exports of that file. app/lib/actions.ts TypeScript JavaScript TypeScript import { auth } from '@/lib/auth' export async function createPost (formData : FormData ) { 'use server' const session = await auth () if ( ! session ?.user) { throw new Error ( 'Unauthorized' ) } const title = formData .get ( 'title' ) const content = formData .get ( 'content' ) // Mutate data // Revalidate cache } export async function deletePost (formData : FormData ) { 'use server' const session = await auth () if ( ! session ?.user) { throw new Error ( 'Unauthorized' ) } const id = formData .get ( 'id' ) // Verify the user owns this resource before deleting // Mutate data // Revalidate cache } Server Components Server Functions can be inlined in Server Components by adding the "use server" directive to the top of the function body: app/page.tsx TypeScript JavaScript TypeScript export default function Page () { // Server Action async function createPost (formData : FormData ) { 'use server' // ... } return <></> } Good to know: Server Components support progressive enhancement by default, meaning forms that call Server Actions will be submitted even if JavaScript hasn't loaded yet or is disabled. Client Components It's not possible to define Server Functions in Client Components. However, you can invoke them in Client Components by importing them from a file that has the "use server" directive at the top of it: app/actions.ts TypeScript JavaScript TypeScript 'use server' export async function createPost () {} app/ui/button.tsx TypeScript JavaScript TypeScript 'use client' import { createPost } from '@/app/actions' export function Button () { return < button formAction = {createPost}>Create</ button > } Good to know: In Client Components, forms invoking Server Actions will queue submissions if JavaScript isn't loaded yet, and will be prioritized for hydration. After hydration, the browser does not refresh on form submission. Passing actions as props You can also pass an action to a Client Component as a prop: < ClientComponent updateItemAction = {updateItem} /> app/client-component.tsx TypeScript JavaScript TypeScript 'use client' export default function ClientComponent ({ updateItemAction , } : { updateItemAction : (formData : FormData ) => void }) { return < form action = {updateItemAction}>{ /* ... */ }</ form > } Invoking Server Functions There are two main ways you can invoke a Server Function: Forms in Server and Client Components Event Handlers and useEffect in Client Components Good to know: Server Functions are designed for server-side mutations. The client currently dispatches and awaits them one at a time. This is an implementation detail and may change. If you need parallel data fetching, use data fetching in Server Components, or perform parallel work inside a single Server Function or Route Handler . Forms React extends the HTML <form> element to allow a Server Function to be invoked with the HTML action prop. When invoked in a form, the function automatically receives the FormData object. You can extract the data using the native FormData methods : app/ui/form.tsx TypeScript JavaScript TypeScript import { createPost } from '@/app/actions' export function Form () { return ( < form action = {createPost}> < input type = "text" name = "title" /> < input type = "text" name = "content" /> < button type = "submit" >Create</ button > </ form > ) } app/actions.ts TypeScript JavaScript TypeScript 'use server' import { auth } from '@/lib/auth' export async function createPost (formData : FormData ) { const session = await auth () if ( ! session ?.user) { throw new Error ( 'Unauthorized' ) } const title = formData .get ( 'title' ) const content = formData .get ( 'content' ) // Mutate data // Revalidate cache } Event Handlers You can invoke a Server Function in a Client Component by using event handlers such as onClick . app/like-button.tsx TypeScript JavaScript TypeScript 'use client' import { incrementLike } from './actions' import { useState } from 'react' export default function LikeButton ({ initialLikes } : { initialLikes : number }) { const [ likes , setLikes ] = useState (initialLikes) return ( <> < p >Total Likes: {likes}</ p > < button onClick = { async () => { const updatedLikes = await incrementLike () setLikes (updatedLikes) }} > Like </ button > </> ) } Examples Showing a pending state While executing a Server Function, you can show a loading indicator with React's useActionState hook. This hook returns a pending boolean: app/ui/button.tsx TypeScript JavaScript TypeScript 'use client' import { useActionState , startTransition } from 'react' import { createPost } from '@/app/actions' import { LoadingSpinner } from '@/app/ui/loading-spinner' export function Button () { const [ state , action , pending ] = useActionState (createPost , false ) return ( < button onClick = {() => startTransition (action)}> {pending ? < LoadingSpinner /> : 'Create Post' } </ button > ) } Refresh data After a mutation, you may want to refresh the current page to show the latest data. You can do this by calling refresh from next/cache in a Server Action: app/lib/actions.ts TypeScript JavaScript TypeScript 'use server' import { auth } from '@/lib/auth' import { refresh } from 'next/cache' export async function updatePost (formData : FormData ) { const session = await auth () if ( ! session ?.user) { throw new Error ( 'Unauthorized' ) } // Mutate data // ... refresh () } This refreshes the client router, ensuring the UI reflects the latest state. The refresh() function does not revalidate tagged data. To revalidate tagged data, use updateTag or revalidateTag instead. Revalidate data After performing a mutation, you can revalidate the Next.js cache and show the updated data by calling revalidatePath or revalidateTag within the Server Function: app/lib/actions.ts TypeScript JavaScript TypeScript import { auth } from '@/lib/auth' import { revalidatePath } from 'next/cache' export async function createPost (formData : FormData ) { 'use server' const session = await auth () if ( ! session ?.user) { throw new Error ( 'Unauthorized' ) } // Mutate data // ... revalidatePath ( '/posts' ) } Redirect after a mutation You may want to redirect the user to a different page after a mutation. You can do this by calling redirect within the Server Function. app/lib/actions.ts TypeScript JavaScript TypeScript 'use server' import { auth } from '@/lib/auth' import { revalidatePath } from 'next/cache' import { redirect } from 'next/navigation' export async function createPost (formData : FormData ) { const session = await auth () if ( ! session ?.user) { throw new Error ( 'Unauthorized' ) } // Mutate data // ... revalidatePath ( '/posts' ) redirect ( '/posts' ) } Calling redirect throws a framework handled control-flow exception. Any code after it won't execute. If you need fresh data, call revalidatePath or revalidateTag beforehand. Cookies You can get , set , and delete cookies inside a Server Action using the cookies API. When you set or delete a cookie in a Server Action, Next.js re-renders the current page and its layouts on the server so the UI reflects the new cookie value . Good to know : The server update applies to the current React tree, re-rendering, mounting, or unmounting components, as needed. Client state is preserved for re-rendered components, and effects re-run if their dependencies changed. app/actions.ts TypeScript JavaScript TypeScript 'use server' import { cookies } from 'next/headers' export async function exampleAction () { const cookieStore = await cookies () // Get cookie cookieStore .get ( 'name' )?.value // Set cookie cookieStore .set ( 'name' , 'Delba' ) // Delete cookie cookieStore .delete ( 'name' ) } useEffect You can use the React useEffect hook to invoke a Server Action when the component mounts or a dependency changes. This is useful for mutations that depend on global events or need to be triggered automatically. For example, onKeyDown for app shortcuts, an intersection observer hook for infinite scrolling, or when the component mounts to update a view count: app/view-count.tsx TypeScript JavaScript TypeScript 'use client' import { incrementViews } from './actions' import { useState , useEffect , useTransition } from 'react' export default function ViewCount ({ initialViews } : { initialViews : number }) { const [ views , setViews ] = useState (initialViews) const [ isPending , startTransition ] = useTransition () useEffect (() => { startTransition ( async () => { const updatedViews = await incrementViews () setViews (updatedViews) }) } , []) // You can use `isPending` to give users feedback return < p >Total Views: {views}</ p > } Next steps Learn more about Server Actions and the APIs mentioned in this page. Server Actions How Server Actions work in Next.js, including the single-roundtrip response model, sequential dispatch, security, and caching integration. revalidatePath API Reference for the revalidatePath function. revalidateTag API Reference for the revalidateTag function. redirect API Reference for the redirect function. Previous Fetching Data Next Caching Was this helpful? supported. Send Resources Docs Support Policy Learn Showcase Blog Team Analytics Next.js Conf Previews Evals More Next.js Commerce Contact Sales Community GitHub Releases Telemetry Governance Ecosystem Working Group About Vercel Next.js + Vercel Open Source Software GitHub Bluesky X Legal Privacy Policy Cookie Preferences Subscribe to our newsletter Stay updated on new releases and features, guides, and case studies. Subscribe © 2026 Vercel, Inc.
