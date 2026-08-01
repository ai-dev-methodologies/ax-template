# nextjs-fetching-data — upstream snapshot (2026-08-01 refresh, append-only)

**Source URL(s):** https://nextjs.org/docs/app/getting-started/fetching-data (re-fetched 2026-08-01; every pre-existing section below the divider is preserved verbatim)
**HTTP status:** 200
**Fetched at:** 2026-08-01T01:46:53Z
**Extractor invocation:** `practices/scripts/snapshot-extract.sh https://nextjs.org/docs/app/getting-started/fetching-data`
**Fetch receipt:** `practices/upstream/_FETCH-RECEIPTS.yaml` id `r089`
**Body SHA-256 (below the `---` divider, header excluded):** 12029e190b44c829a6279a7f63257f7a7c3b14f4b86634e91abfbe07b7037065

---

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

---

## Upstream refresh 2026-08-01 (verbatim extractor output)

Source: https://nextjs.org/docs/app/getting-started/fetching-data
HTTP status: 200 · extracted bytes: 19199 · sha256: 4260475ef2475b280328a1dbbcd4ac1296c1688c65571ca82e1e5f996ecfcb53
Extractor: `practices/scripts/snapshot-extract.sh` (curl -> deterministic HTML->text; no model in the loop)
Fetch receipt: `practices/upstream/_FETCH-RECEIPTS.yaml` id `r089`

Everything above this divider is the previous snapshot, preserved byte-for-byte (append-only:
history is recorded, never rewritten). The block below is the UNMODIFIED extractor output for
the 2026-08-01 re-fetch of the same URL — it is the current upstream text, and any citation that
claims to quote this source verbatim must match it.

Getting Started: Fetching Data | Next.js Skip to content Search documentation... ⌘K Search... ⌘K Showcase Docs Blog Templates Enterprise Search documentation... ⌘K Search... ⌘K Feedback Learn Menu Using App Router Features available in /app Latest Version 16.2.12 Getting Started Installation Project Structure Layouts and Pages Linking and Navigating Server and Client Components Fetching Data Mutating Data Caching Revalidating Error Handling CSS Image Optimization Font Optimization Metadata and OG images Route Handlers Proxy Deploying Upgrading Guides AI Coding Agents Analytics Authentication Backend for Frontend Caching (Previous Model) CDN Caching CI Build Caching Content Security Policy CSS-in-JS Custom Server Data Security Debugging Deploying to Platforms Draft Mode Environment Variables Forms How Revalidation Works ISR Instrumentation Internationalization JSON-LD Lazy Loading Development Environment Next.js MCP Server MDX Memory Usage Migrating App Router Create React App Vite Migrating to Cache Components Multi-tenant Multi-zones OpenTelemetry Package Bundling PPR Platform Guide Prefetching Preserving UI state Preventing Flash Production PWAs Public pages Redirecting Rendering Philosophy Sass Scripts Self-Hosting Server Actions SPAs Static Exports Streaming Tailwind CSS v3 Testing Cypress Jest Playwright Vitest Third Party Libraries Upgrading Codemods Version 14 Version 15 Version 16 Videos View transitions API Reference Directives use cache use cache: private use cache: remote use client use server Components Font Form Component Image Component Link Component Script Component File-system conventions default.js Dynamic Segments error.js forbidden.js instrumentation.js instrumentation-client.js Intercepting Routes layout.js loading.js mdx-components.js not-found.js page.js Parallel Routes proxy.js public route.js Route Groups src template.js unauthorized.js Metadata Files favicon, icon, and apple-icon manifest.json opengraph-image and twitter-image robots.txt sitemap.xml Route Segment Config dynamicParams maxDuration preferredRegion runtime Functions after cacheLife cacheTag unstable_catchError connection cookies draftMode fetch forbidden generateImageMetadata generateMetadata generateSitemaps generateStaticParams generateViewport headers ImageResponse NextRequest NextResponse notFound permanentRedirect redirect refresh revalidatePath revalidateTag unauthorized unstable_cache unstable_noStore unstable_rethrow updateTag useLinkStatus useParams usePathname useReportWebVitals useRouter useSearchParams useSelectedLayoutSegment useSelectedLayoutSegments userAgent Configuration next.config.js adapterPath allowedDevOrigins appDir assetPrefix authInterrupts basePath cacheComponents cacheHandlers cacheLife compress crossOrigin cssChunking deploymentId devIndicators distDir env expireTime exportPathMap generateBuildId generateEtags headers htmlLimitedBots httpAgentOptions images cacheHandler inlineCss logging mdxRs onDemandEntries optimizePackageImports output pageExtensions poweredByHeader productionBrowserSourceMaps proxyClientMaxBodySize reactCompiler reactMaxHeadersLength reactStrictMode redirects rewrites sassOptions serverActions serverComponentsHmrCache serverExternalPackages staleTimes staticGeneration* taint trailingSlash transpilePackages turbopack turbopackFileSystemCache turbopack.ignoreIssue turbopackLocalPostcssConfig typedRoutes typescript urlImports useLightningcss useTypeScriptCli viewTransition webpack webVitalsAttribution TypeScript ESLint CLI create-next-app next CLI Adapters Configuration Creating an Adapter API Reference Testing Adapters Routing with @next/routing Implementing PPR in an Adapter Runtime Integration Invoking Entrypoints Output Types Routing Information Use Cases Edge Runtime Turbopack Glossary Architecture Accessibility Fast Refresh Next.js Compiler Supported Browsers Community Contribution Guide Rspack On this page Fetching data Server Components With the fetch API With an ORM or database Streaming With loading.js With <Suspense> Creating meaningful loading states Client Components Streaming data with the use API Community libraries Examples Sequential data fetching Parallel data fetching Sharing data with context and React.cache API Reference Edit this page on GitHub Scroll to top This page is also available as Markdown at /docs/app/getting-started/fetching-data.md . For an index of Next.js documentation , see /docs/llms.txt . App Router Getting Started Fetching Data Copy page Fetching Data Last updated July 22, 2026 This page will walk you through how you can fetch data in Server and Client Components, and how to stream components that depend on uncached data. Fetching data Server Components You can fetch data in Server Components using any asynchronous I/O, such as: The fetch API An ORM or database With the fetch API To fetch data with the fetch API, turn your component into an asynchronous function, and await the fetch call. For example: app/blog/page.tsx TypeScript JavaScript TypeScript export default async function Page () { const data = await fetch ( 'https://api.vercel.app/blog' ) const posts = await data .json () return ( < ul > { posts .map ((post) => ( < li key = { post .id}>{ post .title}</ li > ))} </ ul > ) } Good to know: Identical fetch requests in a React component tree are memoized by default, so you can fetch data in the component that needs it instead of drilling props. fetch requests are not cached by default and will block the page from rendering until the request is complete. Use the use cache directive to cache results, or wrap the fetching component in <Suspense> to stream fresh data at request time. See caching for details. During development, you can log fetch calls for better visibility and debugging. See the logging API reference . With an ORM or database Since Server Components are rendered on the server, credentials and query logic will not be included in the client bundle so you can safely make database queries using an ORM or database client. app/blog/page.tsx TypeScript JavaScript TypeScript import { db , posts } from '@/lib/db' export default async function Page () { const allPosts = await db .select () .from (posts) return ( < ul > { allPosts .map ((post) => ( < li key = { post .id}>{ post .title}</ li > ))} </ ul > ) } You should still ensure requests are properly authenticated and authorized. For best practices on securing server-side data access, see the data security guide . Streaming When you fetch data in Server Components, the data is fetched and rendered on the server for each request. If you have any slow data requests, the whole route will be blocked from rendering until all the data is fetched. To improve the initial load time and user experience, you can break the page into smaller chunks and progressively send those chunks from the server to the client. This is called streaming. See the Streaming guide for a deeper look at how streaming works, including the HTTP contract, infrastructure considerations, and performance trade-offs. There are two ways you can use streaming in your application: Wrapping a page with a loading.js file Wrapping a component with <Suspense> With loading.js You can create a loading.js file in the same folder as your page to stream the entire page while the data is being fetched. For example, to stream app/blog/page.js , add the file inside the app/blog folder. app/blog/loading.tsx TypeScript JavaScript TypeScript export default function Loading () { // Define the Loading UI here return < div >Loading...</ div > } On navigation, the user will immediately see the layout and a loading state while the page is being rendered. The new content will then be automatically swapped in once rendering is complete. Behind the scenes, loading.js will be nested inside layout.js , and will automatically wrap the page.js file and any children below in a <Suspense> boundary. Because of this, a layout that accesses uncached or runtime data (e.g. cookies() , headers() , or uncached fetches) does not fall back to a same route segment loading.js . Instead, it blocks navigation until the layout finishes rendering. Cache Components prevents this by guiding you with a build-time error. To fix this, wrap the uncached access in its own <Suspense> boundary with a fallback, or move the data fetching into page.js where loading.js can cover it. See loading.js for more details. This is why, while loading.js works well for streaming route segments, using <Suspense> closer to the runtime or uncached data access is recommended. With <Suspense> <Suspense> allows you to be more granular about what parts of the page to stream. For example, you can immediately show any page content that falls outside of the <Suspense> boundary, and stream in the list of blog posts inside the boundary. app/blog/page.tsx TypeScript JavaScript TypeScript import { Suspense } from 'react' import BlogList from '@/components/BlogList' import BlogListSkeleton from '@/components/BlogListSkeleton' export default function BlogPage () { return ( < div > { /* This content will be sent to the client immediately */ } < header > < h1 >Welcome to the Blog</ h1 > < p >Read the latest posts below.</ p > </ header > < main > { /* If there's any dynamic content inside this boundary, it will be streamed in */ } < Suspense fallback = {< BlogListSkeleton />}> < BlogList /> </ Suspense > </ main > </ div > ) } Creating meaningful loading states An instant loading state is fallback UI that is shown immediately to the user after navigation. For the best user experience, we recommend designing loading states that are meaningful and help users understand the app is responding. For example, you can use skeletons and spinners, or a small but meaningful part of future screens such as a cover photo, title, etc. In development, you can preview and inspect the loading state of your components using the React Devtools . Client Components There are two ways to fetch data in Client Components, using: React's use API A community library like SWR or React Query Streaming data with the use API You can use React's use API to stream data from the server to client. Start by fetching data in your Server component, and pass the promise to your Client Component as prop: app/blog/page.tsx TypeScript JavaScript TypeScript import Posts from '@/app/ui/posts' import { Suspense } from 'react' export default function Page () { // Don't await the data fetching function const posts = getPosts () return ( < Suspense fallback = {< div >Loading...</ div >}> < Posts posts = {posts} /> </ Suspense > ) } Then, in your Client Component, use the use API to read the promise: app/ui/posts.tsx TypeScript JavaScript TypeScript 'use client' import { use } from 'react' export default function Posts ({ posts , } : { posts : Promise <{ id : string ; title : string }[]> }) { const allPosts = use (posts) return ( < ul > { allPosts .map ((post) => ( < li key = { post .id}>{ post .title}</ li > ))} </ ul > ) } In the example above, the <Posts> component is wrapped in a <Suspense> boundary . This means the fallback will be shown while the promise is being resolved. Learn more about streaming . Community libraries You can use a community library like SWR or React Query to fetch data in Client Components. These libraries have their own semantics for caching, streaming, and other features. For example, with SWR: app/blog/page.tsx TypeScript JavaScript TypeScript 'use client' import useSWR from 'swr' const fetcher = (url) => fetch (url) .then ((r) => r .json ()) export default function BlogPage () { const { data , error , isLoading } = useSWR ( 'https://api.vercel.app/blog' , fetcher ) if (isLoading) return < div >Loading...</ div > if (error) return < div >Error: { error .message}</ div > return ( < ul > { data .map ((post : { id : string ; title : string }) => ( < li key = { post .id}>{ post .title}</ li > ))} </ ul > ) } Examples Sequential data fetching Sequential data fetching happens when one request depends on data from another. For example, <Playlists> can only fetch data after getArtist() resolves because it needs the artistID : app/artist/[username]/page.tsx TypeScript JavaScript TypeScript export default async function Page ({ params , } : { params : Promise <{ username : string }> }) { const { username } = await params // Get artist information const artist = await getArtist (username) return ( <> < h1 >{ artist .name}</ h1 > { /* Show fallback UI while the Playlists component is loading */ } < Suspense fallback = {< div >Loading...</ div >}> { /* Pass the artist ID to the Playlists component */ } < Playlists artistID = { artist .id} /> </ Suspense > </> ) } async function Playlists ({ artistID } : { artistID : string }) { // Use the artist ID to fetch playlists const playlists = await getArtistPlaylists (artistID) return ( < ul > { playlists .map ((playlist) => ( < li key = { playlist .id}>{ playlist .name}</ li > ))} </ ul > ) } In this example, <Suspense> allows the playlists to stream in after the artist data loads. However, the page still waits for the artist data before displaying anything. To prevent this, you can wrap the entire page component in a <Suspense> boundary (for example, using a loading.js file ) to show a loading state immediately. Ensure your data source can resolve the first request quickly, as it blocks everything else. If you can't optimize the request further, consider caching the result if the data changes infrequently. Parallel data fetching Parallel data fetching happens when data requests in a route are eagerly initiated and start at the same time. By default, layouts and pages are rendered in parallel. So each segment starts fetching data as soon as possible. However, within any component, multiple async / await requests can still be sequential if placed after the other. For example, getAlbums will be blocked until getArtist is resolved: app/artist/[username]/page.tsx TypeScript JavaScript TypeScript import { getArtist , getAlbums } from '@/app/lib/data' export default async function Page ({ params }) { // These requests will be sequential const { username } = await params const artist = await getArtist (username) const albums = await getAlbums (username) return < div >{ artist .name}</ div > } Start multiple requests by calling fetch , then await them with Promise.all . Requests begin as soon as fetch is called. app/artist/[username]/page.tsx TypeScript JavaScript TypeScript import Albums from './albums' async function getArtist (username : string ) { const res = await fetch ( `https://api.example.com/artist/ ${ username } ` ) return res .json () } async function getAlbums (username : string ) { const res = await fetch ( `https://api.example.com/artist/ ${ username } /albums` ) return res .json () } export default async function Page ({ params , } : { params : Promise <{ username : string }> }) { const { username } = await params // Initiate requests const artistData = getArtist (username) const albumsData = getAlbums (username) const [ artist , albums ] = await Promise .all ([artistData , albumsData]) return ( <> < h1 >{ artist .name}</ h1 > < Albums list = {albums} /> </> ) } Good to know: If one request fails when using Promise.all , the entire operation will fail. To handle this, you can use the Promise.allSettled method instead. Sharing data with context and React.cache You can share fetched data across both Server and Client Components by combining React.cache with context providers. Create a cached function that fetches data: app/lib/user.ts TypeScript JavaScript TypeScript import { cache } from 'react' export const getUser = cache ( async () => { const res = await fetch ( 'https://api.example.com/user' ) return res .json () }) Create a context provider that stores the promise: app/user-provider.tsx TypeScript JavaScript TypeScript 'use client' import { createContext } from 'react' type User = { id : string name : string } export const UserContext = createContext < Promise < User > | null >( null ) export default function UserProvider ({ children , userPromise , } : { children : React . ReactNode userPromise : Promise < User > }) { return < UserContext value = {userPromise}>{children}</ UserContext > } In a layout, pass the promise to the provider without awaiting: app/layout.tsx TypeScript JavaScript TypeScript import UserProvider from './user-provider' import { getUser } from './lib/user' export default function RootLayout ({ children , } : { children : React . ReactNode }) { const userPromise = getUser () // Don't await return ( < html > < body > < UserProvider userPromise = {userPromise}>{children}</ UserProvider > </ body > </ html > ) } Client Components use use() to resolve the promise from context, wrapped in <Suspense> for fallback UI: app/ui/profile.tsx TypeScript JavaScript TypeScript 'use client' import { use , useContext } from 'react' import { UserContext } from '../user-provider' export function Profile () { const userPromise = useContext (UserContext) if ( ! userPromise) { throw new Error ( 'useContext must be used within a UserProvider' ) } const user = use (userPromise) return < p >Welcome, { user .name}</ p > } app/page.tsx TypeScript JavaScript TypeScript import { Suspense } from 'react' import { Profile } from './ui/profile' export default function Page () { return ( < Suspense fallback = {< div >Loading profile...</ div >}> < Profile /> </ Suspense > ) } Server Components can also call getUser() directly: app/dashboard/page.tsx TypeScript JavaScript TypeScript import { getUser } from '../lib/user' export default async function DashboardPage () { const user = await getUser () // Cached - same request, no duplicate fetch return < h1 >Dashboard for { user .name}</ h1 > } Since getUser is wrapped with React.cache , multiple calls within the same request return the same memoized result, whether called directly in Server Components or resolved via context in Client Components. Good to know : React.cache is scoped to the current request only. Each request gets its own memoization scope with no sharing between requests. API Reference Learn more about the features mentioned in this page by reading the API Reference. Data Security Learn the built-in data security features in Next.js and learn best practices for protecting your application's data. fetch API reference for the extended fetch function. loading.js API reference for the loading.js file. logging Configure logging behavior in the terminal when running Next.js in development mode, including fetch logging, incoming requests, and forwarding browser console logs to the terminal. taint Enable tainting Objects and Values. Previous Server and Client Components Next Mutating Data Was this helpful? supported. Send Resources Docs Support Policy Learn Showcase Blog Team Analytics Next.js Conf Previews Evals More Next.js Commerce Contact Sales Community GitHub Releases Telemetry Governance Ecosystem Working Group About Vercel Next.js + Vercel Open Source Software GitHub Bluesky X Legal Privacy Policy Cookie Preferences Subscribe to our newsletter Stay updated on new releases and features, guides, and case studies. Subscribe © 2026 Vercel, Inc.
